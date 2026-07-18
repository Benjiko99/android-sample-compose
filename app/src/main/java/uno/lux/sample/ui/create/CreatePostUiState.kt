package uno.lux.sample.ui.create

import uno.lux.sample.data.post.NewPost
import uno.lux.sample.util.AppError

/** Field limits the composer enforces; they mirror the backend's so errors surface before a publish. */
const val CreatePostTitleMaxLength = 120
const val CreatePostBodyMaxLength = 5000

/**
 * The composer's editable fields. Both are kept exactly as typed — trimming happens once, in
 * [toNewPost], so a trailing space the user is still typing past doesn't fight the cursor.
 */
data class CreatePostForm(
    val title: String = "",
    val body: String = "",
) {
    val isEmpty: Boolean
        get() = title.isEmpty() && body.isEmpty()

    val canPublish: Boolean
        get() = title.isNotBlank() && body.isNotBlank()

    fun toNewPost() = NewPost(title = title.trim(), body = body.trim())
}

/**
 * The composer's state. Unlike the other screens there is nothing to load — the form starts
 * blank — so this is a single data class rather than a sealed hierarchy: the live [form], whether
 * a publish is in flight, and the error of a publish that failed.
 */
data class CreatePostUiState(
    val form: CreatePostForm = CreatePostForm(),
    val isPublishing: Boolean = false,
    val publishError: AppError? = null,
)
