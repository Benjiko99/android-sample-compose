package uno.lux.sample.data.post

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class PostRepositoryTest {

    private val unliked = post(id = "unliked", likeCount = 4, isLiked = false)
    private val liked = post(id = "liked", likeCount = 10, isLiked = true)
    private val bookmarked = post(id = "bookmarked", isBookmarked = true)

    private fun repository(dataSource: PostDataSource = FakePostDataSource()) =
        PostRepository(dataSource)

    @Test
    fun `entities starts empty`() = runTest {
        assertTrue(repository().entities.first().isEmpty())
    }

    @Test
    fun `ingest adds posts to the entity map`() = runTest {
        val repo = repository()

        repo.ingest(listOf(unliked))

        assertEquals(unliked, repo.entities.first()["unliked"])
    }

    @Test
    fun `ingest merges without evicting existing entries`() = runTest {
        val repo = repository()
        repo.ingest(listOf(liked))

        repo.ingest(listOf(unliked))

        val entities = repo.entities.first()
        assertEquals(liked, entities["liked"])
        assertEquals(unliked, entities["unliked"])
    }

    @Test
    fun `toggleLike applies the data source result to the entity map`() = runTest {
        val likedResult = unliked.copy(isLiked = true, likeCount = 5)
        val repo = repository(FakePostDataSource(likeResult = likedResult))
        repo.ingest(listOf(unliked))

        repo.toggleLike("unliked")

        val result = repo.entities.first()["unliked"]!!
        assertTrue(result.isLiked)
        assertEquals(5, result.likeCount)
    }

    @Test
    fun `toggleBookmark applies the data source result to the entity map`() = runTest {
        val unbookmarkedResult = bookmarked.copy(isBookmarked = false)
        val repo = repository(FakePostDataSource(bookmarkResult = unbookmarkedResult))
        repo.ingest(listOf(bookmarked))

        repo.toggleBookmark("bookmarked")

        assertFalse(repo.entities.first()["bookmarked"]!!.isBookmarked)
    }

    @Test
    fun `a toggle only affects the targeted post`() = runTest {
        val repo = repository()
        repo.ingest(listOf(unliked, liked))

        repo.toggleLike("unliked")

        assertEquals(liked, repo.entities.first()["liked"])
    }

    @Test
    fun `toggling an unknown id leaves the entities unchanged`() = runTest {
        val repo = repository()
        repo.ingest(listOf(unliked))

        repo.toggleLike("missing")

        assertEquals(mapOf("unliked" to unliked), repo.entities.first())
    }
}

private fun post(
    id: String,
    likeCount: Int = 0,
    isLiked: Boolean = false,
    isBookmarked: Boolean = false,
) = Post(
    id = id,
    authorId = "u-$id",
    title = "Title $id",
    body = "Body $id",
    createdAt = Instant.EPOCH,
    likeCount = likeCount,
    commentCount = 0,
    isLiked = isLiked,
    isBookmarked = isBookmarked,
)

private class FakePostDataSource(
    private val likeResult: Post? = null,
    private val bookmarkResult: Post? = null,
) : PostDataSource {
    override suspend fun toggleLike(post: Post) =
        likeResult ?: post.copy(isLiked = !post.isLiked)

    override suspend fun toggleBookmark(post: Post) =
        bookmarkResult ?: post.copy(isBookmarked = !post.isBookmarked)
}
