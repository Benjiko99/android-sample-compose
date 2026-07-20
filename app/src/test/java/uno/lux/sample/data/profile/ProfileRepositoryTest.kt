package uno.lux.sample.data.profile

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import uno.lux.sample.data.post.Post
import uno.lux.sample.data.post.FakePostDataSource
import uno.lux.sample.data.post.PostRepository
import uno.lux.sample.data.user.FakeUserDataSource
import uno.lux.sample.data.user.User
import uno.lux.sample.data.user.UserRepository
import java.time.Instant

class ProfileRepositoryTest {

    private val adaPost = post(id = "p1", authorId = "u1")
    private val gracePost = post(id = "p2", authorId = "u2")
    private val grace = User(id = "u2", nickname = "Grace", handle = "@grace")

    private fun bookmarksDataSource(
        posts: List<Post>,
        users: List<User> = emptyList(),
    ) = FakeProfileDataSource(
        bookmarks = mapOf("u1" to mapOf(null to PostsWithAuthorsPage(posts, users, null, false))),
    )

    private fun postRepo() = PostRepository(FakePostDataSource())

    private fun userRepo() = UserRepository(FakeUserDataSource())

    private fun repository(
        dataSource: ProfileDataSource = FakeProfileDataSource(),
        postRepo: PostRepository = postRepo(),
        userRepo: UserRepository = userRepo(),
    ) = ProfileRepository(dataSource, postRepo, userRepo)

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

    // ── Bookmarks (the Saved tab) ───────────────────────────────────────────────

    @Test
    fun `bookmarkIds is null before the Saved tab asks for them`() = runTest {
        val dataSource = FakeProfileDataSource()
        val repo = repository(dataSource)

        repo.refresh("u1")

        assertNull(repo.bookmarkIds("u1").first())
        assertFalse(repo.hasLoadedBookmarks("u1"))
        // A profile load must not reach for a list nobody has opened.
        assertTrue(dataSource.bookmarkCalls.isEmpty())
    }

    @Test
    fun `refreshBookmarks populates bookmarkIds`() = runTest {
        val repo = repository(bookmarksDataSource(listOf(gracePost)))

        repo.refreshBookmarks("u1")

        assertEquals(listOf("p2"), repo.bookmarkIds("u1").first())
        assertTrue(repo.hasLoadedBookmarks("u1"))
    }

    // An empty list is still a loaded one — that is what tells the Saved tab to show
    // "nothing saved" rather than keep spinning.
    @Test
    fun `refreshBookmarks marks an empty result as loaded`() = runTest {
        val repo = repository(bookmarksDataSource(emptyList()))

        repo.refreshBookmarks("u1")

        assertEquals(emptyList<String>(), repo.bookmarkIds("u1").first())
        assertTrue(repo.hasLoadedBookmarks("u1"))
    }

    @Test
    fun `refreshBookmarks ingests the saved posts and their authors`() = runTest {
        val postRepo = postRepo()
        val userRepo = userRepo()
        val repo = repository(bookmarksDataSource(listOf(gracePost), listOf(grace)), postRepo, userRepo)

        repo.refreshBookmarks("u1")

        assertEquals("p2", postRepo.entities.first()["p2"]?.id)
        // Saved posts are by arbitrary authors, so they arrive with the page.
        assertEquals(grace, userRepo.users.first()["u2"])
    }

    @Test
    fun `loadMoreBookmarks appends the next page`() = runTest {
        val p3 = post(id = "p3", authorId = "u3")
        val dataSource = FakeProfileDataSource(
            bookmarks = mapOf(
                "u1" to mapOf(
                    null to PostsWithAuthorsPage(listOf(gracePost), listOf(grace), "c2", true),
                    "c2" to PostsWithAuthorsPage(listOf(p3), emptyList(), null, false),
                ),
            ),
        )
        val repo = repository(dataSource)
        repo.refreshBookmarks("u1")

        repo.loadMoreBookmarks("u1")

        assertEquals(listOf("p2", "p3"), repo.bookmarkIds("u1").first())
        assertFalse(repo.hasMoreBookmarks("u1").first())
    }

