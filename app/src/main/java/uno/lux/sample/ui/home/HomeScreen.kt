package uno.lux.sample.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import uno.lux.sample.R
import uno.lux.sample.data.Post
import uno.lux.sample.data.SamplePosts
import uno.lux.sample.ui.theme.SampleTheme

/**
 * Stateful entry point: binds the [HomeViewModel] and forwards state and intent to the
 * stateless overload below.
 */
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    HomeScreen(
        uiState = uiState,
        onToggleLike = viewModel::onToggleLike,
        onToggleBookmark = viewModel::onToggleBookmark,
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
    onToggleLike: (postId: String) -> Unit,
    onToggleBookmark: (postId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.nav_home)) },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { contentPadding ->
        when (uiState) {
            HomeUiState.Loading -> LoadingState(Modifier.padding(contentPadding))
            is HomeUiState.Feed ->
                if (uiState.posts.isEmpty()) {
                    EmptyState(Modifier.padding(contentPadding))
                } else {
                    FeedList(
                        posts = uiState.posts,
                        onToggleLike = onToggleLike,
                        onToggleBookmark = onToggleBookmark,
                        contentPadding = contentPadding,
                    )
                }
        }
    }
}

@Composable
private fun FeedList(
    posts: List<Post>,
    onToggleLike: (postId: String) -> Unit,
    onToggleBookmark: (postId: String) -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(
            top = contentPadding.calculateTopPadding() + 8.dp,
            bottom = contentPadding.calculateBottomPadding() + 16.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(posts, key = { it.id }) { post ->
            PostCard(
                post = post,
                onToggleLike = { onToggleLike(post.id) },
                onToggleBookmark = { onToggleBookmark(post.id) },
            )
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

@Preview(showBackground = true)
@Composable
private fun HomeFeedPreview() {
    SampleTheme {
        HomeScreen(
            uiState = HomeUiState.Feed(SamplePosts),
            onToggleLike = {},
            onToggleBookmark = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeLoadingPreview() {
    SampleTheme {
        HomeScreen(
            uiState = HomeUiState.Loading,
            onToggleLike = {},
            onToggleBookmark = {},
        )
    }
}
