package uno.lux.sample.comment.data.network

import java.time.Instant
import uno.lux.sample.app.core.network.EmptyBody
import uno.lux.sample.app.common.data.network.LikeToggleDto
import uno.lux.sample.app.common.data.network.LikeToggleResponse
import uno.lux.sample.app.core.network.emptyPage
import uno.lux.sample.user.data.network.stubAuthor

class FakeCommentApi(
    private val comments: List<CommentDto> = emptyList(),
    val likeResult: LikeToggleDto = LikeToggleDto(isLiked = true, likeCount = 1),
) : CommentApi {

    override suspend fun getComments(postId: String, cursor: String?, limit: Int): CommentListResponse =
        CommentListResponse(data = comments, page = emptyPage)

    override suspend fun addComment(postId: String, body: AddCommentRequestDto): CommentResponse =
        CommentResponse(
            CommentDto(
                id = "c-new",
                text = body.text,
                createdAt = Instant.parse("2025-01-01T00:00:00.000Z"),
                likeCount = 0,
                isLiked = false,
                author = stubAuthor,
            )
        )

    override suspend fun toggleCommentLike(
        postId: String,
        commentId: String,
        body: EmptyBody,
    ): LikeToggleResponse = LikeToggleResponse(likeResult)
}

fun commentDto(
    id: String,
    text: String,
    isLiked: Boolean = false,
    likeCount: Int = 0,
) = CommentDto(
    id = id,
    text = text,
    createdAt = Instant.parse("2025-01-01T00:00:00.000Z"),
    likeCount = likeCount,
    isLiked = isLiked,
    author = stubAuthor,
)
