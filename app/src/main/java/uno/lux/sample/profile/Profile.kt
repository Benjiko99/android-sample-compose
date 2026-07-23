package uno.lux.sample.profile

import uno.lux.sample.user.UserId

/** A user's profile metadata. Posts are owned by the screen layer. */
data class Profile(
    val userId: UserId,
    val postsCount: Int,
)
