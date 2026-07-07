# AGENTS.md

This file provides guidance to AI agents when working with code in this repository.

## Project purpose

A sample Android app (`uno.lux.sample`, displayed as "Sample") built as a resume/portfolio piece to demonstrate modern Android architecture. Code quality, idiomatic Compose, and architectural clarity are the point of the project, not just working features — prefer the current, recommended Android way of doing things over expedient shortcuts.

## Testing philosophy — tests first

Tests are the **primary consumer** of this codebase; real users come second. Production code exists first to satisfy a test — we write the code that makes tests pass, and only then does it also happen to serve users. In practice:

- **Write the test first.** Express the desired behaviour as a failing test, then write the code that makes it pass. A change isn't done until tests cover it and they're green; new logic lands with its test in the same change.
- **Design for the test.** Keep logic in plain-JVM, dependency-injected units so a test can drive it directly. The `data` layer and ViewModels take their collaborators as constructor parameters and avoid Android dependencies; pure functions take their inputs (e.g. `now`) as parameters instead of reading ambient state. This is *why* the architecture is shaped the way it is — testability drives the design, not the other way round.
- **Same rule for UI.** Stateless composables take data + callbacks so they can be exercised in isolation; ViewModels expose state as a `StateFlow` a test asserts against.

Run the unit suite with `.\gradlew.bat testDebugUnitTest` (single class: `--tests "uno.lux.sample.FormattingTest"`).

## Commands

The shell is Windows PowerShell; invoke the wrapper as `.\gradlew.bat`.

- Build debug APK: `.\gradlew.bat assembleDebug`
- Install on a running device/emulator: `.\gradlew.bat installDebug`
- Android Lint (the only linter configured): `.\gradlew.bat lintDebug` → report at `app/build/reports/lint-results-debug.html`
- All JVM unit tests: `.\gradlew.bat testDebugUnitTest`
- A single unit test class/method: `.\gradlew.bat testDebugUnitTest --tests "uno.lux.sample.ExampleUnitTest.addition_isCorrect"`
- Instrumented tests (needs a connected device/emulator): `.\gradlew.bat connectedDebugAndroidTest`
- A single instrumented test: `.\gradlew.bat connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=uno.lux.sample.ExampleInstrumentedTest`

## Toolchain / build setup

- **Kotlin 2.2.10**, **AGP 9.2.1**, **Gradle 9.4.1**, Compose BOM **2025.12.00**.
- `compileSdk` is **36.1**, expressed with the newer AGP DSL `release(36) { minorApiLevel = 1 }`; `minSdk = 26`, `targetSdk = 36`.
- Source/target compatibility is **Java 11**, but the Gradle daemon runs on **JDK 21** (`gradle/gradle-daemon-jvm.properties`, auto-provisioned via foojay).
- AGP 9's **built-in Kotlin** is in use — there is no standalone `org.jetbrains.kotlin.android` plugin. Annotation processing runs through **KSP** (kapt is incompatible with built-in Kotlin); Hilt's codegen rides on it.
- Dependencies are managed through the **version catalog** at `gradle/libs.versions.toml`. Add or bump dependencies there and reference them via the generated `libs.*` accessors in `app/build.gradle.kts` — do not hardcode versions in the build script.

## Architecture

Single-activity, 100% Jetpack Compose app — there are no Fragments and no XML view layouts (XML under `res/` is only resources: themes, colors, vector icons, backup rules). The entire UI tree is built in Kotlin.

- **Entry point** — `MainActivity` (`app/src/main/java/uno/lux/sample/MainActivity.kt`) is the only Activity and stays a thin shell. In `setContent` it collects the `ThemeMode` preference from `MainViewModel`, resolves the dark/light choice, applies it to `MosaicTheme`, and hosts `SampleApp()` (`ui/SampleApp.kt`).

