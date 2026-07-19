package uno.lux.sample.ui.create

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import timber.log.Timber
import uno.lux.sample.data.post.FeedRepository
import uno.lux.sample.ui.file.FileLoader
import uno.lux.sample.ui.navigation.Navigator
import uno.lux.sample.ui.navigation.Screen
import uno.lux.sample.util.ignoreErrors
import uno.lux.sample.util.launchIfIdle
import uno.lux.sample.util.toAppError
import javax.inject.Inject

/**
 * Drives the post composer. Publishing goes through [FeedRepository], which stores the new post
 * and puts it at the head of the feed; the composer then [replaces][Navigator.replaceTop] itself
 * with the new post's detail page — confirmation the post exists, and backing out of it returns
 * to the shell rather than to a composer the user is done with. A failed publish keeps the typed
 * text and surfaces the error, so nothing is lost to a dropped connection.
 *
 * Leaving with a part-written post asks first, mirroring the profile editor: [goBack] raises the
 * confirmation instead of popping when the form has content.
 */
@HiltViewModel
class CreatePostViewModel @Inject constructor(
    private val feedRepository: FeedRepository,
    private val fileLoader: FileLoader,
    private val navigator: Navigator,
) : ViewModel(), CreatePostActions {

    private val _uiState = MutableStateFlow(CreatePostUiState())
    val uiState: StateFlow<CreatePostUiState> = _uiState.asStateFlow()

    private var publishJob: Job? = null

    override fun onTitleChange(value: String) = updateForm {
        it.copy(title = value.take(CreatePostTitleMaxLength))
    }

    override fun onBodyChange(value: String) = updateForm {
        it.copy(body = value.take(CreatePostBodyMaxLength))
    }

    /**
     * Adds a picked selection, keeping it within [CreatePostMaxImages]. Already-picked URIs are
     * dropped rather than duplicated — the system picker re-reports every image chosen in a
     * session, so a second trip through it would otherwise repeat the earlier ones.
     */
    override fun onImagesPicked(uris: List<String>) = updateForm { form ->
        val added = uris.filterNot { it in form.imageUris }
        form.copy(imageUris = (form.imageUris + added).take(CreatePostMaxImages))
    }

    override fun onRemoveImage(uri: String) = updateForm { form ->
        form.copy(imageUris = form.imageUris - uri)
    }

    override fun publish() {
        if (!_uiState.value.form.canPublish) return

        launchIfIdle(::publishJob) {
            val form = _uiState.value.form
            _uiState.update { it.copy(isPublishing = true, publishError = null) }

            ignoreErrors(
                onError = { e ->
                    Timber.w(e)
                    _uiState.update { it.copy(isPublishing = false, publishError = e.toAppError()) }
                },
            ) {
                // The picked images are only read into memory here, at the point of upload —
                // an unreadable URI fails the publish like any other error, keeping the form.
                val draft = form.toNewPost(images = form.imageUris.map { fileLoader.read(it) })

                // A published draft leaves nothing to edit, so the whole state resets — which
                // clears the in-flight flag along with the form.
                val postId = feedRepository.publish(draft)
                _uiState.value = CreatePostUiState()
                navigator.replaceTop(Screen.PostDetail(postId))
            }
        }
    }

    override fun goBack() {
        if (_uiState.value.form.isEmpty) navigator.goBack()
        else _uiState.update { it.copy(showDiscardConfirmation = true) }
    }

    override fun dismissDiscardConfirmation() {
        _uiState.update { it.copy(showDiscardConfirmation = false) }
    }

    override fun confirmDiscard() {
        _uiState.update { it.copy(showDiscardConfirmation = false) }
        navigator.goBack()
    }

    private fun updateForm(transform: (CreatePostForm) -> CreatePostForm) {
        _uiState.update { it.copy(form = transform(it.form)) }
    }
}
