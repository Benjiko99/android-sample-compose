package uno.lux.sample.data.network

import java.time.Instant
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

// TODO: Should we be doing manual conversion of dto's to domain objects, or let some libraryf
//  handle that automatically?
internal fun PostFeedItemDto.toDomain() = Post(
    id = id,
    authorId = authorId,
    title = title,
    body = body,
    createdAt = Instant.parse(createdAt),
    likeCount = likeCount,
    commentCount = commentCount,
    isLiked = isLiked,
    isBookmarked = isBookmarked,
    album = album?.toDomain(),
    video = video?.toDomain(),
)

internal fun AlbumDto.toDomain() = Album(
    id = id,
    title = title,
    itemCount = itemCount,
    images = images,
)

internal fun VideoDto.toDomain() = Video(
    id = id,
    title = title,
    durationSeconds = durationSeconds,
    viewCount = viewCount,
    videoUrl = videoUrl,
)

internal fun MinimalUserDto.toDomain() = User(
    id = id,
    nickname = nickname,
    handle = handle,
    avatarUrl = avatarUrl,
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
)

internal fun CommentDto.toDomain() = Comment(
    id = id,
    author = author.toDomain(),
    createdAt = Instant.parse(createdAt),
    text = text,
    likeCount = likeCount,
    isLiked = isLiked,
)

// Note: ProfileUpdate is sent as a multipart request (see NetworkUserDataSource), not a JSON
// body, so there is no ProfileUpdate → DTO mapper here.
