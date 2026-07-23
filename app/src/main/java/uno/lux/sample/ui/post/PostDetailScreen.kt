package uno.lux.sample.ui.post

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import uno.lux.sample.R
import uno.lux.sample.data.SampleComments
import uno.lux.sample.data.SamplePosts
import uno.lux.sample.data.SampleUsers
import uno.lux.sample.data.post.Comment
import uno.lux.sample.data.post.CommentId
import uno.lux.sample.data.post.Post
import uno.lux.sample.data.post.PostId
import uno.lux.sample.data.post.Video
import uno.lux.sample.data.user.User
import uno.lux.sample.data.user.UserId
import uno.lux.sample.ui.components.Avatar
import uno.lux.sample.ui.components.FullScreenError
import uno.lux.sample.ui.components.FullScreenProgress
import uno.lux.sample.ui.components.debouncedClickable
import uno.lux.sample.ui.components.rememberDebounced
import uno.lux.sample.ui.format.asText
import uno.lux.sample.ui.theme.LocalMosaicColors
import uno.lux.sample.ui.theme.MosaicTheme
import uno.lux.sample.util.AppError
import uno.lux.sample.util.relativeTime

/**
 * Stateful entry point: binds the [PostDetailViewModel] (keyed by [postId]) and forwards
 * state + intent to the stateless overload. The ViewModel store is per back-stack entry, so
 * each opened post gets its own instance, created on push and cleared on pop.
 */
@Composable
fun PostDetailScreen(
    postId: PostId,
    modifier: Modifier = Modifier,
    viewModel: PostDetailViewModel = hiltViewModel<PostDetailViewModel, PostDetailViewModel.Factory>(
        creationCallback = { factory -> factory.create(postId) },
    ),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    PostDetailScreen(
        uiState = uiState,
        composerUser = viewModel.composerUser,
        onBack = viewModel::goBack,
        onOpenProfile = viewModel::openProfile,
        onOpenVideo = viewModel::openVideo,
        onOpenAlbum = viewModel::openAlbum,
        onToggleLike = viewModel::onToggleLike,
        onToggleBookmark = viewModel::onToggleBookmark,
        onToggleCommentLike = viewModel::onToggleCommentLike,
        onAddComment = viewModel::addComment,
        onRetryComments = viewModel::retryComments,
        onRetry = viewModel::retry,
        onDelete = viewModel::onDelete,
        modifier = modifier,
    )
}

/**
 * Stateless post detail screen — full post content (author header, title/body, media, actions)
 * followed by the comment thread, with a sticky comment composer at the bottom. Holding no
 * ViewModel makes it directly previewable and testable.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PostDetailScreen(
    uiState: PostDetailUiState,
    composerUser: User,
    onBack: () -> Unit,
    onOpenProfile: (userId: UserId) -> Unit,
    onOpenVideo: (Video) -> Unit,
    onOpenAlbum: (List<String>, initialIndex: Int) -> Unit,
    onToggleLike: () -> Unit,
    onToggleBookmark: () -> Unit,
    onToggleCommentLike: (commentId: CommentId) -> Unit,
    onAddComment: (text: String) -> Unit,
    onRetryComments: () -> Unit,
    onRetry: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val loaded = (uiState as? PostDetailUiState.Loaded)
    val listState = rememberLazyListState()
    val elevated = listState.canScrollBackward

    Scaffold(
        modifier = modifier.imePadding(),
        topBar = {
            val elevationPx = with(LocalDensity.current) { TopBarElevation.toPx() }
            val shadowElevation = remember { Animatable(0f) }

            LaunchedEffect(elevated, elevationPx) {
                shadowElevation.animateTo(if (elevated) elevationPx else 0f)
            }

            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.post_detail_title),
                        style = MaterialTheme.typography.titleLarge,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack.rememberDebounced()) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_back),
                            contentDescription = stringResource(R.string.navigate_back),
                        )
                    }
                },
                actions = {
                    if (loaded != null) {
                        PostOverflowMenu(
                            post = loaded.post,
                            author = loaded.author,
                            onToggleBookmark = onToggleBookmark,
                            onDelete = if (loaded.isOwn) onDelete else null,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
                modifier = Modifier.graphicsLayer { this.shadowElevation = shadowElevation.value },
            )
        },
        bottomBar = {
            if (loaded != null) {
                CommentComposer(
                    user = composerUser,
                    onSend = onAddComment,
                )
            }
        },
    ) { contentPadding ->
        when (uiState) {
            PostDetailUiState.Loading -> FullScreenProgress(
                modifier = Modifier.padding(contentPadding),
            )

            PostDetailUiState.NotFound -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding)
                    .padding(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.post_detail_not_found),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            is PostDetailUiState.Error -> FullScreenError(
                message = uiState.error.asText(),
                onRetry = onRetry,
                modifier = Modifier.padding(contentPadding),
            )

            is PostDetailUiState.Loaded -> PostDetailContent(
                post = uiState.post,
                author = uiState.author,
                comments = uiState.comments,
                commentsError = uiState.commentsError,
                commentsLoading = uiState.commentsLoading,
                listState = listState,
                onOpenProfile = onOpenProfile,
                onOpenVideo = onOpenVideo,
                onOpenAlbum = onOpenAlbum,
                onToggleLike = onToggleLike,
                onToggleBookmark = onToggleBookmark,
                onToggleCommentLike = onToggleCommentLike,
                onRetryComments = onRetryComments,
                modifier = Modifier.padding(contentPadding),
            )
        }
    }
}

/** Resting shadow depth of the pinned detail bar once the content is scrolled. */
private val TopBarElevation = 4.dp

