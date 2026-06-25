package uno.lux.sample.data.network

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import uno.lux.sample.data.network.dto.BookmarkToggleDto
import uno.lux.sample.data.network.dto.LikeToggleDto
import uno.lux.sample.data.post.Post
import java.time.Instant

class NetworkPostDataSourceTest {

    private val stubPost = post("p1")

    @Test
    fun `toggleLike returns post with updated isLiked and likeCount from the server response`() = runTest {
        val api = FakeMosaicApi(likeResult = LikeToggleDto(isLiked = true, likeCount = 6))
        val dataSource = NetworkPostDataSource(api)

        val result = dataSource.toggleLike(stubPost.copy(isLiked = false, likeCount = 5))

        assertTrue(result.isLiked)
        assertEquals(6, result.likeCount)
    }

    @Test
    fun `toggleBookmark returns post with updated isBookmarked from the server response`() = runTest {
        val api = FakeMosaicApi(bookmarkResult = BookmarkToggleDto(isBookmarked = true))
        val dataSource = NetworkPostDataSource(api)

        val result = dataSource.toggleBookmark(stubPost.copy(isBookmarked = false))

        assertTrue(result.isBookmarked)
    }

    @Test
    fun `toggleLike preserves all other fields on the post`() = runTest {
        val api = FakeMosaicApi(likeResult = LikeToggleDto(isLiked = true, likeCount = 1))
        val dataSource = NetworkPostDataSource(api)
        val original = stubPost.copy(isLiked = false, likeCount = 0)

        val result = dataSource.toggleLike(original)

        assertEquals(original.id, result.id)
        assertEquals(original.authorId, result.authorId)
        assertEquals(original.title, result.title)
        assertEquals(original.isBookmarked, result.isBookmarked)
    }
}

private fun post(id: String) = Post(
    id = id,
    authorId = "u1",
    title = "Title $id",
    body = "Body $id",
    createdAt = Instant.EPOCH,
    likeCount = 0,
    commentCount = 0,
)
