package uno.lux.sample.util

/**
 * Builds the canonical shareable link for a post. The host is a placeholder until the app has
 * real web / deep links; keeping the construction here — rather than inline at the share and
 * copy-link call sites — makes the link format a single, unit-tested source of truth.
 */
private const val PostLinkBase = "https://mosaic.app/p"

/** The shareable link for the post with [postId], e.g. `"https://mosaic.app/p/p1"`. */
fun postLink(postId: String): String = "$PostLinkBase/$postId"
