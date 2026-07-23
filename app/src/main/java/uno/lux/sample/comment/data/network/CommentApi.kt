package uno.lux.sample.comment.data.network

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import uno.lux.sample.app.core.network.EmptyBody
import uno.lux.sample.app.common.data.network.LikeToggleResponse

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

    @POST("posts/{id}/comments/{commentId}/like")
    suspend fun toggleCommentLike(
        @Path("id") postId: String,
        @Path("commentId") commentId: String,
        @Body body: EmptyBody,
    ): LikeToggleResponse
}