// ── Scrollable content ─────────────────────────────────────────────────────────

@Composable
private fun PostDetailContent(
    post: Post,
    author: User,
    comments: List<Comment>,
    commentsError: AppError?,
    commentsLoading: Boolean,
    listState: LazyListState,
    onOpenProfile: (userId: UserId) -> Unit,
    onOpenVideo: (Video) -> Unit,
    onOpenAlbum: (List<String>, initialIndex: Int) -> Unit,
    onToggleLike: () -> Unit,
    onToggleBookmark: () -> Unit,
    onToggleCommentLike: (commentId: CommentId) -> Unit,
    onRetryComments: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()

    LazyColumn(state = listState, modifier = modifier.fillMaxSize()) {
        item {
            DetailPostCard(
                post = post,
                author = author,
                onOpenProfile = { onOpenProfile(author.id) },
                onOpenVideo = onOpenVideo,
                onOpenAlbum = onOpenAlbum,
                onToggleLike = onToggleLike,
                onToggleBookmark = onToggleBookmark,
                onScrollToComments = { scope.launch { listState.animateScrollToItem(index = 1) } },
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }

        if (commentsError != null) {
            item(key = "comments_error") {
                CommentsError(
                    error = commentsError,
                    onRetry = onRetryComments
                )
            }
        } else if (commentsLoading) {
            item(key = "comments_loading") { CommentsLoading() }
        } else {
            item(key = "comments_header") { CommentsHeader(count = comments.size) }
            if (comments.isNotEmpty()) {
                items(comments, key = { it.id }) { comment ->
                    CommentRow(
                        comment = comment,
                        onLike = { onToggleCommentLike(comment.id) },
                        onOpenProfile = { onOpenProfile(comment.author.id) },
                    )
                }
            } else {
                item(key = "empty_comments") {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(vertical = 32.dp, horizontal = 24.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.post_detail_no_comments),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        item { Spacer(Modifier.height(8.dp)) }
    }
}

@Composable
private fun CommentsLoading() {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp, horizontal = 24.dp),
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun CommentsError(error: AppError, onRetry: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp, horizontal = 24.dp),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = error.asText(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Button(onClick = onRetry) {
                Text(stringResource(R.string.error_retry))
            }
        }
    }
}

// ── Post card inside the detail ────────────────────────────────────────────────

/**
 * The post itself, rendered from the same blocks as the feed's [PostCard] — minus the affordances
 * that only make sense in a list: the body isn't a tap target (this *is* the post), the overflow
 * menu lives in the top bar, and the comment action scrolls to the thread below instead of
 * navigating.
 */
@Composable
private fun DetailPostCard(
    post: Post,
    author: User,
    onOpenProfile: () -> Unit,
    onOpenVideo: (Video) -> Unit,
    onOpenAlbum: (List<String>, initialIndex: Int) -> Unit,
    onToggleLike: () -> Unit,
    onToggleBookmark: () -> Unit,
    onScrollToComments: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface),
    ) {
        PostAuthorHeader(post = post, author = author, onOpenProfile = onOpenProfile)
        PostBody(title = post.title, body = post.body)
        PostMedia(post = post, onOpenVideo = onOpenVideo, onOpenAlbum = onOpenAlbum)
        PostActions(
            post = post,
            onToggleLike = onToggleLike,
            onToggleBookmark = onToggleBookmark,
            onCommentClick = onScrollToComments,
        )
        Spacer(Modifier.height(4.dp))
    }
}

// ── Comments section ───────────────────────────────────────────────────────────

@Composable
private fun CommentsHeader(
    count: Int,
    modifier: Modifier = Modifier,
) {
    Text(
        text = pluralStringResource(R.plurals.post_detail_comments_header, count, count),
        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.04.em),
        color = LocalMosaicColors.current.textTertiary,
        modifier = modifier.padding(start = 16.dp, top = 14.dp, end = 16.dp, bottom = 2.dp),
    )
}

