package uno.lux.sample.data.user

interface UserDataSource {
    suspend fun fetch(userId: UserId): User?

    /** Applies [update] to the user's profile and returns the updated [User]. */
    suspend fun update(userId: UserId, update: ProfileUpdate): User
}
