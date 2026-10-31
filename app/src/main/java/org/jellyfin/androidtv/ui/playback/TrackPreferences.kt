package org.jellyfin.androidtv.ui.playback

import org.jellyfin.sdk.model.api.MediaSourceInfo
import org.jellyfin.sdk.model.api.MediaStream
import org.jellyfin.sdk.model.api.MediaStreamType
import org.jellyfin.sdk.model.api.SubtitlePlaybackMode

/**
 * Default audio and subtitle tracks: English when the source has it, and never Russian unless
 * there is nothing else. Shared by the details page (what it shows) and the player (what starts).
 */
object TrackPreferences {
	private val ENGLISH = setOf("eng", "en")
	private val RUSSIAN = setOf("rus", "ru")

	/** Audio track index (or null) and subtitle track index (-1 means no subtitles). */
	data class Tracks(val audio: Int?, val subtitle: Int)

	@JvmStatic
	fun isRussian(stream: MediaStream): Boolean =
		stream.language?.lowercase() in RUSSIAN ||
			listOfNotNull(stream.displayTitle, stream.title).any { it.contains("russian", ignoreCase = true) }

	@JvmStatic
	fun isEnglish(stream: MediaStream): Boolean =
		stream.language?.lowercase() in ENGLISH ||
			(stream.language == null && listOfNotNull(stream.displayTitle, stream.title).any { it.contains("english", ignoreCase = true) })

	/** True when [index] points at a Russian track in [streams]. */
	@JvmStatic
	fun isRussianStream(streams: List<MediaStream>?, index: Int?): Boolean =
		index != null && streams?.firstOrNull { it.index == index }?.let(::isRussian) == true

	/** English first, then the server default unless it is Russian, then the first non-Russian track. */
	@JvmStatic
	fun defaultAudio(streams: List<MediaStream>?, serverDefault: Int?): Int? {
		val audio = streams.orEmpty().filter { it.type == MediaStreamType.AUDIO }
		val english = audio.filter(::isEnglish)
		english.firstOrNull { it.isDefault }?.let { return it.index }
		english.firstOrNull()?.let { return it.index }

		val server = audio.firstOrNull { it.index == serverDefault }
		if (server != null && !isRussian(server)) return server.index
		audio.firstOrNull { !isRussian(it) }?.let { return it.index }
		return serverDefault ?: audio.firstOrNull()?.index
	}

	/** No subtitles when the audio is English, otherwise full English subtitles if available. */
	@JvmStatic
	fun defaultSubtitle(streams: List<MediaStream>?, audioIndex: Int?): Int {
		val list = streams.orEmpty()
		if (list.firstOrNull { it.type == MediaStreamType.AUDIO && it.index == audioIndex }?.let(::isEnglish) == true) return -1

		val english = list.filter { it.type == MediaStreamType.SUBTITLE && isEnglish(it) && !it.isForced }
		return (english.firstOrNull { it.isDefault } ?: english.firstOrNull())?.index ?: -1
	}

	/** Applies the defaults on top of what the user picked on the details page (null = not picked). */
	@JvmStatic
	fun resolve(source: MediaSourceInfo, pickedAudio: Int?, pickedSubtitle: Int?): Tracks {
		val audio = pickedAudio ?: defaultAudio(source.mediaStreams, source.defaultAudioStreamIndex)
		val subtitle = pickedSubtitle ?: defaultSubtitle(source.mediaStreams, audio)
		return Tracks(audio, subtitle)
	}

	/**
	 * Resolve the tracks when the **server-side user configuration** should be honoured
	 * (preferred audio/subtitle language, subtitle mode). Details page picks still win.
	 *
	 * @param preferredAudioLanguage culture code such as "zh-CN" or ISO code "zho" (null/blank = unset)
	 * @param preferredSubtitleLanguage same format as [preferredAudioLanguage]
	 * @param subtitleMode server subtitle playback mode (null = default)
	 * @param playDefaultAudioTrack when true the server default audio track is used
	 */
	@JvmStatic
	fun resolveWithUserConfiguration(
		source: MediaSourceInfo,
		pickedAudio: Int?,
		pickedSubtitle: Int?,
		preferredAudioLanguage: String?,
		preferredSubtitleLanguage: String?,
		subtitleMode: SubtitlePlaybackMode?,
		playDefaultAudioTrack: Boolean,
	): Tracks {
		val streams = source.mediaStreams

		// Audio: explicit pick > language preference (unless default track requested) > existing logic
		val audio = pickedAudio
			?: if (!playDefaultAudioTrack) audioIndexForLanguage(streams, preferredAudioLanguage) else null
			?: defaultAudio(streams, source.defaultAudioStreamIndex)

		// Subtitles: explicit pick wins, otherwise the server subtitle mode decides
		val subtitle = pickedSubtitle ?: when (subtitleMode) {
			SubtitlePlaybackMode.NONE -> -1
			SubtitlePlaybackMode.ONLY_FORCED ->
				forcedSubtitleIndex(streams, preferredSubtitleLanguage) ?: -1
			SubtitlePlaybackMode.ALWAYS ->
				subtitleIndexForLanguage(streams, preferredSubtitleLanguage)
					?: firstTextSubtitleIndex(streams)
					?: defaultSubtitle(streams, audio)
			SubtitlePlaybackMode.SMART,
			SubtitlePlaybackMode.DEFAULT,
			null -> subtitleIndexForLanguage(streams, preferredSubtitleLanguage)
				?: defaultSubtitle(streams, audio)
		}

		return Tracks(audio, subtitle)
	}

