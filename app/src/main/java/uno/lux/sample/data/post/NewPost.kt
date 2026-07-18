package uno.lux.sample.data.post

import uno.lux.sample.data.user.User

/**
 * A post the signed-in user is about to publish. Carries only what the composer collects —
 * the author is derived server-side from the caller, and media attachments aren't authorable
 * from the client, so a draft has no [Album] or [Video].
 */
data class NewPost(
    val title: String,
    val body: String,
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
