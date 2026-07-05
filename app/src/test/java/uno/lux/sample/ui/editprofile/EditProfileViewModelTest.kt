package uno.lux.sample.ui.editprofile

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import uno.lux.sample.ViewModelTest
import uno.lux.sample.data.user.FakeUserDataSource
import uno.lux.sample.data.user.ProfileUpdate
import uno.lux.sample.data.user.User
import uno.lux.sample.data.user.UserRepository
import uno.lux.sample.util.AppError

@OptIn(ExperimentalCoroutinesApi::class)
class EditProfileViewModelTest : ViewModelTest() {

    private val ada = User(
        id = "u1",
        nickname = "Ada Lovelace",
        handle = "@countess",
        age = 36,
        gender = "Woman",
        bio = "Mathematician & writer.",
    )

    private fun fixture(
        cached: Boolean = true,
        failUpdates: Boolean = false,
    ): Triple<EditProfileViewModel, UserRepository, FakeUserDataSource> {
        val dataSource = FakeUserDataSource(mapOf("u1" to ada), failUpdates = failUpdates)
        val repository = UserRepository(dataSource)
        if (cached) repository.ingest(listOf(ada))

        return Triple(EditProfileViewModel(repository, "u1"), repository, dataSource)
    }

    private fun TestScope.collecting(viewModel: EditProfileViewModel) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }
    }

    private fun EditProfileViewModel.form(): EditProfileForm =
        (uiState.value as EditProfileUiState.Editing).form

    @Test
    fun `form is seeded from the cached user`() = runTest {
        val (viewModel, _, _) = fixture()
        collecting(viewModel)

        val form = viewModel.form()
        assertEquals("Ada Lovelace", form.nickname)
        assertEquals("36", form.age)
        assertEquals(GenderOption.WOMAN, form.gender)
        assertEquals("Mathematician & writer.", form.bio)
        assertNull(form.avatarUrl)
    }

    @Test
    fun `form loads from the data source when the cache is empty`() = runTest {
        val (viewModel, _, _) = fixture(cached = false)
        collecting(viewModel)

        assertEquals("Ada Lovelace", viewModel.form().nickname)
    }

    @Test
    fun `field changes update the form`() = runTest {
        val (viewModel, _, _) = fixture()
        collecting(viewModel)

        viewModel.onNicknameChange("Ada King")
        viewModel.onAgeChange("37")
        viewModel.onGenderChange(GenderOption.MAN)
        viewModel.onBioChange("Countess of Lovelace")
        viewModel.onAvatarChange("content://media/picker/1")

        val form = viewModel.form()
        assertEquals("Ada King", form.nickname)
        assertEquals("37", form.age)
        assertEquals(GenderOption.MAN, form.gender)
        assertEquals("Countess of Lovelace", form.bio)
        assertEquals("content://media/picker/1", form.avatarUrl)
    }

    @Test
    fun `age input keeps only digits and caps the length`() = runTest {
        val (viewModel, _, _) = fixture()
        collecting(viewModel)

        viewModel.onAgeChange("3a6.19")

        assertEquals("361", viewModel.form().age)
    }

    @Test
    fun `form cannot save with a blank nickname or out-of-range age`() = runTest {
        val (viewModel, _, _) = fixture()
        collecting(viewModel)

        viewModel.onNicknameChange("   ")
        assertFalse(viewModel.form().canSave)

        viewModel.onNicknameChange("Ada")
        viewModel.onAgeChange("999")
        assertFalse(viewModel.form().canSave)

        viewModel.onAgeChange("")
        assertTrue(viewModel.form().canSave)
    }

    @Test
    fun `save persists the edited profile and flags isSaved`() = runTest {
        val (viewModel, repository, dataSource) = fixture()
        collecting(viewModel)

        viewModel.onNicknameChange(" Ada King ")
        viewModel.onAgeChange("37")
        viewModel.onBioChange("")
        viewModel.save()

        assertEquals(
            "u1" to ProfileUpdate(
                nickname = "Ada King",
                age = 37,
                gender = "Woman",
                bio = null,
                avatarUrl = null,
            ),
            dataSource.lastUpdate,
        )
        assertEquals("Ada King", repository.user("u1").first()?.nickname)
        assertTrue(viewModel.isSaved.value)
    }

    @Test
    fun `save is ignored while the form is invalid`() = runTest {
        val (viewModel, _, dataSource) = fixture()
        collecting(viewModel)

        viewModel.onNicknameChange("")
        viewModel.save()

        assertNull(dataSource.lastUpdate)
        assertFalse(viewModel.isSaved.value)
    }

    @Test
    fun `a failed save surfaces the error and does not navigate away`() = runTest {
        val (viewModel, _, _) = fixture(failUpdates = true)
        collecting(viewModel)

        viewModel.save()

        val editing = viewModel.uiState.value as EditProfileUiState.Editing
        assertEquals(AppError.NoConnection, editing.saveError)
        assertFalse(editing.isSaving)
        assertFalse(viewModel.isSaved.value)
    }
}
