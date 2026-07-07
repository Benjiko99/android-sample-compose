package uno.lux.sample.ui.navigation

import androidx.navigation3.runtime.NavKey

/**
 * The navigation seam between ViewModels and the Navigation 3 back stack. ViewModels take a
 * [Navigator] as a constructor dependency and express navigation as intent — [goTo] pushes a
 * [Screen], [goBack] pops — instead of screens receiving navigation lambdas from the host.
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
     * top (e.g. the same profile opened from itself); accidental double-pushes are prevented at
     * the source, where every navigation control debounces its clicks.
     */
    fun goTo(screen: Screen) {
        backStack?.add(screen)
    }

    /** Pops the top entry off the back stack. */
    fun goBack() {
        backStack?.removeLastOrNull()
    }
}
