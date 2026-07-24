package uno.lux.sample.post.data

import uno.lux.sample.post.NewPost
import uno.lux.sample.post.Post
import uno.lux.sample.post.PostId
import uno.lux.sample.post.PostWithAuthor

interface PostDataSource {
    /**
     * Fetches one post with its author, or null when there is no such post. A missing post is an
     * answer (the server's 404), not a failure: a caller has to tell "this post is gone" apart
     * from "the request never got through", and only one of the two is worth retrying.
     */
    suspend fun fetch(postId: PostId): PostWithAuthor?

    suspend fun create(draft: NewPost): PostWithAuthor

    suspend fun delete(postId: PostId)

    suspend fun toggleLike(post: Post): Post

    suspend fun toggleBookmark(post: Post): Post
}
