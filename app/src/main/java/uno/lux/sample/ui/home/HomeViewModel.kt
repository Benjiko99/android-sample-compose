package uno.lux.sample.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
        .map { posts ->
            // The in-memory repository returns the whole feed at once, so the end is always
            // reached. A paginated source would drive this flag (and expose a loadMore()).
            HomeUiState.Feed(posts = posts, endReached = true)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = HomeUiState.Loading,
        )

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    /** Re-fetches the feed, surfacing progress through [isRefreshing] for pull-to-refresh. */
    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                repository.refresh()
            } finally {
                _isRefreshing.value = false
            }
        }
    }

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
