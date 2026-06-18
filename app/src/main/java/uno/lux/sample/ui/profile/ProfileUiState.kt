package uno.lux.sample.ui.profile

import uno.lux.sample.data.Profile

/** The profile screen's state: loading, the loaded [Profile], or an unknown user. */
sealed interface ProfileUiState {
    data object Loading : ProfileUiState

    /** [isCurrentUser] is true when this is the signed-in user's own profile. */
    data class Loaded(val profile: Profile, val isCurrentUser: Boolean) : ProfileUiState
    data object NotFound : ProfileUiState
}
