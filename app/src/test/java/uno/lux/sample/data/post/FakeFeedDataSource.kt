package uno.lux.sample.data.post

import uno.lux.sample.data.feed.FeedDataSource
import uno.lux.sample.data.feed.FeedPage

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
