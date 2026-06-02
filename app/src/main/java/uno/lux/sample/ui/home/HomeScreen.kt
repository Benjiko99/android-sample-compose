package uno.lux.sample.ui.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlin.math.roundToInt
import uno.lux.sample.R
import uno.lux.sample.data.Post
import uno.lux.sample.data.SamplePosts
import uno.lux.sample.ui.components.MosaicWordmark
import uno.lux.sample.ui.components.SettingsAction
import uno.lux.sample.ui.theme.LocalMosaicColors
import uno.lux.sample.ui.theme.SampleTheme

/**
 * Stateful entry point: binds the [HomeViewModel] and forwards state and intent to the
 * stateless overload below.
 */
@Composable
fun HomeScreen(
    onOpenSettings: () -> Unit,
    onOpenProfile: (userId: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    HomeScreen(
        uiState = uiState,
        isRefreshing = isRefreshing,
        onRefresh = viewModel::refresh,
        onToggleLike = viewModel::onToggleLike,
        onToggleBookmark = viewModel::onToggleBookmark,
        onOpenSettings = onOpenSettings,
        onOpenProfile = onOpenProfile,
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
    onRefresh: () -> Unit,
    onToggleLike: (postId: String) -> Unit,
    onToggleBookmark: (postId: String) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenProfile: (userId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            CollapsingTopBar(
                scrollBehavior = scrollBehavior,
                onOpenSettings = onOpenSettings,
            )
        },
    ) { contentPadding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = onRefresh,
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
                            onToggleLike = onToggleLike,
                            onToggleBookmark = onToggleBookmark,
                            onOpenProfile = onOpenProfile,
                        )
                    }
            }
        }
    }
}

/**
 * The feed's top bar. When the feed is scrolled up the bar slides off the top edge as a rigid
 * unit — and slides back in when the user scrolls down — instead of the Material default of
 * collapsing its height and squashing its content. It reuses [enterAlwaysScrollBehavior]'s
 * `heightOffset` (so the scroll / fling / re-enter logic is unchanged), but lays the [TopAppBar]
 * out at full height and reports only the still-visible slice to the [Scaffold]: the bar is placed
 * flush to the bottom of that shrinking slot, so its top rides up past the clipped edge while the
 * feed rises to fill the freed space. Reading `heightOffset` in the layout phase keeps the bar's
 * content from recomposing on every scroll frame.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CollapsingTopBar(
    scrollBehavior: TopAppBarScrollBehavior,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val barHeightPx = WindowInsets.statusBars.getTop(density) +
        with(density) { TopBarContentHeight.roundToPx() }

    // This bar is the sole consumer of the scroll state, so it owns the collapse limit.
    SideEffect {
        val limit = -barHeightPx.toFloat()
        if (scrollBehavior.state.heightOffsetLimit != limit) {
            scrollBehavior.state.heightOffsetLimit = limit
        }
    }

    TopAppBar(
        title = { MosaicWordmark() },
        actions = { SettingsAction(onOpenSettings) },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        modifier = modifier
            .clipToBounds()
            .layout { measurable, constraints ->
                val placeable = measurable.measure(
                    constraints.copy(minHeight = barHeightPx, maxHeight = barHeightPx),
                )
                val visibleHeight = (barHeightPx + scrollBehavior.state.heightOffset)
                    .roundToInt()
                    .coerceIn(0, barHeightPx)
                layout(placeable.width, visibleHeight) {
                    placeable.place(0, visibleHeight - barHeightPx)
                }
            },
    )
}

/**
 * Material 3's small top-app-bar content height (the `TopAppBarSmallTokens.ContainerHeight` token);
 * the bar's full height is this plus the status-bar inset.
 */
private val TopBarContentHeight = 64.dp

@Composable
private fun FeedList(
    posts: List<Post>,
    endReached: Boolean,
    onToggleLike: (postId: String) -> Unit,
    onToggleBookmark: (postId: String) -> Unit,
    onOpenProfile: (userId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier = modifier.fillMaxSize()) {
        items(posts, key = { it.id }) { post ->
            PostCard(
                post = post,
                onToggleLike = { onToggleLike(post.id) },
                onToggleBookmark = { onToggleBookmark(post.id) },
                onOpenProfile = { onOpenProfile(post.author.id) },
            )
        }
        if (endReached) {
            item(key = "caught_up") {
                CaughtUpFooter()
            }
        }
    }
}

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

@Preview(showBackground = true)
@Composable
private fun HomeFeedPreview() {
    SampleTheme {
        HomeScreen(
            uiState = HomeUiState.Feed(SamplePosts, endReached = true),
            isRefreshing = false,
            onRefresh = {},
            onToggleLike = {},
            onToggleBookmark = {},
            onOpenSettings = {},
            onOpenProfile = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeLoadingPreview() {
    SampleTheme {
        HomeScreen(
            uiState = HomeUiState.Loading,
            isRefreshing = false,
            onRefresh = {},
            onToggleLike = {},
            onToggleBookmark = {},
            onOpenSettings = {},
            onOpenProfile = {},
        )
    }
}
