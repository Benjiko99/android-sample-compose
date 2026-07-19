package uno.lux.sample.data.post

import uno.lux.sample.data.file.FileUpload
import uno.lux.sample.data.user.User

/**
 * A post the signed-in user is about to publish. The author is derived server-side from the
 * caller, so a draft carries only what the composer collects: the text plus any [images], which
 * the server stores and hands back as the post's [Album]. Video is not authorable from the client.
 */
data class NewPost(
    val title: String,
    val body: String,
    val images: List<FileUpload> = emptyList(),
)

/**
 * The server's answer to publishing a [NewPost]: the stored [post] plus its [author], which the
 * create response embeds in full. Both are returned together so the caller can seed the post and
 * user stores in one step — a freshly launched app may hold neither yet.
 */
data class CreatedPost(
    val post: Post,
    val author: User,
)
