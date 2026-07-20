package uno.lux.sample.ui.post

import androidx.annotation.StringRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.keyframes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import uno.lux.sample.R
import uno.lux.sample.data.post.Post
import uno.lux.sample.data.post.Video
import uno.lux.sample.data.user.User
import uno.lux.sample.ui.components.Avatar
import uno.lux.sample.ui.components.debouncedClickable
import uno.lux.sample.ui.format.asText
import uno.lux.sample.ui.theme.LocalMosaicColors
import uno.lux.sample.ui.video.VideoPostPlayer
import uno.lux.sample.util.compactCount
import uno.lux.sample.util.relativeTime

/**
 * The shared anatomy of a post — header, body, media, actions — composed both by [PostCard]
 * (the feed and profile rows) and by the post detail screen. The two differ only in what they
 * hang off these blocks: the feed card adds an overflow menu and a divider and opens the post on
 * tap, while the detail screen renders the same post as its non-clickable subject. Every block is
 * stateless with hoisted callbacks.
 */

/**
 * Avatar + identity (nickname, `handle · time`) as one tappable target that opens the author's
 * profile, with an optional [trailing] affordance — the feed's overflow menu — kept as its own
 * target beside it.
 */
@Composable
internal fun PostAuthorHeader(
    post: Post,
    author: User,
    onOpenProfile: () -> Unit,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
) {
    // A trailing IconButton already carries the inset its 48dp touch target needs, so the row's
    // own end padding tightens to match when one is present.
    val endPadding = if (trailing == null) 14.dp else 6.dp

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 14.dp, top = 14.dp, end = endPadding, bottom = 10.dp),
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
            Avatar(
                userId = author.id,
                name = author.nickname,
                size = 42.dp,
                imageUrl = author.avatarUrl,
            )
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

        trailing?.invoke()
    }
}

/**
 * The post's title (Bricolage) over its body (Manrope). [onClick] is null on the detail screen,
 * where the post is already the subject and there is nowhere left to open.
 */
@Composable
internal fun PostBody(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val clickable = if (onClick == null) Modifier else Modifier.debouncedClickable(onClick = onClick)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .then(clickable)
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

/** A post's attached media, if any: an album's photo strip and/or its video player. */
@Composable
internal fun PostMedia(
    post: Post,
    onOpenVideo: (Video) -> Unit,
    onOpenAlbum: (List<String>, initialIndex: Int) -> Unit,
) {
    if (post.album != null) {
        Spacer(Modifier.height(12.dp))
        AlbumPostGallery(
            album = post.album,
            onOpenImage = { index -> onOpenAlbum(post.album.images, index) },
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
}

/**
 * The like / comment / bookmark row. [onCommentClick] opens the post from the feed and scrolls to
 * the thread on the detail screen — the button is otherwise the same.
 *
 * [isOwn] grays out and disables the like button: a like endorses someone else's post, so an
 * author cannot like their own (the server rejects it with a 403 either way). Bookmarking and
 * commenting on your own post stay available — only the endorsement is barred.
 */
@Composable
internal fun PostActions(
    post: Post,
    isOwn: Boolean,
    onToggleLike: () -> Unit,
    onToggleBookmark: () -> Unit,
    onCommentClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    // Crossfade the heart between muted and coral so the color change tracks the like pop
    // (and fades back the same way on unlike) rather than snapping.
    val likeTint by animateColorAsState(
        targetValue = when {
            isOwn -> muted.copy(alpha = DisabledAlpha)
            post.isLiked -> LocalMosaicColors.current.like
            else -> muted
        },
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
            contentDescriptionRes = when {
                isOwn -> R.string.post_action_like_own
                post.isLiked -> R.string.post_action_unlike
                else -> R.string.post_action_like
            },
            label = compactCount(post.likeCount).asText(),
            tint = likeTint,
            onClick = onToggleLike,
            enabled = !isOwn,
            iconModifier = Modifier.pop(post.isLiked),
        )
        Spacer(Modifier.width(2.dp))
        ActionButton(
            iconRes = R.drawable.ic_comment,
            contentDescriptionRes = R.string.post_action_comment,
            label = compactCount(post.commentCount).asText(),
            tint = muted,
            onClick = onCommentClick,
        )
        Spacer(Modifier.weight(1f))
        ActionButton(
            iconRes = if (post.isBookmarked) R.drawable.ic_bookmark else R.drawable.ic_bookmark_border,
            contentDescriptionRes = if (post.isBookmarked) R.string.post_action_unbookmark else R.string.post_action_bookmark,
            label = null,
            tint = if (post.isBookmarked) MaterialTheme.colorScheme.primary else muted,
            onClick = onToggleBookmark,
            iconModifier = Modifier.pop(post.isBookmarked),
        )
    }
}

/** The tint alpha of an action that is shown but barred — Material's disabled-content value. */
private const val DisabledAlpha = 0.38f

/**
 * Pill-shaped action: icon + optional count, both tinted by [tint] (accented when active).
 * A disabled button keeps its place and its count — it is grayed out, not hidden — but takes
 * no clicks and reports its own state to accessibility.
 */
@Composable
private fun ActionButton(
    iconRes: Int,
    @StringRes contentDescriptionRes: Int,
    label: String?,
    tint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    iconModifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(100.dp))
            .debouncedClickable(enabled = enabled, onClick = onClick)
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

/** The Mosaic "pop": overshoot to 1.35×, dip to 0.9×, settle — the design's like/save curve. */
private val PopEasing = CubicBezierEasing(0.2f, 1.3f, 0.5f, 1f)

/**
 * Plays the Mosaic "pop" (`@keyframes pop` in the design — scale 1 → 1.35 → 0.9 → 1 over 400ms)
 * whenever [active] *changes*, the shared feedback for toggling like and save. Like the design,
 * it pops in both directions — liking and unliking, saving and unsaving. The current value is
 * captured up front so a post scrolling back into view doesn't replay it; only an actual toggle
 * animates. Scaling happens in a [graphicsLayer], so the pop draws over the row without
 * reflowing it.
 */
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
                    1.35f at 140 using PopEasing // 35%
                    0.9f at 240 using PopEasing // 60%
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
