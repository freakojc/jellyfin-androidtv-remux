package org.jellyfin.androidtv.util.sdk

import org.jellyfin.sdk.api.client.ApiClient


/**
 * Check if the [baseUrl] and [accessToken] are not null.
 */
val ApiClient.isUsable
	get() = baseUrl != null && accessToken != null

/**
 * Render a (possibly nested) throwable chain into a short one-line description, e.g.
 * `ApiClientException: Unexpected response status: 401 -> HttpException: ...`.
 * Used to surface the real reason for a connection/authentication failure in the
 * login UI instead of a generic "unable to connect" message.
 */
fun Throwable.describeChain(maxDepth: Int = 4): String {
	val parts = mutableListOf<String>()
	var current: Throwable? = this
	var depth = 0
	while (current != null && depth < maxDepth) {
		val name = current::class.simpleName ?: current::class.java.simpleName
		val message = current.message?.takeIf { it.isNotBlank() }
		parts += if (message != null) "$name: $message" else name
		current = current.cause?.takeIf { it !== current }
		depth++
	}
	return parts.joinToString(" -> ")
}