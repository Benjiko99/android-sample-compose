package uno.lux.sample.ui.editprofile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import uno.lux.sample.data.user.UserId
import uno.lux.sample.data.user.UserRepository
import uno.lux.sample.di.CurrentUserId
import uno.lux.sample.util.AppError
import uno.lux.sample.util.ignoreErrors
import uno.lux.sample.util.launchIfIdle
import uno.lux.sample.util.stateInWhileSubscribed
import javax.inject.Inject

/**
 * Drives the signed-in user's profile editor. The form is seeded from [UserRepository]'s
 * cached user as a **one-time snapshot** — deliberately not kept in sync afterwards, so a
 * concurrent cache refresh can't clobber in-progress edits. Saving goes back through the
 * repository, which replaces the cached entry, making the change instantly visible on every
 * screen observing the user.
 */
@HiltViewModel
class EditProfileViewModel @Inject constructor(
    private val userRepository: UserRepository,
    @CurrentUserId private val userId: UserId,
) : ViewModel(), EditProfileActions {

    private val _form = MutableStateFlow<EditProfileForm?>(null)
    private val _loadError = MutableStateFlow<AppError?>(null)
    private val _isSaving = MutableStateFlow(false)
    private val _saveError = MutableStateFlow<AppError?>(null)

    private val _isSaved = MutableStateFlow(false)

    /** Flips to true after a successful save; the binder navigates back in response. */
    val isSaved: StateFlow<Boolean> = _isSaved.asStateFlow()

    val uiState: StateFlow<EditProfileUiState> = combine(
        _form,
        _loadError,
        _isSaving,
        _saveError,
    ) { form, loadError, isSaving, saveError ->
        when {
            form != null -> EditProfileUiState.Editing(form, isSaving, saveError)
            loadError != null -> EditProfileUiState.Error(loadError)
            else -> EditProfileUiState.Loading
        }
    }.stateInWhileSubscribed(viewModelScope, EditProfileUiState.Loading)

    private var loadJob: Job? = null
    private var saveJob: Job? = null

    init {
        retry()
    }

    override fun retry() = launchIfIdle(::loadJob) { load() }

    private suspend fun load() {
        _loadError.value = null

        ignoreErrors(_loadError) {
            // The cache normally already holds the user (the editor opens from their loaded
            // profile); it's only empty when the screen is restored after process death.
            if (userRepository.user(userId).first() == null) {
                userRepository.refresh(userId)
            }
            val user = checkNotNull(userRepository.user(userId).first()) {
                "Signed-in user '$userId' not found"
            }

            _form.value = EditProfileForm.from(user)
        }
    }

    override fun onNicknameChange(value: String) = updateForm { it.copy(nickname = value) }

    override fun onAgeChange(value: String) = updateForm { form ->
        form.copy(age = value.filter { it.isDigit() }.take(3))
    }

    override fun onGenderChange(gender: GenderOption) = updateForm { it.copy(gender = gender) }

    override fun onBioChange(value: String) = updateForm { it.copy(bio = value) }

    override fun onAvatarChange(uri: String) = updateForm { it.copy(avatarUrl = uri) }

    override fun save() {
        val form = _form.value ?: return
        if (!form.canSave) return

        launchIfIdle(::saveJob) {
            _saveError.value = null
            _isSaving.value = true
            try {
                ignoreErrors(_saveError) {
                    userRepository.updateProfile(userId, form.toProfileUpdate())
                    _isSaved.value = true
                }
            } finally {
                _isSaving.value = false
            }
        }
    }

    private fun updateForm(transform: (EditProfileForm) -> EditProfileForm) {
        _form.update { form -> form?.let(transform) }
    }
}
