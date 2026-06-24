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
import uno.lux.sample.data.post.PostRepository
import uno.lux.sample.data.profile.ProfileRepository
import uno.lux.sample.data.user.UserRepository
import uno.lux.sample.di.CurrentUserId
import uno.lux.sample.util.launchRefresh
import uno.lux.sample.util.stateInWhileSubscribed

/**
 * Holds the profile state for one [userId]. Combines [UserRepository], [ProfileRepository]
 * (metadata + ordered post IDs), and [PostRepository] (entity map) into a single
 * [ProfileScreenData]. Mutations go directly through [PostRepository] so a like toggled here is
 * immediately visible on the home feed — the shared entity store propagates the update to all
 * observers. [userId] is a runtime arg wired through [Factory].
 */
@HiltViewModel(assistedFactory = ProfileViewModel.Factory::class)
class ProfileViewModel @AssistedInject constructor(
    private val profileRepository: ProfileRepository,
    private val postRepository: PostRepository,
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
        combine(profileRepository.postIds(userId), postRepository.entities) { ids, entities ->
            ids.mapNotNull { entities[it] }
        },
    ) { user, profile, posts ->
        if (user == null) ProfileUiState.NotFound
        else ProfileUiState.Loaded(
            data = ProfileScreenData(user, profile, posts),
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
        viewModelScope.launch { postRepository.toggleLike(postId) }
    }

    override fun onToggleBookmark(postId: String) {
        viewModelScope.launch { postRepository.toggleBookmark(postId) }
    }
}
