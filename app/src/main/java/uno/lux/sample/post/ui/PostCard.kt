package uno.lux.sample.post.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import uno.lux.sample.app.fixtures.SamplePosts
import uno.lux.sample.app.fixtures.SampleUsers
import uno.lux.sample.video.Video
import uno.lux.sample.app.theme.MosaicTheme

/** In the feed the body is a perex — clipped to this many lines, with a "Show more" affordance. */
private const val FeedBodyMaxLines = 5

/**
 * A single feed post (Mosaic design): an edge-to-edge surface item separated by a bottom
 * divider — author header with overflow menu, the title (Bricolage) and body (Manrope), and
 * the like / comment / bookmark actions. Stateless; every interaction is hoisted to the caller.
 *
 * The blocks it composes are shared with the post detail screen — see [PostAuthorHeader] & co.
 */
@Composable
internal fun PostCard(
    data: PostCardData,
    onToggleLike: () -> Unit,
    onToggleBookmark: () -> Unit,
    onOpenProfile: () -> Unit,
    onOpenVideo: (Video) -> Unit,
    onOpenAlbum: (List<String>, initialIndex: Int) -> Unit,
    onOpenPost: () -> Unit,
    modifier: Modifier = Modifier,
    onDelete: (() -> Unit)? = null,
) {
    val post = data.post
    val author = data.author

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface),
    ) {
        PostAuthorHeader(
            post = post,
            author = author,
            onOpenProfile = onOpenProfile,
            // The avatar + identity open the author's profile; the overflow stays its own target.
            trailing = {
                PostOverflowMenu(
                    post = post,
                    author = author,
                    onToggleBookmark = onToggleBookmark,
                    onDelete = onDelete,
                )
            },
        )
        PostBody(
            title = post.title,
            body = post.body,
            maxBodyLines = FeedBodyMaxLines,
            onClick = onOpenPost,
        )
        PostMedia(post = post, onOpenVideo = onOpenVideo, onOpenAlbum = onOpenAlbum)
        PostActions(
            post = post,
            onToggleLike = onToggleLike,
            onToggleBookmark = onToggleBookmark,
            onCommentClick = onOpenPost,
        )
        Spacer(Modifier.height(4.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}

@Preview(showBackground = true)
@Composable
private fun PostCardPreview() {
    val users = SampleUsers.associateBy { it.id }
    val post = SamplePosts.first()

    MosaicTheme {
        PostCard(
            data = PostCardData(post, users.getValue(post.authorId)),
            onToggleLike = {},
            onToggleBookmark = {},
            onOpenProfile = {},
            onOpenVideo = {},
            onOpenAlbum = { _, _ -> },
            onOpenPost = {},
        )
    }
}
