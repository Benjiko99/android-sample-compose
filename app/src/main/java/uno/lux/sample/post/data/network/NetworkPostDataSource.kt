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
import uno.lux.sample.post.data.domain.PostWithAuthor

class NetworkPostDataSource(
    private val api: PostApi,
) : PostDataSource {

    override suspend fun fetch(postId: PostId): PostWithAuthor? = withContext(Dispatchers.IO) {
        notFoundAsNull { api.getPost(postId) }
            ?.let { PostWithAuthorMapper.toPostWithAuthor(it.data) }
    }

    override suspend fun create(draft: NewPost): PostWithAuthor = withContext(Dispatchers.IO) {
        val media = draft.media

        val dto = api
            .createPost(
                title = draft.title.asTextPart(),
                body = draft.body.asTextPart(),
                // The trailing brackets are what make Rack collect the repeated parts into one
                // `images` array; a plain "images" name would let each part overwrite the last.
                images = (media as? NewPostMedia.Images)?.files.orEmpty().map { it.asPart("images[]") },
                video = (media as? NewPostMedia.Video)?.file?.asPart("video"),
            ).data

        PostWithAuthorMapper.toPostWithAuthor(dto)
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
            // Blank details are left off the request rather than sent as an empty string:
            // the field is optional, and "typed nothing" is exactly what absent means.
            report = ReportPostRequestDto(
                reason = reason.toDto(),
                details = details.takeIf { it.isNotBlank() },
            ),
        )
    }
}
