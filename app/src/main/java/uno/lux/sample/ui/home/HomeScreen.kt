package uno.lux.sample.ui.home

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListLayoutInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import uno.lux.sample.R
import uno.lux.sample.data.SamplePosts
import uno.lux.sample.data.SampleUsers
import uno.lux.sample.data.post.Album
import uno.lux.sample.data.post.PostId
import uno.lux.sample.data.post.Video
import uno.lux.sample.data.user.UserId
import uno.lux.sample.ui.components.MosaicWordmark
import uno.lux.sample.ui.components.SettingsAction
import uno.lux.sample.ui.theme.LocalMosaicColors
import uno.lux.sample.ui.theme.MosaicTheme
import uno.lux.sample.ui.video.LocalVideoPlayback
import uno.lux.sample.util.createActionsProxy

/**
 * The feed's ViewModel-backed intents, as one [Stable] seam the stateless [HomeScreen] depends
 * on. [HomeViewModel] implements it, so the binder passes the ViewModel directly and a preview
 * passes a no-op [createActionsProxy]. Navigation (opening settings or a profile) is the host's
 * concern, so it stays a separate lambda rather than living here.
 */
@Stable
interface HomeActions {
    fun refresh()
    fun loadMore()
    fun onToggleLike(postId: PostId)
    fun onToggleBookmark(postId: PostId)
}

/**
 * Stateful entry point: binds the [HomeViewModel] and forwards state and intent to the
 * stateless overload below.
 */
@Composable
fun HomeScreen(
    onOpenSettings: () -> Unit,
    onOpenProfile: (userId: UserId) -> Unit,
    onOpenVideo: (Video) -> Unit,
    onOpenAlbum: (Album, Int) -> Unit,
    onOpenPost: (postId: PostId) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()

    HomeScreen(
        uiState = uiState,
        isRefreshing = isRefreshing,
        actions = viewModel,
        onOpenSettings = onOpenSettings,
        onOpenProfile = onOpenProfile,
        onOpenVideo = onOpenVideo,
        onOpenAlbum = onOpenAlbum,
        onOpenPost = onOpenPost,
        modifier = modifier,
    )
}

/**
 * Stateless feed screen — renders [uiState] and reports interactions through the
 * callbacks. Holding no ViewModel makes it directly previewable and testable.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HomeScreen(
    uiState: HomeUiState,
    isRefreshing: Boolean,
    actions: HomeActions,
    onOpenSettings: () -> Unit,
    onOpenProfile: (userId: UserId) -> Unit,
    onOpenVideo: (Video) -> Unit,
    onOpenAlbum: (Album, Int) -> Unit,
    onOpenPost: (postId: PostId) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    // The bar is pinned; its shadow fades in whenever the list content is scrolled and clears at
    // the very top. canScrollBackward is already a coalesced boolean snapshot — it only changes
    // when crossing the top — so reading it directly needs no derivedStateOf wrapper.
    val elevated = listState.canScrollBackward

    Scaffold(
        modifier = modifier,
        topBar = {
            FeedTopBar(
                elevated = elevated,
                onOpenSettings = onOpenSettings,
            )
        },
    ) { contentPadding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = actions::refresh,
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding),
        ) {
            when (uiState) {
                HomeUiState.Loading -> LoadingState()
                is HomeUiState.Feed ->
                    if (uiState.posts.isEmpty()) {
                        EmptyState()
                    } else {
                        FeedList(
                            posts = uiState.posts,
                            endReached = uiState.endReached,
                            listState = listState,
                            actions = actions,
                            onOpenProfile = onOpenProfile,
                            onOpenVideo = onOpenVideo,
                            onOpenAlbum = onOpenAlbum,
                            onOpenPost = onOpenPost,
                        )
                    }
            }
        }
    }
}

/**
 * The feed's pinned top bar — it stays in place while the feed scrolls and fades in a drop shadow
 * whenever the list content is scrolled away from the top ([elevated]), so the shadow is a stable
 * indicator of scroll position rather than something that rides the bar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FeedTopBar(
    elevated: Boolean,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shadowElevation by animateDpAsState(
        targetValue = if (elevated) TopBarElevation else 0.dp,
        label = "topBarElevation",
    )

    TopAppBar(
        title = { MosaicWordmark() },
        actions = { SettingsAction(onOpenSettings) },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        modifier = modifier.shadow(shadowElevation),
    )
}

/** Resting shadow depth of the pinned feed bar once the list is scrolled. */
private val TopBarElevation = 4.dp

