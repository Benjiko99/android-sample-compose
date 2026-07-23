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
import uno.lux.sample.data.user.UserId
import uno.lux.sample.data.user.UserRepository
import uno.lux.sample.utils.testPostUrl
import java.time.Instant

class ProfileRepositoryTest {

    private val adaPost = post(id = "p1", authorId = "u1")
    private val gracePost = post(
        id = "p2",
        authorId = "u2",
        createdAt = newer,
        isLiked = true,
        isBookmarked = true,
    )
    private val grace = User(id = "u2", nickname = "Grace", handle = "@grace")

    private fun bookmarksDataSource(
        posts: List<Post>,
        users: List<User> = emptyList(),
    ) = FakeProfileDataSource(
        bookmarks = mapOf("u1" to mapOf(null to PostsWithAuthorsPage(posts, users, null, false))),
    )

    private fun likesDataSource(
        posts: List<Post>,
        users: List<User> = emptyList(),
        userId: UserId = "u1",
        cursor: String? = null,
        hasMore: Boolean = false,
    ) = FakeProfileDataSource(
        likes = mapOf(userId to mapOf(null to PostsWithAuthorsPage(posts, users, cursor, hasMore))),
    )

    private fun userRepo() = UserRepository(FakeUserDataSource())

    private fun postRepo(userRepo: UserRepository = userRepo()) =
        PostRepository(FakePostDataSource(), userRepo)

    private fun repository(
        dataSource: ProfileDataSource = FakeProfileDataSource(),
        postRepo: PostRepository = postRepo(),
        userRepo: UserRepository = userRepo(),
        currentUserId: UserId = "u1",
    ) = ProfileRepository(dataSource, postRepo, userRepo, currentUserId)

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
        val p3 = post(id = "p3", authorId = "u3", createdAt = older, isBookmarked = true)
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
        val savedButUnliked = post(id = "p2", authorId = "u2", isBookmarked = true)
        val postRepo = postRepo()
        val repo = repository(bookmarksDataSource(listOf(savedButUnliked), listOf(grace)), postRepo)
        repo.refreshBookmarks("u1")

        postRepo.toggleLike("p2")

