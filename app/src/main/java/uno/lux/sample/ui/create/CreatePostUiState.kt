package uno.lux.sample.ui.create

import uno.lux.sample.data.file.FileUpload
import uno.lux.sample.data.post.NewPost
import uno.lux.sample.data.post.NewPostMedia
import uno.lux.sample.util.AppError

/** Field limits the composer enforces; they mirror the backend's so errors surface before a publish. */
const val CreatePostTitleMaxLength = 120
const val CreatePostBodyMaxLength = 5000

/** Mirrors the server's `Album::MAX_PHOTOS`, so an over-long selection is refused before uploading. */
const val CreatePostMaxImages = 10

/** Mirrors the server's `Video::MAX_BYTES`, so an oversized clip is refused before it is uploaded. */
const val CreatePostMaxVideoBytes = 25L * 1024 * 1024

/**
 * The media attached to a draft. A post carries photos *or* a video, never both, so the states are
 * modelled as a closed hierarchy rather than two independent fields — the illegal combination is
 * unrepresentable here instead of being a rule the form has to remember to enforce.
 *
 * Both variants hold content-URI strings rather than bytes: a selection can run to ten photos or a
 * 25 MB clip, and holding that in memory for the length of the composing session would be wasteful
 * when the thumbnails render straight from the URI. They are read into [FileUpload]s once, at
 * publish (see `CreatePostViewModel.publish`).
 */
sealed interface CreatePostMedia {

    /** Nothing attached — the only state from which either kind can still be chosen. */
    data object None : CreatePostMedia

    data class Images(val uris: List<String>) : CreatePostMedia {
        val canAddMore: Boolean
            get() = uris.size < CreatePostMaxImages
    }

    /**
     * A single clip. [durationSeconds] is read from the file when it is picked, because the server
     * has no way to extract it — see `PostsService.create`.
     */
    data class Video(val uri: String, val durationSeconds: Int) : CreatePostMedia
}

/**
 * The composer's editable fields. The text is kept exactly as typed — trimming happens once, in
 * [toNewPost], so a trailing space the user is still typing past doesn't fight the cursor.
 *
 * [toNewPost] takes the already-loaded [media] as a parameter rather than producing it, since
 * reading the picked files needs a suspending `FileLoader` the form has no business holding.
 */
data class CreatePostForm(
    val title: String = "",
    val body: String = "",
    val media: CreatePostMedia = CreatePostMedia.None,
) {
    val isEmpty: Boolean
        get() = title.isEmpty() && body.isEmpty() && media is CreatePostMedia.None

    val canPublish: Boolean
        get() = title.isNotBlank() && body.isNotBlank()

    fun toNewPost(media: NewPostMedia) =
        NewPost(title = title.trim(), body = body.trim(), media = media)
}

/**
 * A picked file the composer refused, reported before any upload is attempted. Kept as a bucket
 * rather than a message so the display string stays in the `ui/format` layer, like [AppError].
 */
enum class CreatePostMediaError {
    /** The chosen clip is over [CreatePostMaxVideoBytes], which the server would reject anyway. */
    VideoTooLarge,
}

/**
 * The composer's state. Unlike the other screens there is nothing to load — the form starts
 * blank — so this is a single data class rather than a sealed hierarchy: the live [form], whether
 * a publish is in flight, whether leaving with a part-written post is awaiting confirmation, the
 * error of a publish that failed, and the error of a file that was rejected when picked.
 */
data class CreatePostUiState(
    val form: CreatePostForm = CreatePostForm(),
    val isPublishing: Boolean = false,
    val showDiscardConfirmation: Boolean = false,
    val publishError: AppError? = null,
    val mediaError: CreatePostMediaError? = null,
)
