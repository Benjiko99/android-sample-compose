package uno.lux.sample.ui.post

import android.content.ClipData
import android.content.ClipboardManager
import android.os.Build
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.keyframes
import androidx.compose.foundation.background
import uno.lux.sample.ui.components.debouncedClickable
import uno.lux.sample.ui.components.rememberDebounced
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.core.app.ShareCompat
import androidx.core.content.getSystemService
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import uno.lux.sample.R
import uno.lux.sample.data.SampleComments
import uno.lux.sample.data.SamplePosts
import uno.lux.sample.data.SampleUsers
import uno.lux.sample.data.user.User
import uno.lux.sample.data.user.UserId
import uno.lux.sample.data.post.Album
import uno.lux.sample.data.post.Comment
import uno.lux.sample.data.post.CommentId
import uno.lux.sample.data.post.Post
import uno.lux.sample.data.post.PostId
import uno.lux.sample.data.post.Video
import uno.lux.sample.ui.components.Avatar
import uno.lux.sample.ui.format.asText
import uno.lux.sample.util.AppError
import uno.lux.sample.ui.home.AlbumPostGallery
import uno.lux.sample.ui.home.ReportPostDialog
import uno.lux.sample.ui.theme.LocalMosaicColors
import uno.lux.sample.ui.theme.MosaicTheme
import uno.lux.sample.ui.video.VideoPostPlayer
import uno.lux.sample.util.compactCount
import uno.lux.sample.util.postLink
import uno.lux.sample.util.relativeTime

/**
 * Stateful entry point: binds the [PostDetailViewModel] (keyed by [postId]) and forwards
 * state + intent to the stateless overload. The ViewModel store is per back-stack entry, so
 * each opened post gets its own instance, created on push and cleared on pop.
 */