        assertTrue(postRepo.entities.first().getValue("p2").isLiked)
        // The like is not what puts a post on the Saved tab, so the row stays.
        assertEquals(listOf("p2"), repo.bookmarkIds("u1").first())
    }

    // Membership moves both ways: the list is derived from the entity rather than a copy of it,
    // so un-saving drops the row and saving again is a free undo of the accidental tap.
    @Test
    fun `bookmarkIds follows the bookmark flag in both directions`() = runTest {
        val postRepo = postRepo()
        val repo = repository(bookmarksDataSource(listOf(gracePost), listOf(grace)), postRepo)
        repo.refreshBookmarks("u1")

        postRepo.toggleBookmark("p2")
        assertEquals(emptyList<String>(), repo.bookmarkIds("u1").first())

        postRepo.toggleBookmark("p2")
        assertEquals(listOf("p2"), repo.bookmarkIds("u1").first())
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
        val repo = repository(likesDataSource(listOf(gracePost), listOf(grace)), userRepo = userRepo)

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
        val p3 = post(id = "p3", authorId = "u3", createdAt = older, isLiked = true)
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
        val dataSource = likesDataSource(listOf(gracePost))
        val repo = repository(dataSource)
        repo.refreshLikes("u1")

        repo.loadMoreLikes("u1")

        assertEquals(1, dataSource.likeCalls.size)
    }

    @Test
    fun `unliking drops the post from likeIds`() = runTest {
        val postRepo = postRepo()
        val repo = repository(likesDataSource(listOf(gracePost), listOf(grace)), postRepo)
        repo.refreshLikes("u1")

        postRepo.toggleLike("p2")

        assertEquals(emptyList<String>(), repo.likeIds("u1").first())
    }

    // `isLiked` describes the *viewer*, so on someone else's Likes tab it says nothing about why
    // a post is on the list — narrowing there would hide posts the owner still likes.
    @Test
    fun `another user's likeIds are not narrowed by the viewer's own like state`() = runTest {
        val unlikedByViewer = post(id = "p4", authorId = "u3", isLiked = false)
        val repo = repository(
            likesDataSource(listOf(unlikedByViewer), userId = "u2"),
            currentUserId = "u1",
        )

        repo.refreshLikes("u2")

        assertEquals(listOf("p4"), repo.likeIds("u2").first())
    }

    // The point of deriving the list: a post liked anywhere else — the Posts tab, the feed — joins
    // the Likes tab straight away, not only if it happened to be in a page already fetched.
    @Test
    fun `liking a post not in the fetched page adds it to likeIds`() = runTest {
        val postRepo = postRepo()
        postRepo.ingest(listOf(adaPost))
        val dataSource = FakeProfileDataSource(
            likes = mapOf("u1" to mapOf(null to PostsWithAuthorsPage(emptyList(), emptyList(), null, false))),
        )
        val repo = repository(dataSource, postRepo)
        repo.refreshLikes("u1")

        postRepo.toggleLike("p1")

        assertEquals(listOf("p1"), repo.likeIds("u1").first())
    }

    // It lands in the server's own order rather than at whichever end is convenient, so the tab
    // reads the same before and after the next refresh.
    @Test
    fun `a newly liked post is inserted in keyset order`() = runTest {
        val postRepo = postRepo()
        val middle = post(id = "p9", authorId = "u3", createdAt = older.plusSeconds(50))
        postRepo.ingest(listOf(middle))
        val newest = post(id = "p3", authorId = "u3", createdAt = older, isLiked = true)
        val dataSource = FakeProfileDataSource(
            likes = mapOf(
                "u1" to mapOf(null to PostsWithAuthorsPage(listOf(gracePost, newest), emptyList(), null, false)),
            ),
        )
        val repo = repository(dataSource, postRepo)
        repo.refreshLikes("u1")

        postRepo.toggleLike("p9")

        assertEquals(listOf("p2", "p9", "p3"), repo.likeIds("u1").first())
    }

    // A post older than everything loaded belongs to a page the server has not sent. Slotting it
    // in at the end would put it ahead of posts that outrank it, so it waits for its page.
    @Test
    fun `a liked post below the loaded window waits for its page`() = runTest {
        val postRepo = postRepo()
        val ancient = post(id = "p9", authorId = "u3", createdAt = Instant.EPOCH)
        postRepo.ingest(listOf(ancient))
        val dataSource = FakeProfileDataSource(
            likes = mapOf(
                "u1" to mapOf(null to PostsWithAuthorsPage(listOf(gracePost), emptyList(), "c2", true)),
            ),
        )
        val repo = repository(dataSource, postRepo)
        repo.refreshLikes("u1")

        postRepo.toggleLike("p9")

        assertEquals(listOf("p2"), repo.likeIds("u1").first())
    }

    // Someone else's Likes tab is echoed as fetched: the viewer's own flags describe the viewer,
    // so liking a post of your own must not add it to their list.
    @Test
    fun `liking a post does not add it to another user's likeIds`() = runTest {
        val postRepo = postRepo()
        postRepo.ingest(listOf(adaPost))
        val dataSource = FakeProfileDataSource(
            likes = mapOf(
                "u2" to mapOf(null to PostsWithAuthorsPage(listOf(gracePost), emptyList(), null, false)),
            ),
        )
        val repo = repository(dataSource, postRepo, currentUserId = "u1")
        repo.refreshLikes("u2")

        postRepo.toggleLike("p1")

        assertEquals(listOf("p2"), repo.likeIds("u2").first())
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

// These lists come back in (createdAt, id) descending order, so a later page is always older
// than the one before it. Fixtures spanning pages need distinct timestamps to be realistic.
private val newer: Instant = Instant.EPOCH.plusSeconds(200)
private val older: Instant = Instant.EPOCH.plusSeconds(100)

private fun post(
    id: String,
    authorId: UserId,
    createdAt: Instant = Instant.EPOCH,
    isLiked: Boolean = false,
    isBookmarked: Boolean = false,
) = Post(
    id = id,
    url = testPostUrl(id),
    authorId = authorId,
    title = "Title $id",
    body = "Body $id",
    createdAt = createdAt,
    likeCount = 0,
    commentCount = 0,
    isLiked = isLiked,
    isBookmarked = isBookmarked,
)
