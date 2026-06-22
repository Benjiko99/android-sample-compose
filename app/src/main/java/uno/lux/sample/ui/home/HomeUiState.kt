package uno.lux.sample.ui.home

/**
 * Exhaustive state for the feed screen, rendered by a stateless `HomeScreen`. Modeled as
 * a sealed interface so the `when` over it is checked at compile time.
 */
sealed interface HomeUiState {
    data object Loading : HomeUiState

    /** [endReached] is true when the backend has no more posts beyond [posts]. */
    data class Feed(
        val posts: List<PostCardData>,
        val endReached: Boolean,
    ) : HomeUiState
}
