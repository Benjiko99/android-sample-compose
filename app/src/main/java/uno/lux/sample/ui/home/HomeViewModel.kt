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
import uno.lux.sample.data.post.FeedState
import uno.lux.sample.data.post.PostId
import uno.lux.sample.data.post.PostRepository
import uno.lux.sample.data.post.Video
import uno.lux.sample.data.user.UserId
import uno.lux.sample.data.user.UserRepository
import uno.lux.sample.ui.navigation.Navigator
import uno.lux.sample.ui.navigation.Screen
import uno.lux.sample.util.AppError
import uno.lux.sample.util.ignoreErrors
import uno.lux.sample.util.launchIfIdle
import uno.lux.sample.util.launchRefresh
import uno.lux.sample.util.stateInWhileSubscribed
import javax.inject.Inject

/**
 * Holds feed state and translates user intent — likes and bookmarks into repository mutations,
 * opening a post/profile/viewer into pushes on the injected [Navigator]. Combines
 * [FeedRepository] (feed state + pagination), [PostRepository] (entity map), and
 * [UserRepository] (author lookup) to produce [PostCardData] items ready for display. All
 * collaborators are constructor dependencies so the ViewModel can be unit tested against fakes
 * (the navigator against a plain attached list).
 *
 * [FeedState.NotLoaded] is the initial state of [FeedRepository.feedState]; the combine maps it
 * to [HomeUiState.Loading] until [FeedRepository.refresh] completes and emits [FeedState.Loaded].
 * Because entities and users are ingested before that emission, the Loading → Feed transition is
 * atomic: there is no intermediate state where the flag flips but the data has not yet arrived.
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val feedRepository: FeedRepository,
    private val postRepository: PostRepository,
    private val userRepository: UserRepository,
    private val navigator: Navigator,
) : ViewModel(), HomeActions {

    private val _loadError = MutableStateFlow<AppError?>(null)

    // Preserves PostCardData instances across emissions so Compose strong-skipping can use
    // reference equality to skip PostCard recomposition for posts that didn't change.
    private var cardCache = emptyMap<PostId, PostCardData>()

    val uiState: StateFlow<HomeUiState> = combine(
        feedRepository.feedState,
        postRepository.entities,
        userRepository.users,
        _loadError,
    ) { feedState, entities, users, loadError ->
        if (loadError != null) return@combine HomeUiState.Error(loadError)
        when (feedState) {
            FeedState.NotLoaded -> HomeUiState.Loading
            is FeedState.Loaded -> {
                val cards = feedState.postIds.mapNotNull { id ->
                    val post = entities[id] ?: return@mapNotNull null
                    val author = users[post.authorId] ?: return@mapNotNull null
                    val cached = cardCache[id]
                    if (cached != null && cached.post === post && cached.author === author) cached
                    else PostCardData(post = post, author = author)
                }
                cardCache = cards.associateBy { it.post.id }
                HomeUiState.Feed(posts = cards, endReached = !feedState.hasMore)
            }
        }
    }.stateInWhileSubscribed(viewModelScope, HomeUiState.Loading)

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private var loadJob: Job? = null
    private var loadMoreJob: Job? = null

    init {
        retry()
    }

    override fun refresh() = launchRefresh(_isRefreshing) { load() }

    override fun retry() {
        if (loadJob?.isActive == true) return
        feedRepository.reset()
        loadJob = viewModelScope.launch { load() }
    }

    override fun loadMore() = launchIfIdle(::loadMoreJob) {
        ignoreErrors { feedRepository.loadMore() }
    }

    private suspend fun load() {
        _loadError.value = null

        ignoreErrors(_loadError) { feedRepository.refresh() }
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

    override fun openSettings() = navigator.goTo(Screen.Settings)

    override fun openProfile(userId: UserId) = navigator.goTo(Screen.Profile(userId))

    override fun openPost(postId: PostId) = navigator.goTo(Screen.PostDetail(postId))

    override fun openVideo(video: Video) = navigator.goTo(Screen.FullscreenVideo(video))

    override fun openAlbum(imageUrls: List<String>, initialIndex: Int) =
        navigator.goTo(Screen.AlbumViewer(imageUrls, initialIndex))
}
