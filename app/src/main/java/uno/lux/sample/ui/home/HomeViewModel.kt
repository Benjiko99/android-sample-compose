package uno.lux.sample.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import uno.lux.sample.data.post.FeedRepository
import uno.lux.sample.data.post.PostId
import uno.lux.sample.data.post.PostRepository
import uno.lux.sample.data.user.UserRepository
import uno.lux.sample.util.AppError
import uno.lux.sample.util.ignoreErrors
import uno.lux.sample.util.launchIfIdle
import uno.lux.sample.util.launchRefresh
import uno.lux.sample.util.stateInWhileSubscribed
import uno.lux.sample.util.toAppError
import javax.inject.Inject

/**
 * Holds feed state and translates user intent (likes, bookmarks) into repository mutations.
 * Combines [FeedRepository] (ordered post IDs + pagination), [PostRepository] (entity map), and
 * [UserRepository] (author lookup) to produce [PostCardData] items ready for display. All three
 * are constructor dependencies so the ViewModel can be unit tested against fakes.
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val feedRepository: FeedRepository,
    private val postRepository: PostRepository,
    private val userRepository: UserRepository,
) : ViewModel(), HomeActions {

    private val _loadError = MutableStateFlow<AppError?>(null)

    val uiState: StateFlow<HomeUiState> = combine(
        feedRepository.postIds,
        feedRepository.hasMore,
        postRepository.entities,
        userRepository.users,
        _loadError,
    ) { postIds, hasMore, entities, users, loadError ->
        val cards = postIds.mapNotNull { id ->
            val post = entities[id] ?: return@mapNotNull null
            val author = users[post.authorId] ?: return@mapNotNull null
            PostCardData(post = post, author = author)
        }
        if (loadError != null && cards.isEmpty()) HomeUiState.Error(loadError)
        else HomeUiState.Feed(posts = cards, endReached = !hasMore)
    }.stateInWhileSubscribed(viewModelScope, HomeUiState.Loading)

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private var loadJob: Job? = null
    private var loadMoreJob: Job? = null

    init {
        retry()
    }

    override fun refresh() = launchRefresh(_isRefreshing) { load() }

    override fun retry() = launchIfIdle(::loadJob) { load() }

    override fun loadMore() = launchIfIdle(::loadMoreJob) {
        ignoreErrors { feedRepository.loadMore() }
    }

    private suspend fun load() {
        _loadError.value = null

        ignoreErrors(onError = { e -> _loadError.value = e.toAppError() }) { feedRepository.refresh() }
    }

    override fun onToggleLike(postId: PostId) {
        viewModelScope.launch {
            ignoreErrors { postRepository.toggleLike(postId) }
        }
    }

    override fun onToggleBookmark(postId: PostId) {
        viewModelScope.launch {
            ignoreErrors { postRepository.toggleBookmark(postId) }
        }
    }
}
