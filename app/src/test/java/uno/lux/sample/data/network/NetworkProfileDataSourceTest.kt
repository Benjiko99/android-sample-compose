package uno.lux.sample.data.network

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import uno.lux.sample.data.network.dto.CursorPageDto
import uno.lux.sample.data.network.dto.MinimalUserDto
import uno.lux.sample.data.network.dto.PostFeedItemDto
import uno.lux.sample.data.network.response.BookmarksResponse
import uno.lux.sample.data.network.response.FeedIncluded

class NetworkProfileDataSourceTest {

    private fun bookmarksResponse(
        data: List<PostFeedItemDto>,
        users: List<MinimalUserDto> = emptyList(),
        page: CursorPageDto = emptyPage,
    ) = BookmarksResponse(data = data, included = FeedIncluded(users = users), page = page)

    @Test
    fun `bookmarks maps post DTOs to domain posts`() = runTest {
        val api = FakeMosaicApi(
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
        val api = FakeMosaicApi(
            bookmarksResponse = bookmarksResponse(
                data = listOf(feedItemDto("p2", "u2")),
                users = listOf(minimalUserDto("u2", "Grace", isFollowing = true)),
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
        val api = FakeMosaicApi()

        NetworkProfileDataSource(api).bookmarks("u1", cursor = "c2")

        assertEquals(listOf("u1" to "c2"), api.bookmarkCalls)
    }

    @Test
    fun `bookmarks propagates cursor and hasMore from the page`() = runTest {
        val api = FakeMosaicApi(
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
        val result = NetworkProfileDataSource(FakeMosaicApi()).bookmarks("u1", cursor = null)

        assertTrue(result.posts.isEmpty())
        assertNull(result.cursor)
    }
}
