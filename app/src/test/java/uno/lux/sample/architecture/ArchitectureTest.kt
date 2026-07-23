package uno.lux.sample.architecture

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.ext.list.withPackage
import com.lemonappdev.konsist.api.verify.assertTrue
import org.junit.Test

/**
 * The package rules from AGENTS.md, asserted against the real import graph.
 *
 * Kotlin has no package-private and `internal` is module-scoped, so a single-module project gets
 * no compiler enforcement of its own layering — a package layout is a convention until something
 * checks it. These are that something. Both violations found by hand during the slice refactor
 * (a KDoc link that needed an import, and a theme helper typed on `UserId`) would have failed
 * here the moment they were written.
 */
class ArchitectureTest {

    private companion object {
        const val ROOT = "uno.lux.sample"

        /** Slices that own an entity. Everything about a post lives under `post`. */
        val AGGREGATES = listOf("post", "user", "comment", "album", "video")

        /** Slices that own no entity — read models over the aggregates, and the shell features. */
        val FEATURES = listOf("feed", "profile", "composer", "settings")

        /** Shared foundation: knows no domain noun and would survive deleting the product. */
        val FOUNDATION = listOf("common", "core", "theme", "util", "format")
    }

    private fun production() = Konsist.scopeFromProduction()

    @Test
    fun `the foundation never depends on a slice`() {
        production()
            .files
            .withPackage(*FOUNDATION.map { "$ROOT.app.$it.." }.toTypedArray())
            .assertTrue(additionalMessage = FOUNDATION_MESSAGE) { file ->
                file.imports.none { import ->
                    (AGGREGATES + FEATURES).any { import.name.startsWith("$ROOT.$it.") }
                }
            }
    }

    @Test
    fun `aggregates never depend on features`() {
        production()
            .files
            .withPackage(*AGGREGATES.map { "$ROOT.$it.." }.toTypedArray())
            .assertTrue(additionalMessage = AGGREGATE_MESSAGE) { file ->
                file.imports.none { import ->
                    FEATURES.any { import.name.startsWith("$ROOT.$it.") }
                }
            }
    }

    @Test
    fun `HTTP stays in the network layer`() {
        production()
            .files
            .assertTrue(additionalMessage = HTTP_MESSAGE) { file ->
                val pkg = file.packagee?.name.orEmpty()
                val isNetworkLayer = pkg.contains(".data.network") ||
                    pkg.startsWith("$ROOT.app.core.network") ||
                    pkg.startsWith("$ROOT.app.di")

                isNetworkLayer || file.imports.none {
                    it.name.startsWith("retrofit2.") || it.name.startsWith("okhttp3.")
                }
            }
    }

    @Test
    fun `repositories stay plain-JVM testable`() {
        production()
            .files
            // `settings` is the documented exception: DataStoreSettingsRepository persists through
            // DataStore and AppCompatLocaleRepository *is* the wrapper around AppCompat's delegate.
            // Both have in-memory doubles, which is what keeps their consumers plain-JVM.
            .withPackage(*(AGGREGATES + listOf("feed", "profile")).map { "$ROOT.$it.." }.toTypedArray())
            .assertTrue(additionalMessage = REPOSITORY_MESSAGE) { file ->
                file.classes().none { it.name.endsWith("Repository") } ||
                    file.imports.none {
                        it.name.startsWith("android.") || it.name.startsWith("androidx.")
                    }
            }
    }
}

private const val FOUNDATION_MESSAGE =
    "A package under app/ that the whole product sits on has reached up into a slice. Either the " +
        "helper belongs in that slice, or its parameter should be the plain type it really needs " +
        "(MosaicGradients.avatarBrush took a UserId when all it did was hash a String)."

private const val AGGREGATE_MESSAGE =
    "An aggregate imported a feature. Features may depend on aggregates, never the other way — " +
        "that direction is what lets a like toggled in the feed show up on a profile without the " +
        "post store knowing either screen exists. Note a KDoc [Link] needs an import too: write " +
        "cross-slice doc references fully qualified instead."

private const val HTTP_MESSAGE =
    "Retrofit or OkHttp appeared outside a data/network package. Wire types and HTTP calls belong " +
        "behind a DataSource interface, so a repository can be tested with a fake and swapped to a " +
        "local source without touching its callers."

private const val REPOSITORY_MESSAGE =
    "A repository imported the Android framework, which makes it unreachable from a JVM unit test. " +
        "Keep the platform behind an interface the way settings does, and inject it."
