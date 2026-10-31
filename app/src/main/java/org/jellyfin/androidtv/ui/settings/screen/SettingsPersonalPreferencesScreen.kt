package org.jellyfin.androidtv.ui.settings.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.preference.UserConfigurationPreferences
import org.jellyfin.androidtv.preference.UserSettingPreferences
import org.jellyfin.androidtv.ui.base.Icon
import org.jellyfin.androidtv.ui.base.Text
import org.jellyfin.androidtv.ui.base.form.Checkbox
import org.jellyfin.androidtv.ui.base.list.ListButton
import org.jellyfin.androidtv.ui.base.list.ListSection
import org.jellyfin.androidtv.ui.navigation.LocalRouter
import org.jellyfin.androidtv.ui.navigation.focus.focusKey
import org.jellyfin.androidtv.ui.settings.Routes
import org.jellyfin.androidtv.ui.settings.compat.rememberPreference
import org.jellyfin.androidtv.ui.settings.compat.rememberUserConfigurationPreference
import org.jellyfin.androidtv.ui.settings.composable.SettingsColumn
import org.jellyfin.androidtv.ui.settings.util.rememberLanguageOptions
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.model.api.SubtitlePlaybackMode
import org.koin.compose.koinInject

/**
 * Personal preferences stored on the **server** (`UserConfiguration` + the shared
 * "usersettings" display preferences) — the same settings the phone/web client exposes under
 * 个人信息 / 播放 / 字幕, so changing them here is visible in the other Jellyfin clients too.
 * Rows use the standard focusable list buttons: D-pad up/down to move, OK to activate.
 */
