package uno.lux.sample.ui.create

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import uno.lux.sample.R
import uno.lux.sample.ui.components.SettingsAction
import uno.lux.sample.ui.format.asText
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
    fun publish()
    fun discard()
    fun openSettings()
}

/**
 * Stateful entry point: binds the [CreatePostViewModel] and forwards state and intent to the
 * stateless overload below.
 */
@Composable
fun CreatePostScreen(
    modifier: Modifier = Modifier,
    viewModel: CreatePostViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    CreatePostScreen(
        uiState = uiState,
        actions = viewModel,
        modifier = modifier,
    )
}

/**
 * Stateless post composer — a title and a body, published from the top bar. It is a root tab
 * rather than a pushed page, so the bar carries no up-affordance; the discard action clears the
 * form instead of navigating. Holding no ViewModel makes it directly previewable and testable.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CreatePostScreen(
    uiState: CreatePostUiState,
    actions: CreatePostActions,
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
                actions = {
                    if (!uiState.form.isEmpty && !uiState.isPublishing) {
                        TextButton(onClick = actions::discard) {
                            Text(stringResource(R.string.create_post_discard))
                        }
                    }

                    SettingsAction(actions::openSettings)
                },
            )
        },
    ) { contentPadding ->
        CreatePostForm(
            form = uiState.form,
            isPublishing = uiState.isPublishing,
            actions = actions,
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
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
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

        PublishButton(
            isPublishing = isPublishing,
            enabled = form.canPublish,
            onPublish = actions::publish,
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
        )
    }
}
