package uno.lux.sample.data.user

typealias UserId = String

/**
 * A platform user.
 *
 * [nickname] is the display name; [handle] is the unique `@`-mention. Optional profile
 * fields carry defaults so a post author can be constructed without full profile detail.
 * [avatarUrl] is a URI reference to the profile photo; `null` falls back to the generated
 * initials avatar.
 */
data class User(
    val id: UserId,
    val nickname: String,
    val handle: String,
    val age: Int? = null,
    val gender: String? = null,
    val location: String? = null,
    val bio: String? = null,
    val avatarUrl: String? = null,
    val followerCount: Int = 0,
    val followingCount: Int = 0,
)