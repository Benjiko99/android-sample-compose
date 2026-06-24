package uno.lux.sample.data.network.response

import kotlinx.serialization.Serializable
import uno.lux.sample.data.network.dto.CursorPageDto
import uno.lux.sample.data.network.dto.PostFeedItemNetworkDto
import uno.lux.sample.data.network.dto.UserNetworkDto

/** Top-level feed response — not wrapped in an extra `data` key. */
@Serializable
data class FeedResponse(
    val data: List<PostFeedItemNetworkDto>,
    val included: FeedIncluded? = null,
    val page: CursorPageDto,
)

@Serializable
data class FeedIncluded(val users: List<UserNetworkDto>)
