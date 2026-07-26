package uno.lux.sample.comment.data.network

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import uno.lux.sample.comment.data.domain.Comment
import uno.lux.sample.common.data.network.LikeStateDto
import uno.lux.sample.common.data.network.SetLikeRequestDto
import uno.lux.sample.user.data.domain.User
import java.time.Instant

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
        val api = FakeCommentApi(likeResult = LikeStateDto(isLiked = true, likeCount = 3))
        val dataSource = NetworkCommentDataSource(api)

        val updated = dataSource.toggleLike("p1", stubComment)

        assertTrue(updated.isLiked)
        assertEquals(3, updated.likeCount)
    }

    // The flip is worked out here and the wire carries the state it lands on, so the request the
    // server sees is one a timeout-retry can repeat without moving the like twice.
    @Test
    fun `toggleLike asks for the state opposite the comment's own`() = runTest {
        val api = FakeCommentApi()
        val dataSource = NetworkCommentDataSource(api)

        dataSource.toggleLike("p1", stubComment.copy(isLiked = true))

        assertEquals(listOf("c1" to SetLikeRequestDto(liked = false)), api.likeRequests)
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
