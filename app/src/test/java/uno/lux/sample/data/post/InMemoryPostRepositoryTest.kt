package uno.lux.sample.data.post

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class InMemoryPostRepositoryTest {

    private val unliked = post(id = "unliked", likeCount = 4, isLiked = false)
    private val liked = post(id = "liked", likeCount = 10, isLiked = true)
    private val bookmarked = post(id = "bookmarked", isBookmarked = true)

    @Test
    fun `ingest adds posts to the entity map`() = runTest {
        val repository = InMemoryPostRepository(emptyList())

        repository.ingest(listOf(unliked))

        assertEquals(unliked, repository.entities.first()["unliked"])
    }

    @Test
    fun `ingest merges without evicting existing entries`() = runTest {
        val repository = InMemoryPostRepository(listOf(liked))

        repository.ingest(listOf(unliked))

        val entities = repository.entities.first()
        assertEquals(liked, entities["liked"])
        assertEquals(unliked, entities["unliked"])
    }

    @Test
    fun `toggleLike likes an unliked post and increments the count`() = runTest {
        val repository = InMemoryPostRepository(listOf(unliked))

        repository.toggleLike("unliked")

        val result = repository.entities.first()["unliked"]!!
        assertTrue(result.isLiked)
        assertEquals(5, result.likeCount)
    }

    @Test
    fun `toggleLike unlikes a liked post and decrements the count`() = runTest {
        val repository = InMemoryPostRepository(listOf(liked))

        repository.toggleLike("liked")

        val result = repository.entities.first()["liked"]!!
        assertFalse(result.isLiked)
        assertEquals(9, result.likeCount)
    }

    @Test
    fun `toggleBookmark flips the bookmarked flag`() = runTest {
        val repository = InMemoryPostRepository(listOf(bookmarked))

        repository.toggleBookmark("bookmarked")

        assertFalse(repository.entities.first()["bookmarked"]!!.isBookmarked)
    }

    @Test
    fun `a toggle only affects the targeted post`() = runTest {
        val repository = InMemoryPostRepository(listOf(unliked, liked))

        repository.toggleLike("unliked")

        val untouched = repository.entities.first()["liked"]!!
        assertEquals(liked, untouched)
    }

    @Test
    fun `toggling an unknown id leaves the entities unchanged`() = runTest {
        val repository = InMemoryPostRepository(listOf(unliked))

        repository.toggleLike("missing")

        assertEquals(mapOf("unliked" to unliked), repository.entities.first())
    }
}

private fun post(
    id: String,
    likeCount: Int = 0,
    isLiked: Boolean = false,
    isBookmarked: Boolean = false,
) = Post(
    id = id,
    authorId = "u-$id",
    title = "Title $id",
    body = "Body $id",
    createdAt = Instant.EPOCH,
    likeCount = likeCount,
    commentCount = 0,
    isLiked = isLiked,
    isBookmarked = isBookmarked,
)
