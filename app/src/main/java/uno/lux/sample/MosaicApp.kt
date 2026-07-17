package uno.lux.sample

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

/**
 * [HiltAndroidApp] root of the dependency graph. App-wide singletons (the repositories) are
 * declared in [uno.lux.sample.di.DataModule] rather than held here, so this class stays thin.
 */
@HiltAndroidApp
class MosaicApp : Application() {

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) Timber.plant(Timber.DebugTree())
    }
}
