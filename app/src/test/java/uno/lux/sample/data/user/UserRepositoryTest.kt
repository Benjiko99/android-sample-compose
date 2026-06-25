package uno.lux.sample.data.user

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UserRepositoryTest {

    private fun repository(vararg fetchResults: User = emptyArray()) =
        UserRepository(FakeUserDataSource(fetchResults.associateBy { it.id }))

    @Test
    fun `users starts empty`() = runTest {
        assertTrue(repository().users.first().isEmpty())
    }

    @Test
    fun `user returns null for an unknown id`() = runTest {
        assertNull(repository().user("nobody").first())
    }

    @Test
    fun `ingest populates the user cache`() = runTest {
        val repo = repository()
        repo.ingest(listOf(user("u1", "Ada"), user("u2", "Grace")))

        val cache = repo.users.first()
        assertEquals("Ada", cache["u1"]?.nickname)
        assertEquals("Grace", cache["u2"]?.nickname)
    }

    @Test
    fun `ingest merges with existing entries, keeping both`() = runTest {
        val repo = repository()
        repo.ingest(listOf(user("u1", "Ada")))
        repo.ingest(listOf(user("u2", "Grace")))

        assertEquals(2, repo.users.first().size)
    }

    @Test
    fun `ingest overwrites an existing entry when the same id arrives`() = runTest {
        val repo = repository()
        repo.ingest(listOf(user("u1", "Ada")))
        repo.ingest(listOf(user("u1", "Ada Lovelace")))

        assertEquals("Ada Lovelace", repo.users.first()["u1"]?.nickname)
    }

    @Test
    fun `refresh fetches a user and adds them to the cache`() = runTest {
        val repo = repository(user("u1", "Ada"))

        repo.refresh("u1")

        assertEquals("Ada", repo.user("u1").first()?.nickname)
    }

    @Test
    fun `user flow emits the user after a subsequent ingest`() = runTest {
        val repo = repository()
        assertNull(repo.user("u1").first())

        repo.ingest(listOf(user("u1", "Ada")))

        assertEquals("Ada", repo.user("u1").first()?.nickname)
    }
}

private fun user(id: String, nickname: String) = User(id = id, nickname = nickname, handle = "@$id")