- **Navigation — Navigation 3 with a tabbed root shell, driven by ViewModels.** `SampleApp()` owns a `rememberNavBackStack` of `@Serializable` `Screen` keys (`ui/navigation/Screen.kt`): `Screen.Shell` is the permanent root, and `Screen.Profile(userId)` (tapping a post's author) or `Screen.Settings` (the gear in any top bar) push over it as full-screen pages — settings stacks over a profile, too. `NavDisplay` renders the top entry with directional push/pop slide transitions, predictive back included; back handling is just `onBack` popping the stack — there are no hand-rolled `BackHandler`s. The entry decorators give every entry its own saveable state and **ViewModel store**, so each pushed page's ViewModel is created on push and cleared on pop (each opened profile gets its own `ProfileViewModel` without manual keying). The back stack survives configuration changes and process death because the keys are serializable.
  **Navigation intent flows through the ViewModels**, not host-provided lambdas: each screen's ViewModel injects the `Navigator` (`ui/navigation/Navigator.kt`, provided `@ActivityRetainedScoped` in `di/NavigationModule.kt` — the pattern from the official Nav3 Hilt recipe) and calls `goTo(screen)` / `goBack()`, so e.g. `EditProfileViewModel` pops the editor itself once a save lands. The composition keeps *owning* the stack — `SampleApp` attaches the `rememberNavBackStack` list to the `Navigator` in a `DisposableEffect` — which is what preserves the saveable/process-death guarantee the recipe's self-owned `SnapshotStateList` would lose. `Navigator.goTo` deliberately allows pushing a screen equal to the current top (double-push protection lives in the 500 ms click debounce every nav control already has). ViewModel-less leaf screens (`AlbumViewerScreen`, `FullscreenVideoScreen`, `PlaceholderScreen`) still take a lambda, wired to the same `Navigator` in `SampleApp`. Unit tests drive the real `Navigator` by attaching a plain `mutableListOf<NavKey>(…)` and asserting on the list.

- **Adaptive navigation inside the shell.** The `Shell` entry hosts `HomeNavShell`: Material 3's `NavigationSuiteScaffold` (via the project's `DividedNavigationSuiteScaffold`) adapts the navigation UI to the window size — bottom nav bar on phones, navigation rail / drawer on larger or unfolded screens — without per-form-factor code. Destinations are data-driven: the `AppDestinations` enum (a `@StringRes` label + icon drawable per entry: HOME, FAVORITES, PROFILE) is the single source of truth, and the nav items are generated by iterating `AppDestinations.entries` — add or change a tab by editing the enum. **Tab selection is deliberately plain `rememberSaveable` state, not back-stack entries**: switching tabs is not a navigation event, so system back never walks through tabs. HOME → `HomeScreen` (the feed); the PROFILE **tab** → the signed-in user's own `ProfileScreen` (with no up-affordance — `showBackButton` stays false — since it's a root tab, not a pushed page); FAVORITES → a `PlaceholderScreen` for now. Each screen owns its own `Scaffold`/`TopAppBar` rather than sharing one, so screens control their own chrome; tab switches cross-fade (fade-through) while page pushes slide.

