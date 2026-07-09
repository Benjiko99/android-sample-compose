package uno.lux.sample.data.network

import java.time.Instant
import tech.mappie.api.ObjectMappie
import uno.lux.sample.data.network.dto.AlbumDto
import uno.lux.sample.data.network.dto.CommentDto
import uno.lux.sample.data.network.dto.MinimalUserDto
import uno.lux.sample.data.network.dto.PostFeedItemDto
import uno.lux.sample.data.network.dto.UserDto
import uno.lux.sample.data.network.dto.VideoDto
import uno.lux.sample.data.post.Album
import uno.lux.sample.data.post.Comment
import uno.lux.sample.data.post.Post
import uno.lux.sample.data.post.Video
import uno.lux.sample.data.user.User

// DTO → domain mapping runs through Mappie (a Kotlin compiler plugin): the mappers below declare
// only the conversions Mappie can't infer — everything else is matched by name at compile time.
// AlbumMapper/VideoMapper exist so PostMapper's nested album/video resolve to them automatically.
//
// Every body must pass an explicit lambda — `mapping {}`, never `mapping()`. On this version the
// no-arg overload trips a null-dereference in the plugin's IR pass; the empty lambda avoids it.

object AlbumMapper : ObjectMappie<AlbumDto, Album>() {
    override fun map(from: AlbumDto) = mapping {}
}

object VideoMapper : ObjectMappie<VideoDto, Video>() {
    override fun map(from: VideoDto) = mapping {}
}

object PostMapper : ObjectMappie<PostFeedItemDto, Post>() {
    override fun map(from: PostFeedItemDto) = mapping {
        to::createdAt fromProperty from::createdAt transform { Instant.parse(it) }
    }
}

object CommentMapper : ObjectMappie<CommentDto, Comment>() {
    // author reuses the hand-written UserDto → User mapper below; createdAt parses the wire string.
    override fun map(from: CommentDto) = mapping {
        to::author fromProperty from::author transform { it.toDomain() }
        to::createdAt fromProperty from::createdAt transform { Instant.parse(it) }
    }
}

// The two User DTOs stay hand-mapped for now: each populates a different subset of User and leans
// on the domain defaults, which is clearer written out than configured through the mapper DSL.
internal fun MinimalUserDto.toDomain() = User(
    id = id,
    nickname = nickname,
    handle = handle,
    avatarUrl = avatarUrl,
    isFollowing = isFollowing,
)

internal fun UserDto.toDomain() = User(
    id = id,
    nickname = nickname,
    handle = handle,
    age = age,
    gender = gender,
    location = location,
    bio = bio,
    avatarUrl = avatarUrl,
    followerCount = followerCount,
    followingCount = followingCount,
    isFollowing = isFollowing,
)

// Note: ProfileUpdate is sent as a multipart request (see NetworkUserDataSource), not a JSON
// body, so there is no ProfileUpdate → DTO mapper here.
