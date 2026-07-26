package uno.lux.sample.feed.ui

import uno.lux.sample.app.util.AppError
import uno.lux.sample.post.ui.PostCardData

/**
 * Exhaustive state for the feed screen, rendered by a stateless `HomeScreen`. Modeled as
 * a sealed interface so the `when` over it is checked at compile time.
 */
sealed interface HomeUiState {
    data object Loading : HomeUiState

    /** Nothing to show and the load failed — the whole screen is the error. */
    data class Error(
        val error: AppError,
    ) : HomeUiState

    /**
     * [endReached] is true when the backend has no more posts beyond [posts].
     *
     * [refreshError] carries a load that failed *over* these posts — a pull-to-refresh in a
     * tunnel. The feed being readable outranks the failure, so it is reported transiently (a
     * snackbar) instead of replacing the content the user was already looking at; [Error] is
     * reserved for having nothing to show at all.
     */
    data class Feed(
        val posts: List<PostCardData>,
        val endReached: Boolean,
        val refreshError: AppError? = null,
    ) : HomeUiState
}
