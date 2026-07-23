package uno.lux.sample.app.di

import javax.inject.Qualifier

/** Qualifies the injected [uno.lux.sample.user.User] object for the signed-in user. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class CurrentUser
