package uno.lux.sample.data.post

interface PostDataSource {
    suspend fun create(draft: NewPost): CreatedPost
    suspend fun delete(postId: PostId)
    suspend fun toggleLike(post: Post): Post
    suspend fun toggleBookmark(post: Post): Post
}
