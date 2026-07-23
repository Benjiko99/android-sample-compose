package uno.lux.sample.user.data

import uno.lux.sample.user.ProfileUpdate
import uno.lux.sample.user.User
import uno.lux.sample.user.UserId
interface UserDataSource {
    suspend fun fetch(userId: UserId): User?

    /** Applies [update] to the user's profile and returns the updated [User]. */
    suspend fun update(userId: UserId, update: ProfileUpdate): User

    /**
     * Toggles whether the current user follows [user], returning [user] with the flipped
     * `isFollowing` and the server's new `followerCount`.
     */
    suspend fun toggleFollow(user: User): User
}
