package uno.lux.sample.util

import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy

/**
 * A reflective no-op implementation of any interface, for Compose previews.
 *
 * The stateless composables take their callbacks bundled as a single `@Stable` *actions*
 * interface. A preview doesn't care what those callbacks do, so rather than hand-write an empty
 * override for each method, [createActionsProxy] returns a [Proxy] whose every call is a no-op
 * (returns [Unit]). A preview then reads simply:
 *
 * ```
 * HomeScreen(uiState = …, actions = ActionsInvocationHandler.createActionsProxy())
 * ```
 */
class ActionsInvocationHandler : InvocationHandler {

    override operator fun invoke(proxy: Any?, method: Method, args: Array<Any?>?): Any {
        return Unit
    }

    companion object {
        /** Returns a [T] — which must be an interface — whose every method does nothing. */
        inline fun <reified T> createActionsProxy(): T {
            return Proxy.newProxyInstance(
                T::class.java.classLoader,
                arrayOf<Class<*>>(T::class.java),
                ActionsInvocationHandler()
            ) as T
        }
    }
}
