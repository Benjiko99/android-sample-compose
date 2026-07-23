package uno.lux.sample

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.video.VideoFrameDecoder
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

/**
 * [HiltAndroidApp] root of the dependency graph. App-wide singletons (the repositories) are
 * declared in [uno.lux.sample.di.DataModule] rather than held here, so this class stays thin.
 *
 * It also supplies Coil's singleton [ImageLoader], which is the one piece of image loading that
 * can't be configured at the call site: decoders are a loader-wide concern.
 */
@HiltAndroidApp
class MosaicApplication : Application(), SingletonImageLoader.Factory {

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) Timber.plant(Timber.DebugTree())
    }

    /**
     * Adds the video-frame decoder to Coil's defaults. Without it a `content://` video URI has no
     * decoder that matches and the composer's picked-clip thumbnail renders empty — every other
     * image in the app is a still, which the default decoders already handle.
     */
    override fun newImageLoader(context: PlatformContext): ImageLoader =
        ImageLoader.Builder(context)
            .components { add(VideoFrameDecoder.Factory()) }
            .build()
}
