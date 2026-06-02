package uno.lux.sample.ui.home

import uno.lux.sample.data.Post

/**
 * Exhaustive state for the feed screen, rendered by a stateless `HomeScreen`. Modelled as
 * a sealed interface so the `when` over it is checked at compile time.
 */
sealed interface HomeUiState {
    data object Loading : HomeUiState

    /** [endReached] is true when the backend has no more posts beyond [posts]. */
    data class Feed(
        val posts: List<Post>,
        val endReached: Boolean,
    ) : HomeUiState
}
