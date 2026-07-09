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

// DTO → domain mapping runs through Mappie (a Kotlin compiler plugin): fields are matched by name
// at compile time, and the conversions Mappie can't infer are supplied as local conversion methods
// it applies automatically by type — the String → Instant parse, and the comment author, which
// routes through the hand-written UserDto → User mapper. AlbumMapper/VideoMapper exist so
// PostMapper's nested album/video resolve to them.

object AlbumMapper : ObjectMappie<AlbumDto, Album>() {
    override fun map(from: AlbumDto) = mapping()
}

object VideoMapper : ObjectMappie<VideoDto, Video>() {
    override fun map(from: VideoDto) = mapping()
}

object PostMapper : ObjectMappie<PostFeedItemDto, Post>() {
    override fun map(from: PostFeedItemDto) = mapping()

    fun parseInstant(value: String): Instant = Instant.parse(value)
}

object CommentMapper : ObjectMappie<CommentDto, Comment>() {
    override fun map(from: CommentDto) = mapping()

    fun parseInstant(value: String): Instant = Instant.parse(value)

    fun toDomainUser(dto: UserDto): User = dto.toDomain()
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
