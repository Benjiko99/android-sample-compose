package uno.lux.sample.ui.profile

/** The profile screen's state: loading, the loaded profile data, or an unknown user. */
sealed interface ProfileUiState {
    data object Loading : ProfileUiState
    data object Error : ProfileUiState

    /** [isCurrentUser] is true when this is the signed-in user's own profile. */
    data class Loaded(val data: ProfileScreenData, val isCurrentUser: Boolean) : ProfileUiState
    data object NotFound : ProfileUiState
}
