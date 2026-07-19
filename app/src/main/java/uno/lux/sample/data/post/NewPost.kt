package uno.lux.sample.data.post

import uno.lux.sample.data.file.FileUpload
import uno.lux.sample.data.user.User

/**
 * The media a draft carries. A post has photos *or* a video, never both — the server rejects the
 * combination and the clients have no layout for two media blocks — so the alternatives are a
 * closed hierarchy rather than two nullable fields.
 */
sealed interface NewPostMedia {

    data object None : NewPostMedia

    data class Images(val files: List<FileUpload>) : NewPostMedia

    /**
     * [durationSeconds] travels with the file because the server cannot extract it: reading video
     * metadata server-side would mean shipping ffmpeg in the image for one display field, so the
     * client — which already has the file open — reports it instead.
     */
    data class Video(val file: FileUpload, val durationSeconds: Int) : NewPostMedia
}

/**
 * A post the signed-in user is about to publish. The author is derived server-side from the
 * caller, so a draft carries only what the composer collects: the text plus its [media], which the
 * server stores and hands back as the post's [Album] or [Video].
 */
data class NewPost(
    val title: String,
    val body: String,
    val media: NewPostMedia = NewPostMedia.None,
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
