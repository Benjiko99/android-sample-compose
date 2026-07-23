package uno.lux.sample.app.di

import javax.inject.Qualifier

/** Qualifies the injected id of the signed-in user, so it's distinct from any other `String`. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class CurrentUserId
