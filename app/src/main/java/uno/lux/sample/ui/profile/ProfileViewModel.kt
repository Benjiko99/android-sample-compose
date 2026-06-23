package uno.lux.sample.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import uno.lux.sample.data.profile.ProfileRepository
import uno.lux.sample.data.user.UserRepository
import uno.lux.sample.di.CurrentUserId
import uno.lux.sample.util.launchRefresh
import uno.lux.sample.util.stateInWhileSubscribed

/**
 * Holds the profile state for one [userId] and translates like / bookmark intent into
 * repository mutations. Combines [UserRepository] and [ProfileRepository] into a single
 * [ProfileScreenData]; a null user emits [ProfileUiState.NotFound]. [refresh] re-fetches
 * from both repositories in parallel. Both are constructor dependencies so the ViewModel can
 * be unit tested against fakes; [userId] is a runtime arg wired through [Factory].
 */
@HiltViewModel(assistedFactory = ProfileViewModel.Factory::class)
class ProfileViewModel @AssistedInject constructor(
    private val profileRepository: ProfileRepository,
    private val userRepository: UserRepository,
    @CurrentUserId private val currentUserId: String,
    @Assisted private val userId: String,
) : ViewModel(), ProfileActions {

    @AssistedFactory
    interface Factory {
        fun create(userId: String): ProfileViewModel
    }

    val uiState: StateFlow<ProfileUiState> = combine(
        userRepository.user(userId),
        profileRepository.profile(userId),
    ) { user, profile ->
        if (user == null) ProfileUiState.NotFound
        else ProfileUiState.Loaded(
            data = ProfileScreenData(user, profile),
            isCurrentUser = userId == currentUserId,
        )
    }.stateInWhileSubscribed(viewModelScope, ProfileUiState.Loading)

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    init {
        viewModelScope.launch { load() }
    }

    fun refresh() = launchRefresh(_isRefreshing) { load() }

    private suspend fun load() = coroutineScope {
        launch { userRepository.refresh(userId) }
        launch { profileRepository.refresh(userId) }
    }

    override fun onToggleLike(postId: String) {
        viewModelScope.launch { profileRepository.toggleLike(postId) }
    }

    override fun onToggleBookmark(postId: String) {
        viewModelScope.launch { profileRepository.toggleBookmark(postId) }
    }
}
