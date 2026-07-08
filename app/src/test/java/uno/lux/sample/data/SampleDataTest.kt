package uno.lux.sample.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SampleDataTest {

    @Test
    fun `sample video posts carry a video streaming from the sample url`() {
        val videoPosts = SamplePosts.mapNotNull { it.video }

        assertTrue("expected at least one video post", videoPosts.isNotEmpty())
        videoPosts.forEach { video ->
            assertEquals(
                "post video ${video.id} should use the sample stream",
                SampleVideoUrl,
                video.videoUrl,
            )
        }
    }

    @Test
    fun `sample album posts show the sample album images`() {
        val albumPosts = SamplePosts.mapNotNull { it.album }

        assertTrue("expected at least one album post", albumPosts.isNotEmpty())
        albumPosts.forEach { album ->
            assertEquals(
                "album ${album.id} should show the sample images",
                SampleAlbumImages,
                album.images,
            )
        }
    }
}
