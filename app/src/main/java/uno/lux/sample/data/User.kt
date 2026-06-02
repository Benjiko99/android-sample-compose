package uno.lux.sample.data

/**
 * A platform user.
 *
 * [nickname] is the display name shown on a post; [handle] is the unique `@`-mention
 * used as a secondary label.
 */
data class User(
    val id: String,
    val nickname: String,
    val handle: String,
)
