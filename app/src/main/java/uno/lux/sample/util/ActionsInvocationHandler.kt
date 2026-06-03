package uno.lux.sample.util

import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy

/**
 * Returns a no-op implementation of the interface [T], for Compose previews.
 *
 * The stateless composables take their callbacks bundled as a single `@Stable` *actions*
 * interface. A preview doesn't care what those callbacks do, so rather than hand-write an empty
 * override for each method, this returns a [Proxy] — backed by [ActionsInvocationHandler] — whose
 * every call is a no-op. Top-level, so a preview reads simply:
 *
 * ```
 * HomeScreen(uiState = …, actions = createActionsProxy())
 * ```
 *
 * [T] must be an interface, since a [Proxy] can only stand in for interfaces.
 */
inline fun <reified T> createActionsProxy(): T =
    Proxy.newProxyInstance(
        T::class.java.classLoader,
        arrayOf<Class<*>>(T::class.java),
        ActionsInvocationHandler(),
    ) as T

/** The [InvocationHandler] behind [createActionsProxy]: every proxied call does nothing. */
class ActionsInvocationHandler : InvocationHandler {
    override fun invoke(proxy: Any?, method: Method, args: Array<Any?>?): Any = Unit
}
