package uno.lux.sample.data.network

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import uno.lux.sample.data.network.dto.CursorPageDto
import uno.lux.sample.data.network.response.FeedIncluded
import uno.lux.sample.data.network.response.FeedResponse

class NetworkFeedRepositoryTest {

    private fun repository(
        api: FakeMosaicApi = FakeMosaicApi(),
        postRepo: NetworkPostRepository = NetworkPostRepository(FakeMosaicApi()),
        userRepo: NetworkUserRepository = NetworkUserRepository(FakeMosaicApi()),
    ) = NetworkFeedRepository(api, postRepo, userRepo)

    @Test
    fun `postIds starts empty before any refresh`() = runTest {
        assertTrue(repository().postIds.first().isEmpty())
    }

    @Test
    fun `refresh populates postIds in feed order`() = runTest {
        val api = FakeMosaicApi(
            feedResponse = FeedResponse(
                data = listOf(feedItemDto("p1", "u1"), feedItemDto("p2", "u1")),
                page = emptyPage,
            ),
        )
        val repo = repository(api)

        repo.refresh()

        assertEquals(listOf("p1", "p2"), repo.postIds.first())
    }

    @Test
    fun `refresh ingests post entities into the post repository`() = runTest {
        val api = FakeMosaicApi(
            feedResponse = FeedResponse(listOf(feedItemDto("p1", "u1")), page = emptyPage),
        )
        val postRepo = NetworkPostRepository(FakeMosaicApi())
        val repo = repository(api, postRepo)

        repo.refresh()

        assertEquals("p1", postRepo.entities.first()["p1"]?.id)
    }

    @Test
    fun `refresh ingests sideloaded authors into the user repository`() = runTest {
        val api = FakeMosaicApi(
            feedResponse = FeedResponse(
                data = listOf(feedItemDto("p1", "u1")),
                included = FeedIncluded(users = listOf(userDto("u1", "Ada"))),
                page = emptyPage,
            ),
        )
        val userRepo = NetworkUserRepository(FakeMosaicApi())
        val repo = repository(api, userRepo = userRepo)

        repo.refresh()

        assertEquals("Ada", userRepo.user("u1").first()?.nickname)
    }

    @Test
    fun `refresh without sideload leaves the user repository unchanged`() = runTest {
        val api = FakeMosaicApi(
            feedResponse = FeedResponse(
                data = listOf(feedItemDto("p1", "u1")),
                included = null,
                page = emptyPage,
            ),
        )
        val userRepo = NetworkUserRepository(FakeMosaicApi())
        val repo = repository(api, userRepo = userRepo)

        repo.refresh()

        assertTrue(userRepo.users.first().isEmpty())
    }

    @Test
    fun `hasMore is false before any refresh`() = runTest {
        assertFalse(repository().hasMore.first())
    }

    @Test
    fun `hasMore reflects the page flag after refresh`() = runTest {
        val api = FakeMosaicApi(
            feedResponse = FeedResponse(
                data = listOf(feedItemDto("p1", "u1")),
                page = CursorPageDto(nextCursor = "c2", hasMore = true),
            ),
        )
        val repo = repository(api)

        repo.refresh()

        assertTrue(repo.hasMore.first())
    }

    @Test
    fun `loadMore appends the next page of post IDs`() = runTest {
        val page1 = FeedResponse(
            data = listOf(feedItemDto("p1", "u1")),
            page = CursorPageDto(nextCursor = "c2", hasMore = true),
        )
        val page2 = FeedResponse(
            data = listOf(feedItemDto("p2", "u1")),
            page = emptyPage,
        )
        val api = FakeMosaicApi(
            feedResponse = page1,
            feedResponseByCursor = mapOf("c2" to page2),
        )
        val repo = repository(api)
        repo.refresh()

        repo.loadMore()

        assertEquals(listOf("p1", "p2"), repo.postIds.first())
    }

    @Test
    fun `loadMore clears hasMore when the last page is fetched`() = runTest {
        val page1 = FeedResponse(
            data = listOf(feedItemDto("p1", "u1")),
            page = CursorPageDto(nextCursor = "c2", hasMore = true),
        )
        val page2 = FeedResponse(
            data = listOf(feedItemDto("p2", "u1")),
            page = emptyPage,
        )
        val api = FakeMosaicApi(
            feedResponse = page1,
            feedResponseByCursor = mapOf("c2" to page2),
        )
        val repo = repository(api)
        repo.refresh()

        repo.loadMore()

        assertFalse(repo.hasMore.first())
    }

    @Test
    fun `loadMore is a no-op when hasMore is false`() = runTest {
        val api = FakeMosaicApi(
            feedResponse = FeedResponse(listOf(feedItemDto("p1", "u1")), page = emptyPage),
        )
        val repo = repository(api)
        repo.refresh()

        repo.loadMore()

        assertEquals(listOf("p1"), repo.postIds.first())
    }

    @Test
    fun `refresh resets the post list and cursor`() = runTest {
        val page1 = FeedResponse(
            data = listOf(feedItemDto("p1", "u1")),
            page = CursorPageDto(nextCursor = "c2", hasMore = true),
        )
        val api = FakeMosaicApi(
            feedResponse = page1,
            feedResponseByCursor = mapOf(
                "c2" to FeedResponse(listOf(feedItemDto("p2", "u1")), page = emptyPage),
            ),
        )
        val repo = repository(api)
        repo.refresh()
        repo.loadMore()

        repo.refresh()

        assertEquals(listOf("p1"), repo.postIds.first())
        assertTrue(repo.hasMore.first())
    }
}
