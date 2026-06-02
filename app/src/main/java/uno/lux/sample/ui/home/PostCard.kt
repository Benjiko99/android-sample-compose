package uno.lux.sample.ui.home

import androidx.annotation.StringRes
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import uno.lux.sample.R
import uno.lux.sample.data.Post
import uno.lux.sample.data.SamplePosts
import uno.lux.sample.ui.components.Avatar
import uno.lux.sample.ui.format.asText
import uno.lux.sample.ui.theme.LocalMosaicColors
import uno.lux.sample.ui.theme.SampleTheme
import uno.lux.sample.util.compactCount
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
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface),
    ) {
        PostHeader(post = post, onToggleBookmark = onToggleBookmark)
        PostBody(title = post.title, body = post.body)
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
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 14.dp, top = 14.dp, end = 6.dp, bottom = 10.dp),
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
        OverflowMenu(post = post, onToggleBookmark = onToggleBookmark)
    }
}

@Composable
private fun OverflowMenu(
    post: Post,
    onToggleBookmark: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showSheet by remember { mutableStateOf(false) }
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
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PostOverflowSheet(
    post: Post,
    onDismiss: () -> Unit,
    onToggleBookmark: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
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
        SheetRow(R.drawable.ic_share, stringResource(R.string.post_menu_share), onClick = dismiss)
        SheetRow(R.drawable.ic_link, stringResource(R.string.post_menu_copy_link), onClick = dismiss)
        SheetRow(R.drawable.ic_visibility_off, stringResource(R.string.post_menu_hide), onClick = dismiss)
        SheetRow(
            iconRes = R.drawable.ic_volume_off,
            label = stringResource(R.string.post_menu_mute, post.author.handle),
            onClick = dismiss,
        )
        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant,
            modifier = Modifier.padding(horizontal = 22.dp, vertical = 6.dp),
        )
        SheetRow(R.drawable.ic_flag, stringResource(R.string.post_menu_report), danger = true, onClick = dismiss)
        Spacer(Modifier.height(16.dp))
    }
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
            tint = if (post.isLiked) LocalMosaicColors.current.like else muted,
            onClick = onToggleLike,
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
            modifier = Modifier.size(23.dp),
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

@Preview(showBackground = true)
@Composable
private fun PostCardPreview() {
    SampleTheme {
        PostCard(
            post = SamplePosts.first(),
            onToggleLike = {},
            onToggleBookmark = {},
        )
    }
}
