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
 * The core scan pipeline: **OCR first, fully offline**, Gemini only as a last resort.
 *
 * Decision flow for [identify]:
 *  1. OCR the (already reticle-cropped) bitmap.
 *  2. Build candidate strings — each ranked line, adjacent-line joins (names wrap on the banner),
 *     and the whole joined blob — and run every candidate through [IsaacItemDatabase.match].
 *  3. Best match clears [OCR_MATCH_BAR]  → [ScanOutcome.Identified] (source OCR, verdict null).
 *  4. Readable text but no catalog match  → [ScanOutcome.Unrecognized].
 *  5. No usable text + Gemini configured  → Gemini vision fallback (→ Identified / Unrecognized / Failed).
 *  6. No usable text + no key             → [ScanOutcome.NeedCloserLook].
 *
 * [ocr] and [gemini] are injectable so this is unit-testable without ML Kit or the network.
 */
class ScanEngine(
    private val ocr: suspend (Bitmap) -> OcrText = { ItemTextRecognizer.readLines(it) },
    private val gemini: GeminiVisionService = GeminiVisionService()
) {

    suspend fun identify(bitmap: Bitmap, currentRunItemNames: List<String>): ScanOutcome {
        val text = runCatching { ocr(bitmap) }.getOrDefault(OcrText.EMPTY)

        bestOcrMatch(text)?.let { (item, score) ->
            if (score >= OCR_MATCH_BAR) {
                return identified(
                    item = item,
                    confidence = score,
                    source = ScanSource.OCR,
                    verdict = null,
                    runNames = currentRunItemNames,
                    rawText = text.fullText.ifBlank { text.lines.joinToString(" ") },
                    geminiAntiSynergy = false
                )
            }
        }

        if (text.hasText) {
            return ScanOutcome.Unrecognized(text.fullText.ifBlank { text.lines.joinToString(" ") })
        }

        if (!gemini.isConfigured()) {
            return ScanOutcome.NeedCloserLook(NEED_CLOSER_MESSAGE)
        }

        return try {
            val id = gemini.identify(bitmap)
            val item = IsaacItemDatabase.findItemByName(id.itemName)
            if (item == null) {
                ScanOutcome.Unrecognized(id.itemName)
            } else {
                identified(
                    item = item,
                    confidence = id.confidence,
                    source = ScanSource.GEMINI,
                    verdict = id.verdict.ifBlank { null },
                    runNames = currentRunItemNames,
                    rawText = null,
                    geminiAntiSynergy = id.antiSynergy
                )
            }
        } catch (e: ScanException) {
            ScanOutcome.Failed(e)
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
        /** OCR name-match score a candidate must clear to count as an identification. */
        const val OCR_MATCH_BAR = 0.82f

        const val NEED_CLOSER_MESSAGE =
            "Couldn't read the item. Move closer, fill the box with the pedestal, and wait for " +
                "the item-name banner to appear. Add a Gemini API key for AI sprite recognition."
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
