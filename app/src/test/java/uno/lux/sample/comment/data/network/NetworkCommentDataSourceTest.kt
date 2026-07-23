package uno.lux.sample.comment.data.network

import java.time.Instant
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import uno.lux.sample.comment.Comment
import uno.lux.sample.app.common.data.network.LikeToggleDto
import uno.lux.sample.user.User

class NetworkCommentDataSourceTest {

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
        val api = FakeCommentApi(comments = listOf(commentDto("c1", "Hello")))
        val dataSource = NetworkCommentDataSource(api)

        val loaded = dataSource.loadComments("p1")

        assertEquals(1, loaded.size)
        assertEquals("c1", loaded.single().id)
        assertEquals("Hello", loaded.single().text)
    }

    @Test
    fun `loadComments returns empty list when the API returns none`() = runTest {
        val dataSource = NetworkCommentDataSource(FakeCommentApi(comments = emptyList()))

        val loaded = dataSource.loadComments("p1")

        assertTrue(loaded.isEmpty())
    }

    @Test
    fun `addComment returns the server-created comment`() = runTest {
        val dataSource = NetworkCommentDataSource(FakeCommentApi())

        val comment = dataSource.addComment("p1", "New thought")

        assertEquals("c-new", comment.id)
        assertEquals("New thought", comment.text)
    }

    @Test
    fun `toggleLike updates isLiked and likeCount from the server response`() = runTest {
        val api = FakeCommentApi(likeResult = LikeToggleDto(isLiked = true, likeCount = 3))
        val dataSource = NetworkCommentDataSource(api)

        val updated = dataSource.toggleLike("p1", stubComment)

        assertTrue(updated.isLiked)
        assertEquals(3, updated.likeCount)
    }

    @Test
    fun `toggleLike preserves all other fields on the comment`() = runTest {
        val dataSource = NetworkCommentDataSource(FakeCommentApi())

        val updated = dataSource.toggleLike("p1", stubComment)

        assertEquals(stubComment.id, updated.id)
        assertEquals(stubComment.text, updated.text)
        assertEquals(stubComment.author, updated.author)
        assertEquals(stubComment.createdAt, updated.createdAt)
    }
}
