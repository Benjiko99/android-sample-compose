package uno.lux.sample.data.network

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import uno.lux.sample.data.network.dto.AddCommentRequestDto
import uno.lux.sample.data.network.dto.BookmarkToggleResponse
import uno.lux.sample.data.network.dto.CommentListResponse
import uno.lux.sample.data.network.dto.CommentResponse
import uno.lux.sample.data.network.dto.EmptyBody
import uno.lux.sample.data.network.dto.FeedResponse
import uno.lux.sample.data.network.dto.LikeToggleResponse
import uno.lux.sample.data.network.dto.ProfileStatsResponse
import uno.lux.sample.data.network.dto.UserPostsResponse
import uno.lux.sample.data.network.dto.UserResponse

interface MosaicApi {

    @GET("feed")
    suspend fun getFeed(
        @Query("cursor") cursor: String? = null,
        @Query("limit") limit: Int = 20,
        @Query("include") include: String = "author",
    ): FeedResponse

    @GET("users/{id}")
    suspend fun getUser(@Path("id") id: String): UserResponse

    @GET("users/{id}/profile")
    suspend fun getProfileStats(@Path("id") id: String): ProfileStatsResponse

    @GET("users/{id}/posts")
    suspend fun getUserPosts(
        @Path("id") id: String,
        @Query("type") type: String? = null,
        @Query("cursor") cursor: String? = null,
        @Query("limit") limit: Int = 20,
    ): UserPostsResponse

    @GET("posts/{id}/comments")
    suspend fun getComments(
        @Path("id") postId: String,
        @Query("cursor") cursor: String? = null,
        @Query("limit") limit: Int = 20,
    ): CommentListResponse

    @POST("posts/{id}/like")
    suspend fun toggleLike(
        @Path("id") postId: String,
        @Body body: EmptyBody,
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
