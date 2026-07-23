package uno.lux.sample.feed.data

import uno.lux.sample.feed.data.FeedDataSource
import uno.lux.sample.feed.data.FeedPage

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
