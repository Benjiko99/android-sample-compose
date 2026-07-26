package uno.lux.sample.comment.data

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import uno.lux.sample.comment.data.domain.Comment
import uno.lux.sample.user.data.domain.User
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
    fun `setLike asks the data source for the state and answers with the server's`() = runTest {
        val dataSource = FakeCommentDataSource(author).apply { likeCounts["c1"] = 3 }
        val repository = CommentRepository(dataSource)

        val result = repository.setLike("p1", "c1", liked = true)

        assertEquals(listOf("c1" to true), dataSource.likeRequests)
        assertTrue(result.isLiked)
        assertEquals(4, result.likeCount)
    }

    // Unliking is the same call with the other state, not a second operation — which is what lets
    // a retry of either repeat a state rather than repeat a flip.
    @Test
    fun `setLike asks for false the same way it asks for true`() = runTest {
        val dataSource = FakeCommentDataSource(author).apply { likeCounts["c1"] = 7 }
        val repository = CommentRepository(dataSource)

        val result = repository.setLike("p1", "c1", liked = false)

        assertEquals(listOf("c1" to false), dataSource.likeRequests)
        assertFalse(result.isLiked)
        assertEquals(6, result.likeCount)
    }
}
