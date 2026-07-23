package uno.lux.sample.data.network

import tech.mappie.api.ObjectMappie
import uno.lux.sample.data.network.dto.AlbumDto
import uno.lux.sample.data.network.dto.CommentDto
import uno.lux.sample.data.network.dto.PostDto
import uno.lux.sample.data.network.dto.PostFeedItemDto
import uno.lux.sample.data.network.dto.UserDto
import uno.lux.sample.data.network.dto.VideoDto
import uno.lux.sample.data.post.Album
import uno.lux.sample.data.post.Comment
import uno.lux.sample.data.post.Post
import uno.lux.sample.data.post.PostWithAuthor
import uno.lux.sample.data.post.Video
import uno.lux.sample.data.user.User

// DTO → domain mapping runs through Mappie (a Kotlin compiler plugin)

object AlbumMapper : ObjectMappie<AlbumDto, Album>() {
    override fun map(from: AlbumDto) = mapping()
}

object VideoMapper : ObjectMappie<VideoDto, Video>() {
    override fun map(from: VideoDto) = mapping()
}

object PostMapper : ObjectMappie<PostFeedItemDto, Post>() {
    override fun map(from: PostFeedItemDto) = mapping()
}

/**
 * The full projection embeds its author, while the domain [Post] references it by ID (the user
 * itself belongs in the user store), so the author is flattened to `authorId` here and returned
 * alongside as a [PostWithAuthor]. Serves both endpoints that answer with that projection —
 * publishing a post and fetching a single one.
 */
object PostWithAuthorMapper : ObjectMappie<PostDto, Post>() {
    override fun map(from: PostDto) = mapping {
        to::authorId fromExpression { dto -> dto.author.id }
    }

    fun toPostWithAuthor(dto: PostDto) =
        PostWithAuthor(post = map(dto), author = dto.author.toDomain())
}

object CommentMapper : ObjectMappie<CommentDto, Comment>() {
    override fun map(from: CommentDto) = mapping()
}

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
