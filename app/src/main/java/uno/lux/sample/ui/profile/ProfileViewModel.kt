package uno.lux.sample.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import uno.lux.sample.data.InMemoryProfileRepository
import uno.lux.sample.data.ProfileRepository
import uno.lux.sample.util.stateInWhileSubscribed

/**
 * Holds the profile state for one [userId] and translates like / bookmark intent into
 * repository mutations. The repository and id are constructor dependencies so the ViewModel
 * can be unit tested against a fake; [provideFactory] supplies the production wiring (and
 * binds the id) until a DI framework is introduced.
 */
class ProfileViewModel(
    private val repository: ProfileRepository,
    userId: String,
) : ViewModel() {

    val uiState: StateFlow<ProfileUiState> = repository.profile(userId)
        .map { profile ->
            if (profile == null) ProfileUiState.NotFound else ProfileUiState.Loaded(profile)
        }
        .stateInWhileSubscribed(viewModelScope, ProfileUiState.Loading)

    fun onToggleLike(postId: String) {
        viewModelScope.launch { repository.toggleLike(postId) }
    }

    fun onToggleBookmark(postId: String) {
        viewModelScope.launch { repository.toggleBookmark(postId) }
    }

    companion object {
        /** A factory bound to [userId], since a ViewModel can't take runtime arguments directly. */
        fun provideFactory(userId: String): ViewModelProvider.Factory = viewModelFactory {
            initializer { ProfileViewModel(InMemoryProfileRepository(), userId) }
        }
    }
}
