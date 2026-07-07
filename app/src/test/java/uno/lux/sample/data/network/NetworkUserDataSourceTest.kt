package uno.lux.sample.data.network

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import uno.lux.sample.data.user.AvatarUpload
import uno.lux.sample.data.user.ProfileUpdate

class NetworkUserDataSourceTest {

    @Test
    fun `fetch returns the user mapped from the API response`() = runTest {
        val api = FakeMosaicApi(userById = mapOf("u1" to userDto("u1", "Ada")))
        val dataSource = NetworkUserDataSource(api)

        val user = dataSource.fetch("u1")!!

        assertEquals("u1", user.id)
        assertEquals("Ada", user.nickname)
    }

    @Test
    fun `fetch maps all user fields`() = runTest {
        val dto = userDto("u1", "Ada").copy(
            handle = "@ada",
            age = 30,
            gender = "Woman",
            location = "London",
            bio = "Mathematician",
            avatarUrl = "https://example.com/ada.jpg",
            followerCount = 500,
            followingCount = 10,
        )
        val api = FakeMosaicApi(userById = mapOf("u1" to dto))
        val dataSource = NetworkUserDataSource(api)

        val user = dataSource.fetch("u1")!!

        assertEquals("@ada", user.handle)
        assertEquals(30, user.age)
        assertEquals("Woman", user.gender)
        assertEquals("London", user.location)
        assertEquals("Mathematician", user.bio)
        assertEquals("https://example.com/ada.jpg", user.avatarUrl)
        assertEquals(500, user.followerCount)
        assertEquals(10, user.followingCount)
    }

    @Test
    fun `update sends text fields as multipart parts and returns the updated user`() = runTest {
        val api = FakeMosaicApi(userById = mapOf("u1" to userDto("u1", "Ada")))
        val dataSource = NetworkUserDataSource(api)

        val user = dataSource.update(
            "u1",
            ProfileUpdate(
                nickname = "Ada King",
                age = 37,
                gender = "Woman",
                bio = null,
                avatar = null,
            ),
        )

        assertEquals("Ada King", user.nickname)
        assertEquals(37, user.age)
        assertEquals("Woman", user.gender)
        assertNull(user.bio) // sent as an empty part, cleared server-side
        assertNull("no avatar part when none was chosen", api.lastAvatarPart)
    }

    @Test
    fun `update uploads the avatar as a file part`() = runTest {
        val api = FakeMosaicApi(userById = mapOf("u1" to userDto("u1", "Ada")))
        val dataSource = NetworkUserDataSource(api)

        val user = dataSource.update(
            "u1",
            ProfileUpdate(
                nickname = "Ada",
                age = null,
                gender = null,
                bio = null,
                avatar = AvatarUpload(
                    bytes = byteArrayOf(1, 2, 3, 4),
                    mimeType = "image/png",
                    filename = "avatar.png",
                ),
            ),
        )

        assertNotNull("an avatar file part is sent", api.lastAvatarPart)
        assertEquals(UPLOADED_AVATAR_URL, user.avatarUrl)
    }
}
