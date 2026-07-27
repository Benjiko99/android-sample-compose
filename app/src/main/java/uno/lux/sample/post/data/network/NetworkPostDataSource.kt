package uno.lux.sample.post.data.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import uno.lux.sample.common.data.LikeState
import uno.lux.sample.common.data.ReportReason
import uno.lux.sample.common.data.network.SetLikeRequestDto
import uno.lux.sample.common.data.network.asPart
import uno.lux.sample.common.data.network.asTextPart
import uno.lux.sample.common.data.network.notFoundAsNull
import uno.lux.sample.post.data.PostDataSource
import uno.lux.sample.post.data.domain.NewPost
import uno.lux.sample.post.data.domain.NewPostMedia
import uno.lux.sample.post.data.domain.PostId
import uno.lux.sample.post.data.domain.PostWithUsers
import uno.lux.sample.user.data.network.toDomain

class NetworkPostDataSource(
    private val api: PostApi,
) : PostDataSource {

    override suspend fun fetch(postId: PostId): PostWithUsers? = withContext(Dispatchers.IO) {
        notFoundAsNull { api.getPost(postId) }?.toPostWithUsers()
    }

    override suspend fun create(draft: NewPost): PostWithUsers = withContext(Dispatchers.IO) {
        val media = draft.media

        api
            .createPost(
                title = draft.title.asTextPart(),
                body = draft.body.asTextPart(),
                images = (media as? NewPostMedia.Images)?.files.orEmpty().map { it.asPart("images[]") },
                video = (media as? NewPostMedia.Video)?.file?.asPart("video"),
            ).toPostWithUsers()
    }

    override suspend fun delete(postId: PostId) = withContext(Dispatchers.IO) {
        api.deletePost(postId)
    }

    override suspend fun setLike(postId: PostId, liked: Boolean): LikeState = withContext(Dispatchers.IO) {
        val result = api.setLike(postId, SetLikeRequestDto(liked)).data

        LikeState(isLiked = result.isLiked, likeCount = result.likeCount)
    }

    override suspend fun setBookmark(postId: PostId, bookmarked: Boolean): Boolean = withContext(Dispatchers.IO) {
        api.setBookmark(postId, SetBookmarkRequestDto(bookmarked)).data.isBookmarked
    }

    override suspend fun report(
        postId: PostId,
        reason: ReportReason,
        details: String,
    ) = withContext(Dispatchers.IO) {
        api.reportPost(
            postId = postId,
            report = ReportPostRequestDto(
                reason = reason.toDto(),
                details = details.takeIf { it.isNotBlank() },
            ),
        )
    }

    private fun PostResponse.toPostWithUsers() = PostWithUsers(
        post = PostMapper.map(data),
        users = included.users.map { it.toDomain() },
    )
}
