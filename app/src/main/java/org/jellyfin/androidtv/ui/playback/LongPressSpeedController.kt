package org.jellyfin.androidtv.ui.playback

/**
 * Boosts the playback speed while the remote's fast-forward key is held (long press) and restores
 * the previous speed on release. Mirrors the "long press to speed up" behaviour of the phone
 * client, adapted for D-pad/remote input.
 *
 * Only works for the forward direction - rewinding keeps the normal repeated-seek behaviour.
 */
class LongPressSpeedController(
	private val parentController: PlaybackController,
) {
	private var holding = false
	private var restoreSpeed = 1f

	/** True while a boost is active. */
	val isHolding: Boolean
		get() = holding

	/** Start boosting the playback speed by [multiplier] (stored, e.g. 2.0 for 2x). */
	fun startHold(multiplier: Float) {
		if (holding) return

		val current = parentController.playbackSpeed.takeIf { it > 0f } ?: 1f
		if (current >= 2f * multiplier) return

		holding = true
		restoreSpeed = current
		parentController.setPlaybackSpeed((current * multiplier).coerceIn(0.25f, 4f))
	}

	/** Restore the speed that was active before [startHold]. */
	fun endHold() {
		if (!holding) return
		holding = false
		parentController.setPlaybackSpeed(restoreSpeed)
	}
}