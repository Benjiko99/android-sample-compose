package uno.lux.sample.data

/**
 * A platform user.
 *
 * [nickname] is the display name shown on a post; [handle] is the unique `@`-mention used
 * as a secondary label. The remaining fields back the profile screen — [age], [gender] and
 * [location] render as the identity chips, [bio] as the blurb, and the follow counts as the
 * stat row. They carry defaults so a post author can be constructed without profile detail.
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