@Composable
private fun FeedList(
    posts: List<PostCardData>,
    endReached: Boolean,
    listState: LazyListState,
    actions: HomeActions,
    onOpenProfile: (userId: String) -> Unit,
    onOpenVideo: (Video) -> Unit,
    onOpenAlbum: (Album, Int) -> Unit,
    onOpenPost: (postId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val playback = LocalVideoPlayback.current
    // Read the latest posts inside the collector without keying the effect on them: a like or
    // bookmark toggle replaces the list instance but never changes which posts carry a video or
    // where they sit, so restarting the autoplay collector on every toggle is wasted work. A
    // change that actually matters (scroll, refresh) re-lays out the list and re-emits layoutInfo.
    val currentPosts by rememberUpdatedState(posts)
    val currentEndReached by rememberUpdatedState(endReached)
    val currentLoadMore by rememberUpdatedState(actions::loadMore)

    LaunchedEffect(listState, playback) {
        snapshotFlow { listState.layoutInfo }.collect { layoutInfo ->
            if (playback == null || playback.isFullscreen) return@collect
            val video = autoPlayVideo(layoutInfo, currentPosts)
            if (video != null) playback.playInline(video.id, video.videoUrl) else playback.stop()
        }
    }

    LaunchedEffect(listState) {
        snapshotFlow {
            val info = listState.layoutInfo
            val lastVisible = info.visibleItemsInfo.lastOrNull()?.index ?: return@snapshotFlow false
            !currentEndReached && lastVisible >= info.totalItemsCount - LOAD_MORE_PREFETCH
        }.distinctUntilChanged()
            .filter { it }
            .collect { currentLoadMore() }
    }

    LazyColumn(state = listState, modifier = modifier.fillMaxSize()) {
        items(posts, key = { it.post.id }) { data ->
            PostCard(
                data = data,
                onToggleLike = { actions.onToggleLike(data.post.id) },
                onToggleBookmark = { actions.onToggleBookmark(data.post.id) },
                onOpenProfile = { onOpenProfile(data.author.id) },
                onOpenVideo = onOpenVideo,
                onOpenAlbum = onOpenAlbum,
                onOpenPost = { onOpenPost(data.post.id) },
            )
        }
        if (endReached) {
            item(key = "caught_up") {
                CaughtUpFooter()
            }
        } else {
            item(key = "loading_more") {
                LoadingMoreFooter()
            }
        }
    }
}

private const val LOAD_MORE_PREFETCH = 3

@Composable
private fun LoadingState(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.feed_empty),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Spinner shown as the last list item while the next page is being fetched. */
@Composable
private fun LoadingMoreFooter(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
    }
}

/** End-of-feed marker shown as the last list item once there are no more posts to load. */
@Composable
private fun CaughtUpFooter(modifier: Modifier = Modifier) {
    Text(
        text = stringResource(R.string.feed_caught_up),
        style = MaterialTheme.typography.bodySmall.copy(letterSpacing = 0.02.em),
        color = LocalMosaicColors.current.textTertiary,
        textAlign = TextAlign.Center,
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 14.dp, bottom = 8.dp),
    )
}

/**
 * Returns the [Video] that should auto-play — the video post with the greatest on-screen fraction
 * that meets the 50 % visibility threshold — or null when no video qualifies.
 *
 * [LazyListItemInfo.index] maps 1:1 to [posts] because [FeedList] emits posts first (indices
 * 0..lastIndex) then optionally a footer item at [posts.size].
 */
private fun autoPlayVideo(layoutInfo: LazyListLayoutInfo, posts: List<PostCardData>): Video? {
    val viewportStart = layoutInfo.viewportStartOffset
    val viewportEnd = layoutInfo.viewportEndOffset
    fun visibleFraction(offset: Int, size: Int): Float {
        val visibleTop = maxOf(offset, viewportStart)
        val visibleBottom = minOf(offset + size, viewportEnd)
        return maxOf(0, visibleBottom - visibleTop).toFloat() / size
    }

    return layoutInfo.visibleItemsInfo
        .filter { it.index < posts.size && posts[it.index].post.video != null }
        .maxByOrNull { visibleFraction(it.offset, it.size) }
        ?.takeIf { visibleFraction(it.offset, it.size) >= 0.5f }
        ?.let { posts[it.index].post.video }
}

@Preview(showBackground = true)
@Composable
private fun HomeFeedPreview() {
    val users = SampleUsers.associateBy { it.id }
    val feed = SamplePosts.mapNotNull { post ->
        PostCardData(post, users[post.authorId] ?: return@mapNotNull null)
    }

    MosaicTheme {
        HomeScreen(
            uiState = HomeUiState.Feed(feed, endReached = true),
            isRefreshing = false,
            actions = createActionsProxy(),
            onOpenSettings = {},
            onOpenProfile = {},
            onOpenVideo = {},
            onOpenAlbum = { _, _ -> },
            onOpenPost = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeLoadingPreview() {
    MosaicTheme {
        HomeScreen(
            uiState = HomeUiState.Loading,
            isRefreshing = false,
            actions = createActionsProxy(),
            onOpenSettings = {},
            onOpenProfile = {},
            onOpenVideo = {},
            onOpenAlbum = { _, _ -> },
            onOpenPost = {},
        )
    }
}
