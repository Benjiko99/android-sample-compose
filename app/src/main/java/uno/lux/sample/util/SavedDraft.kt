package uno.lux.sample.util

import androidx.lifecycle.SavedStateHandle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer

/**
 * Persisting a **draft** — the part of a screen's state the user typed — across process death.
 *
 * The back stack restores which page was open; everything a ViewModel holds is built again from
 * scratch. A page that fetches can simply re-fetch (see `PostDetailViewModel`), but nobody can
 * re-derive a half-written post, so a form's own state is the one thing that has to be *saved*.
 * [SavedStateHandle] is the mechanism: each back-stack entry has its own, tied to the same
 * instance state the [uno.lux.sample.ui.navigation.Screen] keys ride in.
 *
 * The draft is stored as **JSON in a single string**, not through `encodeToSavedState`, for one
 * reason: `SavedState` is a `Bundle` on Android, and a Bundle can't be exercised by a plain-JVM
 * unit test. Encoding to a string keeps the whole round trip — type a draft, rebuild the ViewModel
 * from the same handle, get the draft back — provable without Robolectric or a device, which is
 * the trade this codebase makes everywhere else too. A form is a few hundred bytes; the encoding
 * cost is not the point.
 */
private val DraftJson = Json { encodeDefaults = true }

/** What [saveDraft] stored under [key] in a previous process, or null on a fresh start. */
inline fun <reified T : Any> SavedStateHandle.restoreDraft(key: String): T? =
    restoreDraft(serializer(), key)

fun <T : Any> SavedStateHandle.restoreDraft(serializer: KSerializer<T>, key: String): T? =
    get<String>(key)?.let { DraftJson.decodeFromString(serializer, it) }

/**
 * Writes every value [drafts] emits into this handle under [key], for [restoreDraft] to find
 * after a restart. Runs for the ViewModel's lifetime: [scope] is its `viewModelScope`, so the
 * mirroring stops — and the handle goes — when the entry is popped.
 */
inline fun <reified T : Any> SavedStateHandle.saveDraft(
    scope: CoroutineScope,
    key: String,
    drafts: Flow<T>,
) = saveDraft(serializer(), scope, key, drafts)

fun <T : Any> SavedStateHandle.saveDraft(
    serializer: KSerializer<T>,
    scope: CoroutineScope,
    key: String,
    drafts: Flow<T>,
) {
    scope.launch {
        drafts.distinctUntilChanged().collect { draft ->
            set(key, DraftJson.encodeToString(serializer, draft))
        }
    }
}
