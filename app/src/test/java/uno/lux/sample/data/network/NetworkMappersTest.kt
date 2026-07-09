package uno.lux.sample.data.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import uno.lux.sample.data.network.dto.AlbumDto
import uno.lux.sample.data.network.dto.CommentDto
import uno.lux.sample.data.network.dto.PostFeedItemDto
import uno.lux.sample.data.network.dto.UserDto
import uno.lux.sample.data.network.dto.VideoDto
import java.time.Instant

/**
 * Covers the conversions Mappie can't infer by name — the ISO string → [Instant] parse and the
 * nested/author mappings — since those are what would silently break if a mapper regressed.
 */
class NetworkMappersTest {

    @Test
    fun `PostMapper parses the createdAt string into an Instant`() {
        val dto = postDto(createdAt = "2025-01-01T00:00:00.000Z")

        assertEquals(Instant.parse("2025-01-01T00:00:00.000Z"), PostMapper.map(dto).createdAt)
    }

    @Test
    fun `PostMapper carries scalar fields across unchanged`() {
        val post = PostMapper.map(postDto())

        assertEquals("p1", post.id)
        assertEquals("u1", post.authorId)
        assertEquals("Title", post.title)
        assertEquals("Body", post.body)
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
    fun `CommentMapper parses createdAt and maps the author through the manual UserDto mapper`() {
        val dto = CommentDto(
            id = "c1",
            text = "Hello",
            createdAt = "2025-01-01T00:00:00.000Z",
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

        assertEquals(Instant.parse("2025-01-01T00:00:00.000Z"), comment.createdAt)
        assertEquals("u1", comment.author.id)
        assertEquals("Ada", comment.author.nickname)
        assertEquals(true, comment.author.isFollowing)
    }

    private fun postDto(
        createdAt: String = "2025-01-01T00:00:00.000Z",
        album: AlbumDto? = null,
        video: VideoDto? = null,
    ) = PostFeedItemDto(
        id = "p1",
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
