package uno.lux.sample.data.network.response

import kotlinx.serialization.Serializable
import uno.lux.sample.data.network.dto.CursorPageDto
import uno.lux.sample.data.network.dto.PostFeedItemDto
import uno.lux.sample.data.network.dto.UserDto

/** Top-level feed response — not wrapped in an extra `data` key. */
@Serializable
data class FeedResponse(
    val data: List<PostFeedItemDto>,
    val included: FeedIncluded? = null,
    val page: CursorPageDto,
)

/**
 * The sideloaded authors for a page. These are the *same* [UserDto] `GET /users/:id` serves — the
 * API has one user projection on purpose. An identity-only variant would be indistinguishable from
 * a user who has genuinely left the optional fields empty, so ingesting it would blank the bio,
 * identity chips and counts of a profile already loaded in full.
 */
@Serializable
data class FeedIncluded(val users: List<UserDto>)
