package uno.lux.sample.app

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.video.VideoFrameDecoder
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import uno.lux.sample.BuildConfig

/**
 * [HiltAndroidApp] root of the dependency graph.
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
