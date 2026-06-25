package uno.lux.sample.data.network

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

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
        assertEquals(500, user.followerCount)
        assertEquals(10, user.followingCount)
    }
}
