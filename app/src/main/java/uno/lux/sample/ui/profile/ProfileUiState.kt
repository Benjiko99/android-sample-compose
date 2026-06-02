package uno.lux.sample.ui.profile

import uno.lux.sample.data.Profile

/** The profile screen's state: loading, the loaded [Profile], or an unknown user. */
sealed interface ProfileUiState {
    data object Loading : ProfileUiState
    data class Loaded(val profile: Profile) : ProfileUiState
    data object NotFound : ProfileUiState
}
