package uno.lux.sample.post.data.network

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import uno.lux.sample.common.data.ReportReason
import uno.lux.sample.common.data.files.FileUpload
import uno.lux.sample.common.data.network.LikeToggleDto
import uno.lux.sample.post.data.domain.NewPost
import uno.lux.sample.post.data.domain.NewPostMedia
import uno.lux.sample.post.data.domain.Post
import uno.lux.sample.testing.testPostUrl
import uno.lux.sample.user.data.network.userDto
import java.net.UnknownHostException
import java.time.Instant

class NetworkPostDataSourceTest {

    private val stubPost = post("p1")

    @Test
    fun `fetch maps the full projection with its author flattened to an id`() = runTest {
        val api = FakePostApi(
            postById = mapOf("p1" to fullPostDto("p1", author = userDto("u7", "Grace"))),
        )
        val dataSource = NetworkPostDataSource(api)

        val fetched = dataSource.fetch("p1")!!

        assertEquals(listOf("p1"), api.fetchedPostIds)
        assertEquals("p1", fetched.post.id)
        assertEquals("u7", fetched.post.authorId)
        assertEquals("Grace", fetched.author.nickname)
    }

    // A 404 is the server saying the post is gone. It has to arrive as an answer the caller can
    // render, not as the exception every other failure raises — the two mean different screens.
    @Test
    fun `fetch returns null for a post the server does not have`() = runTest {
        val dataSource = NetworkPostDataSource(FakePostApi())

        assertNull(dataSource.fetch("gone"))
    }

    @Test
    fun `fetch lets any other failure through`() = runTest {
        val api = FakePostApi().apply { getPostError = UnknownHostException("offline") }
        val dataSource = NetworkPostDataSource(api)

        assertThrows(UnknownHostException::class.java) {
            runBlocking { dataSource.fetch("p1") }
        }
    }

    @Test
    fun `create sends the draft and maps the created post with its author flattened to an id`() = runTest {
        val api = FakePostApi()
        val dataSource = NetworkPostDataSource(api)

        val created = dataSource.create(NewPost(title = "Title", body = "Body"))

        val parts = api.lastCreatePostParts
        assertEquals("Title", parts?.title)
        assertEquals("Body", parts?.body)
        assertEquals(emptyList<FakePostApi.UploadedPart>(), parts?.images)
        assertEquals("p-new", created.post.id)
        assertEquals("Title", created.post.title)
        assertEquals("Body", created.post.body)
        assertEquals("u1", created.post.authorId)
        assertEquals(0, created.post.likeCount)
    }

    @Test
    fun `create uploads each image as its own part, in order`() = runTest {
        val api = FakePostApi()
        val dataSource = NetworkPostDataSource(api)
        val images = listOf(
            FileUpload(bytes = byteArrayOf(1, 2), mimeType = "image/png", filename = "one.png"),
            FileUpload(bytes = byteArrayOf(3, 4), mimeType = "image/jpeg", filename = "two.jpg"),
        )

        dataSource.create(
            NewPost(title = "Title", body = "Body", media = NewPostMedia.Images(images)),
        )

        val parts = api.lastCreatePostParts
        assertEquals(
            listOf(
                FakePostApi.UploadedPart(filename = "one.png", bytes = byteArrayOf(1, 2)),
                FakePostApi.UploadedPart(filename = "two.jpg", bytes = byteArrayOf(3, 4)),
            ),
            parts?.images,
        )
        // Media is exclusive on the wire, not just in the model.
        assertNull(parts?.video)
    }

    @Test
    fun `create uploads a video as a single part`() = runTest {
        val api = FakePostApi()
        val dataSource = NetworkPostDataSource(api)
        val clip = FileUpload(bytes = byteArrayOf(9), mimeType = "video/mp4", filename = "clip.mp4")

        dataSource.create(
            NewPost(
                title = "Title",
                body = "Body",
                media = NewPostMedia.Video(clip),
            ),
        )

        val parts = api.lastCreatePostParts
        assertEquals(
            FakePostApi.UploadedPart(filename = "clip.mp4", bytes = byteArrayOf(9)),
            parts?.video,
        )
        assertEquals(emptyList<FakePostApi.UploadedPart>(), parts?.images)
    }

    @Test
    fun `create returns the embedded author as a domain user`() = runTest {
        val dataSource = NetworkPostDataSource(FakePostApi())

        val created = dataSource.create(NewPost(title = "Title", body = "Body"))

        assertEquals("u1", created.author.id)
        assertEquals("Ada", created.author.nickname)
    }

    @Test
    fun `delete asks the API to remove that post`() = runTest {
        val api = FakePostApi()
        val dataSource = NetworkPostDataSource(api)

        dataSource.delete("p1")

        assertEquals(listOf("p1"), api.deletedPostIds)
    }

    @Test
    fun `toggleLike returns post with updated isLiked and likeCount from the server response`() = runTest {
        val api = FakePostApi(likeResult = LikeToggleDto(isLiked = true, likeCount = 6))
        val dataSource = NetworkPostDataSource(api)

        val result = dataSource.toggleLike(stubPost.copy(isLiked = false, likeCount = 5))

        assertTrue(result.isLiked)
        assertEquals(6, result.likeCount)
    }

    @Test
    fun `toggleBookmark returns post with updated isBookmarked from the server response`() = runTest {
        val api = FakePostApi(bookmarkResult = BookmarkToggleDto(isBookmarked = true))
        val dataSource = NetworkPostDataSource(api)

        val result = dataSource.toggleBookmark(stubPost.copy(isBookmarked = false))

        assertTrue(result.isBookmarked)
    }

    @Test
    fun `report sends the reason in the server's own spelling, with the details`() = runTest {
        val api = FakePostApi()
        val dataSource = NetworkPostDataSource(api)

        dataSource.report("p1", ReportReason.HATE_SPEECH, "Read the third paragraph")

        assertEquals(
            listOf(
                "p1" to ReportPostRequestDto(
                    reason = ReportReasonDto.HATE_SPEECH,
                    details = "Read the third paragraph",
                ),
            ),
            api.reports,
        )
    }

    // The free-text field is optional, so typing nothing has to reach the server as an absent
    // field rather than an empty one it would have to treat as absent anyway.
    @Test
    fun `report omits details the reporter left blank`() = runTest {
        val api = FakePostApi()
        val dataSource = NetworkPostDataSource(api)

        dataSource.report("p1", ReportReason.SPAM, "   ")

        assertNull(
            api.reports
                .single()
                .second.details,
        )
    }

    @Test
    fun `toggleLike preserves all other fields on the post`() = runTest {
        val api = FakePostApi(likeResult = LikeToggleDto(isLiked = true, likeCount = 1))
        val dataSource = NetworkPostDataSource(api)
        val original = stubPost.copy(isLiked = false, likeCount = 0)

        val result = dataSource.toggleLike(original)

        assertEquals(original.id, result.id)
        assertEquals(original.authorId, result.authorId)
        assertEquals(original.title, result.title)
        assertEquals(original.isBookmarked, result.isBookmarked)
    }
}

private fun post(id: String) = Post(
    id = id,
    url = testPostUrl(id),
    authorId = "u1",
    title = "Title $id",
    body = "Body $id",
    createdAt = Instant.EPOCH,
    likeCount = 0,
    commentCount = 0,
)
