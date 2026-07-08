package uno.lux.sample.data.profile

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import uno.lux.sample.data.post.Post
import uno.lux.sample.data.post.FakePostDataSource
import uno.lux.sample.data.post.PostRepository
import java.time.Instant

class ProfileRepositoryTest {

    private val adaPost = post(id = "p1", authorId = "u1")
    private val gracePost = post(id = "p2", authorId = "u2")

    private fun postRepo() = PostRepository(FakePostDataSource())

    private fun repository(
        dataSource: ProfileDataSource = FakeProfileDataSource(),
        postRepo: PostRepository = postRepo(),
    ) = ProfileRepository(dataSource, postRepo)

    @Test
    fun `profile is empty before refresh`() = runTest {
        val profile = repository().profile("u1").first()

        assertEquals("u1", profile.userId)
        assertEquals(0, profile.postsCount)
    }

    @Test
    fun `postIds is empty before refresh`() = runTest {
        assertTrue(repository().postIds("u1").first().isEmpty())
    }

    @Test
    fun `hasMorePosts is false before refresh`() = runTest {
        assertFalse(repository().hasMorePosts("u1").first())
    }

    @Test
    fun `refresh populates profile with data source result`() = runTest {
        val dataSource = FakeProfileDataSource(
            refreshData = mapOf(
                "u1" to ProfileRefreshData(
                    postsCount = 1,
                    posts = listOf(adaPost),
                    postCursor = null, postHasMore = false,
                ),
            ),
        )
        val repo = repository(dataSource)

        repo.refresh("u1")

        val profile = repo.profile("u1").first()
        assertEquals("u1", profile.userId)
        assertEquals(1, profile.postsCount)
    }

    @Test
    fun `refresh populates postIds from data source result`() = runTest {
        val dataSource = FakeProfileDataSource(
            refreshData = mapOf("u1" to refreshData(posts = listOf(adaPost, gracePost))),
        )
        val repo = repository(dataSource)

        repo.refresh("u1")

        assertEquals(listOf("p1", "p2"), repo.postIds("u1").first())
    }

    @Test
    fun `refresh ingests posts into the post repository`() = runTest {
        val postRepo = postRepo()
        val dataSource = FakeProfileDataSource(
            refreshData = mapOf("u1" to refreshData(posts = listOf(adaPost))),
        )
        val repo = repository(dataSource, postRepo)

        repo.refresh("u1")

        assertEquals("p1", postRepo.entities.first()["p1"]?.id)
    }

    @Test
    fun `refresh sets hasMorePosts from data source`() = runTest {
        val dataSource = FakeProfileDataSource(
            refreshData = mapOf(
                "u1" to ProfileRefreshData(
                    postsCount = 1,
                    posts = listOf(adaPost), postCursor = "c2", postHasMore = true,
                ),
            ),
        )
        val repo = repository(dataSource)

        repo.refresh("u1")

        assertTrue(repo.hasMorePosts("u1").first())
    }

    @Test
    fun `loadMorePosts appends post IDs`() = runTest {
        val p3 = post("p3", "u1")
        val dataSource = FakeProfileDataSource(
            refreshData = mapOf("u1" to ProfileRefreshData(
                postsCount = 1,
                posts = listOf(adaPost), postCursor = "c2", postHasMore = true,
            )),
            morePosts = mapOf("u1" to PostsPage(listOf(p3), null, false)),
        )
        val repo = repository(dataSource)
        repo.refresh("u1")

        repo.loadMorePosts("u1")

        assertEquals(listOf("p1", "p3"), repo.postIds("u1").first())
    }

    @Test
    fun `loadMorePosts is a no-op when hasMore is false`() = runTest {
        val dataSource = FakeProfileDataSource(
            refreshData = mapOf("u1" to refreshData(posts = listOf(adaPost))),
        )
        val repo = repository(dataSource)
        repo.refresh("u1")

        repo.loadMorePosts("u1")

        assertEquals(listOf("p1"), repo.postIds("u1").first())
    }
}

private fun refreshData(posts: List<Post> = emptyList()) = ProfileRefreshData(
    postsCount = posts.size,
    posts = posts, postCursor = null, postHasMore = false,
)

private fun post(id: String, authorId: String) = Post(
    id = id,
    authorId = authorId,
    title = "Title $id",
    body = "Body $id",
    createdAt = Instant.EPOCH,
    likeCount = 0,
    commentCount = 0,
)
