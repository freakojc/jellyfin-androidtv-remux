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
import org.jellyfin.androidtv.ui.settings.util.rememberLanguageOptions
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.model.api.UserConfiguration
import org.koin.compose.koinInject

/**
 * Preferred subtitle language, stored server-side in
 * [UserConfiguration.subtitleLanguagePreference] — same setting as the web client.
 */
@Composable
fun SettingsSubtitlesLanguageScreen() {
	val router = LocalRouter.current
	val api = koinInject<ApiClient>()
	val configurationPreferences = koinInject<UserConfigurationPreferences>()
	val options = rememberLanguageOptions(api)

	var subtitleLanguage by rememberUserConfigurationPreference(
		configurationPreferences,
		get = { it.subtitleLanguagePreference ?: "" },
		set = { configuration, value -> configuration.copy(subtitleLanguagePreference = value.ifEmpty { null }) },
		fallback = "",
	)

	SettingsColumn {
		item {
			ListSection(
				overlineContent = { Text(stringResource(R.string.pref_customization).uppercase()) },
				headingContent = { Text(stringResource(R.string.pref_subtitle_language)) },
			)
		}

		items(options, key = { it.value }) { option ->
			ListButton(
				headingContent = { Text(option.label) },
				trailingContent = { RadioButton(checked = subtitleLanguage == option.value) },
				onClick = {
					subtitleLanguage = option.value
					router.back()
				},
				modifier = Modifier.focusKey("subtitle_language_${option.value}", initialFocus = subtitleLanguage == option.value)
			)
		}
	}
}