package uno.lux.sample.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSerializable
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.serialization.NavBackStackSerializer
import kotlinx.serialization.Serializable

/*
 * The back stack is a list of positions, not a list of pages — which is the distinction that
 * lets the same page be open twice with its own state on each.
 */

/**
 * One position on the back stack: the [screen] to render, plus the [id] that gives that position
 * its own state.
 *
 * Navigation 3 scopes everything per-entry — the `rememberSaveable` state holder, the
 * `ViewModelStore`, the scene identity — by `NavEntry.contentKey`, which is a function of the
 * back-stack key *alone*: `entryProvider` is handed the key and nothing else, so nothing outside
 * the key can tell two identical entries apart. With a bare `Screen` as the key that made
 * identity a property of the *page*, and opening the same post twice handed the second page the
 * first one's ViewModel and its half-typed comment. Carrying [id] here moves identity onto the
 * position instead, where it belongs, and `MosaicApp` uses it as the content key.
 *
 * [Navigator] is the only thing that mints an [id] — see [Navigator.entryFor] — so a new [Screen]
 * cannot forget to take part. Keeping identity out of [Screen] is also what preserves `Screen`
 * equality as a statement about *pages*, which is what [Navigator.goToSingleTop] asks about.
 *
 * The whole entry is `@Serializable`, so the stack survives process death exactly as before; the
 * polymorphism now sits on the sealed [Screen] field, resolved by the compiler rather than by
 * `NavKeySerializer`'s reflection.
 */
@Serializable
data class BackStackEntry(
    val screen: Screen,
    val id: String,
) : NavKey

/**
 * The composition-owned back stack, seeded with [root] the first time it is created and restored
 * from the instance state on every recreation after that — the guarantee `MosaicApp` rests on.
 *
 * This stands in for `rememberNavBackStack`, whose overloads both return a `NavBackStack<NavKey>`
 * serialized through a reflective per-element `NavKeySerializer`. [BackStackEntry] is the only
 * key type this app has, so naming it here keeps the stack typed end to end — [Navigator] takes a
 * `MutableList<BackStackEntry>` and needs no cast to read the screen off the top entry.
 *
 * [root] is a lambda rather than a value because it is consulted only on a cold start, and
 * building an entry mints an identity.
 */
@Composable
fun rememberBackStack(root: () -> BackStackEntry): NavBackStack<BackStackEntry> =
    rememberSerializable(serializer = NavBackStackSerializer(BackStackEntry.serializer())) {
        NavBackStack(root())
    }