	/** Index of the first audio track matching [language], or null when unset/not found. */
	@JvmStatic
	fun audioIndexForLanguage(streams: List<MediaStream>?, language: String?): Int? =
		streams.orEmpty()
			.filter { it.type == MediaStreamType.AUDIO && matchesLanguage(it, language) }
			.let { list -> (list.firstOrNull { it.isDefault } ?: list.firstOrNull())?.index }

	/** Index of the first (non-forced) subtitle track matching [language], or null. */
	@JvmStatic
	fun subtitleIndexForLanguage(streams: List<MediaStream>?, language: String?): Int? =
		streams.orEmpty()
			.filter { it.type == MediaStreamType.SUBTITLE && !it.isForced && matchesLanguage(it, language) }
			.let { list -> (list.firstOrNull { it.isDefault } ?: list.firstOrNull())?.index }

	/** Index of a forced subtitle track, preferring one matching [language]. */
	@JvmStatic
	fun forcedSubtitleIndex(streams: List<MediaStream>?, language: String?): Int? {
		val forced = streams.orEmpty().filter { it.type == MediaStreamType.SUBTITLE && it.isForced }
		return (forced.firstOrNull { matchesLanguage(it, language) } ?: forced.firstOrNull())?.index
	}

	/** Index of the first text subtitle track, or null. */
	@JvmStatic
	fun firstTextSubtitleIndex(streams: List<MediaStream>?): Int? =
		streams.orEmpty().firstOrNull { it.type == MediaStreamType.SUBTITLE && it.isTextSubtitle() }?.index

	private fun MediaStream.isTextSubtitle(): Boolean = codec?.lowercase()?.startsWith("pgs") != true &&
		codec?.lowercase() != "dvdsub" && codec?.lowercase() != "vobsub"

	/**
	 * Compare a media stream language (often a 3-letter ISO 639-2 code like "zho"/"chi") with a
	 * preference value (culture name like "zh-CN" or code like "zh"). Handles the common aliases.
	 */
	@JvmStatic
	fun matchesLanguage(stream: MediaStream, language: String?): Boolean {
		val preference = language?.trim()?.lowercase()?.takeIf { it.isNotEmpty() } ?: return false
		val streamLanguage = stream.language?.trim()?.lowercase()?.takeIf { it.isNotEmpty() }
			?: return false
		val preferencePrimary = preference.substringBefore('-').substringBefore('_')
		val streamPrimary = streamLanguage.substringBefore('-').substringBefore('_')
		if (streamLanguage == preference || streamPrimary == preferencePrimary) return true

		val preferenceAlias = ISO_ALIASES[preferencePrimary] ?: preferencePrimary
		val streamAlias = ISO_ALIASES[streamPrimary] ?: streamPrimary
		return preferenceAlias == streamAlias
	}

	private val ISO_ALIASES = mapOf(
		"eng" to "en", "chi" to "zh", "zho" to "zh", "rus" to "ru", "jpn" to "ja", "kor" to "ko",
		"fre" to "fr", "fra" to "fr", "ger" to "de", "deu" to "de", "spa" to "es", "ita" to "it",
		"por" to "pt", "ara" to "ar", "hin" to "hi", "tha" to "th", "vie" to "vi", "nld" to "nl",
		"dut" to "nl", "pol" to "pl", "tur" to "tr", "ukr" to "uk", "swe" to "sv", "nor" to "no",
		"dan" to "da", "fin" to "fi", "ces" to "cs", "cze" to "cs", "slo" to "sk", "slk" to "sk",
		"ell" to "el", "gre" to "el", "heb" to "he", "ind" to "id", "msa" to "ms", "hun" to "hu",
		"ron" to "ro", "rum" to "ro", "bul" to "bg", "hrv" to "hr", "srp" to "sr", "cat" to "ca",
		"lit" to "lt", "lav" to "lv", "est" to "et", "isl" to "is", "mkd" to "mk", "bel" to "be",
		"kaz" to "kk", "aze" to "az", "geo" to "ka", "kat" to "ka", "arm" to "hy", "hye" to "hy",
		"amh" to "am", "swa" to "sw", "urd" to "ur", "fas" to "fa", "per" to "fa", "ben" to "bn",
		"tam" to "ta", "tel" to "te", "mar" to "mr", "guj" to "gu", "kan" to "kn", "mal" to "ml",
		"pan" to "pa", "sin" to "si", "nep" to "ne", "khm" to "km", "lao" to "lo", "mya" to "my",
		"zul" to "zu", "afr" to "af", "eus" to "eu", "baq" to "eu", "glg" to "gl", "cym" to "cy",
		"wel" to "cy", "gle" to "ga", "sqi" to "sq", "alb" to "sq", "hye" to "hy",
	)
}
