package uno.lux.sample.ui.profile

import uno.lux.sample.data.post.Post
import uno.lux.sample.data.profile.Profile
import uno.lux.sample.data.user.User

/** ViewModel-level aggregate for the profile screen: the resolved user, their profile metadata, and their posts. */
data class ProfileScreenData(
    val user: User,
    val profile: Profile,
    val posts: List<Post>,
    val postsEndReached: Boolean = true,
)
