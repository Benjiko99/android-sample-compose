package uno.lux.sample.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import uno.lux.sample.data.InMemoryPostRepository
import uno.lux.sample.data.PostRepository

/**
 * Holds feed state and translates user intent (likes, bookmarks) into repository
 * mutations. The repository is a constructor dependency so the ViewModel can be unit
 * tested against a fake; [Factory] supplies the production wiring until a DI framework is
 * introduced.
 */
class HomeViewModel(
    private val repository: PostRepository,
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = repository.posts
        .map { posts -> HomeUiState.Feed(posts) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = HomeUiState.Loading,
        )

    fun onToggleLike(postId: String) {
        viewModelScope.launch { repository.toggleLike(postId) }
    }

    fun onToggleBookmark(postId: String) {
        viewModelScope.launch { repository.toggleBookmark(postId) }
    }

    companion object {
        /** Keeps the feed flow warm briefly across config changes and tab switches. */
        private const val STOP_TIMEOUT_MILLIS = 5_000L

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { HomeViewModel(InMemoryPostRepository()) }
        }
    }
}
