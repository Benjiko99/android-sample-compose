package uno.lux.sample.ui.create

import uno.lux.sample.data.file.FileUpload
import uno.lux.sample.data.post.NewPost
import uno.lux.sample.util.AppError

/** Field limits the composer enforces; they mirror the backend's so errors surface before a publish. */
const val CreatePostTitleMaxLength = 120
const val CreatePostBodyMaxLength = 5000

/** Mirrors the server's `Album::MAX_PHOTOS`, so an over-long selection is refused before uploading. */
const val CreatePostMaxImages = 10

/**
 * The composer's editable fields. The text is kept exactly as typed — trimming happens once, in
 * [toNewPost], so a trailing space the user is still typing past doesn't fight the cursor.
 *
 * [imageUris] holds the picked images as content-URI strings rather than their bytes: a selection
 * can run to ten photos, and holding all of them in memory for the length of the composing
 * session would be wasteful when the thumbnails render straight from the URI. They are read into
 * [FileUpload]s once, at publish (see `CreatePostViewModel.publish`), which is why [toNewPost]
 * takes the loaded [images] as a parameter instead of producing them.
 */
data class CreatePostForm(
    val title: String = "",
    val body: String = "",
    val imageUris: List<String> = emptyList(),
) {
    val isEmpty: Boolean
        get() = title.isEmpty() && body.isEmpty() && imageUris.isEmpty()

    val canPublish: Boolean
        get() = title.isNotBlank() && body.isNotBlank()

    val canAddImages: Boolean
        get() = imageUris.size < CreatePostMaxImages

    fun toNewPost(images: List<FileUpload>) =
        NewPost(title = title.trim(), body = body.trim(), images = images)
}

/**
 * The composer's state. Unlike the other screens there is nothing to load — the form starts
 * blank — so this is a single data class rather than a sealed hierarchy: the live [form], whether
 * a publish is in flight, whether leaving with a part-written post is awaiting confirmation, and
 * the error of a publish that failed.
 */
data class CreatePostUiState(
    val form: CreatePostForm = CreatePostForm(),
    val isPublishing: Boolean = false,
    val showDiscardConfirmation: Boolean = false,
    val publishError: AppError? = null,
)
