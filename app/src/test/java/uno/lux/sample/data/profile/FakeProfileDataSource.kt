package uno.lux.sample.data.profile

internal class FakeProfileDataSource(
    private val refreshData: Map<String, ProfileRefreshData> = emptyMap(),
    private val morePosts: Map<String, PostsPage> = emptyMap(),
) : ProfileDataSource {

    override suspend fun refresh(userId: String) =
        refreshData[userId] ?: ProfileRefreshData(
            postsCount = 0,
            posts = emptyList(), postCursor = null, postHasMore = false,
        )

    override suspend fun loadMorePosts(userId: String, cursor: String?) =
        morePosts[userId] ?: PostsPage(emptyList(), null, false)
}
