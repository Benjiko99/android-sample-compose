package uno.lux.sample.comment.data.network

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import uno.lux.sample.common.data.network.LikeToggleResponse

interface CommentApi {

    @GET("posts/{id}/comments")
    suspend fun getComments(
        @Path("id") postId: String,
        @Query("cursor") cursor: String? = null,
        @Query("limit") limit: Int = 20,
    ): CommentListResponse

    @POST("posts/{id}/comments")
    suspend fun addComment(
        @Path("id") postId: String,
        @Body body: AddCommentRequestDto,
    ): CommentResponse

    // Toggles the current user's (the X-User-Id header) like on [commentId]. Bodiless for the
    // same reason [uno.lux.sample.post.data.network.PostApi.toggleLike] is.
    @POST("posts/{id}/comments/{commentId}/like")
    suspend fun toggleCommentLike(
        @Path("id") postId: String,
        @Path("commentId") commentId: String,
    ): LikeToggleResponse
}
