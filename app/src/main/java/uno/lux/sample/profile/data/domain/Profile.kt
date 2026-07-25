package uno.lux.sample.profile.data.domain

import uno.lux.sample.user.data.domain.UserId

/** A user's profile metadata. Posts are owned by the screen layer. */
data class Profile(
    val userId: UserId,
    val postsCount: Int,
)
