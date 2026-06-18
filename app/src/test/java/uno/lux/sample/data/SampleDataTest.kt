package uno.lux.sample.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SampleDataTest {

    @Test
    fun `every sample video streams from the sample url`() {
        val videos = SampleVideos.values.flatten()

        assertTrue("expected sample videos to exist", videos.isNotEmpty())
        videos.forEach { video ->
            assertEquals(
                "video ${video.id} should use the sample stream",
                SampleVideoUrl,
                video.videoUrl,
            )
        }
    }
}
