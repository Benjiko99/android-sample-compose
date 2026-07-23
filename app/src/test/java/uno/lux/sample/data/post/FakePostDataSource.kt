package uno.lux.sample.data.post

import uno.lux.sample.data.user.User
import java.time.Instant

internal class FakePostDataSource : PostDataSource {

    /** The draft passed to the most recent [create] call, for test assertions. */
    var lastDraft: NewPost? = null
        private set

    /** Thrown by [create] instead of returning, so tests can drive the failure path. */
    var createError: Exception? = null

    override suspend fun create(draft: NewPost): CreatedPost {
        lastDraft = draft
        createError?.let { throw it }

        return CreatedPost(
            post = Post(
                id = "p-new",
                url = "https://mosaic.test/p/p-new",
                authorId = "u1",
                title = draft.title,
                body = draft.body,
                createdAt = Instant.EPOCH,
                likeCount = 0,
                commentCount = 0,
            ),
            author = User(id = "u1", nickname = "Ada", handle = "@u1"),
        )
    }

    /** IDs passed to [delete], in call order, for test assertions. */
    val deletedPostIds = mutableListOf<PostId>()

    /** Thrown by [delete] instead of returning, so tests can drive the failure path. */
    var deleteError: Exception? = null

    override suspend fun delete(postId: PostId) {
        deleteError?.let { throw it }
        deletedPostIds += postId
    }

    override suspend fun toggleLike(post: Post) =
        post.copy(
            isLiked = !post.isLiked,
            likeCount = post.likeCount + if (!post.isLiked) 1 else -1
        )

    override suspend fun toggleBookmark(post: Post) =
        post.copy(isBookmarked = !post.isBookmarked)
}
