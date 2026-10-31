package org.jellyfin.androidtv.ui.settings.screen.playback

import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.preference.UserConfigurationPreferences
import org.jellyfin.androidtv.ui.base.Text
import org.jellyfin.androidtv.ui.base.form.RadioButton
import org.jellyfin.androidtv.ui.base.list.ListButton
import org.jellyfin.androidtv.ui.base.list.ListSection
import org.jellyfin.androidtv.ui.navigation.LocalRouter
import org.jellyfin.androidtv.ui.navigation.focus.focusKey
import org.jellyfin.androidtv.ui.settings.compat.rememberUserConfigurationPreference
import org.jellyfin.androidtv.ui.settings.composable.SettingsColumn
import org.jellyfin.androidtv.ui.settings.util.rememberLanguageOptions
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.model.api.UserConfiguration
import org.koin.compose.koinInject

/**
 * Preferred audio language, stored server-side in [UserConfiguration.audioLanguagePreference]
 * (the same setting "个人设置 ▸ 播放 ▸ 首选的音频语言" on the web/mobile clients).
 */
@Composable
fun SettingsPlaybackAudioLanguageScreen() {
	val router = LocalRouter.current
	val api = koinInject<ApiClient>()
	val configurationPreferences = koinInject<UserConfigurationPreferences>()
	val options = rememberLanguageOptions(api)

	var audioLanguage by rememberUserConfigurationPreference(
		configurationPreferences,
		get = { it.audioLanguagePreference ?: "" },
		set = { configuration, value -> configuration.copy(audioLanguagePreference = value.ifEmpty { null }) },
		fallback = "",
	)

	SettingsColumn {
		item {
			ListSection(
				overlineContent = { Text(stringResource(R.string.pref_playback).uppercase()) },
				headingContent = { Text(stringResource(R.string.pref_audio_language)) },
			)
		}

		items(options, key = { it.value }) { option ->
			ListButton(
				headingContent = { Text(option.label) },
				trailingContent = { RadioButton(checked = audioLanguage == option.value) },
				onClick = {
					audioLanguage = option.value
					router.back()
				},
				modifier = Modifier.focusKey("audio_language_${option.value}", initialFocus = audioLanguage == option.value)
			)
		}
	}
}