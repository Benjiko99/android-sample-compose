package uno.lux.sample.comment.data

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import uno.lux.sample.comment.Comment
import uno.lux.sample.user.User
import java.time.Instant

class CommentRepositoryTest {

    private val author = User(id = "u1", nickname = "Ada", handle = "@ada")

    private fun comment(
        id: String,
        text: String = "A comment",
        likeCount: Int = 0,
        isLiked: Boolean = false,
    ) = Comment(
        id = id,
        author = author,
        createdAt = Instant.EPOCH,
        text = text,
        likeCount = likeCount,
        isLiked = isLiked,
    )

    @Test
    fun `loadComments returns empty list for unknown post`() = runTest {
        val repository = CommentRepository(FakeCommentDataSource(author))

        val comments = repository.loadComments("no-such-post")

        assertEquals(emptyList<Comment>(), comments)
    }

    @Test
    fun `loadComments returns the seeded list for a known post`() = runTest {
        val c = comment("c1")
        val repository = CommentRepository(
            FakeCommentDataSource(
                author,
                comments = mapOf(
                    "p1" to listOf(c),
                ),
            ),
        )

        val comments = repository.loadComments("p1")

        assertEquals(listOf(c), comments)
    }

    @Test
    fun `addComment returns a new comment authored by the current user`() = runTest {
        val repository = CommentRepository(FakeCommentDataSource(author))

        val added = repository.addComment("p1", "New thought")

        assertEquals("New thought", added.text)
        assertEquals(author, added.author)
        assertEquals(0, added.likeCount)
        assertFalse(added.isLiked)
    }

    @Test
    fun `toggleLike likes an unliked comment and increments the count`() = runTest {
        val unliked = comment("c1", likeCount = 3, isLiked = false)
        val repository = CommentRepository(FakeCommentDataSource(author))

        val result = repository.toggleLike("p1", unliked)

        assertTrue(result.isLiked)
        assertEquals(4, result.likeCount)
    }

    @Test
    fun `toggleLike unlikes a liked comment and decrements the count`() = runTest {
        val liked = comment("c1", likeCount = 7, isLiked = true)
        val repository = CommentRepository(FakeCommentDataSource(author))

        val result = repository.toggleLike("p1", liked)

        assertFalse(result.isLiked)
        assertEquals(6, result.likeCount)
    }
}
