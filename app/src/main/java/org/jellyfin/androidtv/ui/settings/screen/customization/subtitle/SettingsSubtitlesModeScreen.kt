package org.jellyfin.androidtv.ui.settings.screen.customization.subtitle

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
import org.jellyfin.sdk.model.api.SubtitlePlaybackMode
import org.jellyfin.sdk.model.api.UserConfiguration
import org.koin.compose.koinInject

private val subtitleModeLabels = mapOf(
	SubtitlePlaybackMode.DEFAULT to R.string.subtitle_mode_default,
	SubtitlePlaybackMode.ALWAYS to R.string.subtitle_mode_always,
	SubtitlePlaybackMode.ONLY_FORCED to R.string.subtitle_mode_only_forced,
	SubtitlePlaybackMode.SMART to R.string.subtitle_mode_smart,
	SubtitlePlaybackMode.NONE to R.string.subtitle_mode_none,
)

/**
 * Subtitle playback mode, stored server-side in [UserConfiguration.subtitleMode]
 * ("个人设置 ▸ 字幕 ▸ 字幕模式" on the web/mobile clients).
 */
@Composable
fun SettingsSubtitlesModeScreen() {
	val router = LocalRouter.current
	val configurationPreferences = koinInject<UserConfigurationPreferences>()

	var subtitleMode by rememberUserConfigurationPreference(
		configurationPreferences,
		get = { it.subtitleMode },
		set = { configuration, value -> configuration.copy(subtitleMode = value) },
		fallback = SubtitlePlaybackMode.DEFAULT,
	)

	SettingsColumn {
		item {
			ListSection(
				overlineContent = { Text(stringResource(R.string.pref_customization).uppercase()) },
				headingContent = { Text(stringResource(R.string.pref_subtitle_mode)) },
			)
		}

		items(SubtitlePlaybackMode.entries, key = { it.name }) { mode ->
			ListButton(
				headingContent = { Text(stringResource(subtitleModeLabels.getOrElse(mode) { R.string.subtitle_mode_default })) },
				trailingContent = { RadioButton(checked = subtitleMode == mode) },
				onClick = {
					subtitleMode = mode
					router.back()
				},
				modifier = Modifier.focusKey("subtitle_mode_${mode.name}", initialFocus = subtitleMode == mode)
			)
		}
	}
}