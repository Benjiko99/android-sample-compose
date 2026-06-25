package uno.lux.sample.data.post

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import uno.lux.sample.data.user.FakeUserDataSource
import uno.lux.sample.data.user.User
import uno.lux.sample.data.user.UserRepository
import java.time.Instant

class FeedRepositoryTest {

    private fun postRepo() = PostRepository(FakePostDataSource())
    private fun userRepo() = UserRepository(FakeUserDataSource())

    private fun feedRepo(
        dataSource: FeedDataSource = FakeFeedDataSource(),
        postRepo: PostRepository = postRepo(),
        userRepo: UserRepository = userRepo(),
    ) = FeedRepository(dataSource, postRepo, userRepo)

    @Test
    fun `postIds starts empty before any refresh`() = runTest {
        assertTrue(feedRepo().postIds.first().isEmpty())
    }

    @Test
    fun `hasMore is false before any refresh`() = runTest {
        assertFalse(feedRepo().hasMore.first())
    }

    @Test
    fun `refresh populates postIds in feed order`() = runTest {
        val dataSource = FakeFeedDataSource(
            pages = listOf(FeedPage(listOf(post("p1"), post("p2")), emptyList(), null, false)),
        )
        val repo = feedRepo(dataSource)

        repo.refresh()

        assertEquals(listOf("p1", "p2"), repo.postIds.first())
    }

    @Test
    fun `refresh ingests post entities into the post repository`() = runTest {
        val postRepo = postRepo()
        val dataSource = FakeFeedDataSource(
            pages = listOf(FeedPage(listOf(post("p1")), emptyList(), null, false)),
        )
        val repo = feedRepo(dataSource, postRepo)

        repo.refresh()

        assertEquals("p1", postRepo.entities.first()["p1"]?.id)
    }

    @Test
    fun `refresh ingests users into the user repository`() = runTest {
        val userRepo = userRepo()
        val dataSource = FakeFeedDataSource(
            pages = listOf(FeedPage(emptyList(), listOf(user("u1", "Ada")), null, false)),
        )
        val repo = feedRepo(dataSource, userRepo = userRepo)

        repo.refresh()

        assertEquals("Ada", userRepo.user("u1").first()?.nickname)
    }

    @Test
    fun `refresh without users leaves the user repository unchanged`() = runTest {
        val userRepo = userRepo()
        val dataSource = FakeFeedDataSource(
            pages = listOf(FeedPage(listOf(post("p1")), emptyList(), null, false)),
        )
        val repo = feedRepo(dataSource, userRepo = userRepo)

        repo.refresh()

        assertTrue(userRepo.users.first().isEmpty())
    }

    @Test
    fun `hasMore reflects the page flag after refresh`() = runTest {
        val dataSource = FakeFeedDataSource(
            pages = listOf(FeedPage(listOf(post("p1")), emptyList(), "c2", true)),
        )
        val repo = feedRepo(dataSource)

        repo.refresh()

        assertTrue(repo.hasMore.first())
    }

    @Test
    fun `loadMore appends the next page of post IDs`() = runTest {
        val dataSource = FakeFeedDataSource(
            pages = listOf(
                FeedPage(listOf(post("p1")), emptyList(), "c2", true),
                FeedPage(listOf(post("p2")), emptyList(), null, false),
            ),
        )
        val repo = feedRepo(dataSource)
        repo.refresh()

        repo.loadMore()

        assertEquals(listOf("p1", "p2"), repo.postIds.first())
    }

    @Test
    fun `loadMore clears hasMore when the last page is fetched`() = runTest {
        val dataSource = FakeFeedDataSource(
            pages = listOf(
                FeedPage(listOf(post("p1")), emptyList(), "c2", true),
                FeedPage(listOf(post("p2")), emptyList(), null, false),
            ),
        )
        val repo = feedRepo(dataSource)
        repo.refresh()

        repo.loadMore()

        assertFalse(repo.hasMore.first())
    }

    @Test
    fun `loadMore is a no-op when hasMore is false`() = runTest {
        val dataSource = FakeFeedDataSource(
            pages = listOf(FeedPage(listOf(post("p1")), emptyList(), null, false)),
        )
        val repo = feedRepo(dataSource)
        repo.refresh()

        repo.loadMore()

        assertEquals(listOf("p1"), repo.postIds.first())
    }

    @Test
    fun `refresh resets the post list and cursor`() = runTest {
        val page1 = FeedPage(listOf(post("p1")), emptyList(), "c2", true)
        val page2 = FeedPage(listOf(post("p2")), emptyList(), null, false)
        val dataSource = FakeFeedDataSource(pages = listOf(page1, page2, page1))
        val repo = feedRepo(dataSource)
        repo.refresh()
        repo.loadMore()

        repo.refresh()

        assertEquals(listOf("p1"), repo.postIds.first())
        assertTrue(repo.hasMore.first())
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

private fun user(id: String, nickname: String) = User(id = id, nickname = nickname, handle = "@$id")

