package com.example.data.ocr

import android.graphics.Bitmap
import com.example.data.scan.OcrText
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.tasks.await
import kotlin.math.hypot

/**
 * On-device text recognition over a captured frame. Fully offline — the
 * `com.google.mlkit:text-recognition` artifact bundles the Latin model into the APK, so this
 * needs no Play Services and no network.
 *
 * The client is a process singleton (cheap to hold, expensive to spin up per call).
 */
object ItemTextRecognizer {

    private val client: TextRecognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    /**
     * OCR [bitmap] (assumed already cropped tight to the reticle by the capture layer).
     * Returns lines ranked biggest-and-most-central first plus the full reading-order dump.
     */
    suspend fun readLines(bitmap: Bitmap): OcrText {
        val image = InputImage.fromBitmap(bitmap, 0)
        val visionText = client.process(image).await()

        val cx = bitmap.width / 2f
        val cy = bitmap.height / 2f

        data class Ranked(val text: String, val area: Long, val dist: Float)

        val ranked = ArrayList<Ranked>()
        for (block in visionText.textBlocks) {
            for (line in block.lines) {
                val text = line.text.trim()
                if (text.isEmpty()) continue
                val box = line.boundingBox
                val area = box?.let { it.width().toLong() * it.height().toLong() } ?: 0L
                val dist = box?.let {
                    hypot((it.exactCenterX() - cx).toDouble(), (it.exactCenterY() - cy).toDouble()).toFloat()
                } ?: Float.MAX_VALUE
                ranked.add(Ranked(text, area, dist))
            }
        }

        val ordered = ranked
            .sortedWith(compareByDescending<Ranked> { it.area }.thenBy { it.dist })
            .map { it.text }

        return OcrText(lines = ordered, fullText = visionText.text.trim())
    }
}
