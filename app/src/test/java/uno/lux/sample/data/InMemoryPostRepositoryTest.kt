package uno.lux.sample.data

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
    fun `toggleLike likes an unliked post and increments the count`() = runTest {
        val repository = InMemoryPostRepository(listOf(unliked))

        repository.toggleLike("unliked")

        val result = repository.posts.first().single()
        assertTrue(result.isLiked)
        assertEquals(5, result.likeCount)
    }

    @Test
    fun `toggleLike unlikes a liked post and decrements the count`() = runTest {
        val repository = InMemoryPostRepository(listOf(liked))

        repository.toggleLike("liked")

        val result = repository.posts.first().single()
        assertFalse(result.isLiked)
        assertEquals(9, result.likeCount)
    }

    @Test
    fun `toggleBookmark flips the bookmarked flag`() = runTest {
        val repository = InMemoryPostRepository(listOf(bookmarked))

        repository.toggleBookmark("bookmarked")

        assertFalse(repository.posts.first().single().isBookmarked)
    }

    @Test
    fun `a toggle only affects the targeted post`() = runTest {
        val repository = InMemoryPostRepository(listOf(unliked, liked))

        repository.toggleLike("unliked")

        val untouched = repository.posts.first().single { it.id == "liked" }
        assertEquals(liked, untouched)
    }

    @Test
    fun `toggling an unknown id leaves the feed unchanged`() = runTest {
        val repository = InMemoryPostRepository(listOf(unliked))

        repository.toggleLike("missing")

        assertEquals(listOf(unliked), repository.posts.first())
    }

    @Test
    fun `refresh leaves the existing feed in place`() = runTest {
        val repository = InMemoryPostRepository(listOf(unliked, liked))

        repository.refresh()

        assertEquals(listOf(unliked, liked), repository.posts.first())
    }
}

private fun post(
    id: String,
    likeCount: Int = 0,
    isLiked: Boolean = false,
    isBookmarked: Boolean = false,
) = Post(
    id = id,
    author = User(id = "u-$id", nickname = "User $id", handle = "@$id"),
    title = "Title $id",
    body = "Body $id",
    createdAt = Instant.EPOCH,
    likeCount = likeCount,
    commentCount = 0,
    isLiked = isLiked,
    isBookmarked = isBookmarked,
)
