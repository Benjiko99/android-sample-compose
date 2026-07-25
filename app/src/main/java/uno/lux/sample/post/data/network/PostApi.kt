package uno.lux.sample.post.data.network

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import uno.lux.sample.common.data.network.LikeToggleResponse

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

    @POST("posts/{id}/like")
    suspend fun toggleLike(
        @Path("id") postId: String,
    ): LikeToggleResponse

    @POST("posts/{id}/bookmark")
    suspend fun toggleBookmark(
        @Path("id") postId: String,
    ): BookmarkToggleResponse

    // The server keeps no report, so it answers an empty 204 — hence no response type.
    @POST("posts/{id}/report")
    suspend fun reportPost(
        @Path("id") postId: String,
        @Body report: ReportPostRequestDto,
    )
}
