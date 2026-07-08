package uno.lux.sample.data.profile

import uno.lux.sample.data.user.UserId

/** A user's profile metadata. Posts are owned by the screen layer. */
data class Profile(
    val userId: UserId,
    val postsCount: Int,
)
