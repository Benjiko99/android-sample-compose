package uno.lux.sample.data.user

/**
 * A platform user.
 *
 * [nickname] is the display name; [handle] is the unique `@`-mention. Optional profile
 * fields carry defaults so a post author can be constructed without full profile detail.
 */
data class User(
    val id: String,
    val nickname: String,
    val handle: String,
    val age: Int? = null,
    val gender: String? = null,
    val location: String? = null,
    val bio: String? = null,
    val followerCount: Int = 0,
    val followingCount: Int = 0,
)