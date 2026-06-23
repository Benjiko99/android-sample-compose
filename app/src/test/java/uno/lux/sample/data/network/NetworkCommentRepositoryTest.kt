package uno.lux.sample.data.network

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NetworkCommentRepositoryTest {

    @Test
    fun `comments loads from the API on first collection`() = runTest {
        val api = FakeSampleApi(comments = listOf(commentDto("c1", "Hello")))
        val repository = NetworkCommentRepository(api)

        val loaded = repository.comments("p1").first()

        assertEquals(1, loaded.size)
        assertEquals("c1", loaded.single().id)
        assertEquals("Hello", loaded.single().text)
    }

    @Test
    fun `comments returns an empty list when the API returns none`() = runTest {
        val repository = NetworkCommentRepository(FakeSampleApi(comments = emptyList()))

        val loaded = repository.comments("p1").first()

        assertTrue(loaded.isEmpty())
    }

    @Test
    fun `addComment prepends the new comment to the list`() = runTest {
        val api = FakeSampleApi(comments = listOf(commentDto("c1", "First")))
        val repository = NetworkCommentRepository(api)
        repository.comments("p1").first() // trigger initial load

        repository.addComment("p1", "Second")

        val comments = repository.comments("p1").first()
        assertEquals(2, comments.size)
        assertEquals("c-new", comments.first().id)
        assertEquals("c1", comments.last().id)
    }

    @Test
    fun `toggleLike updates isLiked and likeCount from the server response`() = runTest {
        val api = FakeSampleApi(
            comments = listOf(commentDto("c1", "Hello", isLiked = false, likeCount = 2)),
            likeResult = uno.lux.sample.data.network.dto.LikeToggleNetworkDto(
                isLiked = true, likeCount = 3,
            ),
        )
        val repository = NetworkCommentRepository(api)
        repository.comments("p1").first() // trigger initial load

        repository.toggleLike("p1", "c1")

        val comment = repository.comments("p1").first().single()
        assertTrue(comment.isLiked)
        assertEquals(3, comment.likeCount)
    }

    @Test
    fun `toggleLike only affects the targeted comment`() = runTest {
        val api = FakeSampleApi(
            comments = listOf(
                commentDto("c1", "First"),
                commentDto("c2", "Second"),
            ),
        )
        val repository = NetworkCommentRepository(api)
        repository.comments("p1").first()

        repository.toggleLike("p1", "c1")

        assertFalse(repository.comments("p1").first().single { it.id == "c2" }.isLiked)
    }
}
