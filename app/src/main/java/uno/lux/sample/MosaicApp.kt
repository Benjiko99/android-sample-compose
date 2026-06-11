package uno.lux.sample

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * [HiltAndroidApp] root of the dependency graph. App-wide singletons (the repositories) are
 * declared in [uno.lux.sample.di.DataModule] rather than held here, so this class stays
 * empty — it only anchors Hilt's code generation.
 */
@HiltAndroidApp
class MosaicApp : Application()
