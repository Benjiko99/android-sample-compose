package uno.lux.sample.ui.create

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import uno.lux.sample.R
import uno.lux.sample.ui.components.DiscardChangesDialog
import uno.lux.sample.ui.components.debouncedClickable
import uno.lux.sample.ui.components.rememberDebounced
import uno.lux.sample.ui.format.asText
import uno.lux.sample.ui.theme.LocalMosaicColors
import uno.lux.sample.ui.theme.MosaicTheme
import uno.lux.sample.util.createActionsProxy

/**
 * The composer's ViewModel-backed intents, as one [Stable] seam the stateless
 * [CreatePostScreen] depends on — the field edits plus publishing and opening settings, both of
 * which the ViewModel routes through the injected `Navigator`. [CreatePostViewModel] implements
 * it, so the binder passes the ViewModel directly and a preview passes a no-op
 * [createActionsProxy].
 */
@Stable
interface CreatePostActions {
    fun onTitleChange(value: String)
    fun onBodyChange(value: String)
    fun onImagesPicked(uris: List<String>)
    fun onRemoveImage(uri: String)
    fun publish()
    fun goBack()
    fun dismissDiscardConfirmation()
    fun confirmDiscard()
}

/**
 * Stateful entry point: binds the [CreatePostViewModel] and forwards state and intent to the
 * stateless overload below. System back is routed through the ViewModel like the top bar's up
 * affordance, so both ask before dropping a part-written post.
 */
@Composable
fun CreatePostScreen(
    modifier: Modifier = Modifier,
    viewModel: CreatePostViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val pickImages = rememberLauncherForActivityResult(
        // The picker caps the selection itself, so the user can't overshoot the album limit;
        // the ViewModel still trims, since a second trip through the picker could push it over.
        ActivityResultContracts.PickMultipleVisualMedia(CreatePostMaxImages)
    ) { uris ->
        // The picker's session-scoped read grant is enough — the bytes are read and uploaded
        // on publish, so no persistable permission is needed.
        if (uris.isNotEmpty()) viewModel.onImagesPicked(uris.map { it.toString() })
    }

    BackHandler {
        viewModel.goBack()
    }

    CreatePostScreen(
        uiState = uiState,
        actions = viewModel,
        onPickImages = {
            pickImages.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        },
        modifier = modifier,
    )

    if (uiState.showDiscardConfirmation) {
        DiscardChangesDialog(
            onConfirm = viewModel::confirmDiscard,
            onDismiss = viewModel::dismissDiscardConfirmation,
        )
    }
}

/**
 * Stateless post composer — a title, a body, and up to [CreatePostMaxImages] photos. It is pushed
 * over the shell rather than being a tab, so the bar carries an up-affordance. Holding no
 * ViewModel makes it directly previewable and testable; [onPickImages] is passed in rather than
 * living on [CreatePostActions] because launching the system picker needs a composition-scoped
 * launcher, not a ViewModel.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CreatePostScreen(
    uiState: CreatePostUiState,
    actions: CreatePostActions,
    onPickImages: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val publishErrorMessage = uiState.publishError?.asText()

    LaunchedEffect(publishErrorMessage) {
        if (publishErrorMessage != null) snackbarHostState.showSnackbar(publishErrorMessage)
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.create_post_title)) },
                modifier = Modifier.shadow(4.dp),
                navigationIcon = {
                    IconButton(onClick = actions::goBack.rememberDebounced()) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_back),
                            contentDescription = stringResource(R.string.navigate_back),
                        )
                    }
                },
            )
        },
    ) { contentPadding ->
        CreatePostForm(
            form = uiState.form,
            isPublishing = uiState.isPublishing,
            actions = actions,
            onPickImages = onPickImages,
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .imePadding(),
        )
    }
}

@Composable
private fun CreatePostForm(
    form: CreatePostForm,
    isPublishing: Boolean,
    actions: CreatePostActions,
    onPickImages: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surface)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        OutlinedTextField(
            value = form.title,
            onValueChange = actions::onTitleChange,
            label = { Text(stringResource(R.string.create_post_title_label)) },
            singleLine = true,
            enabled = !isPublishing,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            supportingText = { Text("${form.title.length} / $CreatePostTitleMaxLength") },
            modifier = Modifier.fillMaxWidth(),
        )

        OutlinedTextField(
            value = form.body,
            onValueChange = actions::onBodyChange,
            label = { Text(stringResource(R.string.create_post_body_label)) },
            minLines = 8,
            enabled = !isPublishing,
            modifier = Modifier.fillMaxWidth(),
        )

        PostImages(
            imageUris = form.imageUris,
            canAddImages = form.canAddImages,
            enabled = !isPublishing,
            onPickImages = onPickImages,
            onRemoveImage = actions::onRemoveImage,
        )

        PublishButton(
            isPublishing = isPublishing,
            enabled = form.canPublish,
            onPublish = actions::publish,
        )
    }
}

/**
 * The picked photos as a horizontally scrolling strip of thumbnails, each with a remove
 * affordance, followed by the add-photos tile until the album limit is reached. Images are
 * optional, so an empty selection shows just the tile rather than an empty-state message.
 */
