package uno.lux.sample.post.data

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import uno.lux.sample.post.NewPost
import uno.lux.sample.post.Post
import uno.lux.sample.post.PostId
import uno.lux.sample.post.PostWithAuthor
import uno.lux.sample.testing.testPostUrl
import uno.lux.sample.user.User
import uno.lux.sample.user.data.FakeUserDataSource
import uno.lux.sample.user.data.UserRepository
import java.time.Instant

class PostRepositoryTest {

    private val unliked = post(id = "unliked", likeCount = 4, isLiked = false)
    private val liked = post(id = "liked", likeCount = 10, isLiked = true)
    private val bookmarked = post(id = "bookmarked", isBookmarked = true)
    private val author = User(id = "u-unliked", nickname = "Ada", handle = "@ada")

    private val userRepository = UserRepository(FakeUserDataSource())

    private fun repository(dataSource: PostDataSource = FakePostDataSource()) =
        PostRepository(dataSource, userRepository)

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

    // load() is what a screen with nothing but a post ID falls back on — a detail page restored
    // after process death, whose stores start empty.
    @Test
    fun `load stores the fetched post and seeds its embedded author`() = runTest {
        val dataSource = FakePostDataSource()
        dataSource.fetchable["unliked"] = PostWithAuthor(unliked, author)
        val repo = repository(dataSource)

        assertEquals(unliked, repo.load("unliked"))

        assertEquals(unliked, repo.entities.first()["unliked"])
        // The author rides along, so a screen resolving the post finds its header user too.
        assertEquals(author, userRepository.users.first()[author.id])
    }

    @Test
    fun `load returns null for a post the source no longer has`() = runTest {
        val repo = repository()

        assertNull(repo.load("gone"))
        assertTrue(repo.entities.first().isEmpty())
    }

    @Test
    fun `load leaves previously ingested posts in place`() = runTest {
        val dataSource = FakePostDataSource()
        dataSource.fetchable["unliked"] = PostWithAuthor(unliked, author)
        val repo = repository(dataSource)
        repo.ingest(listOf(liked))

        repo.load("unliked")

        assertEquals(liked, repo.entities.first()["liked"])
    }

    @Test
    fun `create stores the published post and seeds its embedded author`() = runTest {
        val repo = repository()

        val created = repo.create(NewPost(title = "Title", body = "Body"))

        assertEquals(created, repo.entities.first()[created.id])
        assertEquals("u1", userRepository.users.first()["u1"]?.id)
    }

    @Test
    fun `create leaves previously ingested posts in place`() = runTest {
        val repo = repository()
        repo.ingest(listOf(liked))

        repo.create(NewPost(title = "Title", body = "Body"))

        assertEquals(liked, repo.entities.first()["liked"])
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
        val repo = repository()
        repo.ingest(listOf(unliked))

        repo.toggleLike("unliked")

        val result = repo.entities.first()["unliked"]!!
        assertTrue(result.isLiked)
        assertEquals(5, result.likeCount)
    }

    @Test
    fun `toggleBookmark applies the data source result to the entity map`() = runTest {
        val repo = repository()
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
    fun `delete removes the post from the entity map`() = runTest {
        val repo = repository()
        repo.ingest(listOf(unliked, liked))

        repo.delete("unliked")

        assertEquals(mapOf("liked" to liked), repo.entities.first())
    }

    @Test
    fun `delete records the id as deleted`() = runTest {
        val repo = repository()
        repo.ingest(listOf(unliked))

        repo.delete("unliked")

        assertEquals(setOf<PostId>("unliked"), repo.deletedIds.first())
    }

    // The store already knows the answer for an id it deleted, and "no such post" must hold
    // even offline — so no request is made.
    @Test
    fun `load answers a deleted id from the store without a request`() = runTest {
        val dataSource = FakePostDataSource()
        dataSource.fetchable["unliked"] = PostWithAuthor(unliked, author)
        val repo = repository(dataSource)
        repo.ingest(listOf(unliked))
        repo.delete("unliked")

        assertNull(repo.load("unliked"))
        assertEquals(emptyList<PostId>(), dataSource.fetchedPostIds)
    }

    @Test
    fun `delete goes through the data source`() = runTest {
        val dataSource = FakePostDataSource()
        val repo = repository(dataSource)
        repo.ingest(listOf(unliked))

        repo.delete("unliked")

        assertEquals(listOf("unliked"), dataSource.deletedPostIds)
    }

    @Test
    fun `a failed delete keeps the post in the entity map`() = runTest {
        val dataSource = FakePostDataSource().apply { deleteError = IllegalStateException("boom") }
        val repo = repository(dataSource)
        repo.ingest(listOf(unliked))

        runCatching { repo.delete("unliked") }

        assertEquals(unliked, repo.entities.first()["unliked"])
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
    url = testPostUrl(id),
    authorId = "u-$id",
    title = "Title $id",
    body = "Body $id",
    createdAt = Instant.EPOCH,
    likeCount = likeCount,
    commentCount = 0,
    isLiked = isLiked,
    isBookmarked = isBookmarked,
)
