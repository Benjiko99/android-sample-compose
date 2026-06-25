package uno.lux.sample.data.post

internal class FakePostDataSource : PostDataSource {

    override suspend fun toggleLike(post: Post) =
        post.copy(
            isLiked = !post.isLiked,
            likeCount = post.likeCount + if (!post.isLiked) 1 else -1
        )

    override suspend fun toggleBookmark(post: Post) =
        post.copy(isBookmarked = !post.isBookmarked)
}
