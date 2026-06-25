package uno.lux.sample.data.profile

internal class FakeProfileDataSource(
    private val refreshData: Map<String, ProfileRefreshData> = emptyMap(),
    private val morePosts: Map<String, PostsPage> = emptyMap(),
    private val moreAlbums: Map<String, AlbumsPage> = emptyMap(),
    private val moreVideos: Map<String, VideosPage> = emptyMap(),
) : ProfileDataSource {

    override suspend fun refresh(userId: String) =
        refreshData[userId] ?: ProfileRefreshData(
            postsCount = 0, albumsCount = 0, videosCount = 0,
            posts = emptyList(), postCursor = null, postHasMore = false,
            albums = emptyList(), albumCursor = null, albumHasMore = false,
            videos = emptyList(), videoCursor = null, videoHasMore = false,
        )

    override suspend fun loadMorePosts(userId: String, cursor: String?) =
        morePosts[userId] ?: PostsPage(emptyList(), null, false)

    override suspend fun loadMoreAlbums(userId: String, cursor: String?) =
        moreAlbums[userId] ?: AlbumsPage(emptyList(), null, false)

    override suspend fun loadMoreVideos(userId: String, cursor: String?) =
        moreVideos[userId] ?: VideosPage(emptyList(), null, false)
}
