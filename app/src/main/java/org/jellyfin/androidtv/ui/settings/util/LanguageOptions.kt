package org.jellyfin.androidtv.ui.settings.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.platform.LocalContext
import org.jellyfin.androidtv.R
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.localizationApi
import timber.log.Timber

/**
 * A selectable language for the server-side audio/subtitle language preference.
 *
 * [value] is stored in `UserConfiguration` and matched by the server, [label] is shown in the UI.
 * An empty [value] means "no preference" (server default behaviour).
 */
data class LanguageOption(
	val value: String,
	val label: String,
)

/**
 * Load the languages supported by the server from `/Localization/Cultures`, falling back to a
 * short built-in list when the request fails (offline, older server, etc.).
 */
@Composable
fun rememberLanguageOptions(
	api: ApiClient,
	fallbackLabelPrefixRes: Int = R.string.language_none,
): List<LanguageOption> {
	val context = LocalContext.current

	return produceState<List<LanguageOption>>(initialValue = fallbackLanguages(context.getString(fallbackLabelPrefixRes)), api) {
		value = try {
			val cultures = api.localizationApi.getCultures().content
			buildList {
				add(LanguageOption("", context.getString(fallbackLabelPrefixRes)))
				addAll(
					cultures
						.map { it.name to (it.displayName ?: it.name) }
						.distinctBy { it.first }
						.sortedBy { it.second }
						.map { (name, display) -> LanguageOption(name, display) }
				)
			}
		} catch (err: Exception) {
			Timber.e(err, "Unable to load the server culture list, using the built-in fallback")
			value
		}
	}.value
}

private fun fallbackLanguages(noneLabel: String): List<LanguageOption> = listOf(
	LanguageOption("", noneLabel),
	// Common values in the shape the server expects (culture names)
	LanguageOption("zh-CN", "中文（简体，中国）"),
	LanguageOption("zh-TW", "中文（繁體，台灣）"),
	LanguageOption("en", "English"),
	LanguageOption("ja", "日本語"),
	LanguageOption("ko", "한국어"),
	LanguageOption("ru", "Русский"),
)
