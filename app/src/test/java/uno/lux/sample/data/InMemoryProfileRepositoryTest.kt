package uno.lux.sample.data

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class InMemoryProfileRepositoryTest {

    private val ada = User(id = "u1", nickname = "Ada", handle = "@ada")
    private val grace = User(id = "u2", nickname = "Grace", handle = "@grace")

    private val adaPost = post(id = "p1", author = ada, likeCount = 10)
    private val gracePost = post(id = "p2", author = grace)

    private val albums = mapOf("u1" to listOf(Album(id = "a1", title = "Sketches", itemCount = 8)))
    private val videos = mapOf("u1" to listOf(Video(id = "v1", title = "Talk", durationSeconds = 95, viewCount = 40, videoUrl = "https://example.test/v1.mp4")))

    private fun repository() = InMemoryProfileRepository(
        users = listOf(ada, grace),
        postRepository = InMemoryPostRepository(listOf(adaPost, gracePost)),
        albumsByUser = albums,
        videosByUser = videos,
    )

    @Test
    fun `profile gathers the user, their posts, albums and videos`() = runTest {
        val profile = repository().profile("u1").first()!!

        assertEquals(ada, profile.user)
        assertEquals(listOf(adaPost), profile.posts)
        assertEquals(albums.getValue("u1"), profile.albums)
        assertEquals(videos.getValue("u1"), profile.videos)
    }

    @Test
    fun `profile posts are limited to that author`() = runTest {
        val profile = repository().profile("u2").first()!!

        assertEquals(listOf(gracePost), profile.posts)
        assertEquals(1, profile.postsCount)
    }

    @Test
    fun `a user with no albums or videos gets empty tabs`() = runTest {
        val profile = repository().profile("u2").first()!!

        assertTrue(profile.albums.isEmpty())
        assertTrue(profile.videos.isEmpty())
    }

    @Test
    fun `an unknown user id emits null`() = runTest {
        assertNull(repository().profile("nobody").first())
    }

    @Test
    fun `toggleLike is reflected in the streamed profile`() = runTest {
        val repository = repository()

        repository.toggleLike("p1")

        val liked = repository.profile("u1").first()!!.posts.single()
        assertTrue(liked.isLiked)
        assertEquals(11, liked.likeCount)
    }

    @Test
    fun `toggleBookmark is reflected in the streamed profile`() = runTest {
        val repository = repository()

        repository.toggleBookmark("p1")

        assertTrue(repository.profile("u1").first()!!.posts.single().isBookmarked)
    }
}

private fun post(
    id: String,
    author: User,
    likeCount: Int = 0,
) = Post(
    id = id,
    author = author,
    title = "Title $id",
    body = "Body $id",
    createdAt = Instant.EPOCH,
    likeCount = likeCount,
    commentCount = 0,
)
