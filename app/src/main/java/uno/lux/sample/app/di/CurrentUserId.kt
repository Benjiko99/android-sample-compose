package uno.lux.sample.app.di

import javax.inject.Qualifier

/** Qualifies the injected id of the signed-in user. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class CurrentUserId
