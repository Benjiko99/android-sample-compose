package uno.lux.sample.data.network

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import uno.lux.sample.data.network.dto.LikeToggleNetworkDto
import uno.lux.sample.data.post.Comment
import uno.lux.sample.data.user.User
import java.time.Instant

class NetworkCommentRepositoryTest {

    private val stubComment = Comment(
        id = "c1",
        author = User(id = "u1", nickname = "Ada", handle = "@ada"),
        createdAt = Instant.EPOCH,
        text = "Hello",
        likeCount = 2,
        isLiked = false,
    )

    @Test
    fun `loadComments fetches from the API`() = runTest {
        val api = FakeMosaicApi(comments = listOf(commentDto("c1", "Hello")))
        val repository = NetworkCommentRepository(api)

        val loaded = repository.loadComments("p1")

        assertEquals(1, loaded.size)
        assertEquals("c1", loaded.single().id)
        assertEquals("Hello", loaded.single().text)
    }

    @Test
    fun `loadComments returns empty list when the API returns none`() = runTest {
        val repository = NetworkCommentRepository(FakeMosaicApi(comments = emptyList()))

        val loaded = repository.loadComments("p1")

        assertTrue(loaded.isEmpty())
    }

    @Test
    fun `addComment returns the server-created comment`() = runTest {
        val repository = NetworkCommentRepository(FakeMosaicApi())

        val comment = repository.addComment("p1", "New thought")

        assertEquals("c-new", comment.id)
        assertEquals("New thought", comment.text)
    }

    @Test
    fun `toggleLike updates isLiked and likeCount from the server response`() = runTest {
        val api = FakeMosaicApi(
            likeResult = LikeToggleNetworkDto(isLiked = true, likeCount = 3),
        )
        val repository = NetworkCommentRepository(api)

        val updated = repository.toggleLike("p1", stubComment)

        assertTrue(updated.isLiked)
        assertEquals(3, updated.likeCount)
    }

    @Test
    fun `toggleLike preserves all other fields on the comment`() = runTest {
        val repository = NetworkCommentRepository(FakeMosaicApi())

        val updated = repository.toggleLike("p1", stubComment)

        assertEquals(stubComment.id, updated.id)
        assertEquals(stubComment.text, updated.text)
        assertEquals(stubComment.author, updated.author)
        assertEquals(stubComment.createdAt, updated.createdAt)
    }

    @Test
    fun `toggleLike on different comments are independent`() = runTest {
        val other = stubComment.copy(id = "c2", isLiked = false)
        val api = FakeMosaicApi(
            likeResult = LikeToggleNetworkDto(isLiked = true, likeCount = 1),
        )
        val repository = NetworkCommentRepository(api)

        repository.toggleLike("p1", stubComment)

        assertFalse(other.isLiked)
    }
}