- **Feature architecture — the HOME feed sets the pattern to follow.** Code is layered by responsibility:
  - `data/` — domain models (`Post`, `User`) and repository interfaces, the seam real implementations slot into. `PostRepository` wraps a `PostDataSource` and holds a `StateFlow<List<Post>>` cache so like/bookmark mutations are instantly reflected across screens without a re-fetch. `SettingsRepository` has an `InMemorySettingsRepository` (the test double) and the production `DataStoreSettingsRepository`, which persists the theme choice in a Preferences **DataStore**; the store is a constructor dependency, so its unit tests drive a *real* DataStore over a temp file on the JVM. The DataStore itself is provided in `DataModule`; the persisted key `theme_mode` is the contract with the legacy SharedPreferences file it replaced — tests pin it to ensure continuity. The profile layer adds `Profile` (a `User` with their `posts`/`albums`/`videos`), the `Album`/`Video` stand-ins, and `ProfileRepository` — which **composes** a `PostRepository` (a profile's posts are simply the feed posts authored by that user) rather than duplicating mutation logic. Domain models, the interfaces, and every repository implementation carry no Android dependencies, so the whole layer is plain-JVM testable.
  - `ui/home/` — MVVM with unidirectional data flow. `HomeViewModel` exposes `StateFlow<HomeUiState>` (a sealed interface: `Loading` / `Feed`) and converts intent into repository mutations via `viewModelScope` (`onToggleLike`, `onToggleBookmark`) or `Navigator` pushes (`openPost`, `openProfile`, `openSettings`, the media viewers) — both kinds live on the one `HomeActions` seam the stateless screen depends on. `HomeScreen` is split into a **stateful** binder (collects state, injects the ViewModel) and an **internal stateless** composable (pure inputs + callbacks) so it previews and tests without a ViewModel. `PostCard` and its sub-composables are likewise stateless with hoisted callbacks; the header (avatar / name / handle) is one tappable target that opens the author's profile.
  - `ui/profile/` — the same MVVM split, parameterized by user. `ProfileViewModel` is bound to a `userId` through Hilt assisted injection (a ViewModel can't take runtime args directly), is also given the `@CurrentUserId` signed-in user, and exposes `StateFlow<ProfileUiState>` (`Loading` / `Loaded(profile, isCurrentUser)` / `NotFound`) — `isCurrentUser` is the viewed `userId` matching the signed-in one. `ProfileScreen` is a stateful binder + internal stateless composable: a full-bleed gradient cover with an overlapping surface-ringed avatar, an identity block (age/gender/location chips, bio, follower/following stats, *Edit profile* — shown only on your own profile — + settings), then **sticky** Material tabs — Posts (reusing `PostCard`), Albums and Videos rendered as 2-column grids built from `chunked(2)` rows inside the single `LazyColumn`. Cover/album/video imagery is deterministic gradient stand-ins keyed by id, mirroring `Avatar`.
  - `util/` — pure, unit-tested formatters (`relativeTime`, `compactCount`, `formatVideoDuration`); reusable UI like `Avatar` lives in `ui/components/`.

- **Dependency injection — Hilt.** `MosaicApp` is the `@HiltAndroidApp` root and `MainActivity` an `@AndroidEntryPoint`; both stay empty of wiring. All production bindings live in `di/DataModule.kt`, `di/NetworkModule.kt` and `di/NavigationModule.kt`. Repositories are `@Singleton` so the `StateFlow` cache in `PostRepository` is shared across the feed and profile — a like toggled in one screen is immediately visible in the other (`ProfileRepository` explicitly composes the bound `PostRepository` so both screens see the same instance). ViewModels are `@HiltViewModel` with `@Inject` constructors, bound in screens via `hiltViewModel()` (from `androidx.hilt:hilt-lifecycle-viewmodel-compose` — the `hilt-navigation-compose` copy is deprecated). `ProfileViewModel` takes its runtime `userId` through **assisted injection** (`@HiltViewModel(assistedFactory = …)` + `hiltViewModel(key, creationCallback)`). Tests never touch Hilt: they construct ViewModels directly with fakes, which is why ViewModels keep plain constructor parameters. The repositories themselves carry no DI annotations (constructed in the module, not `@Inject`), keeping the data layer framework-free.

- **Theming** lives in `app/src/main/java/uno/lux/sample/ui/theme/` and implements the **Mosaic** design system (a claude.ai/design handoff). `MosaicTheme` applies a fixed brand palette — indigo accent, warm-neutral surfaces, coral like-state, with full light/dark token sets in `Color.kt`. There is **no dynamic color** (the brand must stay consistent), and `surfaceTint` is transparent so cards keep their exact colour while still casting the soft shadow. `Type.kt` wires two **bundled variable fonts** (`res/font/`, OFL in `licenses/`): **Bricolage Grotesque** for the brand wordmark and post titles, **Manrope** for UI and body. Tokens with no Material role — the muted `textTertiary` and the coral `like` — are carried as `MosaicColors` via `LocalMosaicColors`. Wrap any new top-level Compose content (and `@Preview`s) in `MosaicTheme`. The light/dark choice is **user-controlled**: `MainActivity` collects the `ThemeMode` (Light/Dark/System) from `MainViewModel`, resolves it with `ThemeMode.isDark(isSystemInDarkTheme())`, and passes the result to `MosaicTheme(darkTheme = …)`; the settings screen changes it through `SettingsViewModel`. Edge-to-edge system-bar styling is re-applied in a `DisposableEffect` keyed on the resolved theme so the bar icons keep contrast.

## Compose performance — keeping recomposition tight

Holding recomposition to what actually changed is part of "idiomatic Compose" here, but it leans on the compiler rather than hand-tuning. The built-in Compose compiler (Kotlin 2.2.10) runs with **strong skipping on**, which shapes what's worth doing:

- **Don't reflexively wrap lambdas in `remember` or chase `@Stable`/`@Immutable` on every model.** Strong skipping auto-remembers lambdas (even ones capturing unstable values) and lets composables with *unstable* parameters still skip via instance equality. The domain models are technically unstable — `Post` carries a `java.time.Instant` and `List<…>` is an unstable type — yet feed and profile rows skip fine because each `LazyColumn` item is compared by instance.
- **Skipping is by instance, so a mutation must preserve identity for everything that didn't change.** `InMemoryPostRepository.updatePost` rebuilds the list but returns the *same* `Post` instance for every row except the toggled one (`posts.map { if (it.id == postId) transform(it) else it }`), so a like/bookmark recomposes only that one `PostCard`. Mapping `copy()` over the whole list (or otherwise handing back fresh instances for unchanged rows) would recompose every visible row — avoid it.
- **Read snapshot-backed state in the smallest composable that uses it.** Scroll / pager / animation state read at a screen's top level invalidates that whole scope on every change. The album viewer keeps its page counter in a separate `PageIndicator` composable so swiping recomposes only the pill, not the pager or back button; prefer pushing such a read down into a child over hoisting it up.
- **Coalesce continuous signals with `derivedStateOf`.** When a boolean or bucketed value is derived from something that changes every frame (scroll offset, `overlappedFraction`), wrap it so readers recompose only when the *result* flips — see `ProfileTopBar`'s `scrolled` and the sticky-tab `tabInset`. The flip side: a signal that is *already* coalesced needs no wrapper, so `HomeScreen` reads `listState.canScrollBackward` directly (it only changes when crossing the very top).
- **Don't key a long-lived effect on state you only read inside it.** Keying a `LaunchedEffect` on a frequently-replaced value restarts it on every change. The feed's inline-autoplay collector reads the current posts through `rememberUpdatedState` and keys only on the stable `listState`/playback, so a like toggle no longer tears down and restarts its `snapshotFlow`.
- **Animate in the draw/layer phase when you can.** The like/bookmark "pop" reads its `Animatable` inside `graphicsLayer { scaleX = … }`, so each frame re-runs the layer, not composition. Composition-scope animation (`animateColorAsState`, `animateFloatAsState`) is still right for short, intentional transitions where recomposing a small subtree per frame costs nothing — that's idiomatic, not something to optimize away.

## Code style

**Separate logical sections within a function with a blank line.** When a function body groups setup/declarations together and then does something with them, put a blank line between the groups. This applies equally to plain Kotlin functions and composables.

```kotlin
// Declarations first, then a blank line before the work that uses them.
override fun profile(userId: String): Flow<Profile> =
    postRepository.posts.map { posts ->
        val userPosts = posts.filter { it.authorId == userId }
        val userAlbums = albumsByUser[userId].orEmpty()

        Profile(...)
    }

// In composables: state declarations first, then a blank line before each UI element.
@Composable
private fun OverflowMenu(...) {
    val context = LocalContext.current
    var showSheet by remember { mutableStateOf(false) }

    IconButton(...) { ... }

    if (showSheet) {
        PostOverflowSheet(...)
    }

    if (showReportDialog) {
        ReportPostDialog(...)
    }
}
```

**Break function parameters onto separate lines when there are three or more.** Two parameters fit comfortably inline; three or more get one per line with a trailing comma on the last.

```kotlin
// 2 params — inline is fine
fun FeedTopBar(elevated: Boolean, onOpenSettings: () -> Unit)

// 3+ params — one per line
fun HomeScreen(
    uiState: HomeUiState,
    isRefreshing: Boolean,
    actions: HomeActions,
    onOpenSettings: () -> Unit,
)
```

**Use named arguments at call sites when the value doesn't self-document its role.** A single-argument call is almost always clear; a multi-argument call where some values are bare literals, same-typed, or opaque without context should use names. Two specific cases that always warrant names:

- *Lambda type parameters* — when a function type has a non-obvious parameter, name it at the type declaration so every call site is self-explanatory without back-tracking to the definition. E.g. `(Album, initialIndex: Int) -> Unit`, not `(Album, Int) -> Unit`.
- *Coordinate and geometry constructors* — `Offset`, `Size`, and similar value types take `x`/`y` or `width`/`height` that are easy to transpose; always name them: `Offset(x = 0f, y = thickness / 2f)`.

Exception: enum constructor entries (`HOME(R.string.nav_home, R.drawable.ic_home)`) — Kotlin does not allow named arguments for enum constructors, so positional is the only option.

## Localization

All user-facing text lives in `app/src/main/res/values/strings.xml` and is read with `stringResource(...)` — never hardcode display strings in Kotlin. Exception: strings that contain no actual words — only numbers, punctuation, or symbols (e.g. `"${page} / $total"`) — are fine as plain Kotlin interpolation; there is nothing for a translator to change. Navigation labels are `@StringRes` IDs on the `AppDestinations` enum; `PlaceholderScreen` takes a `@StringRes` title.

Computed text (relative time, compact counts) keeps its *logic* pure and testable in `util`: `relativeTime()` and `compactCount()` return structured buckets (`RelativeTime`, `CompactCount`) with no display strings, and the `ui/format` layer's `asText()` resolves a bucket to a localized resource. That's how format tokens like "5m" or "1.2K" stay localizable without making the formatters depend on Android. Post *content* in `SampleData` is stand-in data, not UI chrome, so it remains literal.
