package uno.lux.sample.data.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import uno.lux.sample.data.post.FeedDataSource
import uno.lux.sample.data.post.FeedPage

class NetworkFeedDataSource(
    private val api: MosaicApi,
) : FeedDataSource {

    override suspend fun fetch(cursor: String?): FeedPage = withContext(Dispatchers.IO) {
        val response = api.getFeed(cursor = cursor, include = "author")
        FeedPage(
            posts = response.data.map { it.toDomain() },
            users = response.included?.users.orEmpty().map { it.toDomain() },
            nextCursor = response.page.nextCursor,
            hasMore = response.page.hasMore,
        )
    }
}
