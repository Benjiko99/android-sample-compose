package uno.lux.sample.di

import javax.inject.Qualifier

/** Qualifies the injected [uno.lux.sample.data.user.User] object for the signed-in user. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class CurrentUser
