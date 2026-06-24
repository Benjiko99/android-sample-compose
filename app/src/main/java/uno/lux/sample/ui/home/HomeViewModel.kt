package uno.lux.sample.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
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
import uno.lux.sample.util.launchIfIdle
import uno.lux.sample.util.launchRefresh
import uno.lux.sample.util.stateInWhileSubscribed
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

    private val _loadError = MutableStateFlow(false)

    private data class FeedSnapshot(val cards: List<PostCardData>, val endReached: Boolean)

    val uiState: StateFlow<HomeUiState> = combine(
        combine(
            feedRepository.postIds,
            feedRepository.hasMore,
            postRepository.entities,
            userRepository.users,
        ) { postIds, hasMore, entities, users ->
            FeedSnapshot(
                cards = postIds.mapNotNull { id ->
                    val post = entities[id] ?: return@mapNotNull null
                    val author = users[post.authorId] ?: return@mapNotNull null
                    PostCardData(post = post, author = author)
                },
                endReached = !hasMore,
            )
        },
        _loadError,
    ) { snapshot, loadError ->
        if (loadError && snapshot.cards.isEmpty()) HomeUiState.Error
        else HomeUiState.Feed(posts = snapshot.cards, endReached = snapshot.endReached)
    }.stateInWhileSubscribed(viewModelScope, HomeUiState.Loading)

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private var loadMoreJob: Job? = null

    init {
        viewModelScope.launch { load() }
    }

    override fun refresh() = launchRefresh(_isRefreshing) { load() }

    override fun retry() {
        viewModelScope.launch { load() }
    }

    override fun loadMore() = launchIfIdle(::loadMoreJob) {
        try {
            feedRepository.loadMore()
        } catch (e: Exception) {
            if (e is CancellationException) throw e
        }
    }

    private suspend fun load() {
        _loadError.value = false
        try {
            feedRepository.refresh()
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            _loadError.value = true
        }
    }

    override fun onToggleLike(postId: PostId) {
        viewModelScope.launch {
            try {
                postRepository.toggleLike(postId)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
            }
        }
    }

    override fun onToggleBookmark(postId: PostId) {
        viewModelScope.launch {
            try {
                postRepository.toggleBookmark(postId)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
            }
        }
    }
}
