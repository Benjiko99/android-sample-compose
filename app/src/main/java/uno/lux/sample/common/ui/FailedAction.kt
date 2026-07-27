package uno.lux.sample.common.ui

/**
 * A fire-and-forget user action whose request failed after the UI had already moved on — the
 * confirmation dialog closed, the report sheet dismissed, the follow button flipped back — so
 * there is nothing left on screen for the failure to show up in. The ViewModel that ran the
 * action names it here for the screen to announce once, transiently, and then spend, the same
 * lifetime `HomeUiState.Feed.refreshError` has.
 *
 * Optimistic toggles (likes, bookmarks) are deliberately absent: a failed toggle reverts the
 * control in place, and the revert is its own announcement.
 */
enum class FailedAction {
    DELETE_POST,
    REPORT_POST,
    FOLLOW,
}
