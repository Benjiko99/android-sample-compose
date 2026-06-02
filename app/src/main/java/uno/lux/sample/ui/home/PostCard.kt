package uno.lux.sample.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import uno.lux.sample.R
import uno.lux.sample.data.Post
import uno.lux.sample.data.SamplePosts
import uno.lux.sample.data.User
import uno.lux.sample.ui.components.Avatar
import uno.lux.sample.ui.theme.SampleTheme
import uno.lux.sample.util.formatCount
import uno.lux.sample.util.formatRelativeTime
import java.time.Instant

/**
 * A single feed post: author header with an overflow menu, the title and body, and the
 * like / comment / bookmark actions. Stateless — every interaction is hoisted to the
 * caller, which keeps the card trivially previewable.
 */
@Composable
internal fun PostCard(
    post: Post,
    onToggleLike: () -> Unit,
    onToggleBookmark: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            PostHeader(author = post.author, createdAt = post.createdAt)
            Spacer(Modifier.height(12.dp))
            Text(
                text = post.title,
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = post.body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            PostActions(
                post = post,
                onToggleLike = onToggleLike,
                onToggleBookmark = onToggleBookmark,
            )
        }
    }
}

@Composable
private fun PostHeader(
    author: User,
    createdAt: Instant,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Avatar(name = author.nickname, id = author.id)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = author.nickname,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "${author.handle} · ${formatRelativeTime(createdAt)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        OverflowMenu()
    }
}

@Composable
private fun OverflowMenu(modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier) {
        IconButton(onClick = { expanded = true }) {
            Icon(
                painter = painterResource(R.drawable.ic_more_vert),
                contentDescription = "More options",
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(text = { Text("Share") }, onClick = { expanded = false })
            DropdownMenuItem(text = { Text("Mute author") }, onClick = { expanded = false })
            DropdownMenuItem(text = { Text("Report") }, onClick = { expanded = false })
        }
    }
}

@Composable
private fun PostActions(
    post: Post,
    onToggleLike: () -> Unit,
    onToggleBookmark: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ActionButton(
            iconRes = if (post.isLiked) R.drawable.ic_favorite else R.drawable.ic_favorite_border,
            contentDescription = if (post.isLiked) "Unlike" else "Like",
            label = formatCount(post.likeCount),
            tint = if (post.isLiked) MaterialTheme.colorScheme.primary else LocalContentColor.current,
            onClick = onToggleLike,
        )
        Spacer(Modifier.width(4.dp))
        ActionButton(
            iconRes = R.drawable.ic_comment,
            contentDescription = "Comment",
            label = formatCount(post.commentCount),
            onClick = { /* Opens the comment thread in a later iteration. */ },
        )
        Spacer(Modifier.weight(1f))
        IconButton(onClick = onToggleBookmark) {
            Icon(
                painter = painterResource(
                    if (post.isBookmarked) R.drawable.ic_bookmark else R.drawable.ic_bookmark_border,
                ),
                contentDescription = if (post.isBookmarked) "Remove bookmark" else "Bookmark",
                tint = if (post.isBookmarked) {
                    MaterialTheme.colorScheme.primary
                } else {
                    LocalContentColor.current
                },
            )
        }
    }
}

/** Icon + count, styled as a tappable pill. Used for like and comment. */
@Composable
private fun ActionButton(
    iconRes: Int,
    contentDescription: String,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current,
) {
    Row(
        modifier = modifier
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = tint,
        )
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
            modifier = Modifier.padding(16.dp),
        )
    }
}
