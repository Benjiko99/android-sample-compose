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
import uno.lux.sample.data.file.FileUpload
import uno.lux.sample.data.post.NewPost
import uno.lux.sample.data.post.NewPostMedia
import uno.lux.sample.di.NetworkModule

/**
 * Pins the `POST /posts` **wire format** by driving the real Retrofit stack over loopback.
 *
 * The other data-source tests swap in a fake [MosaicApi], which proves what the data source asks
 * for but not what Retrofit actually serializes — and the multipart details are exactly where the
 * client and the Rails backend have to agree: the `images[]` part name Rack needs to build an
 * array, and the omission (rather than empty encoding) of the optional video parts. Both are
 * invisible to a faked API and would only surface as a failed upload against a real server.
 */
class MosaicApiMultipartTest {

    private lateinit var server: MockWebServer
    private lateinit var dataSource: NetworkPostDataSource

    private val createdPostJson = """
        {"data":{"id":"p-new","title":"T","body":"B",
        "createdAt":"2025-01-01T00:00:00.000Z",
        "author":{"id":"u1","nickname":"Ada","handle":"@countess","followerCount":0,
                  "followingCount":0,"isFollowing":false},
        "likeCount":0,"commentCount":0,"isLiked":false,"isBookmarked":false}}
    """.trimIndent()

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        server.enqueue(MockResponse().setResponseCode(201).setBody(createdPostJson))

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

        dataSource = NetworkPostDataSource(api)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private fun upload(name: String, mimeType: String) =
        FileUpload(bytes = name.toByteArray(), mimeType = mimeType, filename = name)

    @Test
    fun `images are sent as repeated 'images' array parts and no video parts`() = runTest {
        dataSource.create(
            NewPost(
                title = "T",
                body = "B",
                media = NewPostMedia.Images(
                    listOf(upload("one.png", "image/png"), upload("two.png", "image/png"))
                ),
            )
        )

        val body = server.takeRequest().body.readUtf8()

        // The trailing brackets are what make Rack collect these into one `images` array.
        assertEquals(2, Regex("""name="images\[\]"""").findAll(body).count())
        assertTrue(body.contains("""filename="one.png""""))
        assertTrue(body.contains("""filename="two.png""""))
        // Omitted entirely rather than sent empty — an empty part would reach the server as a
        // present-but-blank video and trip the media-conflict check.
        assertFalse(body.contains("""name="video""""))
        assertFalse(body.contains("videoDurationSeconds"))
    }

    @Test
    fun `a video is sent as a single part alongside its duration, with no image parts`() = runTest {
        dataSource.create(
            NewPost(
                title = "T",
                body = "B",
                media = NewPostMedia.Video(upload("clip.mp4", "video/mp4"), durationSeconds = 42),
            )
        )

        val body = server.takeRequest().body.readUtf8()

        assertEquals(1, Regex("""name="video"""").findAll(body).count())
        assertTrue(body.contains("""filename="clip.mp4""""))
        assertTrue(body.contains("Content-Type: video/mp4"))
        assertTrue(body.contains("""name="videoDurationSeconds""""))
        assertTrue(body.contains("42"))
        assertFalse(body.contains("images["))
    }

    @Test
    fun `a text-only post sends neither media part`() = runTest {
        dataSource.create(NewPost(title = "T", body = "B"))

        val request = server.takeRequest()
        val body = request.body.readUtf8()

        assertTrue(request.getHeader("Content-Type").orEmpty().startsWith("multipart/form-data"))
        assertTrue(body.contains("""name="title""""))
        assertTrue(body.contains("""name="body""""))
        assertFalse(body.contains("images["))
        assertFalse(body.contains("""name="video""""))
    }
}
