package uno.lux.sample.data.network

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import uno.lux.sample.data.network.dto.FeedIncluded
import uno.lux.sample.data.network.dto.FeedResponse
import uno.lux.sample.data.network.dto.LikeToggleNetworkDto
import uno.lux.sample.data.network.dto.BookmarkToggleNetworkDto

class NetworkPostRepositoryTest {

    private fun repository(
        api: FakeMosaicApi = FakeMosaicApi(),
        userRepo: NetworkUserRepository = NetworkUserRepository(FakeMosaicApi()),
    ) = NetworkPostRepository(api, userRepo)

    @Test
    fun `posts starts empty before any refresh`() = runTest {
        assertTrue(repository().posts.first().isEmpty())
    }

    @Test
    fun `refresh loads posts from the feed API`() = runTest {
        val api = FakeMosaicApi(
            feedResponse = FeedResponse(listOf(feedItemDto("p1", "u1")), page = emptyPage),
        )
        val repo = repository(api)

        repo.refresh()

        val posts = repo.posts.first()
        assertEquals(1, posts.size)
        assertEquals("p1", posts.single().id)
    }

    @Test
    fun `refresh ingests sideloaded authors into the user cache`() = runTest {
        val api = FakeMosaicApi(
            feedResponse = FeedResponse(
                data = listOf(feedItemDto("p1", "u1")),
                included = FeedIncluded(users = listOf(userDto("u1", "Ada"))),
                page = emptyPage,
            ),
        )
        val userRepo = NetworkUserRepository(FakeMosaicApi())
        val repo = repository(api, userRepo)

        repo.refresh()

        assertEquals("Ada", userRepo.user("u1").first()?.nickname)
    }

    @Test
    fun `refresh without sideload leaves the user cache empty`() = runTest {
        val api = FakeMosaicApi(
            feedResponse = FeedResponse(
                data = listOf(feedItemDto("p1", "u1")),
                included = null,
                page = emptyPage,
            ),
        )
        val userRepo = NetworkUserRepository(FakeMosaicApi())
        val repo = repository(api, userRepo)

        repo.refresh()

        assertTrue(userRepo.users.first().isEmpty())
    }

    @Test
    fun `toggleLike updates isLiked and likeCount from the server response`() = runTest {
        val api = FakeMosaicApi(
            feedResponse = FeedResponse(
                listOf(feedItemDto("p1", "u1", isLiked = false, likeCount = 5)),
                page = emptyPage,
            ),
            likeResult = LikeToggleNetworkDto(isLiked = true, likeCount = 6),
        )
        val repo = repository(api)
        repo.refresh()

        repo.toggleLike("p1")

        val post = repo.posts.first().single()
        assertTrue(post.isLiked)
        assertEquals(6, post.likeCount)
    }

    @Test
    fun `toggleBookmark updates isBookmarked from the server response`() = runTest {
        val api = FakeMosaicApi(
            feedResponse = FeedResponse(
                listOf(feedItemDto("p1", "u1", isBookmarked = false)),
                page = emptyPage,
            ),
            bookmarkResult = BookmarkToggleNetworkDto(isBookmarked = true),
        )
        val repo = repository(api)
        repo.refresh()

        repo.toggleBookmark("p1")

        assertTrue(repo.posts.first().single().isBookmarked)
    }

    @Test
    fun `a toggle only affects the targeted post`() = runTest {
        val api = FakeMosaicApi(
            feedResponse = FeedResponse(
                listOf(feedItemDto("p1", "u1"), feedItemDto("p2", "u1")),
                page = emptyPage,
            ),
        )
        val repo = repository(api)
        repo.refresh()

        repo.toggleLike("p1")

        assertFalse(repo.posts.first().single { it.id == "p2" }.isLiked)
    }
}
