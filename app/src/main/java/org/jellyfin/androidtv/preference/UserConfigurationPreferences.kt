package org.jellyfin.androidtv.preference

import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.userApi
import org.jellyfin.sdk.model.api.UserConfiguration
import timber.log.Timber

/**
 * Holder for the **server-side** user configuration (the same data jellyfin-web edits in
 * "个人信息 / 播放 / 字幕" settings). Because it lives on the server, changes are shared with
 * other Jellyfin clients automatically.
 *
 * Reads lazily through the active session ([ApiClient] configured by the SessionRepository)
 * and writes the full configuration back, so unrelated fields are never lost.
 */
class UserConfigurationPreferences(
	private val api: ApiClient,
) {
	private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

	private val _configuration = MutableStateFlow<UserConfiguration?>(null)
	val configuration: StateFlow<UserConfiguration?> = _configuration.asStateFlow()

	private var userId: UUID? = null

	/** Latest known configuration without suspending, or null when not loaded yet. */
	fun peek(): UserConfiguration? = _configuration.value

	/** Kick off a background refresh (e.g. right after a session was established). */
	fun refresh() {
		scope.launch { ensureLoaded(force = true) }
	}

	/**
	 * Return the cached configuration, loading it from the server when needed.
	 * Returns null when there is no active session or the request failed.
	 */
	suspend fun ensureLoaded(force: Boolean = false): UserConfiguration? {
		if (!force) _configuration.value?.let { return it }
		return try {
			val user = api.userApi.getCurrentUser().content
			userId = user.id
			user.configuration.also { _configuration.value = it }
		} catch (err: Exception) {
			Timber.e(err, "Unable to load the user configuration")
			null
		}
	}

	/**
	 * Apply [transform] to the current configuration and persist it on the server.
	 * Returns true when the server accepted the update.
	 */
	suspend fun update(transform: (UserConfiguration) -> UserConfiguration): Boolean {
		val current = ensureLoaded() ?: return false
		val id = userId ?: try {
			api.userApi.getCurrentUser().content.id.also { userId = it }
		} catch (err: Exception) {
			Timber.e(err, "Unable to resolve the current user id")
			return false
		}

		val updated = transform(current)
		if (updated == current) return true

		return try {
			api.userApi.updateUserConfiguration(userId = id, data = updated)
			_configuration.value = updated
			true
		} catch (err: Exception) {
			Timber.e(err, "Unable to save the user configuration")
			false
		}
	}

	/** Fire-and-forget update for callers that cannot suspend (e.g. the Java playback controller). */
	fun updateAsync(
		transform: (UserConfiguration) -> UserConfiguration,
		onDone: (Boolean) -> Unit = {},
	) {
		scope.launch { onDone(update(transform)) }
	}

	/** Drop the cache, e.g. on sign out / user switch. */
	fun invalidate() {
		_configuration.value = null
		userId = null
	}
}
