package uno.lux.sample.ui.editprofile

import android.content.Context
import android.webkit.MimeTypeMap
import androidx.core.net.toUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import uno.lux.sample.data.user.AvatarUpload
import java.io.IOException

/**
 * Reads a picked image URI into an [AvatarUpload] the data layer can upload. Kept as an
 * interface so [EditProfileViewModel] can depend on it without touching Android
 * `ContentResolver` types, and so tests can supply a fake.
 */
interface AvatarImageLoader {
    /** Reads the image at [uri] (a `content://` / `file://` URI string) into an upload payload. */
    suspend fun read(uri: String): AvatarUpload
}

/** Android implementation backed by [Context.getContentResolver]; bound in `DataModule`. */
class AndroidAvatarImageLoader(
    private val context: Context,
) : AvatarImageLoader {

    override suspend fun read(uri: String): AvatarUpload = withContext(Dispatchers.IO) {
        val parsed = uri.toUri()
        val resolver = context.contentResolver

        val mimeType = resolver.getType(parsed) ?: DEFAULT_MIME_TYPE
        val bytes = resolver.openInputStream(parsed)?.use { it.readBytes() }
            ?: throw IOException("Unable to open avatar image at $uri")
        val extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType) ?: "jpg"

        AvatarUpload(bytes = bytes, mimeType = mimeType, filename = "avatar.$extension")
    }

    private companion object {
        const val DEFAULT_MIME_TYPE = "image/jpeg"
    }
}
