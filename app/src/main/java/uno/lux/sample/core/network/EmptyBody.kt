package uno.lux.sample.core.network

import kotlinx.serialization.Serializable

/** Sent as the body of POST requests that carry no domain payload. Serialises to `{}`. */
@Serializable
class EmptyBody
