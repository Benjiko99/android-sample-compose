package uno.lux.sample.data.profile

internal class FakeProfileDataSource(
    private val refreshData: Map<String, ProfileRefreshData> = emptyMap(),
    private val morePosts: Map<String, PostsPage> = emptyMap(),
    /** Keyed by cursor, so a test can drive a multi-page Saved tab; null is the first page. */
    private val bookmarks: Map<String, Map<String?, BookmarksPage>> = emptyMap(),
) : ProfileDataSource {

    /** The (userId, cursor) pairs [bookmarks] was called with, in call order. */
    val bookmarkCalls = mutableListOf<Pair<String, String?>>()

    override suspend fun refresh(userId: String) =
        refreshData[userId] ?: ProfileRefreshData(
            postsCount = 0,
            posts = emptyList(), postCursor = null, postHasMore = false,
        )

    override suspend fun loadMorePosts(userId: String, cursor: String?) =
        morePosts[userId] ?: PostsPage(emptyList(), null, false)

    override suspend fun bookmarks(userId: String, cursor: String?): BookmarksPage {
        bookmarkCalls += userId to cursor

        return bookmarks[userId]?.get(cursor)
            ?: BookmarksPage(emptyList(), emptyList(), null, false)
    }
}
