package uno.lux.sample.post.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.app.ShareCompat
import androidx.core.content.getSystemService
import kotlinx.coroutines.launch
import uno.lux.sample.R
import uno.lux.sample.post.Post
import uno.lux.sample.user.User
import uno.lux.sample.user.ui.Avatar
import uno.lux.sample.app.util.debouncedClickable
import uno.lux.sample.app.util.rememberDebounced
import uno.lux.sample.app.theme.LocalMosaicColors

/**
 * The post's "⋮" affordance and everything behind it: a bottom sheet of save / share / copy-link /
 * report actions, plus the dialogs it can raise. Hosted by the feed card's header and by the
 * detail screen's top bar, so both offer the same menu from the same code.
 *
 * [onDelete] is null for a post the signed-in user didn't write, and the sheet then omits the
 * delete row entirely — a nullable action rather than a separate `canDelete` flag, so there is no
 * way for the two to disagree. Deleting is irreversible, so it is confirmed before it fires.
 */
@Composable
internal fun PostOverflowMenu(
    post: Post,
    author: User,
    onToggleBookmark: () -> Unit,
    modifier: Modifier = Modifier,
    onDelete: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    // Saveable so an open sheet or dialog survives the activity recreation a rotation causes.
    var showSheet by rememberSaveable { mutableStateOf(false) }
    var showReportDialog by rememberSaveable { mutableStateOf(false) }
    var showDeleteDialog by rememberSaveable { mutableStateOf(false) }

    IconButton(onClick = ({ showSheet = true }).rememberDebounced(), modifier = modifier) {
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
            author = author,
            onDismiss = { showSheet = false },
            onToggleBookmark = onToggleBookmark,
            onReport = { showReportDialog = true },
            onDelete = onDelete?.let { { showDeleteDialog = true } },
        )
    }

    if (showDeleteDialog) {
        DeletePostDialog(
            onDismiss = { showDeleteDialog = false },
            onConfirm = {
                showDeleteDialog = false
                onDelete?.invoke()
            },
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
    author: User,
    onDismiss: () -> Unit,
    onToggleBookmark: () -> Unit,
    onReport: () -> Unit,
    onDelete: (() -> Unit)?,
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
            Avatar(user = author, size = 38.dp)
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
        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant,
            modifier = Modifier.padding(horizontal = 22.dp, vertical = 6.dp),
        )
        SheetRow(
            iconRes = R.drawable.ic_flag,
            label = stringResource(R.string.post_menu_report),
            onClick = { onReport(); dismiss() },
        )
        if (onDelete != null) {
            SheetRow(
                iconRes = R.drawable.ic_delete,
                label = stringResource(R.string.post_menu_delete),
                danger = true,
                // Dismiss first: the confirmation dialog replaces the sheet rather than
                // stacking on top of it.
                onClick = { dismiss(); onDelete() },
            )
        }
        Spacer(Modifier.height(16.dp))
    }
}

/**
 * Hands the post's link to the system share sheet. The link comes from the server as [Post.url],
 * the post title rides along as the subject for targets that use one (e.g. email).
 */
private fun sharePostLink(context: Context, post: Post) {
    ShareCompat.IntentBuilder(context)
        .setType("text/plain")
        .setSubject(post.title)
        .setText(post.url)
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
    clipboard.setPrimaryClip(ClipData.newPlainText(label, post.url))

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
