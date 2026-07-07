package uno.lux.sample.util

import uno.lux.sample.di.NetworkModule

/**
 * Builds the canonical shareable link for a post.
 */
private const val PostLinkBase = "${NetworkModule.BASE_URL}/p"

/** The shareable link for the post. */
fun postLink(postId: String): String = "$PostLinkBase/$postId"
