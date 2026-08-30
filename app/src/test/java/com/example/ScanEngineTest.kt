package com.example

import android.graphics.Bitmap
import com.example.data.scan.OcrText
import com.example.data.scan.ScanEngine
import com.example.data.scan.ScanOutcome
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], application = IsaacApp::class)
class ScanEngineTest {

    private val bitmap: Bitmap = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888)

    private fun engineReading(vararg lines: String): ScanEngine =
        ScanEngine(ocr = { OcrText(lines = lines.toList(), fullText = lines.joinToString("\n")) })

    @Test
    fun `clean OCR line identifies the item via the offline path`() = runTest {
        val outcome = engineReading("BRIMSTONE").identify(bitmap, emptyList())
        assertTrue(outcome is ScanOutcome.Identified)
        outcome as ScanOutcome.Identified
        assertEquals("Brimstone", outcome.item.name)
        assertEquals(com.example.data.model.ScanSource.OCR, outcome.source)
        assertNull(outcome.verdict)
    }

    @Test
    fun `wrapped two-line banner name is rejoined and matched`() = runTest {
        val outcome = engineReading("SACRED", "HEART").identify(bitmap, emptyList())
        assertTrue(outcome is ScanOutcome.Identified)
        assertEquals("Sacred Heart", (outcome as ScanOutcome.Identified).item.name)
    }

    @Test
    fun `readable but unmatchable text is Unrecognized`() = runTest {
        val outcome = engineReading(" zzzq not an item xyzzy").identify(bitmap, emptyList())
        assertTrue(outcome is ScanOutcome.Unrecognized)
    }

    @Test
    fun `no text and no api key asks for a closer look`() = runTest {
        val outcome = ScanEngine(ocr = { OcrText.EMPTY }).identify(bitmap, emptyList())
        assertTrue(outcome is ScanOutcome.NeedCloserLook)
    }

    @Test
    fun `run-aware anti-synergy is flagged`() = runTest {
        val outcome = engineReading("Brimstone").identify(bitmap, listOf("My Reflection"))
        outcome as ScanOutcome.Identified
        assertTrue(outcome.isAntiSynergyDetected)
        assertTrue(outcome.activeSynergiesWithRun.isNotEmpty())
    }

    @Test
    fun `god-tier run synergy is not an anti-synergy`() = runTest {
        val outcome = engineReading("Brimstone").identify(bitmap, listOf("Soy Milk"))
        outcome as ScanOutcome.Identified
        assertFalse(outcome.isAntiSynergyDetected)
        assertTrue(outcome.activeSynergiesWithRun.any { it.itemB == "Soy Milk" || it.itemA == "Soy Milk" })
    }
}
