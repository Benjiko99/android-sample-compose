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
import uno.lux.sample.app.common.data.network.LikeToggleResponse
import uno.lux.sample.app.core.network.EmptyBody

interface PostApi {

    // One post in the full projection — the same shape [createPost] answers with, author embedded.
    // 404 when the post is unknown or has been deleted, which callers read as "gone" rather than
    // as a failure. Only a client that doesn't already hold the post asks for it (a detail page
    // restored after process death).
    @GET("posts/{id}")
    suspend fun getPost(
        @Path("id") postId: String,
    ): PostResponse

    // Publishes a post authored by the current user (the X-User-Id header). Sent as
    // multipart/form-data — like the avatar upload — so the post's media rides along with the
    // text in one request. Media is exclusive: either [images] is non-empty or [video] is
    // present, never both (the server 422s the combination). The response is the full projection,
    // so the created post arrives with its author and stored album or video embedded — the
    // video's duration among it, which the server derives from the uploaded file.
    @Multipart
    @POST("posts")
    suspend fun createPost(
        @Part("title") title: RequestBody,
        @Part("body") body: RequestBody,
        @Part images: List<MultipartBody.Part>,
        @Part video: MultipartBody.Part?,
    ): PostResponse

    // Deletes a post authored by the current user (the X-User-Id header). 403 when the caller is
    // not the author, 404 when the post is unknown. The server answers 204, so there is no body
    // to parse — hence the Unit return rather than one of the response envelopes.
    @DELETE("posts/{id}")
    suspend fun deletePost(
        @Path("id") postId: String,
    )

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
}
