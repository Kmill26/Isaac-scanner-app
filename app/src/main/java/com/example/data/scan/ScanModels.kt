package com.example.data.scan

import com.example.data.model.ActiveRunSynergy
import com.example.data.model.IsaacItem
import com.example.data.model.ScanSource

/**
 * Raw text pulled off a captured frame by the OCR layer.
 *
 * [lines] are ranked largest-and-most-central first (the pedestal item banner is usually the
 * biggest text on screen). [fullText] is ML Kit's whole block dump in reading order, newlines
 * intact — useful for names that wrap across two lines.
 */
data class OcrText(
    val lines: List<String>,
    val fullText: String
) {
    val hasText: Boolean get() = fullText.isNotBlank() || lines.any { it.isNotBlank() }

    companion object {
        val EMPTY = OcrText(emptyList(), "")
    }
}

/**
 * Every failure mode of the optional Gemini path, each carrying a user-facing [message].
 * The offline OCR path never throws these — it returns a [ScanOutcome] instead.
 */
sealed class ScanException(override val message: String) : Exception() {

    /** No real key configured — caller should not have invoked Gemini. */
    object NoApiKey : ScanException(
        "No AI key configured. Add a Gemini API key to enable sprite-only recognition."
    )

    /** Gemini ran but reported no Isaac item in frame (or below the 0.35 confidence floor). */
    object NoItemDetected : ScanException(
        "No Isaac item found in that shot. Fill the box with the pedestal item and try again."
    )

    /** HTTP 429. [retryAfterSec] is the server's requested delay when it supplied one. */
    data class RateLimited(val retryAfterSec: Int?) : ScanException(
        "AI recognition is rate limited right now" +
            (retryAfterSec?.let { ". Try again in ${it}s." } ?: ". Try again shortly.")
    )

    /** HTTP 5xx from the model endpoint. */
    data class ServerError(val code: Int) : ScanException(
        "AI recognition service error ($code). Try again in a moment."
    )

    /** Transport failure — no connectivity, timeout, TLS, etc. */
    data class Network(override val cause: Throwable) : ScanException(
        "Couldn't reach AI recognition. Check your connection and retry."
    )

    /** Reached the model but the response was unusable (blocked, truncated, malformed, API error). */
    data class BadResponse(val detail: String) : ScanException(
        "AI recognition returned an unexpected response."
    )
}

/**
 * The single value [ScanEngine.identify] resolves to. Phase 4's ViewModel switches on this.
 */
sealed class ScanOutcome {

    /**
     * OCR (or the Gemini fallback) resolved a catalog item.
     *
     * @param confidence 0..1. For OCR this is the name-match score; for Gemini, the model's own.
     * @param verdict a run-aware "should I take this?" note — only populated on the Gemini path,
     *   `null` after a plain OCR match (Phase 4 fetches one on demand via [ScanEngine.verdictFor]).
     * @param rawText the OCR text the match came from, for debugging / display; `null` for Gemini.
     */
    data class Identified(
        val item: IsaacItem,
        val confidence: Float,
        val source: ScanSource,
        val verdict: String?,
        val activeSynergiesWithRun: List<ActiveRunSynergy>,
        val isAntiSynergyDetected: Boolean,
        val rawText: String?
    ) : ScanOutcome()

    /** OCR produced readable text but nothing in it matched the catalog. */
    data class Unrecognized(val rawText: String) : ScanOutcome()

    /** No usable text and no Gemini key — the user needs to reframe / get closer. */
    data class NeedCloserLook(val message: String) : ScanOutcome()

    /** The Gemini fallback was attempted and threw. Carries the typed [error]. */
    data class Failed(val error: ScanException) : ScanOutcome()
}
