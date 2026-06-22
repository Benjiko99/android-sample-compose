package uno.lux.sample.data.profile

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import uno.lux.sample.data.post.Album
import uno.lux.sample.data.post.InMemoryPostRepository
import uno.lux.sample.data.post.Post
import uno.lux.sample.data.post.Video
import java.time.Instant

class InMemoryProfileRepositoryTest {

    private val adaPost = post(id = "p1", authorId = "u1", likeCount = 10)
    private val gracePost = post(id = "p2", authorId = "u2")

    private val albums = mapOf("u1" to listOf(Album(id = "a1", title = "Sketches", itemCount = 8)))
    private val videos = mapOf("u1" to listOf(Video(id = "v1", title = "Talk", durationSeconds = 95, viewCount = 40, videoUrl = "https://example.test/v1.mp4")))

    private fun repository() = InMemoryProfileRepository(
        postRepository = InMemoryPostRepository(listOf(adaPost, gracePost)),
        albumsByUser = albums,
        videosByUser = videos,
    )

    @Test
    fun `profile exposes the user id, their posts, albums and videos`() = runTest {
        val profile = repository().profile("u1").first()

        assertEquals("u1", profile.userId)
        assertEquals(listOf(adaPost), profile.posts)
        assertEquals(albums.getValue("u1"), profile.albums)
        assertEquals(videos.getValue("u1"), profile.videos)
    }

    @Test
    fun `profile posts are limited to that author`() = runTest {
        val profile = repository().profile("u2").first()

        assertEquals(listOf(gracePost), profile.posts)
        assertEquals(1, profile.postsCount)
    }

    @Test
    fun `a user with no albums or videos gets empty tabs`() = runTest {
        val profile = repository().profile("u2").first()

        assertTrue(profile.albums.isEmpty())
        assertTrue(profile.videos.isEmpty())
    }

    @Test
    fun `an unknown user id returns an empty profile`() = runTest {
        val profile = repository().profile("nobody").first()

        assertEquals("nobody", profile.userId)
        assertTrue(profile.posts.isEmpty())
        assertTrue(profile.albums.isEmpty())
        assertTrue(profile.videos.isEmpty())
    }

    @Test
    fun `toggleLike is reflected in the streamed profile`() = runTest {
        val repository = repository()

        repository.toggleLike("p1")

        val liked = repository.profile("u1").first().posts.single()
        assertTrue(liked.isLiked)
        assertEquals(11, liked.likeCount)
    }

    @Test
    fun `toggleBookmark is reflected in the streamed profile`() = runTest {
        val repository = repository()

        repository.toggleBookmark("p1")

        assertTrue(repository.profile("u1").first().posts.single().isBookmarked)
    }
}

private fun post(
    id: String,
    authorId: String,
    likeCount: Int = 0,
) = Post(
    id = id,
    authorId = authorId,
    title = "Title $id",
    body = "Body $id",
    createdAt = Instant.EPOCH,
    likeCount = likeCount,
    commentCount = 0,
)
