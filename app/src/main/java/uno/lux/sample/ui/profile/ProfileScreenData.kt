package uno.lux.sample.ui.profile

import uno.lux.sample.data.post.Post
import uno.lux.sample.data.profile.Profile
import uno.lux.sample.data.user.User
import uno.lux.sample.ui.post.PostCardData

/**
 * ViewModel-level aggregate for the profile screen: the resolved user, their profile metadata,
 * their posts, and — on their own profile only — the posts they saved.
 */
data class ProfileScreenData(
    val user: User,
    val profile: Profile,
    val posts: List<Post>,
    val postsEndReached: Boolean = true,
    val bookmarks: ProfileBookmarks? = null,
)

/**
 * The Saved tab's content, absent until the tab is first opened. Its posts carry their authors
 * — unlike [ProfileScreenData.posts], which are all by the profile's own user — because a saved
 * post can be by anyone.
 */
data class ProfileBookmarks(
    val posts: List<PostCardData>,
    val endReached: Boolean = true,
)
