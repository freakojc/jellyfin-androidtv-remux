package org.jellyfin.androidtv.ui.settings.compat

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import org.jellyfin.androidtv.preference.UserConfigurationPreferences
import org.jellyfin.sdk.model.api.UserConfiguration

/**
 * Wrap one field of the **server-side** [UserConfiguration] as [MutableState], mirroring
 * [rememberPreference]. The value is cached by [UserConfigurationPreferences]; assigning a new
 * value patches the full configuration and persists it to the server, so it stays in sync with
 * other Jellyfin clients (web/mobile show the same setting).
 *
 * @param get Read the field from a configuration.
 * @param set Return a copy of the configuration with the field applied.
 * @param fallback Value used while no configuration is available (no session or failed request).
 */
@Composable
fun <T : Any> rememberUserConfigurationPreference(
	preferences: UserConfigurationPreferences,
	get: (UserConfiguration) -> T,
	set: (UserConfiguration, T) -> UserConfiguration,
	fallback: T,
): MutableState<T> {
	val state = remember { mutableStateOf(fallback) }
	val scope = rememberCoroutineScope()

	// Seed and follow the server-side value
	LaunchedEffect(preferences) {
		preferences.ensureLoaded()?.let { state.value = get(it) }
		snapshotFlow { preferences.configuration.value }
			.distinctUntilChanged()
			.collect { configuration ->
				if (configuration != null) {
					val remote = get(configuration)
					if (remote != state.value) state.value = remote
				}
			}
	}

	return remember(state, scope) {
		object : MutableState<T> {
			override var value: T
				get() = state.value
				set(newValue) {
					if (newValue != state.value) {
						state.value = newValue
						scope.launch { preferences.update { set(it, newValue) } }
					}
				}

			override fun component1(): T = value
			override fun component2(): (T) -> Unit = { value = it }
		}
	}
}
