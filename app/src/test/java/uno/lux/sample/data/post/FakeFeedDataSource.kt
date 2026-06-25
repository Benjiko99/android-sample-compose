package uno.lux.sample.data.post

internal class FakeFeedDataSource(
    private val pages: List<FeedPage> = emptyList(),
) : FeedDataSource {

    private var callCount = 0

    override suspend fun fetch(cursor: String?) =
        pages.getOrElse(callCount++) {
            FeedPage(
                posts = emptyList(),
                users = emptyList(),
                nextCursor = null,
                hasMore = false
            )
        }
}
