package uno.lux.sample.app.core.files

import android.content.Context
import android.media.MediaMetadataRetriever
import androidx.core.net.toUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * Reads the metadata of a picked video that the *server* cannot derive. Extracting duration
 * server-side would mean shipping ffmpeg in the backend image for one display field, so the
 * client — which already has the file open — reports it instead.
 *
 * Kept as an interface for the same reason as [FileLoader]: ViewModels depend on it without
 * touching Android media types, and tests supply a fake.
 */
interface VideoMetadataReader {
    /** Whole-second duration of the video at [uri], or 0 when it can't be determined. */
    suspend fun durationSeconds(uri: String): Int
}

/** Android implementation backed by [MediaMetadataRetriever]. */
class AndroidVideoMetadataReader(
    private val context: Context,
) : VideoMetadataReader {

    override suspend fun durationSeconds(uri: String): Int = withContext(Dispatchers.IO) {
        // Not `use`: MediaMetadataRetriever only became AutoCloseable in API 29 and minSdk is 26.
        val retriever = MediaMetadataRetriever()

        try {
            retriever.setDataSource(context, uri.toUri())
            val millis = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)

            // Round to the nearest second: a 4.6s clip reading as "4s" looks like a bug.
            millis?.toLongOrNull()?.let { ms -> ((ms + 500) / 1000).toInt() } ?: 0
        } catch (e: RuntimeException) {
            // A DRM-protected or malformed file throws rather than returning null. Duration is
            // cosmetic, so this degrades to 0 rather than blocking the post.
            Timber.w(e, "Could not read duration of %s", uri)
            0
        } finally {
            retriever.release()
        }
    }
}
