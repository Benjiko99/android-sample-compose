package uno.lux.sample.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import uno.lux.sample.data.post.PostRepository
import uno.lux.sample.data.user.UserRepository
import uno.lux.sample.util.launchRefresh
import uno.lux.sample.util.stateInWhileSubscribed
import javax.inject.Inject

/**
 * Holds feed state and translates user intent (likes, bookmarks) into repository mutations.
 * Combines [PostRepository] and [UserRepository] to resolve each post's author, producing
 * [PostCardData] items ready for display. Both repositories are constructor dependencies so
 * the ViewModel can be unit tested against fakes.
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val postRepository: PostRepository,
    private val userRepository: UserRepository,
) : ViewModel(), HomeActions {

    val uiState: StateFlow<HomeUiState> = combine(
        postRepository.posts,
        userRepository.users,
    ) { posts, users ->
        val cards = posts.mapNotNull { post ->
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
        postRepository.refresh()
    }

    override fun onToggleLike(postId: String) {
        viewModelScope.launch { postRepository.toggleLike(postId) }
    }

    override fun onToggleBookmark(postId: String) {
        viewModelScope.launch { postRepository.toggleBookmark(postId) }
    }
}
