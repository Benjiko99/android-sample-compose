package uno.lux.sample.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import uno.lux.sample.data.post.PostId
import uno.lux.sample.data.post.PostRepository
import uno.lux.sample.data.post.Video
import uno.lux.sample.data.profile.ProfileRepository
import uno.lux.sample.data.user.UserId
import uno.lux.sample.data.user.UserRepository
import uno.lux.sample.di.CurrentUserId
import uno.lux.sample.ui.navigation.Navigator
import uno.lux.sample.ui.navigation.Screen
import uno.lux.sample.util.AppError
import uno.lux.sample.util.ignoreErrors
import uno.lux.sample.util.launchIfIdle
import uno.lux.sample.util.launchRefresh
import uno.lux.sample.util.stateInWhileSubscribed

/**
 * Holds the profile state for one [userId]. Combines [UserRepository], [ProfileRepository]
 * (metadata + ordered post IDs + pagination), and [PostRepository] (entity map) into a single
 * [ProfileScreenData]. Mutations go directly through [PostRepository] so a like toggled here is
 * immediately visible on the home feed — the shared entity store propagates the update to all
 * observers. Navigation intents (opening a post, viewer or the editor, going back) are pushes
 * and pops on the injected [Navigator]. [userId] is a runtime arg wired through [Factory].
 */
@HiltViewModel(assistedFactory = ProfileViewModel.Factory::class)
class ProfileViewModel @AssistedInject constructor(
    private val profileRepository: ProfileRepository,
    private val postRepository: PostRepository,
    private val userRepository: UserRepository,
    private val navigator: Navigator,
    @CurrentUserId private val currentUserId: UserId,
    @Assisted private val userId: UserId,
) : ViewModel(), ProfileActions {

    @AssistedFactory
    interface Factory {
        fun create(userId: UserId): ProfileViewModel
    }

    private data class HasMoreState(val posts: Boolean, val albums: Boolean, val videos: Boolean)

    private val _loadError = MutableStateFlow<AppError?>(null)

    val uiState: StateFlow<ProfileUiState> = combine(
        combine(
            userRepository.user(userId),
            profileRepository.profile(userId),
            combine(profileRepository.postIds(userId), postRepository.entities) { ids, entities ->
                ids.mapNotNull { entities[it] }
            },
            combine(
                profileRepository.hasMorePosts(userId),
                profileRepository.hasMoreAlbums(userId),
                profileRepository.hasMoreVideos(userId),
            ) { posts, albums, videos -> HasMoreState(posts, albums, videos) },
        ) { user, profile, posts, hasMore ->
            if (user == null) null
            else ProfileUiState.Loaded(
                data = ProfileScreenData(
                    user = user,
                    profile = profile,
                    posts = posts,
                    postsEndReached = !hasMore.posts,
                    albumsEndReached = !hasMore.albums,
                    videosEndReached = !hasMore.videos,
                ),
                isCurrentUser = userId == currentUserId,
            )
        },
        _loadError,
    ) { state, loadError ->
        when {
            state != null -> state
            loadError != null -> ProfileUiState.Error(loadError)
            else -> ProfileUiState.NotFound
        }
    }.stateInWhileSubscribed(viewModelScope, ProfileUiState.Loading)

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private var loadJob: Job? = null
    private var loadMorePostsJob: Job? = null
    private var loadMoreAlbumsJob: Job? = null
    private var loadMoreVideosJob: Job? = null

    init {
        retry()
    }

    fun refresh() = launchRefresh(_isRefreshing) { load() }

    fun retry() = launchIfIdle(::loadJob) { load() }

    private suspend fun load() {
        _loadError.value = null

        ignoreErrors(_loadError) {
            coroutineScope {
                launch { userRepository.refresh(userId) }
                launch { profileRepository.refresh(userId) }
            }
        }
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

    override fun loadMorePosts() = launchIfIdle(::loadMorePostsJob) {
        ignoreErrors { profileRepository.loadMorePosts(userId) }
    }

    override fun loadMoreAlbums() = launchIfIdle(::loadMoreAlbumsJob) {
        ignoreErrors { profileRepository.loadMoreAlbums(userId) }
    }

    override fun loadMoreVideos() = launchIfIdle(::loadMoreVideosJob) {
        ignoreErrors { profileRepository.loadMoreVideos(userId) }
    }

    override fun goBack() = navigator.goBack()

    override fun openEditProfile() = navigator.goTo(Screen.EditProfile)

    override fun openPost(postId: PostId) = navigator.goTo(Screen.PostDetail(postId))

    override fun openVideo(video: Video) = navigator.goTo(Screen.FullscreenVideo(video))

    override fun openAlbum(imageUrls: List<String>, initialIndex: Int) =
        navigator.goTo(Screen.AlbumViewer(imageUrls, initialIndex))

    override fun openAvatar(avatarUrl: String) =
        navigator.goTo(Screen.AlbumViewer(listOf(avatarUrl), initialIndex = 0))
}
