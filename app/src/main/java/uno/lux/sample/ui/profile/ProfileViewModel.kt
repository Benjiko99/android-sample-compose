package uno.lux.sample.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import uno.lux.sample.data.ProfileRepository
import uno.lux.sample.util.stateInWhileSubscribed

/**
 * Holds the profile state for one [userId] and translates like / bookmark intent into
 * repository mutations. The repository is a constructor dependency so the ViewModel can be
 * unit tested against a fake; the id is a runtime argument, so production wiring goes
 * through assisted injection — Hilt fills in the repository, the caller supplies the id
 * via [Factory].
 */
@HiltViewModel(assistedFactory = ProfileViewModel.Factory::class)
class ProfileViewModel @AssistedInject constructor(
    private val repository: ProfileRepository,
    @Assisted userId: String,
) : ViewModel(), ProfileActions {

    /** Creates a [ProfileViewModel] bound to one user; Hilt generates the implementation. */
    @AssistedFactory
    interface Factory {
        fun create(userId: String): ProfileViewModel
    }

    val uiState: StateFlow<ProfileUiState> = repository.profile(userId)
        .map { profile ->
            if (profile == null) ProfileUiState.NotFound else ProfileUiState.Loaded(profile)
        }
        .stateInWhileSubscribed(viewModelScope, ProfileUiState.Loading)

    override fun onToggleLike(postId: String) {
        viewModelScope.launch { repository.toggleLike(postId) }
    }

    override fun onToggleBookmark(postId: String) {
        viewModelScope.launch { repository.toggleBookmark(postId) }
    }
}
