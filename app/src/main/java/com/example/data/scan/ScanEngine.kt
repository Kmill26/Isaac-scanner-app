package com.example.data.scan

import android.graphics.Bitmap
import com.example.data.gemini.GeminiVisionService
import com.example.data.model.ActiveRunSynergy
import com.example.data.model.IsaacItem
import com.example.data.model.IsaacItemDatabase
import com.example.data.model.ScanDetectionResult
import com.example.data.model.ScanSource
import com.example.data.model.SynergyRating
import com.example.data.ocr.ItemTextRecognizer

/**
 * The core scan pipeline.
 *
 * **When a Gemini key is configured, AI vision is the primary path** — reading Isaac's
 * stylised pickup-banner font and recognising sprites in a phone photo of a TV is exactly
 * what a vision model handles well and what on-device OCR + fuzzy matching handles badly.
 * OCR still runs first (fast, free), but its result is only trusted outright on a
 * near-certain exact hit; otherwise Gemini decides, with OCR as the fallback if Gemini errors.
 *
 * **With no key it's OCR-only** (offline, private) with honest "couldn't read it" outcomes.
 *
 * Decision flow for [identify]:
 *  1. OCR the (already reticle-cropped) bitmap; compute the best catalog match across candidates.
 *  2. Near-certain exact OCR hit ([STRONG_OCR_BAR]) → Identified, offline, no API call.
 *  3. Else if a key is configured → Gemini vision (identify + run-aware verdict in one call).
 *     Success → Identified. Failure → fall back to any decent OCR match, else the error / text.
 *  4. Else (no key): OCR match at [OCR_MATCH_BAR] → Identified; readable-but-unmatched text →
 *     Unrecognized; nothing → NeedCloserLook.
 *
 * [ocr] and [gemini] are injectable so this is unit-testable without ML Kit or the network.
 */
