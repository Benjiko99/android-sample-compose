package uno.lux.sample.data.post

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import uno.lux.sample.data.user.User
import java.time.Instant

class InMemoryCommentRepositoryTest {

    private val author = User(id = "u1", nickname = "Ada", handle = "@ada")
    private val commenter = User(id = "u2", nickname = "Bab", handle = "@bab")

    private fun comment(
        id: String,
        text: String = "A comment",
        likeCount: Int = 0,
        isLiked: Boolean = false,
    ) = Comment(
        id = id,
        author = commenter,
        createdAt = Instant.EPOCH,
        text = text,
        likeCount = likeCount,
        isLiked = isLiked,
    )

    @Test
    fun `loadComments returns empty list for unknown post`() = runTest {
        val repository = InMemoryCommentRepository(author, initial = emptyMap())

        val comments = repository.loadComments("no-such-post")

        assertEquals(emptyList<Comment>(), comments)
    }

    @Test
    fun `loadComments returns the seeded list for a known post`() = runTest {
        val c = comment("c1")
        val repository = InMemoryCommentRepository(author, initial = mapOf("p1" to listOf(c)))

        val comments = repository.loadComments("p1")

        assertEquals(listOf(c), comments)
    }

    @Test
    fun `addComment returns a new comment authored by the current user`() = runTest {
        val repository = InMemoryCommentRepository(author, initial = emptyMap())

        val added = repository.addComment("p1", "New thought")

        assertEquals("New thought", added.text)
        assertEquals(author, added.author)
        assertEquals(0, added.likeCount)
        assertFalse(added.isLiked)
    }

    @Test
    fun `toggleLike likes an unliked comment and increments the count`() = runTest {
        val unliked = comment("c1", likeCount = 3, isLiked = false)
        val repository = InMemoryCommentRepository(author, initial = emptyMap())

        val result = repository.toggleLike("p1", unliked)

        assertTrue(result.isLiked)
        assertEquals(4, result.likeCount)
    }

    @Test
    fun `toggleLike unlikes a liked comment and decrements the count`() = runTest {
        val liked = comment("c1", likeCount = 7, isLiked = true)
        val repository = InMemoryCommentRepository(author, initial = emptyMap())

        val result = repository.toggleLike("p1", liked)

        assertFalse(result.isLiked)
        assertEquals(6, result.likeCount)
    }

    @Test
    fun `toggleLike preserves all other comment fields`() = runTest {
        val original = comment("c1", text = "Hello", likeCount = 2, isLiked = false)
        val repository = InMemoryCommentRepository(author, initial = emptyMap())

        val result = repository.toggleLike("p1", original)

        assertEquals(original.id, result.id)
        assertEquals(original.text, result.text)
        assertEquals(original.author, result.author)
        assertEquals(original.createdAt, result.createdAt)
    }
}
