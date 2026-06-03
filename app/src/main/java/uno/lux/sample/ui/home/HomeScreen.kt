package uno.lux.sample.ui.home

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import uno.lux.sample.R
import uno.lux.sample.data.Post
import uno.lux.sample.data.SamplePosts
import uno.lux.sample.ui.components.MosaicWordmark
import uno.lux.sample.ui.components.SettingsAction
import uno.lux.sample.ui.theme.LocalMosaicColors
import uno.lux.sample.ui.theme.MosaicTheme
import uno.lux.sample.util.ActionsInvocationHandler

/**
 * Everything the feed can ask its host to do — refresh, like / bookmark a post by id, open
 * settings, or open an author's profile. Bundled into one [Stable] interface so the screen takes
 * a single parameter rather than a callback each, keeps skipping recomposition when only its
 * data changes, and lets a preview supply a no-op proxy via
 * [ActionsInvocationHandler.createActionsProxy].
 */
@Stable
interface HomeActions {
    fun refresh()
    fun toggleLike(postId: String)
    fun toggleBookmark(postId: String)
    fun openSettings()
    fun openProfile(userId: String)
}

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
    val actions = remember(viewModel, onOpenSettings, onOpenProfile) {
        object : HomeActions {
            override fun refresh() = viewModel.refresh()
            override fun toggleLike(postId: String) = viewModel.onToggleLike(postId)
            override fun toggleBookmark(postId: String) = viewModel.onToggleBookmark(postId)
            override fun openSettings() = onOpenSettings()
            override fun openProfile(userId: String) = onOpenProfile(userId)
        }
    }
    HomeScreen(
        uiState = uiState,
        isRefreshing = isRefreshing,
        actions = actions,
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
                onOpenSettings = actions::openSettings,
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
    posts: List<Post>,
    endReached: Boolean,
    listState: LazyListState,
    actions: HomeActions,
    modifier: Modifier = Modifier,
) {
    LazyColumn(state = listState, modifier = modifier.fillMaxSize()) {
        items(posts, key = { it.id }) { post ->
            // Adapt the feed-level actions to this post: the card's argument-free callbacks map
            // onto HomeActions with the post's ids. Remembered per post so the card stays skippable.
            val cardActions = remember(post, actions) {
                object : PostCardActions {
                    override fun toggleLike() = actions.toggleLike(post.id)
                    override fun toggleBookmark() = actions.toggleBookmark(post.id)
                    override fun openProfile() = actions.openProfile(post.author.id)
                }
            }
            PostCard(post = post, actions = cardActions)
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
    MosaicTheme {
        HomeScreen(
            uiState = HomeUiState.Feed(SamplePosts, endReached = true),
            isRefreshing = false,
            actions = ActionsInvocationHandler.createActionsProxy(),
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
            actions = ActionsInvocationHandler.createActionsProxy(),
        )
    }
}
