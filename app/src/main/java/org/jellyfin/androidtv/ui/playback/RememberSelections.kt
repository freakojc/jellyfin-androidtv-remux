package org.jellyfin.androidtv.ui.playback

import org.jellyfin.androidtv.preference.UserConfigurationPreferences
import org.jellyfin.sdk.model.api.MediaStream
import org.koin.java.KoinJavaComponent

/**
 * Persist the track the user picked during playback back into the **server-side** user
 * configuration, mirroring "记住音频选择 / 记住字幕选择" in the web/mobile clients. Writes are
 * no-ops while the corresponding flag is off.
 */
object RememberSelections {
	private val preferences: UserConfigurationPreferences
		get() = KoinJavaComponent.get(UserConfigurationPreferences::class.java)

	private fun languageOf(streams: List<MediaStream>?, index: Int?): String? =
		index?.let { wanted -> streams?.firstOrNull { it.index == wanted }?.language }

	@JvmStatic
	fun audio(streams: List<MediaStream>?, index: Int?) {
		val language = languageOf(streams, index) ?: return
		preferences.updateAsync(transform = { configuration ->
			if (!configuration.rememberAudioSelections) configuration
			else configuration.copy(audioLanguagePreference = language)
		})
	}

	@JvmStatic
	fun subtitle(streams: List<MediaStream>?, index: Int?) {
		preferences.updateAsync(transform = { configuration ->
			if (!configuration.rememberSubtitleSelections) return@updateAsync configuration
			// -1 / null means the user turned subtitles off: store an empty preference for that
			val language = if (index == null || index < 0) "" else languageOf(streams, index) ?: return@updateAsync configuration
			configuration.copy(subtitleLanguagePreference = language)
		})
	}
}