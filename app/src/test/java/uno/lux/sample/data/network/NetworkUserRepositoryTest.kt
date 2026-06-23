package uno.lux.sample.data.network

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NetworkUserRepositoryTest {

    private val repository = NetworkUserRepository(FakeSampleApi())

    @Test
    fun `users starts empty`() = runTest {
        assertTrue(repository.users.first().isEmpty())
    }

    @Test
    fun `user returns null for an unknown id`() = runTest {
        assertNull(repository.user("nobody").first())
    }

    @Test
    fun `ingest populates the user cache`() = runTest {
        repository.ingest(listOf(userDto("u1", "Ada"), userDto("u2", "Grace")))

        val cache = repository.users.first()
        assertEquals("Ada", cache["u1"]?.nickname)
        assertEquals("Grace", cache["u2"]?.nickname)
    }

    @Test
    fun `ingest merges with existing entries, keeping both`() = runTest {
        repository.ingest(listOf(userDto("u1", "Ada")))
        repository.ingest(listOf(userDto("u2", "Grace")))

        assertEquals(2, repository.users.first().size)
    }

    @Test
    fun `ingest overwrites an existing entry when the same id arrives`() = runTest {
        repository.ingest(listOf(userDto("u1", "Ada")))
        repository.ingest(listOf(userDto("u1", "Ada Lovelace")))

        assertEquals("Ada Lovelace", repository.users.first()["u1"]?.nickname)
    }

    @Test
    fun `refresh fetches a user by id and adds them to the cache`() = runTest {
        val api = FakeSampleApi(userById = mapOf("u1" to userDto("u1", "Ada")))
        val repo = NetworkUserRepository(api)

        repo.refresh("u1")

        assertEquals("Ada", repo.user("u1").first()?.nickname)
    }

    @Test
    fun `user flow emits the user after a subsequent ingest`() = runTest {
        assertNull(repository.user("u1").first())

        repository.ingest(listOf(userDto("u1", "Ada")))

        assertEquals("Ada", repository.user("u1").first()?.nickname)
    }
}
