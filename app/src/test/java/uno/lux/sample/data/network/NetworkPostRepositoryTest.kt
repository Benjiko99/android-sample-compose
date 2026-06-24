package uno.lux.sample.data.network

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import uno.lux.sample.data.network.dto.BookmarkToggleNetworkDto
import uno.lux.sample.data.network.dto.LikeToggleNetworkDto

class NetworkPostRepositoryTest {

    private fun repository(api: FakeMosaicApi = FakeMosaicApi()) = NetworkPostRepository(api)

    @Test
    fun `entities starts empty`() = runTest {
        assertTrue(repository().entities.first().isEmpty())
    }

    @Test
    fun `ingest adds posts to the entity map`() = runTest {
        val repo = repository()
        val post = feedItemDto("p1", "u1").toDomain()

        repo.ingest(listOf(post))

        assertEquals(post, repo.entities.first()["p1"])
    }

    @Test
    fun `ingest merges without evicting existing entries`() = runTest {
        val repo = repository()
        val p1 = feedItemDto("p1", "u1").toDomain()
        val p2 = feedItemDto("p2", "u1").toDomain()
        repo.ingest(listOf(p1))

        repo.ingest(listOf(p2))

        val entities = repo.entities.first()
        assertEquals(p1, entities["p1"])
        assertEquals(p2, entities["p2"])
    }

    @Test
    fun `toggleLike updates isLiked and likeCount from the server response`() = runTest {
        val api = FakeMosaicApi(likeResult = LikeToggleNetworkDto(isLiked = true, likeCount = 6))
        val repo = repository(api)
        repo.ingest(listOf(feedItemDto("p1", "u1", isLiked = false, likeCount = 5).toDomain()))

        repo.toggleLike("p1")

        val post = repo.entities.first()["p1"]!!
        assertTrue(post.isLiked)
        assertEquals(6, post.likeCount)
    }

    @Test
    fun `toggleBookmark updates isBookmarked from the server response`() = runTest {
        val api = FakeMosaicApi(bookmarkResult = BookmarkToggleNetworkDto(isBookmarked = true))
        val repo = repository(api)
        repo.ingest(listOf(feedItemDto("p1", "u1", isBookmarked = false).toDomain()))

        repo.toggleBookmark("p1")

        assertTrue(repo.entities.first()["p1"]!!.isBookmarked)
    }

    @Test
    fun `a toggle only affects the targeted post`() = runTest {
        val repo = repository()
        val p1 = feedItemDto("p1", "u1").toDomain()
        val p2 = feedItemDto("p2", "u1").toDomain()
        repo.ingest(listOf(p1, p2))

        repo.toggleLike("p1")

        assertFalse(repo.entities.first()["p2"]!!.isLiked)
    }
}
