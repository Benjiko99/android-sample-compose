package uno.lux.sample.data.post

import kotlinx.coroutines.delay
import uno.lux.sample.data.SamplePosts
import uno.lux.sample.data.SampleUsers

class LocalFeedDataSource : FeedDataSource {

    override suspend fun fetch(cursor: String?): FeedPage {
        delay(REFRESH_DELAY_MILLIS)
        return FeedPage(
            posts = SamplePosts,
            users = SampleUsers,
            nextCursor = null,
            hasMore = false,
        )
    }

    private companion object {
        const val REFRESH_DELAY_MILLIS = 800L
    }
}
