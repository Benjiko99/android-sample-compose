package uno.lux.sample.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import uno.lux.sample.data.post.FeedRepository
import uno.lux.sample.data.post.PostRepository
import uno.lux.sample.data.user.UserRepository
import uno.lux.sample.util.launchRefresh
import uno.lux.sample.util.stateInWhileSubscribed
import javax.inject.Inject

/**
 * Holds feed state and translates user intent (likes, bookmarks) into repository mutations.
 * Combines [FeedRepository] (ordered post IDs), [PostRepository] (entity map), and
 * [UserRepository] (author lookup) to produce [PostCardData] items ready for display. All three
 * are constructor dependencies so the ViewModel can be unit tested against fakes.
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val feedRepository: FeedRepository,
    private val postRepository: PostRepository,
    private val userRepository: UserRepository,
) : ViewModel(), HomeActions {

    val uiState: StateFlow<HomeUiState> = combine(
        feedRepository.postIds,
        postRepository.entities,
        userRepository.users,
    ) { postIds, entities, users ->
        val cards = postIds.mapNotNull { id ->
            val post = entities[id] ?: return@mapNotNull null
            val author = users[post.authorId] ?: return@mapNotNull null
            PostCardData(post = post, author = author)
        }
        HomeUiState.Feed(posts = cards, endReached = true)
    }.stateInWhileSubscribed(viewModelScope, HomeUiState.Loading)

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    init {
        viewModelScope.launch { load() }
    }

    override fun refresh() = launchRefresh(_isRefreshing) { load() }

    private suspend fun load() {
        feedRepository.refresh()
    }

    override fun onToggleLike(postId: String) {
        viewModelScope.launch { postRepository.toggleLike(postId) }
    }

    override fun onToggleBookmark(postId: String) {
        viewModelScope.launch { postRepository.toggleBookmark(postId) }
    }
}
