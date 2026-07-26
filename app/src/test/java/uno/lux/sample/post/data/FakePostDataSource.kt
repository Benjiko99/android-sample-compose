package uno.lux.sample.post.data

import uno.lux.sample.common.data.ReportReason
import uno.lux.sample.post.data.domain.LikeState
import uno.lux.sample.post.data.domain.NewPost
import uno.lux.sample.post.data.domain.Post
import uno.lux.sample.post.data.domain.PostId
import uno.lux.sample.post.data.domain.PostWithAuthor
import uno.lux.sample.testing.testPostUrl
import uno.lux.sample.user.data.domain.User
import java.time.Instant

internal class FakePostDataSource : PostDataSource {

    /**
     * What [fetch] serves, keyed by post ID. An ID that isn't here answers null — the fake's
     * stand-in for the server's 404.
     */
    val fetchable = mutableMapOf<PostId, PostWithAuthor>()

    /** IDs passed to [fetch], in call order, so a test can assert a fetch was (or wasn't) made. */
    val fetchedPostIds = mutableListOf<PostId>()

    /** Thrown by [fetch] instead of returning, so tests can drive the failure path. */
    var fetchError: Exception? = null

    override suspend fun fetch(postId: PostId): PostWithAuthor? {
        fetchedPostIds += postId
        fetchError?.let { throw it }

        return fetchable[postId]
    }

    /** The draft passed to the most recent [create] call, for test assertions. */
    var lastDraft: NewPost? = null
        private set

    /** Thrown by [create] instead of returning, so tests can drive the failure path. */
    var createError: Exception? = null

    override suspend fun create(draft: NewPost): PostWithAuthor {
        lastDraft = draft
        createError?.let { throw it }

        return PostWithAuthor(
            post = Post(
                id = "p-new",
                url = testPostUrl("p-new"),
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

    /**
     * The like counts this fake's server holds, keyed by post ID.
     *
     * A like request names a post and a state, not a count, so the number in the answer is the
     * server's own — a test asserting on one seeds it here to say what the server started from.
     * An unseeded post counts from zero, which is loud enough to spot when a test meant to seed.
     */
    val likeCounts = mutableMapOf<PostId, Int>()

    /** The `(postId, liked)` pairs passed to [setLike], in call order, for test assertions. */
    val likeRequests = mutableListOf<Pair<PostId, Boolean>>()

    /** Thrown by [setLike] instead of answering, so tests can drive the failure path. */
    var setLikeError: Exception? = null

    /**
     * Runs inside [setLike] and [setBookmark] before either answers, which is the only moment a
     * test can see the optimistic value the repository wrote before the request went out.
     */
    var whileInFlight: (suspend () -> Unit)? = null

    override suspend fun setLike(postId: PostId, liked: Boolean): LikeState {
        likeRequests += postId to liked
        whileInFlight?.invoke()
        setLikeError?.let { throw it }

        val settled = (likeCounts[postId] ?: 0) + if (liked) 1 else -1
        likeCounts[postId] = settled

        return LikeState(isLiked = liked, likeCount = settled)
    }

    /** The `(postId, bookmarked)` pairs passed to [setBookmark], in call order. */
    val bookmarkRequests = mutableListOf<Pair<PostId, Boolean>>()

    /** Thrown by [setBookmark] instead of answering, so tests can drive the failure path. */
    var setBookmarkError: Exception? = null

    override suspend fun setBookmark(postId: PostId, bookmarked: Boolean): Boolean {
        bookmarkRequests += postId to bookmarked
        whileInFlight?.invoke()
        setBookmarkError?.let { throw it }

        return bookmarked
    }

    /** The reports passed to [report], in call order, for test assertions. */
    val reports = mutableListOf<Report>()

    data class Report(
        val postId: PostId,
        val reason: ReportReason,
        val details: String,
    )

    override suspend fun report(
        postId: PostId,
        reason: ReportReason,
        details: String,
    ) {
        reports += Report(postId, reason, details)
    }
}
