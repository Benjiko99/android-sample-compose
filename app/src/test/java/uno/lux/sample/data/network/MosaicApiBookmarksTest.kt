package uno.lux.sample.data.network

import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import uno.lux.sample.di.NetworkModule

/**
 * Pins the `GET /users/:id/bookmarks` **wire format** by driving the real Retrofit stack over
 * loopback, against a body captured verbatim from the Rails backend.
 *
 * [NetworkProfileDataSourceTest] fakes [MosaicApi], which proves what the data source does with a
 * response but not that the response parses. The parts client and server have to agree on live
 * here: the request path, the snake_case `page` keys next to the camelCase item fields, and the
 * `included.users` sideload the Saved tab cannot render without.
 */
class MosaicApiBookmarksTest {

    private lateinit var server: MockWebServer
    private lateinit var dataSource: NetworkProfileDataSource

    // Captured from the running backend: GET /api/users/u1/bookmarks as u1.
    private val bookmarksJson = """
        {"data":[
          {"id":"p2","title":"Found the bug","body":"It was an actual moth.",
           "createdAt":"2026-07-20T11:34:00Z","likeCount":342,"commentCount":51,
           "isLiked":false,"isBookmarked":true,"album":null,"video":null,"authorId":"u2"},
          {"id":"p4","title":"Priority scheduling saved the landing","body":"A 1202 alarm.",
           "createdAt":"2026-07-20T06:12:00Z","likeCount":1543,"commentCount":88,
           "isLiked":true,"isBookmarked":true,
           "album":{"id":"pa4","title":"Launch room","itemCount":1,
                    "images":["https://cdn.test/1.jpg"]},
           "video":null,"authorId":"u4"}],
         "page":{"next_cursor":"djE6MTc4NDU0NzI0MDMxNDpwMg","has_more":true},
         "included":{"users":[
           {"id":"u2","handle":"@amazinggrace","nickname":"Grace Hopper",
            "avatarUrl":null,"isFollowing":false},
           {"id":"u4","handle":"@mhamilton","nickname":"Margaret Hamilton",
            "avatarUrl":null,"isFollowing":true}]}}
    """.trimIndent()

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        server.enqueue(MockResponse().setResponseCode(200).setBody(bookmarksJson))

        // Production's own Json, not a copy of its settings — a test that pins the wire format
        // against a config nobody ships would keep passing while the real one drifted.
        val api = Retrofit.Builder()
            .baseUrl(server.url("/api/"))
            .addConverterFactory(
                NetworkModule.provideJson()
                    .asConverterFactory("application/json; charset=UTF-8".toMediaType())
            )
            .build()
            .create(MosaicApi::class.java)

        dataSource = NetworkProfileDataSource(api)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `bookmarks are requested from the user's own bookmarks path`() = runTest {
        dataSource.bookmarks("u1", cursor = "c2")

        val request = server.takeRequest()
        assertEquals("GET", request.method)
        assertEquals("/api/users/u1/bookmarks?cursor=c2&limit=20", request.path)
    }

    @Test
    fun `a real backend response parses into saved posts`() = runTest {
        val page = dataSource.bookmarks("u1", cursor = null)

        assertEquals(listOf("p2", "p4"), page.posts.map { it.id })
        assertTrue(page.posts.all { it.isBookmarked })
        assertEquals(listOf("https://cdn.test/1.jpg"), page.posts[1].album?.images)
    }

    @Test
    fun `the sideloaded authors parse into domain users`() = runTest {
        val page = dataSource.bookmarks("u1", cursor = null)

        assertEquals(listOf("u2", "u4"), page.users.map { it.id })
        assertEquals("Grace Hopper", page.users.first().nickname)
        assertFalse(page.users.first().isFollowing)
        assertTrue(page.users[1].isFollowing)
    }

    @Test
    fun `the snake_case page meta parses into the cursor and hasMore`() = runTest {
        val page = dataSource.bookmarks("u1", cursor = null)

        assertEquals("djE6MTc4NDU0NzI0MDMxNDpwMg", page.cursor)
        assertTrue(page.hasMore)
    }
}