@Composable
fun PostDetailScreen(
    postId: PostId,
    onBack: () -> Unit,
    onOpenProfile: (userId: UserId) -> Unit,
    onOpenVideo: (Video) -> Unit,
    onOpenAlbum: (Album, initialIndex: Int) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PostDetailViewModel = hiltViewModel<PostDetailViewModel, PostDetailViewModel.Factory>(
        creationCallback = { factory -> factory.create(postId) },
    ),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    PostDetailScreen(
        uiState = uiState,
        composerUserName = viewModel.composerUserName,
        onBack = onBack,
        onOpenProfile = onOpenProfile,
        onOpenVideo = onOpenVideo,
        onOpenAlbum = onOpenAlbum,
        onToggleLike = viewModel::onToggleLike,
        onToggleBookmark = viewModel::onToggleBookmark,
        onToggleCommentLike = viewModel::onToggleCommentLike,
        onAddComment = viewModel::addComment,
        onRetryComments = viewModel::retryComments,
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
    composerUserName: String,
    onBack: () -> Unit,
    onOpenProfile: (userId: UserId) -> Unit,
    onOpenVideo: (Video) -> Unit,
    onOpenAlbum: (Album, initialIndex: Int) -> Unit,
    onToggleLike: () -> Unit,
    onToggleBookmark: () -> Unit,
    onToggleCommentLike: (commentId: CommentId) -> Unit,
    onAddComment: (text: String) -> Unit,
    onRetryComments: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val loaded = (uiState as? PostDetailUiState.Loaded)
    val post = loaded?.post
    val author = loaded?.author

    Scaffold(
        modifier = modifier.imePadding(),
        topBar = {
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
                    if (post != null && author != null) {
                        PostDetailMoreButton(post = post, author = author, onToggleBookmark = onToggleBookmark)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
        bottomBar = {
            if (uiState is PostDetailUiState.Loaded) {
                CommentComposer(
                    userNickname = composerUserName,
                    onSend = onAddComment,
                )
            }
        },
    ) { contentPadding ->
        when (uiState) {
            PostDetailUiState.Loading -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }

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

            is PostDetailUiState.Loaded -> PostDetailContent(
                post = uiState.post,
                author = uiState.author,
                comments = uiState.comments,
                commentsError = uiState.commentsError,
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

// ── Top-bar overflow ───────────────────────────────────────────────────────────

@Composable
private fun PostDetailMoreButton(
    post: Post,
    author: User,
    onToggleBookmark: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var showSheet by remember { mutableStateOf(false) }
    var showReport by remember { mutableStateOf(false) }

    IconButton(onClick = ({ showSheet = true }).rememberDebounced(), modifier = modifier) {
        Icon(
            painter = painterResource(R.drawable.ic_more_vert),
            contentDescription = stringResource(R.string.post_more_options),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(22.dp),
        )
    }

    if (showSheet) {
        PostDetailMoreSheet(
            post = post,
            author = author,
            onDismiss = { showSheet = false },
            onToggleBookmark = onToggleBookmark,
            onReport = { showReport = true },
        )
    }

    if (showReport) {
        ReportPostDialog(
            onDismiss = { showReport = false },
            onSubmit = { _, _ ->
                Toast.makeText(context, R.string.report_sent, Toast.LENGTH_SHORT).show()
                showReport = false
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PostDetailMoreSheet(
    post: Post,
    author: User,
    onDismiss: () -> Unit,
    onToggleBookmark: () -> Unit,
    onReport: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val dismiss: () -> Unit = {
        scope.launch { sheetState.hide() }.invokeOnCompletion { if (!sheetState.isVisible) onDismiss() }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 22.dp, top = 4.dp, end = 22.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Avatar(name = author.nickname, size = 38.dp)
            Column(Modifier.weight(1f)) {
                Text(
                    text = author.nickname,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = post.title,
                    style = MaterialTheme.typography.bodySmall,
                    color = LocalMosaicColors.current.textTertiary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Spacer(Modifier.height(4.dp))

        MoreSheetRow(
            iconRes = R.drawable.ic_bookmark_border,
            label = stringResource(
                if (post.isBookmarked) R.string.post_menu_unsave else R.string.post_menu_save,
            ),
            onClick = { onToggleBookmark(); dismiss() },
        )
        MoreSheetRow(
            iconRes = R.drawable.ic_share,
            label = stringResource(R.string.post_menu_share),
            onClick = {
                ShareCompat.IntentBuilder(context)
                    .setType("text/plain")
                    .setSubject(post.title)
                    .setText(postLink(post.id))
                    .setChooserTitle(R.string.post_share_chooser)
                    .startChooser()
                dismiss()
            },
        )
        MoreSheetRow(
            iconRes = R.drawable.ic_link,
            label = stringResource(R.string.post_menu_copy_link),
            onClick = {
                val clipboard = context.getSystemService<ClipboardManager>()!!
                clipboard.setPrimaryClip(
                    ClipData.newPlainText(
                        context.getString(R.string.post_link_clip_label),
                        postLink(post.id),
                    ),
                )
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                    Toast.makeText(context, R.string.post_link_copied, Toast.LENGTH_SHORT).show()
                }
                dismiss()
            },
        )
        MoreSheetRow(
            iconRes = R.drawable.ic_visibility_off,
            label = stringResource(R.string.post_menu_hide),
            onClick = { dismiss() },
        )
        MoreSheetRow(
            iconRes = R.drawable.ic_volume_off,
            label = stringResource(R.string.post_menu_mute, author.handle),
            onClick = { dismiss() },
        )
        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant,
            modifier = Modifier.padding(horizontal = 22.dp, vertical = 6.dp),
        )
        MoreSheetRow(
            iconRes = R.drawable.ic_flag,
            label = stringResource(R.string.post_menu_report),
            danger = true,
            onClick = { onReport(); dismiss() },
        )
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun MoreSheetRow(
    iconRes: Int,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    danger: Boolean = false,
) {
    val contentColor = if (danger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
    val iconColor = if (danger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        modifier = modifier
            .fillMaxWidth()
            .debouncedClickable(onClick = onClick)
            .padding(horizontal = 22.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(22.dp),
        )
        Text(text = label, style = MaterialTheme.typography.bodyLarge, color = contentColor)
    }
}

// ── Scrollable content ─────────────────────────────────────────────────────────

@Composable
private fun PostDetailContent(
    post: Post,
    author: User,
    comments: List<Comment>,
    commentsError: AppError?,
    onOpenProfile: (userId: UserId) -> Unit,
    onOpenVideo: (Video) -> Unit,
    onOpenAlbum: (Album, initialIndex: Int) -> Unit,
    onToggleLike: () -> Unit,
    onToggleBookmark: () -> Unit,
    onToggleCommentLike: (commentId: CommentId) -> Unit,
    onRetryComments: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
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
            item(key = "comments_error") { CommentsError(error = commentsError, onRetry = onRetryComments) }
        } else {
            item(key = "comments_header") { CommentsHeader(count = comments.size) }
            items(comments, key = { it.id }) { comment ->
                CommentRow(
                    comment = comment,
                    onLike = { onToggleCommentLike(comment.id) },
                    onOpenProfile = { onOpenProfile(comment.author.id) },
                )
            }
        }

        item { Spacer(Modifier.height(8.dp)) }
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

@Composable
private fun DetailPostCard(
    post: Post,
    author: User,
    onOpenProfile: () -> Unit,
    onOpenVideo: (Video) -> Unit,
    onOpenAlbum: (Album, initialIndex: Int) -> Unit,
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
        DetailPostHeader(post = post, author = author, onOpenProfile = onOpenProfile)
        DetailPostBody(title = post.title, body = post.body)
        if (post.album != null) {
            Spacer(Modifier.height(12.dp))
            AlbumPostGallery(
                album = post.album,
                onOpenImage = { index -> onOpenAlbum(post.album, index) },
            )
        }
        if (post.video != null) {
            Spacer(Modifier.height(12.dp))
            VideoPostPlayer(
                video = post.video,
                onOpenFullscreen = onOpenVideo,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }
        DetailPostActions(
            post = post,
            onToggleLike = onToggleLike,
            onToggleBookmark = onToggleBookmark,
            onScrollToComments = onScrollToComments,
        )
        Spacer(Modifier.height(4.dp))
    }
}

@Composable
private fun DetailPostHeader(
    post: Post,
    author: User,
    onOpenProfile: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 14.dp, top = 14.dp, end = 14.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(12.dp))
                .debouncedClickable(onClick = onOpenProfile)
                .padding(vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Avatar(name = author.nickname, size = 42.dp)
            Spacer(Modifier.width(11.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = author.nickname,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "${author.handle} · ${relativeTime(post.createdAt).asText()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = LocalMosaicColors.current.textTertiary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun DetailPostBody(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun DetailPostActions(
    post: Post,
    onToggleLike: () -> Unit,
    onToggleBookmark: () -> Unit,
    onScrollToComments: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val likeTint by animateColorAsState(
        targetValue = if (post.isLiked) LocalMosaicColors.current.like else muted,
        label = "likeTint",
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 12.dp, top = 12.dp, end = 12.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DetailActionButton(
            iconRes = if (post.isLiked) R.drawable.ic_favorite_filled else R.drawable.ic_favorite_border,
            contentDescriptionRes = if (post.isLiked) R.string.post_action_unlike else R.string.post_action_like,
            label = compactCount(post.likeCount).asText(),
            tint = likeTint,
            onClick = onToggleLike,
            iconModifier = Modifier.pop(post.isLiked),
        )
        Spacer(Modifier.width(2.dp))
        DetailActionButton(
            iconRes = R.drawable.ic_comment,
            contentDescriptionRes = R.string.post_action_comment,
            label = compactCount(post.commentCount).asText(),
            tint = muted,
            onClick = onScrollToComments,
        )
        Spacer(Modifier.weight(1f))
        DetailActionButton(
            iconRes = if (post.isBookmarked) R.drawable.ic_bookmark else R.drawable.ic_bookmark_border,
            contentDescriptionRes = if (post.isBookmarked) R.string.post_action_unbookmark else R.string.post_action_bookmark,
            label = null,
            tint = if (post.isBookmarked) MaterialTheme.colorScheme.primary else muted,
            onClick = onToggleBookmark,
            iconModifier = Modifier.pop(post.isBookmarked),
        )
    }
}

@Composable
private fun DetailActionButton(
    iconRes: Int,
    @StringRes contentDescriptionRes: Int,
    label: String?,
    tint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconModifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(100.dp))
            .debouncedClickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = stringResource(contentDescriptionRes),
            tint = tint,
            modifier = Modifier
                .size(23.dp)
                .then(iconModifier),
        )
        if (label != null) {
            Spacer(Modifier.width(7.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = tint,
            )
        }
    }
}

private val PopEasing = CubicBezierEasing(0.2f, 1.3f, 0.5f, 1f)

@Composable
private fun Modifier.pop(active: Boolean): Modifier {
    val scale = remember { Animatable(1f) }
    var wasActive by remember { mutableStateOf(active) }

    LaunchedEffect(active) {
        if (active != wasActive) {
            scale.snapTo(1f)
            scale.animateTo(
                targetValue = 1f,
                animationSpec = keyframes {
                    durationMillis = 400
                    1f at 0 using PopEasing
                    1.35f at 140 using PopEasing
                    0.9f at 240 using PopEasing
                    1f at 400
                },
            )
        }
        wasActive = active
    }

    return graphicsLayer {
        scaleX = scale.value
        scaleY = scale.value
    }
}

// ── Comments section ───────────────────────────────────────────────────────────

@Composable
private fun CommentsHeader(
    count: Int,
    modifier: Modifier = Modifier,
) {
    Text(
        text = stringResource(R.string.post_detail_comments_header, count),
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
            Avatar(name = comment.author.nickname, size = 36.dp)
        }

        Column(Modifier.weight(1f)) {
            // Inline "name  body" — author bold, text regular — as one reflowing block.
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)) {
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
                    text = stringResource(R.string.comment_like_count, comment.likeCount),
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
    userNickname: String,
    onSend: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var text by rememberSaveable { mutableStateOf("") }
    val send = {
        val trimmed = text.trim()
        if (trimmed.isNotEmpty()) {
            onSend(trimmed)
            text = ""
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
            Avatar(name = userNickname, size = 32.dp)
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                placeholder = {
                    Text(
                        text = stringResource(R.string.post_detail_comment_hint),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { send() }),
                shape = RoundedCornerShape(100.dp),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium,
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.background,
                    focusedContainerColor = MaterialTheme.colorScheme.background,
                ),
                modifier = Modifier.weight(1f),
            )
            val hasText = text.isNotBlank()
            val sendTint by animateColorAsState(
                targetValue = if (hasText) MaterialTheme.colorScheme.primary else muted,
                label = "sendTint",
            )

            IconButton(onClick = send, enabled = hasText) {
                Icon(
                    painter = painterResource(R.drawable.ic_send),
                    contentDescription = stringResource(R.string.post_detail_send_comment),
                    tint = sendTint,
                    modifier = Modifier.size(22.dp),
                )
            }
        }
    }
}

private val muted @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant

// ── Previews ───────────────────────────────────────────────────────────────────

@Preview(showBackground = true)
@Composable
private fun PostDetailLoadedPreview() {
    MosaicTheme {
        PostDetailScreen(
            uiState = PostDetailUiState.Loaded(
                post = SamplePosts.first(),
                author = SampleUsers.first(),
                comments = SampleComments["p1"] ?: emptyList(),
            ),
            composerUserName = SampleUsers.first().nickname,
            onBack = {},
            onOpenProfile = {},
            onOpenVideo = {},
            onOpenAlbum = { _, _ -> },
            onToggleLike = {},
            onToggleBookmark = {},
            onToggleCommentLike = {},
            onAddComment = {},
            onRetryComments = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PostDetailLoadingPreview() {
    MosaicTheme {
        PostDetailScreen(
            uiState = PostDetailUiState.Loading,
            composerUserName = "",
            onBack = {},
            onOpenProfile = {},
            onOpenVideo = {},
            onOpenAlbum = { _, _ -> },
            onToggleLike = {},
            onToggleBookmark = {},
            onToggleCommentLike = {},
            onAddComment = {},
            onRetryComments = {},
        )
    }
}
