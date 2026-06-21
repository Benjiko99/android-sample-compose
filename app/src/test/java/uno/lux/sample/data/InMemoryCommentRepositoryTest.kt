package uno.lux.sample.data

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
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
    fun `comments returns empty list for unknown post`() = runTest {
        val repository = InMemoryCommentRepository(author, initial = emptyMap())

        val comments = repository.comments("no-such-post").first()

        assertEquals(emptyList<Comment>(), comments)
    }

    @Test
    fun `comments returns the seeded list for a known post`() = runTest {
        val c = comment("c1")
        val repository = InMemoryCommentRepository(author, initial = mapOf("p1" to listOf(c)))

        val comments = repository.comments("p1").first()

        assertEquals(listOf(c), comments)
    }

    @Test
    fun `addComment prepends a new comment by the current user`() = runTest {
        val existing = comment("c1")
        val repository = InMemoryCommentRepository(author, initial = mapOf("p1" to listOf(existing)))

        repository.addComment("p1", "New thought")

        val comments = repository.comments("p1").first()
        assertEquals(2, comments.size)
        val added = comments.first()
        assertEquals("New thought", added.text)
        assertEquals(author, added.author)
        assertEquals(0, added.likeCount)
        assertFalse(added.isLiked)
        // Existing comment is now second.
        assertEquals(existing, comments[1])
    }

    @Test
    fun `addComment creates a fresh list when the post had no comments`() = runTest {
        val repository = InMemoryCommentRepository(author, initial = emptyMap())

        repository.addComment("p1", "First!")

        val comments = repository.comments("p1").first()
        assertEquals(1, comments.size)
        assertEquals("First!", comments.first().text)
    }

    @Test
    fun `toggleLike likes an unliked comment and increments the count`() = runTest {
        val unliked = comment("c1", likeCount = 3, isLiked = false)
        val repository = InMemoryCommentRepository(author, initial = mapOf("p1" to listOf(unliked)))

        repository.toggleLike("p1", "c1")

        val result = repository.comments("p1").first().single()
        assertTrue(result.isLiked)
        assertEquals(4, result.likeCount)
    }

    @Test
    fun `toggleLike unlikes a liked comment and decrements the count`() = runTest {
        val liked = comment("c1", likeCount = 7, isLiked = true)
        val repository = InMemoryCommentRepository(author, initial = mapOf("p1" to listOf(liked)))

        repository.toggleLike("p1", "c1")

        val result = repository.comments("p1").first().single()
        assertFalse(result.isLiked)
        assertEquals(6, result.likeCount)
    }

    @Test
    fun `toggleLike only affects the targeted comment`() = runTest {
        val target = comment("c1", likeCount = 1, isLiked = false)
        val other = comment("c2", likeCount = 5, isLiked = false)
        val repository = InMemoryCommentRepository(
            author,
            initial = mapOf("p1" to listOf(target, other)),
        )

        repository.toggleLike("p1", "c1")

        val untouched = repository.comments("p1").first().single { it.id == "c2" }
        assertEquals(other, untouched)
    }

    @Test
    fun `toggleLike on unknown post id leaves state unchanged`() = runTest {
        val c = comment("c1")
        val repository = InMemoryCommentRepository(author, initial = mapOf("p1" to listOf(c)))

        repository.toggleLike("unknown-post", "c1")

        assertEquals(listOf(c), repository.comments("p1").first())
    }

    @Test
    fun `operations on different posts are isolated`() = runTest {
        val c1 = comment("c1")
        val c2 = comment("c2")
        val repository = InMemoryCommentRepository(
            author,
            initial = mapOf("p1" to listOf(c1), "p2" to listOf(c2)),
        )

        repository.addComment("p1", "On post 1")

        assertEquals(2, repository.comments("p1").first().size)
        assertEquals(listOf(c2), repository.comments("p2").first())
    }
}
