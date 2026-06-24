package uno.lux.sample.data.network

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
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
}
