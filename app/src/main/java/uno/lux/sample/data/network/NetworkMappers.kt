package uno.lux.sample.data.network

import java.time.Instant
import uno.lux.sample.data.network.dto.AlbumNetworkDto
import uno.lux.sample.data.network.dto.CommentNetworkDto
import uno.lux.sample.data.network.dto.PostFeedItemNetworkDto
import uno.lux.sample.data.network.dto.UserNetworkDto
import uno.lux.sample.data.network.dto.VideoNetworkDto
import uno.lux.sample.data.post.Album
import uno.lux.sample.data.post.Comment
import uno.lux.sample.data.post.Post
import uno.lux.sample.data.post.Video
import uno.lux.sample.data.user.User

internal fun PostFeedItemNetworkDto.toDomain() = Post(
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

internal fun AlbumNetworkDto.toDomain() = Album(
    id = id,
    title = title,
    itemCount = itemCount,
    images = images,
)

internal fun VideoNetworkDto.toDomain() = Video(
    id = id,
    title = title,
    durationSeconds = durationSeconds,
    viewCount = viewCount,
    videoUrl = videoUrl,
)

internal fun UserNetworkDto.toDomain() = User(
    id = id,
    nickname = nickname,
    handle = handle,
    age = age,
    gender = gender,
    location = location,
    bio = bio,
    followerCount = followerCount,
    followingCount = followingCount,
)

internal fun CommentNetworkDto.toDomain() = Comment(
    id = id,
    author = author.toDomain(),
    createdAt = Instant.parse(createdAt),
    text = text,
    likeCount = likeCount,
    isLiked = isLiked,
)