class ScanEngine(
    private val ocr: suspend (Bitmap) -> OcrText = { ItemTextRecognizer.readLines(it) },
    private val gemini: GeminiVisionService = GeminiVisionService()
) {

    suspend fun identify(bitmap: Bitmap, currentRunItemNames: List<String>): ScanOutcome {
        val text = runCatching { ocr(bitmap) }.getOrDefault(OcrText.EMPTY)
        val ocrMatch = bestOcrMatch(text)
        val rawText = text.fullText.ifBlank { text.lines.joinToString(" ") }

        fun ocrIdentified(m: Pair<IsaacItem, Float>) = identified(
            item = m.first, confidence = m.second, source = ScanSource.OCR,
            verdict = null, runNames = currentRunItemNames, rawText = rawText,
            geminiAntiSynergy = false
        )

        // Only a near-certain exact OCR hit (e.g. an actual full pickup banner) bypasses Gemini.
        ocrMatch?.takeIf { it.second >= STRONG_OCR_BAR }?.let { return ocrIdentified(it) }

        if (gemini.isConfigured()) {
            return try {
                val id = gemini.identify(bitmap, currentRunItemNames)
                val item = IsaacItemDatabase.findItemByName(id.itemName)
                if (item != null) {
                    identified(
                        item = item,
                        confidence = id.confidence,
                        source = ScanSource.GEMINI,
                        verdict = id.verdict.ifBlank { null },
                        runNames = currentRunItemNames,
                        rawText = null,
                        geminiAntiSynergy = id.antiSynergy
                    )
                } else {
                    // AI named something not in the bundled catalog — still worth showing.
                    ScanOutcome.Unrecognized(id.itemName)
                }
            } catch (e: ScanException) {
                // AI unavailable (rate limit / network / bad response) — fall back to OCR.
                ocrMatch?.takeIf { it.second >= OCR_MATCH_BAR }?.let { return ocrIdentified(it) }
                if (text.hasText) ScanOutcome.Unrecognized(rawText) else ScanOutcome.Failed(e)
            }
        }

        // No key: offline OCR only.
        ocrMatch?.takeIf { it.second >= OCR_MATCH_BAR }?.let { return ocrIdentified(it) }
        return if (text.hasText) {
            ScanOutcome.Unrecognized(rawText)
        } else {
            ScanOutcome.NeedCloserLook(NEED_CLOSER_MESSAGE)
        }
    }

    /**
     * On-demand run-aware verdict for an already-identified item (the user tapped the button on
     * the result card). Throws [ScanException] — the caller keeps showing the offline result.
     */
    suspend fun verdictFor(itemName: String, currentRunItemNames: List<String>): String =
        gemini.verdictFor(itemName, currentRunItemNames)

    fun geminiConfigured(): Boolean = gemini.isConfigured()

    // ------------------------------------------------------------------------------------------

    private fun bestOcrMatch(text: OcrText): Pair<IsaacItem, Float>? {
        var best: Pair<IsaacItem, Float>? = null
        for (candidate in buildCandidates(text)) {
            val m = IsaacItemDatabase.match(candidate) ?: continue
            if (best == null || m.score > best!!.second) best = m.item to m.score
        }
        return best
    }

    private fun buildCandidates(text: OcrText): List<String> {
        val out = LinkedHashSet<String>()

        text.lines.forEach { if (it.isNotBlank()) out.add(it.trim()) }

        // adjacent joins in rank order
        for (i in 0 until text.lines.size - 1) {
            out.add("${text.lines[i].trim()} ${text.lines[i + 1].trim()}".trim())
        }

        // adjacent joins in reading order (banner name wraps top-to-bottom)
        val readingLines = text.fullText.split('\n').map { it.trim() }.filter { it.isNotEmpty() }
        readingLines.forEach { out.add(it) }
        for (i in 0 until readingLines.size - 1) {
            out.add("${readingLines[i]} ${readingLines[i + 1]}")
        }

        if (text.fullText.isNotBlank()) out.add(text.fullText.replace('\n', ' ').trim())

        return out.filter { it.isNotBlank() }
    }

    private fun identified(
        item: IsaacItem,
        confidence: Float,
        source: ScanSource,
        verdict: String?,
        runNames: List<String>,
        rawText: String?,
        geminiAntiSynergy: Boolean
    ): ScanOutcome.Identified {
        val runItems = runNames.mapNotNull { IsaacItemDatabase.findItemByName(it) }
        val synergies = IsaacItemDatabase.calculateSynergies(item, runItems)
        val anti = geminiAntiSynergy || synergies.any { it.rating == SynergyRating.ANTI_SYNERGY }
        return ScanOutcome.Identified(
            item = item,
            confidence = confidence,
            source = source,
            verdict = verdict,
            activeSynergiesWithRun = synergies,
            isAntiSynergyDetected = anti,
            rawText = rawText
        )
    }

    companion object {
        /** OCR match score to accept as an identification when there's no AI to defer to. */
        const val OCR_MATCH_BAR = 0.82f

        /** OCR match score high enough to trust outright even when a Gemini key is present. */
        const val STRONG_OCR_BAR = 0.99f

        const val NEED_CLOSER_MESSAGE =
            "Couldn't identify the item. On console, items have no text before pickup — add a Gemini API key " +
                "to enable AI sprite recognition, or frame the item-name banner after pickup."
    }
}

/** Adapts an [ScanOutcome.Identified] into the legacy [ScanDetectionResult] the current UI consumes. */
fun ScanOutcome.Identified.toDetectionResult(): ScanDetectionResult = ScanDetectionResult(
    detectedName = item.name,
    confidence = confidence,
    rawGeminiVerdict = verdict ?: item.description,
    matchedItem = item,
    activeSynergiesWithRun = activeSynergiesWithRun,
    isAntiSynergyDetected = isAntiSynergyDetected,
    source = source
)

/** Best-effort user-facing message for the non-identified outcomes. */
fun ScanOutcome.messageOrNull(): String? = when (this) {
    is ScanOutcome.Identified -> null
    is ScanOutcome.Unrecognized -> "Read \"$rawText\" but couldn't match it to a catalog item."
    is ScanOutcome.NeedCloserLook -> message
    is ScanOutcome.Failed -> error.message
}
