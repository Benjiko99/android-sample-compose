package uno.lux.sample.data.post

class LocalPostDataSource : PostDataSource {

    override suspend fun toggleLike(post: Post): Post {
        val liked = !post.isLiked
        return post.copy(isLiked = liked, likeCount = post.likeCount + if (liked) 1 else -1)
    }

    override suspend fun toggleBookmark(post: Post): Post =
        post.copy(isBookmarked = !post.isBookmarked)
}