    @Test
    fun `loadMoreBookmarks is a no-op when hasMore is false`() = runTest {
        val dataSource = bookmarksDataSource(listOf(gracePost))
        val repo = repository(dataSource)
        repo.refreshBookmarks("u1")

        repo.loadMoreBookmarks("u1")

        assertEquals(1, dataSource.bookmarkCalls.size)
    }

    @Test
    fun `loadMoreBookmarks is a no-op before the first page has loaded`() = runTest {
        val dataSource = bookmarksDataSource(listOf(gracePost))
        val repo = repository(dataSource)

        repo.loadMoreBookmarks("u1")

        assertTrue(dataSource.bookmarkCalls.isEmpty())
        assertNull(repo.bookmarkIds("u1").first())
    }

    // The saved posts resolve through PostRepository like the profile's own, so a like
    // toggled anywhere lands in the Saved tab without this repository being involved.
    @Test
    fun `a saved post reflects a like toggled through the post repository`() = runTest {
        val postRepo = postRepo()
        val repo = repository(bookmarksDataSource(listOf(gracePost), listOf(grace)), postRepo)
        repo.refreshBookmarks("u1")

        postRepo.toggleLike("p2")

        assertTrue(postRepo.entities.first().getValue("p2").isLiked)
    }

    // ── Likes (the public tab) ──────────────────────────────────────────────────

    @Test
    fun `likeIds is null before the Likes tab asks for them`() = runTest {
        val dataSource = FakeProfileDataSource()
        val repo = repository(dataSource)

        repo.refresh("u1")

        assertNull(repo.likeIds("u1").first())
        assertFalse(repo.hasLoadedLikes("u1"))
        assertTrue(dataSource.likeCalls.isEmpty())
    }

    @Test
    fun `refreshLikes populates likeIds and ingests the authors`() = runTest {
        val userRepo = userRepo()
        val dataSource = FakeProfileDataSource(
            likes = mapOf(
                "u1" to mapOf(null to PostsWithAuthorsPage(listOf(gracePost), listOf(grace), null, false)),
            ),
        )
        val repo = repository(dataSource, userRepo = userRepo)

        repo.refreshLikes("u1")

        assertEquals(listOf("p2"), repo.likeIds("u1").first())
        assertEquals(grace, userRepo.users.first()["u2"])
    }

    // The two tabs are separate lists behind separate endpoints; filling one must leave
    // the other untouched and still unloaded.
    @Test
    fun `likes and bookmarks are independent lists`() = runTest {
        val dataSource = FakeProfileDataSource(
            likes = mapOf(
                "u1" to mapOf(null to PostsWithAuthorsPage(listOf(gracePost), emptyList(), null, false)),
            ),
            bookmarks = mapOf(
                "u1" to mapOf(null to PostsWithAuthorsPage(listOf(adaPost), emptyList(), null, false)),
            ),
        )
        val repo = repository(dataSource)

        repo.refreshLikes("u1")

        assertEquals(listOf("p2"), repo.likeIds("u1").first())
        assertNull(repo.bookmarkIds("u1").first())
        assertTrue(dataSource.bookmarkCalls.isEmpty())
    }

    @Test
    fun `loadMoreLikes appends the next page`() = runTest {
        val p3 = post(id = "p3", authorId = "u3")
        val dataSource = FakeProfileDataSource(
            likes = mapOf(
                "u1" to mapOf(
                    null to PostsWithAuthorsPage(listOf(gracePost), listOf(grace), "c2", true),
                    "c2" to PostsWithAuthorsPage(listOf(p3), emptyList(), null, false),
                ),
            ),
        )
        val repo = repository(dataSource)
        repo.refreshLikes("u1")

        repo.loadMoreLikes("u1")

        assertEquals(listOf("p2", "p3"), repo.likeIds("u1").first())
        assertFalse(repo.hasMoreLikes("u1").first())
    }

    @Test
    fun `loadMoreLikes is a no-op when hasMore is false`() = runTest {
        val dataSource = FakeProfileDataSource(
            likes = mapOf(
                "u1" to mapOf(null to PostsWithAuthorsPage(listOf(gracePost), emptyList(), null, false)),
            ),
        )
        val repo = repository(dataSource)
        repo.refreshLikes("u1")

        repo.loadMoreLikes("u1")

        assertEquals(1, dataSource.likeCalls.size)
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
