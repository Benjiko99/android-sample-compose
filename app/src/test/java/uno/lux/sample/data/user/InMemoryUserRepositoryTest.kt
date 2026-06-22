package uno.lux.sample.data.user

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import uno.lux.sample.data.user.User

class InMemoryUserRepositoryTest {

    private val ada = User(id = "u1", nickname = "Ada", handle = "@ada")
    private val grace = User(id = "u2", nickname = "Grace", handle = "@grace")

    private fun repository() = InMemoryUserRepository(listOf(ada, grace))

    @Test
    fun `user returns the matching user for a known id`() = runTest {
        assertEquals(ada, repository().user("u1").first())
    }

    @Test
    fun `user returns null for an unknown id`() = runTest {
        assertNull(repository().user("nobody").first())
    }

    @Test
    fun `users emits all known users keyed by id`() = runTest {
        val map = repository().users.first()
        assertEquals(mapOf("u1" to ada, "u2" to grace), map)
    }
}