@Composable
private fun PostImages(
    imageUris: List<String>,
    canAddImages: Boolean,
    enabled: Boolean,
    onPickImages: () -> Unit,
    onRemoveImage: (uri: String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.create_post_photos_label),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            // Keyed by URI so removing one thumbnail doesn't recompose (or re-fetch) the rest.
            items(imageUris, key = { it }) { uri ->
                PostImageThumbnail(
                    uri = uri,
                    enabled = enabled,
                    onRemove = { onRemoveImage(uri) },
                    modifier = Modifier.animateItem(),
                )
            }

            if (canAddImages) {
                item(key = AddPhotosTileKey) {
                    AddPhotosTile(
                        enabled = enabled,
                        onClick = onPickImages,
                        modifier = Modifier.animateItem(),
                    )
                }
            }
        }

        Text(
            text = "${imageUris.size} / $CreatePostMaxImages",
            style = MaterialTheme.typography.bodySmall,
            color = LocalMosaicColors.current.textTertiary,
        )
    }
}

@Composable
private fun PostImageThumbnail(
    uri: String,
    enabled: Boolean,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.size(ThumbnailSize)) {
        AsyncImage(
            model = uri,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .clip(MaterialTheme.shapes.medium)
                .background(MaterialTheme.colorScheme.surfaceVariant),
        )

        // The scrim that keeps the icon legible over a light photo is an *inner* box: the
        // button itself is expanded to the 48dp minimum touch target, so drawing the
        // background on it directly would spill a large circle past the thumbnail's corner.
        IconButton(
            onClick = onRemove,
            enabled = enabled,
            modifier = Modifier.align(Alignment.TopEnd),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(24.dp)
                    .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.6f), CircleShape),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_close),
                    contentDescription = stringResource(R.string.create_post_remove_photo),
                    tint = MaterialTheme.colorScheme.inverseOnSurface,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

@Composable
private fun AddPhotosTile(
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val contentColor = MaterialTheme.colorScheme.onSurfaceVariant

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .size(ThumbnailSize)
            .clip(MaterialTheme.shapes.medium)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = MaterialTheme.shapes.medium,
            )
            .debouncedClickable(enabled = enabled, onClick = onClick),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_add),
            contentDescription = null,
            tint = contentColor,
        )

        Text(
            text = stringResource(R.string.create_post_add_photos),
            style = MaterialTheme.typography.labelSmall,
            color = contentColor,
        )
    }
}

/** The publish affordance: a filled button whose label yields to a spinner while a publish runs. */
@Composable
private fun PublishButton(
    isPublishing: Boolean,
    enabled: Boolean,
    onPublish: () -> Unit,
) {
    Button(
        onClick = onPublish,
        enabled = enabled && !isPublishing,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
    ) {
        if (isPublishing) {
            CircularProgressIndicator(
                strokeWidth = 2.5.dp,
                color = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(20.dp),
            )
        } else {
            Text(stringResource(R.string.create_post_publish))
        }
    }
}

private val ThumbnailSize = 88.dp

/** Stable list key for the trailing add tile, so it isn't confused with an image URI. */
private const val AddPhotosTileKey = "add-photos"

@Preview(showBackground = true)
@Composable
private fun CreatePostScreenPreview() {
    MosaicTheme {
        CreatePostScreen(
            uiState = CreatePostUiState(
                form = CreatePostForm(
                    title = "Difference Engine, sketch 12",
                    body = "Finally got the carry mechanism to behave.",
                ),
            ),
            actions = createActionsProxy(),
            onPickImages = {},
        )
    }
}
