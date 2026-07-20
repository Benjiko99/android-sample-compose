package uno.lux.sample.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
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
import uno.lux.sample.ui.post.PostCardData
import uno.lux.sample.util.AppError
import uno.lux.sample.util.ignoreErrors
import uno.lux.sample.util.launchIfIdle
import uno.lux.sample.util.launchRefresh
import uno.lux.sample.util.stateInWhileSubscribed

/**
 * Holds the profile state for one [userId]. Combines [UserRepository], [ProfileRepository]
 * (counts + ordered post IDs + pagination), and [PostRepository] (entity map) into a single
 * [ProfileScreenData]. Mutations go directly through [PostRepository] so a like toggled here is
 * immediately visible on the home feed — the shared entity store propagates the update to all
 * observers. Navigation intents (opening a post, profile, viewer or the editor, going back) are
 * pushes and pops on the injected [Navigator]. [userId] is a runtime arg wired through [Factory].
 *
 * The saved posts behind the Saved tab are loaded lazily, on [onSavedTabShown]; the screen only
 * offers that tab on the signed-in user's own profile, and the server refuses the list to anyone
 * else regardless.
 */
@HiltViewModel(assistedFactory = ProfileViewModel.Factory::class)
class ProfileViewModel @AssistedInject constructor(
    private val profileRepository: ProfileRepository,
    private val postRepository: PostRepository,
    private val userRepository: UserRepository,
    private val navigator: Navigator,
    @param:CurrentUserId private val currentUserId: UserId,
    @Assisted private val userId: UserId,
) : ViewModel(), ProfileActions {

    @AssistedFactory
    interface Factory {
        fun create(userId: UserId): ProfileViewModel
    }

    private val _loadError = MutableStateFlow<AppError?>(null)

    // Preserves PostCardData instances across emissions so Compose strong-skipping can use
    // reference equality to skip PostCard recomposition for saved posts that didn't change.
    private var bookmarkCardCache = emptyMap<PostId, PostCardData>()

    // Null until the Saved tab is first opened, which is what keeps this private list unfetched
    // on every profile the signed-in user merely looks at.
    private val bookmarks: Flow<ProfileBookmarks?> = combine(
        profileRepository.bookmarkIds(userId),
        profileRepository.hasMoreBookmarks(userId),
        postRepository.entities,
        userRepository.users,
    ) { ids, hasMore, entities, users ->
        if (ids == null) return@combine null

        val cards = ids.mapNotNull { id ->
            val post = entities[id] ?: return@mapNotNull null
            val author = users[post.authorId] ?: return@mapNotNull null
            val cached = bookmarkCardCache[id]
            if (cached != null && cached.post === post && cached.author === author) cached
            else PostCardData(post = post, author = author, isOwn = post.authorId == currentUserId)
        }
        bookmarkCardCache = cards.associateBy { it.post.id }

        ProfileBookmarks(posts = cards, endReached = !hasMore)
    }

    val uiState: StateFlow<ProfileUiState> = combine(
        combine(
            userRepository.user(userId),
            profileRepository.profile(userId),
            combine(profileRepository.postIds(userId), postRepository.entities) { ids, entities ->
                ids.mapNotNull { entities[it] }
            },
            profileRepository.hasMorePosts(userId),
            bookmarks,
        ) { user, profile, posts, hasMorePosts, savedPosts ->
            if (user == null) null
            else ProfileUiState.Loaded(
                data = ProfileScreenData(
                    user = user,
                    profile = profile,
                    posts = posts,
                    postsEndReached = !hasMorePosts,
                    bookmarks = savedPosts,
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
    private var bookmarksJob: Job? = null

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
                // Only once the Saved tab has been opened — a profile load must not reach for
                // a private list nobody has asked to see.
                if (profileRepository.hasLoadedBookmarks(userId)) {
                    launch { profileRepository.refreshBookmarks(userId) }
                }
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

    override fun onDeletePost(postId: PostId) {
        viewModelScope.launch {
            ignoreErrors { postRepository.delete(postId) }
        }
    }

    override fun onToggleFollow() {
        viewModelScope.launch {
            ignoreErrors { userRepository.toggleFollow(userId) }
        }
    }

    override fun loadMorePosts() = launchIfIdle(::loadMorePostsJob) {
        ignoreErrors { profileRepository.loadMorePosts(userId) }
    }

    /**
     * The Saved tab became visible. Fetches the list the first time only — later visits are
     * served from the repository, and a pull-to-refresh is what re-fetches it.
     */
    override fun onSavedTabShown() {
        if (profileRepository.hasLoadedBookmarks(userId)) return

        launchIfIdle(::bookmarksJob) {
            ignoreErrors { profileRepository.refreshBookmarks(userId) }
        }
    }

    override fun loadMoreBookmarks() = launchIfIdle(::bookmarksJob) {
        ignoreErrors { profileRepository.loadMoreBookmarks(userId) }
    }

    override fun goBack() = navigator.goBack()

    override fun openEditProfile() = navigator.goToSingleTop(Screen.EditProfile)

    override fun openPost(postId: PostId) = navigator.goTo(Screen.PostDetail(postId))

    override fun openProfile(userId: UserId) = navigator.goTo(Screen.Profile(userId))

    override fun openVideo(video: Video) = navigator.goTo(Screen.FullscreenVideo(video))

    override fun openAlbum(imageUrls: List<String>, initialIndex: Int) =
        navigator.goTo(Screen.AlbumViewer(imageUrls, initialIndex))

    override fun openAvatar(avatarUrl: String) =
        navigator.goTo(Screen.AlbumViewer(listOf(avatarUrl), initialIndex = 0))
}