@Composable
private fun CommentRow(
    comment: Comment,
    onLike: () -> Unit,
    onOpenProfile: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val likeTint by animateColorAsState(
        targetValue = if (comment.isLiked) LocalMosaicColors.current.like else muted,
        label = "commentLikeTint",
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 16.dp, top = 12.dp, end = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(11.dp),
    ) {
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .debouncedClickable(onClick = onOpenProfile),
        ) {
            Avatar(user = comment.author, size = 36.dp)
        }

        Column(Modifier.weight(1f)) {
            // Inline "name  body" — author bold, text regular — as one reflowing block.
            Text(
                text = buildAnnotatedString {
                    withStyle(
                        SpanStyle(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        append(comment.author.nickname)
                    }
                    append("  ")
                    withStyle(SpanStyle(color = MaterialTheme.colorScheme.onSurfaceVariant)) {
                        append(comment.text)
                    }
                },
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.height(6.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = relativeTime(comment.createdAt).asText(),
                    style = MaterialTheme.typography.labelMedium,
                    color = LocalMosaicColors.current.textTertiary,
                )
                Text(
                    text = pluralStringResource(
                        R.plurals.comment_like_count,
                        comment.likeCount,
                        comment.likeCount,
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    color = LocalMosaicColors.current.textTertiary,
                )
            }
        }

        IconButton(
            onClick = onLike.rememberDebounced(),
            modifier = Modifier.size(36.dp),
        ) {
            Icon(
                painter = painterResource(
                    if (comment.isLiked) R.drawable.ic_favorite_filled else R.drawable.ic_favorite_border,
                ),
                contentDescription = stringResource(
                    if (comment.isLiked) R.string.comment_action_unlike else R.string.comment_action_like,
                ),
                tint = likeTint,
                modifier = Modifier.size(18.dp),
            )
        }
    }
    Spacer(Modifier.height(12.dp))
}

// ── Sticky comment composer ────────────────────────────────────────────────────

@Composable
private fun CommentComposer(
    user: User,
    onSend: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val textState = rememberTextFieldState()
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val keyboardController = LocalSoftwareKeyboardController.current
    val send = {
        val trimmed = textState.text.toString().trim()
        if (trimmed.isNotEmpty()) {
            onSend(trimmed)
            textState.clearText()
            keyboardController?.hide()
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 14.dp, vertical = 10.dp)
                .navigationBarsPadding(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Avatar(user = user, size = 36.dp)
            OutlinedTextField(
                state = textState,
                placeholder = {
                    Text(
                        text = stringResource(R.string.post_detail_comment_hint),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                onKeyboardAction = { send() },
                shape = RoundedCornerShape(12.dp),
                lineLimits = TextFieldLineLimits.SingleLine,
                textStyle = MaterialTheme.typography.bodyMedium,
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.background,
                    focusedContainerColor = MaterialTheme.colorScheme.background,
                ),
                contentPadding = OutlinedTextFieldDefaults.contentPadding(
                    top = 8.dp,
                    bottom = 8.dp,
                ),
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 42.dp),
            )
            val hasText by remember { derivedStateOf { textState.text.isNotBlank() } }
            val sendTint by animateColorAsState(
                targetValue = if (hasText) MaterialTheme.colorScheme.primary else muted,
                label = "sendTint",
            )

            IconButton(onClick = send, enabled = hasText) {
                Icon(
                    painter = painterResource(R.drawable.ic_send),
                    contentDescription = stringResource(R.string.post_detail_send_comment),
                    tint = sendTint,
                    modifier = Modifier.size(28.dp),
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PostDetailLoadedPreview() {
    PostDetailPreview(
        PostDetailUiState.Loaded(
            post = SamplePosts.first(),
            author = SampleUsers.first(),
            comments = SampleComments["p1"] ?: emptyList(),
        )
    )
}

@Preview(showBackground = true)
@Composable
private fun PostDetailErrorPreview() {
    PostDetailPreview(PostDetailUiState.Error(AppError.NoConnection))
}

@Preview(showBackground = true)
@Composable
private fun PostDetailLoadingPreview() {
    PostDetailPreview(PostDetailUiState.Loading)
}

/** The screen with every callback stubbed, so each preview supplies only the state it shows. */
@Composable
private fun PostDetailPreview(uiState: PostDetailUiState) {
    MosaicTheme {
        PostDetailScreen(
            uiState = uiState,
            composerUser = SampleUsers.first(),
            onBack = {},
            onOpenProfile = {},
            onOpenVideo = {},
            onOpenAlbum = { _, _ -> },
            onToggleLike = {},
            onToggleBookmark = {},
            onToggleCommentLike = {},
            onAddComment = {},
            onRetryComments = {},
            onRetry = {},
            onDelete = {},
        )
    }
}