@Composable
fun SettingsPersonalPreferencesScreen() {
	val router = LocalRouter.current
	val api = koinInject<ApiClient>()
	val configurationPreferences = koinInject<UserConfigurationPreferences>()
	val userSettingPreferences = koinInject<UserSettingPreferences>()
	val languageOptions = rememberLanguageOptions(api)
	val noLanguageLabel = stringResource(R.string.language_none)

	fun languageLabel(value: String): String =
		languageOptions.firstOrNull { it.value == value }?.label ?: value.ifEmpty { noLanguageLabel }

	val audioLanguage by rememberUserConfigurationPreference(
		configurationPreferences,
		get = { it.audioLanguagePreference ?: "" },
		set = { configuration, value -> configuration.copy(audioLanguagePreference = value.ifEmpty { null }) },
		fallback = "",
	)
	val subtitleLanguage by rememberUserConfigurationPreference(
		configurationPreferences,
		get = { it.subtitleLanguagePreference ?: "" },
		set = { configuration, value -> configuration.copy(subtitleLanguagePreference = value.ifEmpty { null }) },
		fallback = "",
	)
	val subtitleMode by rememberUserConfigurationPreference(
		configurationPreferences,
		get = { it.subtitleMode },
		set = { configuration, value -> configuration.copy(subtitleMode = value) },
		fallback = SubtitlePlaybackMode.DEFAULT,
	)
	var playDefaultAudioTrack by rememberUserConfigurationPreference(
		configurationPreferences,
		get = { it.playDefaultAudioTrack },
		set = { configuration, value -> configuration.copy(playDefaultAudioTrack = value) },
		fallback = true,
	)
	var rememberAudioSelections by rememberUserConfigurationPreference(
		configurationPreferences,
		get = { it.rememberAudioSelections },
		set = { configuration, value -> configuration.copy(rememberAudioSelections = value) },
		fallback = false,
	)
	var rememberSubtitleSelections by rememberUserConfigurationPreference(
		configurationPreferences,
		get = { it.rememberSubtitleSelections },
		set = { configuration, value -> configuration.copy(rememberSubtitleSelections = value) },
		fallback = false,
	)
	var enableNextEpisodeAutoPlay by rememberUserConfigurationPreference(
		configurationPreferences,
		get = { it.enableNextEpisodeAutoPlay },
		set = { configuration, value -> configuration.copy(enableNextEpisodeAutoPlay = value) },
		fallback = true,
	)
	var enableRewatchingInNextUp by rememberPreference(userSettingPreferences, UserSettingPreferences.enableRewatchingInNextUp)
	var useEpisodeImagesInNextUpAndResume by rememberPreference(userSettingPreferences, UserSettingPreferences.useEpisodeImagesInNextUpAndResume)

	SettingsColumn {
		item {
			ListSection(
				overlineContent = { Text(stringResource(R.string.settings).uppercase()) },
				headingContent = { Text(stringResource(R.string.pref_personal_preferences)) },
				captionContent = { Text(stringResource(R.string.pref_personal_preferences_description)) },
			)
		}

		// Audio
		item {
			ListButton(
				leadingContent = { Icon(painterResource(R.drawable.ic_playback_speed), contentDescription = null) },
				headingContent = { Text(stringResource(R.string.pref_audio_language)) },
				captionContent = { Text(languageLabel(audioLanguage)) },
				onClick = { router.push(Routes.PLAYBACK_AUDIO_LANGUAGE) },
				modifier = Modifier.focusKey(Routes.PLAYBACK_AUDIO_LANGUAGE),
			)
		}
		item {
			ListButton(
				headingContent = { Text(stringResource(R.string.pref_play_default_audio_track)) },
				trailingContent = { Checkbox(checked = playDefaultAudioTrack) },
				captionContent = { Text(stringResource(R.string.pref_play_default_audio_track_description)) },
				onClick = { playDefaultAudioTrack = !playDefaultAudioTrack },
				modifier = Modifier.focusKey("play_default_audio_track"),
			)
		}
		item {
			ListButton(
				headingContent = { Text(stringResource(R.string.pref_remember_audio_selections)) },
				trailingContent = { Checkbox(checked = rememberAudioSelections) },
				captionContent = { Text(stringResource(R.string.pref_remember_audio_selections_description)) },
				onClick = { rememberAudioSelections = !rememberAudioSelections },
				modifier = Modifier.focusKey("remember_audio_selections"),
			)
		}

		// Subtitles
		item {
			ListButton(
				leadingContent = { Icon(painterResource(R.drawable.ic_subtitles), contentDescription = null) },
				headingContent = { Text(stringResource(R.string.pref_subtitle_language)) },
				captionContent = { Text(languageLabel(subtitleLanguage)) },
				onClick = { router.push(Routes.CUSTOMIZATION_SUBTITLES_LANGUAGE) },
				modifier = Modifier.focusKey(Routes.CUSTOMIZATION_SUBTITLES_LANGUAGE),
			)
		}
		item {
			ListButton(
				headingContent = { Text(stringResource(R.string.pref_subtitle_mode)) },
				captionContent = {
					Text(
						stringResource(
							when (subtitleMode) {
								SubtitlePlaybackMode.ALWAYS -> R.string.subtitle_mode_always
								SubtitlePlaybackMode.ONLY_FORCED -> R.string.subtitle_mode_only_forced
								SubtitlePlaybackMode.SMART -> R.string.subtitle_mode_smart
								SubtitlePlaybackMode.NONE -> R.string.subtitle_mode_none
								else -> R.string.subtitle_mode_default
							}
						)
					)
				},
				onClick = { router.push(Routes.CUSTOMIZATION_SUBTITLES_MODE) },
				modifier = Modifier.focusKey(Routes.CUSTOMIZATION_SUBTITLES_MODE),
			)
		}
		item {
			ListButton(
				headingContent = { Text(stringResource(R.string.pref_remember_subtitle_selections)) },
				trailingContent = { Checkbox(checked = rememberSubtitleSelections) },
				captionContent = { Text(stringResource(R.string.pref_remember_subtitle_selections_description)) },
				onClick = { rememberSubtitleSelections = !rememberSubtitleSelections },
				modifier = Modifier.focusKey("remember_subtitle_selections"),
			)
		}

		// Series continuation
		item {
			ListButton(
				headingContent = { Text(stringResource(R.string.pref_next_episode_auto_play)) },
				trailingContent = { Checkbox(checked = enableNextEpisodeAutoPlay) },
				captionContent = { Text(stringResource(R.string.pref_next_episode_auto_play_description)) },
				onClick = { enableNextEpisodeAutoPlay = !enableNextEpisodeAutoPlay },
				modifier = Modifier.focusKey("next_episode_auto_play"),
			)
		}
		item {
			ListButton(
				headingContent = { Text(stringResource(R.string.pref_rewatching_in_next_up)) },
				trailingContent = { Checkbox(checked = enableRewatchingInNextUp) },
				captionContent = { Text(stringResource(R.string.pref_rewatching_in_next_up_description)) },
				onClick = { enableRewatchingInNextUp = !enableRewatchingInNextUp },
				modifier = Modifier.focusKey("rewatching_in_next_up"),
			)
		}
		item {
			ListButton(
				headingContent = { Text(stringResource(R.string.pref_episode_images_next_up)) },
				trailingContent = { Checkbox(checked = useEpisodeImagesInNextUpAndResume) },
				captionContent = { Text(stringResource(R.string.pref_episode_images_next_up_description)) },
				onClick = { useEpisodeImagesInNextUpAndResume = !useEpisodeImagesInNextUpAndResume },
				modifier = Modifier.focusKey("episode_images_next_up"),
			)
		}
	}
}