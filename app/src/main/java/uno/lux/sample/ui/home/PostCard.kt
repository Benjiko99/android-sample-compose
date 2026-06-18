package uno.lux.sample.ui.home

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.ShareCompat
import androidx.core.content.getSystemService
import kotlinx.coroutines.launch
import uno.lux.sample.R
import uno.lux.sample.data.Post
import uno.lux.sample.data.SamplePosts
import uno.lux.sample.data.Video
import uno.lux.sample.ui.components.Avatar
import uno.lux.sample.ui.format.asText
import uno.lux.sample.ui.theme.LocalMosaicColors
import uno.lux.sample.ui.theme.MosaicTheme
import uno.lux.sample.ui.video.VideoPostPlayer
import uno.lux.sample.util.compactCount
import uno.lux.sample.util.postLink
import uno.lux.sample.util.relativeTime

/**
 * A single feed post (Mosaic design): an edge-to-edge surface item separated by a bottom
 * divider — author header with overflow menu, the title (Bricolage) and body (Manrope), and
 * the like / comment / bookmark actions. Stateless; every interaction is hoisted to the caller.
 */
@Composable
internal fun PostCard(
    post: Post,
    onToggleLike: () -> Unit,
    onToggleBookmark: () -> Unit,
    onOpenProfile: () -> Unit,
    onOpenVideo: (Video) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface),
    ) {
        PostHeader(
            post = post,
            onToggleBookmark = onToggleBookmark,
            onOpenProfile = onOpenProfile,
        )
        PostBody(title = post.title, body = post.body)
        if (post.album != null) {
            Spacer(Modifier.height(12.dp))
            AlbumPostGallery(album = post.album)
        }
        if (post.video != null) {
            Spacer(Modifier.height(12.dp))
            VideoPostPlayer(
                video = post.video,
                onOpenFullscreen = onOpenVideo,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }
        PostActions(
            post = post,
            onToggleLike = onToggleLike,
            onToggleBookmark = onToggleBookmark,
        )
        Spacer(Modifier.height(4.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}

@Composable
private fun PostHeader(
    post: Post,
    onToggleBookmark: () -> Unit,
    onOpenProfile: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 14.dp, top = 14.dp, end = 6.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Avatar + identity open the author's profile; the overflow stays its own target.
        Row(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(12.dp))
                .clickable(onClick = onOpenProfile)
                .padding(vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Avatar(name = post.author.nickname, size = 42.dp)
            Spacer(Modifier.width(11.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = post.author.nickname,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = stringResource(
                        R.string.post_byline,
                        post.author.handle,
                        relativeTime(post.createdAt).asText(),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = LocalMosaicColors.current.textTertiary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        OverflowMenu(post = post, onToggleBookmark = onToggleBookmark)
    }
}

@Composable
private fun OverflowMenu(
    post: Post,
    onToggleBookmark: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var showSheet by remember { mutableStateOf(false) }
    var showReportDialog by remember { mutableStateOf(false) }
    IconButton(onClick = { showSheet = true }, modifier = modifier) {
        Icon(
            painter = painterResource(R.drawable.ic_more_vert),
            contentDescription = stringResource(R.string.post_more_options),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(22.dp),
        )
    }
    if (showSheet) {
        PostOverflowSheet(
            post = post,
            onDismiss = { showSheet = false },
            onToggleBookmark = onToggleBookmark,
            onReport = { showReportDialog = true },
        )
    }
    if (showReportDialog) {
        ReportPostDialog(
            onDismiss = { showReportDialog = false },
            // Reporting isn't wired to a backend yet — acknowledge and discard the report.
            onSubmit = { _, _ ->
                context.toast(R.string.report_sent)
                showReportDialog = false
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PostOverflowSheet(
    post: Post,
    onDismiss: () -> Unit,
    onToggleBookmark: () -> Unit,
    onReport: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val dismiss: () -> Unit = {
        scope.launch { sheetState.hide() }.invokeOnCompletion {
            if (!sheetState.isVisible) onDismiss()
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 22.dp, top = 4.dp, end = 22.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Avatar(name = post.author.nickname, size = 38.dp)
            Column(Modifier.weight(1f)) {
                Text(
                    text = post.author.nickname,
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

        SheetRow(
            iconRes = R.drawable.ic_bookmark_border,
            label = stringResource(
                if (post.isBookmarked) R.string.post_menu_unsave else R.string.post_menu_save,
            ),
            onClick = { onToggleBookmark(); dismiss() },
        )
        SheetRow(
            iconRes = R.drawable.ic_share,
            label = stringResource(R.string.post_menu_share),
            onClick = { sharePostLink(context, post); dismiss() },
        )
        SheetRow(
            iconRes = R.drawable.ic_link,
            label = stringResource(R.string.post_menu_copy_link),
            onClick = { copyPostLink(context, post); dismiss() },
        )
        SheetRow(
            iconRes = R.drawable.ic_visibility_off,
            label = stringResource(R.string.post_menu_hide),
            onClick = { context.toast(R.string.action_not_implemented); dismiss() },
        )
        SheetRow(
            iconRes = R.drawable.ic_volume_off,
            label = stringResource(R.string.post_menu_mute, post.author.handle),
            onClick = { context.toast(R.string.action_not_implemented); dismiss() },
        )
        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant,
            modifier = Modifier.padding(horizontal = 22.dp, vertical = 6.dp),
        )
        SheetRow(
            iconRes = R.drawable.ic_flag,
            label = stringResource(R.string.post_menu_report),
            danger = true,
            onClick = { onReport(); dismiss() },
        )
        Spacer(Modifier.height(16.dp))
    }
}

/**
 * Hands the post's link to the system share sheet. The link is a placeholder until the app has
 * real deep links; the post title rides along as the subject for targets that use one (e.g. email).
 */
private fun sharePostLink(context: Context, post: Post) {
    ShareCompat.IntentBuilder(context)
        .setType("text/plain")
        .setSubject(post.title)
        .setText(postLink(post.id))
        .setChooserTitle(R.string.post_share_chooser)
        .startChooser()
}

/**
 * Copies the post's link to the clipboard. From Android 13 the system shows its own copy
 * confirmation, so the toast fallback is only needed on older versions.
 */
private fun copyPostLink(context: Context, post: Post) {
    val clipboard = context.getSystemService<ClipboardManager>()!!
    val label = context.getString(R.string.post_link_clip_label)
    clipboard.setPrimaryClip(ClipData.newPlainText(label, postLink(post.id)))
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        context.toast(R.string.post_link_copied)
    }
}

/** Shows a short toast for [messageRes] — the shared form behind the sheet's placeholder actions. */
private fun Context.toast(@StringRes messageRes: Int) {
    Toast.makeText(this, messageRes, Toast.LENGTH_SHORT).show()
}

@Composable
private fun SheetRow(
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
            .clickable(onClick = onClick)
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

@Composable
private fun PostBody(
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
private fun PostActions(
    post: Post,
    onToggleLike: () -> Unit,
    onToggleBookmark: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    // Crossfade the heart between muted and coral so the color change tracks the like pop
    // (and fades back the same way on unlike) rather than snapping.
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
        ActionButton(
            iconRes = if (post.isLiked) R.drawable.ic_favorite_filled else R.drawable.ic_favorite_border,
            contentDescriptionRes = if (post.isLiked) R.string.post_action_unlike else R.string.post_action_like,
            label = compactCount(post.likeCount).asText(),
            tint = likeTint,
            onClick = onToggleLike,
            iconModifier = Modifier.likePop(post.isLiked),
        )
        Spacer(Modifier.width(2.dp))
        ActionButton(
            iconRes = R.drawable.ic_comment,
            contentDescriptionRes = R.string.post_action_comment,
            label = compactCount(post.commentCount).asText(),
            tint = muted,
            onClick = { /* Opens the comment thread in a later iteration. */ },
        )
        Spacer(Modifier.weight(1f))
        ActionButton(
            iconRes = if (post.isBookmarked) R.drawable.ic_bookmark else R.drawable.ic_bookmark_border,
            contentDescriptionRes = if (post.isBookmarked) R.string.post_action_unbookmark else R.string.post_action_bookmark,
            label = null,
            tint = if (post.isBookmarked) MaterialTheme.colorScheme.primary else muted,
            onClick = onToggleBookmark,
        )
    }
}

/** Pill-shaped action: icon + optional count, both tinted by [tint] (accented when active). */
@Composable
private fun ActionButton(
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
            .clickable(onClick = onClick)
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

/**
 * Scales the icon up and lets it spring back to rest whenever [active] turns true — the "pop"
 * when a post is liked. The current value is captured up front so an already-liked post
 * scrolling back into view doesn't replay it; only a fresh like (false → true) animates.
 * Scaling happens in a [graphicsLayer], so the pop draws over the row without reflowing it.
 */
@Composable
private fun Modifier.likePop(active: Boolean): Modifier {
    val scale = remember { Animatable(1f) }
    var wasActive by remember { mutableStateOf(active) }
    LaunchedEffect(active) {
        if (active && !wasActive) {
            scale.animateTo(1.3f, spring(stiffness = Spring.StiffnessHigh))
            scale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium,
                ),
            )
        }
        wasActive = active
    }
    return graphicsLayer {
        scaleX = scale.value
        scaleY = scale.value
    }
}

@Preview(showBackground = true)
@Composable
private fun PostCardPreview() {
    MosaicTheme {
        PostCard(
            post = SamplePosts.first(),
            onToggleLike = {},
            onToggleBookmark = {},
            onOpenProfile = {},
            onOpenVideo = {},
        )
    }
}
