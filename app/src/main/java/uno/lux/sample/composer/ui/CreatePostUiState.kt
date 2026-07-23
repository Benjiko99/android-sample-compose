package uno.lux.sample.composer.ui

import kotlinx.serialization.Serializable
import uno.lux.sample.app.core.files.FileUpload
import uno.lux.sample.post.NewPost
import uno.lux.sample.post.NewPostMedia
import uno.lux.sample.app.util.AppError

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
 *
 * [Serializable] because a picked selection is part of the draft that survives process death.
 */
@Serializable
sealed interface CreatePostMedia {

    /** Nothing attached — the only state from which either kind can still be chosen. */
    @Serializable
    data object None : CreatePostMedia

    @Serializable
    data class Images(val uris: List<String>) : CreatePostMedia {
        val canAddMore: Boolean
            get() = uris.size < CreatePostMaxImages
    }

    /**
     * A single clip. [durationSeconds] is read off the file when it is picked, purely to badge the
     * composer's own thumbnail: the clip has not been uploaded yet, so the server's derived
     * duration — the one a published post shows — does not exist for it. It is not sent with the
     * upload; see `NewPostMedia.Video`.
     */
    @Serializable
    data class Video(val uri: String, val durationSeconds: Int) : CreatePostMedia
}

/**
 * The composer's editable fields. The text is kept exactly as typed — trimming happens once, in
 * [toNewPost], so a trailing space the user is still typing past doesn't fight the cursor.
 *
 * [toNewPost] takes the already-loaded [media] as a parameter rather than producing it, since
 * reading the picked files needs a suspending `FileLoader` the form has no business holding.
 *
 * [Serializable] so a part-written post survives process death — see [uno.lux.sample.util.saveDraft].
 */
@Serializable
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
 * Whatever the composer currently has to tell the user, as a bucket rather than a message so the
 * display string stays in the `ui/format` layer (like [AppError]). One field, not one per source:
 * the screen has a single snackbar, so two nullable errors would only ever mean "whichever is set"
 * — an invariant kept by convention at every update site instead of by the type.
 */
sealed interface CreatePostError {

    /** A publish, or reading a picked file, failed. */
    data class Failed(val error: AppError) : CreatePostError

    /** The chosen clip is over [CreatePostMaxVideoBytes], which the server would reject anyway. */
    data object VideoTooLarge : CreatePostError
}

/**
 * The composer's state. Unlike the other screens there is nothing to load — the form starts
 * blank — so this is a single data class rather than a sealed hierarchy: the live [form], whether
 * a publish is in flight, whether leaving with a part-written post is awaiting confirmation, and
 * anything that went wrong.
 */
data class CreatePostUiState(
    val form: CreatePostForm = CreatePostForm(),
    val isPublishing: Boolean = false,
    val showDiscardConfirmation: Boolean = false,
    val error: CreatePostError? = null,
)
