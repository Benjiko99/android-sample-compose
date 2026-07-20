package uno.lux.sample.data.network

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query
import uno.lux.sample.data.network.dto.AddCommentRequestDto
import uno.lux.sample.data.network.dto.EmptyBody
import uno.lux.sample.data.network.response.BookmarkToggleResponse
import uno.lux.sample.data.network.response.BookmarksResponse
import uno.lux.sample.data.network.response.CommentListResponse
import uno.lux.sample.data.network.response.CommentResponse
import uno.lux.sample.data.network.response.FeedResponse
import uno.lux.sample.data.network.response.FollowToggleResponse
import uno.lux.sample.data.network.response.LikeToggleResponse
import uno.lux.sample.data.network.response.PostResponse
import uno.lux.sample.data.network.response.ProfileStatsResponse
import uno.lux.sample.data.network.response.UserPostsResponse
import uno.lux.sample.data.network.response.UserResponse

interface MosaicApi {

    @GET("feed")
    suspend fun getFeed(
        @Query("cursor") cursor: String? = null,
        @Query("limit") limit: Int = 20,
        @Query("include") include: String = "author",
    ): FeedResponse

    @GET("users/{id}")
    suspend fun getUser(@Path("id") id: String): UserResponse

    // Profile edits are sent as multipart/form-data so the avatar can ride along as an
    // uploaded file. Text fields are always present (empty string clears a nullable one);
    // [avatar] is omitted to leave the current photo untouched.
    @Multipart
    @PATCH("users/{id}")
    suspend fun updateUser(
        @Path("id") id: String,
        @Part("nickname") nickname: RequestBody,
        @Part("age") age: RequestBody,
        @Part("gender") gender: RequestBody,
        @Part("bio") bio: RequestBody,
        @Part avatar: MultipartBody.Part?,
    ): UserResponse

    // Toggles whether the current user (the X-User-Id header) follows [id]. The server derives
    // both sides from the header and path, so — unlike the older toggle endpoints that still
    // carry an EmptyBody (see the TODO on toggleLike) — this one deliberately sends no body.
    // 403 if [id] is the current user, 404 if [id] is unknown.
    @POST("users/{id}/follow")
    suspend fun toggleFollow(@Path("id") id: String): FollowToggleResponse

    @GET("users/{id}/profile")
    suspend fun getProfileStats(@Path("id") id: String): ProfileStatsResponse

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
    ): BookmarksResponse

    @GET("posts/{id}/comments")
    suspend fun getComments(
        @Path("id") postId: String,
        @Query("cursor") cursor: String? = null,
        @Query("limit") limit: Int = 20,
    ): CommentListResponse

    // Publishes a post authored by the current user (the X-User-Id header). Sent as
    // multipart/form-data — like the avatar upload above — so the post's media rides along with
    // the text in one request. Media is exclusive: either [images] is non-empty or [video] is
    // present, never both (the server 422s the combination). [videoDurationSeconds] accompanies a
    // video because the server cannot extract it. The response is the full projection, so the
    // created post arrives with its author and stored album or video embedded.
    @Multipart
    @POST("posts")
    suspend fun createPost(
        @Part("title") title: RequestBody,
        @Part("body") body: RequestBody,
        @Part images: List<MultipartBody.Part>,
        @Part video: MultipartBody.Part?,
        @Part("videoDurationSeconds") videoDurationSeconds: RequestBody?,
    ): PostResponse

    // Deletes a post authored by the current user (the X-User-Id header). 403 when the caller is
    // not the author, 404 when the post is unknown. The server answers 204, so there is no body
    // to parse — hence the Unit return rather than one of the response envelopes.
    @DELETE("posts/{id}")
    suspend fun deletePost(@Path("id") postId: String)

    @POST("posts/{id}/like")
    suspend fun toggleLike(
        @Path("id") postId: String,
        @Body body: EmptyBody, // TODO: Why do we use EmptyBody in many API calls? We should drop the body.
    ): LikeToggleResponse

    @POST("posts/{id}/bookmark")
    suspend fun toggleBookmark(
        @Path("id") postId: String,
        @Body body: EmptyBody,
    ): BookmarkToggleResponse

    @POST("posts/{id}/comments")
    suspend fun addComment(
        @Path("id") postId: String,
        @Body body: AddCommentRequestDto,
    ): CommentResponse

    @POST("posts/{id}/comments/{commentId}/like")
    suspend fun toggleCommentLike(
        @Path("id") postId: String,
        @Path("commentId") commentId: String,
        @Body body: EmptyBody,
    ): LikeToggleResponse
}
