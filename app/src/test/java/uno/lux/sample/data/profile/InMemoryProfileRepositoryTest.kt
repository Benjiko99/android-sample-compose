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

    private val adaPost = post(id = "p1", authorId = "u1")
    private val gracePost = post(id = "p2", authorId = "u2")

    private val albums = mapOf("u1" to listOf(Album(id = "a1", title = "Sketches", itemCount = 8)))
    private val videos = mapOf("u1" to listOf(Video(id = "v1", title = "Talk", durationSeconds = 95, viewCount = 40, videoUrl = "https://example.test/v1.mp4")))

    private fun repository() = InMemoryProfileRepository(
        postRepository = InMemoryPostRepository(listOf(adaPost, gracePost)),
        albumsByUser = albums,
        videosByUser = videos,
    )

    @Test
    fun `profile exposes the user id, albums and videos`() = runTest {
        val profile = repository().profile("u1").first()

        assertEquals("u1", profile.userId)
        assertEquals(albums.getValue("u1"), profile.albums)
        assertEquals(videos.getValue("u1"), profile.videos)
    }

    @Test
    fun `postsCount reflects the number of posts by that user`() = runTest {
        val profile = repository().profile("u2").first()

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
        assertEquals(0, profile.postsCount)
        assertTrue(profile.albums.isEmpty())
        assertTrue(profile.videos.isEmpty())
    }

    @Test
    fun `postIds returns the ids of posts authored by that user`() = runTest {
        val ids = repository().postIds("u1").first()

        assertEquals(listOf("p1"), ids)
    }

    @Test
    fun `postIds excludes posts by other users`() = runTest {
        val ids = repository().postIds("u2").first()

        assertEquals(listOf("p2"), ids)
    }

    @Test
    fun `postIds returns empty list for unknown user`() = runTest {
        val ids = repository().postIds("nobody").first()

        assertTrue(ids.isEmpty())
    }

    @Test
    fun `postIds updates when the entity store changes`() = runTest {
        val postRepository = InMemoryPostRepository(listOf(adaPost))
        val profileRepository = InMemoryProfileRepository(postRepository)

        val before = profileRepository.postIds("u1").first()
        assertEquals(listOf("p1"), before)

        postRepository.ingest(listOf(post(id = "p3", authorId = "u1")))

        val after = profileRepository.postIds("u1").first()
        assertEquals(2, after.size)
        assertTrue(after.contains("p3"))
    }
}

private fun post(
    id: String,
    authorId: String,
) = Post(
    id = id,
    authorId = authorId,
    title = "Title $id",
    body = "Body $id",
    createdAt = Instant.EPOCH,
    likeCount = 0,
    commentCount = 0,
)
