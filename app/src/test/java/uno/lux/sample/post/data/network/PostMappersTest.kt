package uno.lux.sample.post.data.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import uno.lux.sample.post.data.network.AlbumDto
import uno.lux.sample.comment.data.network.CommentDto
import uno.lux.sample.post.data.network.PostFeedItemDto
import uno.lux.sample.user.data.network.UserDto
import uno.lux.sample.post.data.network.VideoDto
import uno.lux.sample.testing.testPostUrl
import java.time.Instant

/**
 * Covers what Mappie can't verify structurally on its own — the nested album/video mapping and the
 * author routed through the manual UserDto mapper. (The createdAt timestamp arrives already parsed
 * from the DTO layer; its parsing is covered by [uno.lux.sample.data.network.dto.InstantSerializerTest].)
 */
class NetworkMappersTest {

    @Test
    fun `PostMapper carries scalar fields across unchanged`() {
        val createdAt = Instant.parse("2025-06-01T12:00:00Z")

        val post = PostMapper.map(postDto(createdAt = createdAt))

        assertEquals("p1", post.id)
        assertEquals(testPostUrl("p1"), post.url)
        assertEquals("u1", post.authorId)
        assertEquals("Title", post.title)
        assertEquals("Body", post.body)
        assertEquals(createdAt, post.createdAt)
        assertEquals(6, post.likeCount)
        assertEquals(2, post.commentCount)
        assertEquals(true, post.isLiked)
        assertEquals(true, post.isBookmarked)
    }

    @Test
    fun `PostMapper maps a nested album via AlbumMapper`() {
        val dto = postDto(
            album = AlbumDto(id = "a1", title = "Trip", itemCount = 9, images = listOf("i1", "i2")),
        )

        val album = PostMapper.map(dto).album!!

        assertEquals("a1", album.id)
        assertEquals("Trip", album.title)
        assertEquals(9, album.itemCount)
        assertEquals(listOf("i1", "i2"), album.images)
    }

    @Test
    fun `PostMapper maps a nested video via VideoMapper`() {
        val dto = postDto(
            video = VideoDto(id = "v1", title = "Clip", durationSeconds = 42, videoUrl = "http://v"),
        )

        val video = PostMapper.map(dto).video!!

        assertEquals("v1", video.id)
        assertEquals(42, video.durationSeconds)
        assertEquals("http://v", video.videoUrl)
    }

    @Test
    fun `PostMapper leaves album and video null when absent`() {
        val post = PostMapper.map(postDto())

        assertNull(post.album)
        assertNull(post.video)
    }

    @Test
    fun `CommentMapper maps the author through the manual UserDto mapper`() {
        val dto = CommentDto(
            id = "c1",
            text = "Hello",
            createdAt = Instant.parse("2025-06-01T12:00:00Z"),
            likeCount = 2,
            isLiked = true,
            author = UserDto(
                id = "u1",
                nickname = "Ada",
                handle = "@ada",
                followerCount = 5,
                followingCount = 1,
                isFollowing = true,
            ),
        )

        val comment = CommentMapper.map(dto)

        assertEquals("u1", comment.author.id)
        assertEquals("Ada", comment.author.nickname)
        assertEquals(true, comment.author.isFollowing)
        assertEquals(dto.createdAt, comment.createdAt)
    }

    private fun postDto(
        createdAt: Instant = Instant.parse("2025-01-01T00:00:00Z"),
        album: AlbumDto? = null,
        video: VideoDto? = null,
    ) = PostFeedItemDto(
        id = "p1",
        url = testPostUrl("p1"),
        title = "Title",
        body = "Body",
        createdAt = createdAt,
        authorId = "u1",
        likeCount = 6,
        commentCount = 2,
        isLiked = true,
        isBookmarked = true,
        album = album,
        video = video,
    )
}
