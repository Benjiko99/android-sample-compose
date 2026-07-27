package uno.lux.sample.post.data.network

import tech.mappie.api.ObjectMappie
import uno.lux.sample.album.data.domain.Album
import uno.lux.sample.post.data.domain.Post
import uno.lux.sample.video.data.domain.Video

// DTO → domain mapping runs through Mappie (a Kotlin compiler plugin)

object AlbumMapper : ObjectMappie<AlbumDto, Album>() {
    override fun map(from: AlbumDto) = mapping()
}

object VideoMapper : ObjectMappie<VideoDto, Video>() {
    override fun map(from: VideoDto) = mapping()
}

object PostMapper : ObjectMappie<PostDto, Post>() {
    override fun map(from: PostDto) = mapping()
}
