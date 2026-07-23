package uno.lux.sample.profile.data.network

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import uno.lux.sample.core.network.CursorPageDto
import uno.lux.sample.core.network.emptyPage
import uno.lux.sample.post.data.network.feedItemDto
import uno.lux.sample.user.data.network.userDto
import uno.lux.sample.feed.data.network.FeedIncluded
import uno.lux.sample.post.data.network.PostFeedItemDto
import uno.lux.sample.user.data.network.UserDto

class NetworkProfileDataSourceTest {

    private fun postsResponse(
        data: List<PostFeedItemDto>,
        users: List<UserDto> = emptyList(),
        page: CursorPageDto = emptyPage,
    ) = PostsWithAuthorsResponse(data = data, included = FeedIncluded(users = users), page = page)

    private fun bookmarksResponse(
        data: List<PostFeedItemDto>,
        users: List<UserDto> = emptyList(),
        page: CursorPageDto = emptyPage,
    ) = postsResponse(data, users, page)

    @Test
    fun `bookmarks maps post DTOs to domain posts`() = runTest {
        val api = FakeProfileApi(
            bookmarksResponse = bookmarksResponse(
                data = listOf(feedItemDto("p2", "u2"), feedItemDto("p4", "u4")),
            ),
        )
        val result = NetworkProfileDataSource(api).bookmarks("u1", cursor = null)

        assertEquals(listOf("p2", "p4"), result.posts.map { it.id })
    }

    // A saved post can be by anyone, so its author always arrives with the page — the caller
    // has no other way to render the card.
    @Test
    fun `bookmarks maps the sideloaded authors to domain users`() = runTest {
        val api = FakeProfileApi(
            bookmarksResponse = bookmarksResponse(
                data = listOf(feedItemDto("p2", "u2")),
                users = listOf(userDto("u2", "Grace", isFollowing = true)),
            ),
        )
        val result = NetworkProfileDataSource(api).bookmarks("u1", cursor = null)

        val author = result.users.single()
        assertEquals("u2", author.id)
        assertEquals("Grace", author.nickname)
        assertTrue(author.isFollowing)
    }

    @Test
    fun `bookmarks requests the given user and cursor`() = runTest {
        val api = FakeProfileApi()

        NetworkProfileDataSource(api).bookmarks("u1", cursor = "c2")

        assertEquals(listOf("u1" to "c2"), api.bookmarkCalls)
    }

    @Test
    fun `bookmarks propagates cursor and hasMore from the page`() = runTest {
        val api = FakeProfileApi(
            bookmarksResponse = bookmarksResponse(
                data = listOf(feedItemDto("p2", "u2")),
                page = CursorPageDto(nextCursor = "c2", hasMore = true),
            ),
        )
        val result = NetworkProfileDataSource(api).bookmarks("u1", cursor = null)

        assertEquals("c2", result.cursor)
        assertTrue(result.hasMore)
    }

    @Test
    fun `bookmarks with an empty page yields no posts and no cursor`() = runTest {
        val result = NetworkProfileDataSource(FakeProfileApi()).bookmarks("u1", cursor = null)

        assertTrue(result.posts.isEmpty())
        assertNull(result.cursor)
    }

    // ── Likes ───────────────────────────────────────────────────────────────────

    @Test
    fun `likes maps post DTOs and their sideloaded authors`() = runTest {
        val api = FakeProfileApi(
            likesResponse = postsResponse(
                data = listOf(feedItemDto("p3", "u3"), feedItemDto("p4", "u4")),
                users = listOf(userDto("u3", "Alan"), userDto("u4", "Margaret")),
            ),
        )
        val result = NetworkProfileDataSource(api).likes("u1", cursor = null)

        assertEquals(listOf("p3", "p4"), result.posts.map { it.id })
        assertEquals(listOf("Alan", "Margaret"), result.users.map { it.nickname })
    }

    // Likes and bookmarks are separate lists behind separate endpoints — asking for one
    // must not touch the other.
    @Test
    fun `likes requests the likes endpoint, not bookmarks`() = runTest {
        val api = FakeProfileApi()

        NetworkProfileDataSource(api).likes("u2", cursor = "c3")

        assertEquals(listOf("u2" to "c3"), api.likeCalls)
        assertTrue(api.bookmarkCalls.isEmpty())
    }

    @Test
    fun `likes propagates cursor and hasMore from the page`() = runTest {
        val api = FakeProfileApi(
            likesResponse = postsResponse(
                data = listOf(feedItemDto("p3", "u3")),
                page = CursorPageDto(nextCursor = "c2", hasMore = true),
            ),
        )
        val result = NetworkProfileDataSource(api).likes("u1", cursor = null)

        assertEquals("c2", result.cursor)
        assertTrue(result.hasMore)
    }
}
