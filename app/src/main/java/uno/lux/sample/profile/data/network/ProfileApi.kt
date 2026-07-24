package uno.lux.sample.profile.data.network

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface ProfileApi {

    @GET("users/{id}/profile")
    suspend fun getProfileStats(
        @Path("id") id: String,
    ): ProfileStatsResponse

    @GET("users/{id}/posts")
    suspend fun getUserPosts(
        @Path("id") id: String,
        @Query("cursor") cursor: String? = null,
        @Query("limit") limit: Int = 20,
    ): UserPostsResponse

    // The posts [id] saved. Bookmarks are private: the server answers only when [id] is the
    // current user (the X-User-Id header) and 403s otherwise, so this is called only for the
    // signed-in user's own profile.
    @GET("users/{id}/bookmarks")
    suspend fun getBookmarks(
        @Path("id") id: String,
        @Query("cursor") cursor: String? = null,
        @Query("limit") limit: Int = 20,
    ): PostsWithAuthorsResponse

    // The posts [id] liked. Public — any caller may read anyone's — which is the one way this
    // differs from [getBookmarks]. The viewer flags on the items still belong to the current
    // user, not to [id].
    @GET("users/{id}/likes")
    suspend fun getLikes(
        @Path("id") id: String,
        @Query("cursor") cursor: String? = null,
        @Query("limit") limit: Int = 20,
    ): PostsWithAuthorsResponse
}
