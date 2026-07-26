package uno.lux.sample.post.data.network

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import uno.lux.sample.common.data.network.LikeStateResponse
import uno.lux.sample.common.data.network.SetLikeRequestDto

interface PostApi {

    @GET("posts/{id}")
    suspend fun getPost(
        @Path("id") postId: String,
    ): PostResponse

    @Multipart
    @POST("posts")
    suspend fun createPost(
        @Part("title") title: RequestBody,
        @Part("body") body: RequestBody,
        @Part images: List<MultipartBody.Part>,
        @Part video: MultipartBody.Part?,
    ): PostResponse

    @DELETE("posts/{id}")
    suspend fun deletePost(
        @Path("id") postId: String,
    )

    // PUT, not POST: the request names the state it wants rather than asking for a flip, so
    // sending it twice lands where sending it once does.
    @PUT("posts/{id}/like")
    suspend fun setLike(
        @Path("id") postId: String,
        @Body body: SetLikeRequestDto,
    ): LikeStateResponse

    @PUT("posts/{id}/bookmark")
    suspend fun setBookmark(
        @Path("id") postId: String,
        @Body body: SetBookmarkRequestDto,
    ): BookmarkStateResponse

    // The server keeps no report, so it answers an empty 204 — hence no response type.
    @POST("posts/{id}/report")
    suspend fun reportPost(
        @Path("id") postId: String,
        @Body report: ReportPostRequestDto,
    )
}
