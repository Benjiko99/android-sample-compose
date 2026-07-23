package uno.lux.sample.app.navigation

import androidx.navigation3.runtime.NavKey

/**
 * The navigation seam between ViewModels and the Navigation 3 back stack. ViewModels take a
 * [Navigator] as a constructor dependency and express navigation as intent — [goTo] pushes a
 * [Screen], [goBack] pops, [replaceTop] swaps — instead of screens receiving navigation lambdas
 * from the host.
 *
 * The back stack itself stays owned by the composition (`rememberNavBackStack` in `SampleApp`),
 * which is what keeps it saveable across configuration changes and process death; the UI [attach]es
 * it here so ViewModels can mutate it. Before a stack is attached (or after [detach]) navigation
 * calls are dropped — there is no UI to navigate.
 *
 * The class is deliberately free of DI annotations (provided in `NavigationModule`, retained
 * across configuration changes) and framework types beyond [NavKey], so unit tests drive the
 * real implementation by attaching a plain `mutableListOf<NavKey>(...)`.
 */
class Navigator {

    private var backStack: MutableList<NavKey>? = null

    /** Binds [backStack] as the stack [goTo] and [goBack] mutate. */
    fun attach(backStack: MutableList<NavKey>) {
        this.backStack = backStack
    }

    /** Releases [backStack] if it is still the attached stack; a newer attachment stays. */
    fun detach(backStack: MutableList<NavKey>) {
        if (this.backStack === backStack) this.backStack = null
    }

    /**
     * Pushes [screen] on top of the back stack. Deliberately allows a screen equal to the current
     * top (e.g. the same profile opened from a post on that profile); the click debounce every
     * navigation control carries only guards against an accidental fast double-tap, not this
     * intentional re-open. Screens that must never stack on themselves use [goToSingleTop].
     */
    fun goTo(screen: Screen) {
        backStack?.add(screen)
    }

    /**
     * Pushes [screen] unless it already sits on top of the back stack — the "single top" launch
     * behaviour. Used for pages that are semantically unique wherever they're reached (Settings,
     * the profile editor): re-invoking the affordance while the page is showing is a no-op rather
     * than a second copy on the stack. This is a real guarantee independent of tap timing, which
     * is why it can't be left to the debounce.
     */
    fun goToSingleTop(screen: Screen) {
        val backStack = backStack ?: return

        if (backStack.lastOrNull() != screen) backStack.add(screen)
    }

    /**
     * Swaps the top entry for [screen] — pop and push as one step. Used by a page that has served
     * its purpose and hands off to another: back from [screen] then returns to whatever sat below
     * the replaced page, not to the page itself. The composer replaces itself with the published
     * post's detail page this way, so backing out of that post lands on the feed rather than on a
     * composer the user is done with.
     */
    fun replaceTop(screen: Screen) {
        val backStack = backStack ?: return

        backStack.removeLastOrNull()
        backStack.add(screen)
    }

    /** Pops the top entry off the back stack. */
    fun goBack() {
        backStack?.removeLastOrNull()
    }
}
