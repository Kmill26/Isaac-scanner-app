package com.example.data.repository

import android.graphics.Bitmap
import com.example.data.db.AppDatabase
import com.example.data.db.RunEntity
import com.example.data.db.ScanEntity
import com.example.data.model.IsaacItem
import com.example.data.model.IsaacItemDatabase
import com.example.data.model.ScanDetectionResult
import com.example.data.scan.ScanEngine
import com.example.data.scan.ScanOutcome
import com.example.data.scan.messageOrNull
import com.example.data.scan.toDetectionResult
import kotlinx.coroutines.flow.Flow

class IsaacRepository(
    private val database: AppDatabase,
    private val scanEngine: ScanEngine = ScanEngine()
) {
    val recentScans: Flow<List<ScanEntity>> = database.scanDao().getRecentScans()
    val savedRuns: Flow<List<RunEntity>> = database.runDao().getAllSavedRuns()

    val geminiConfigured: Boolean get() = scanEngine.geminiConfigured()

    /**
     * Full pipeline outcome. Phase 4's ViewModel should call this and `when`-branch on
     * [ScanOutcome] directly. Only a successful [ScanOutcome.Identified] is written to history.
     */
    suspend fun identify(bitmap: Bitmap, currentRunItemNames: List<String>): ScanOutcome {
        val outcome = scanEngine.identify(bitmap, currentRunItemNames)
        if (outcome is ScanOutcome.Identified) persist(outcome.toDetectionResult())
        return outcome
    }

    /**
     * Legacy shim kept so the pre-Phase-4 ViewModel still compiles. Identified scans return a
     * real [ScanDetectionResult]; every other outcome throws [ScanException] (via the message).
     */
    suspend fun scanImage(bitmap: Bitmap, currentRunItemNames: List<String>): ScanDetectionResult {
        return when (val outcome = identify(bitmap, currentRunItemNames)) {
            is ScanOutcome.Identified -> outcome.toDetectionResult()
            is ScanOutcome.Failed -> throw outcome.error
            else -> throw IllegalStateException(outcome.messageOrNull() ?: "Scan failed")
        }
    }

    /** On-demand run-aware "should I take this?" verdict after a successful OCR match. */
    suspend fun verdictFor(itemName: String, currentRunItemNames: List<String>): String =
        scanEngine.verdictFor(itemName, currentRunItemNames)

    private suspend fun persist(result: ScanDetectionResult) {
        database.scanDao().insertScan(
            ScanEntity(
                itemName = result.detectedName,
                itemQuality = result.matchedItem?.quality ?: 0,
                confidence = result.confidence,
                verdict = result.rawGeminiVerdict
            )
        )
    }

    suspend fun saveRun(run: RunEntity): Long {
        return database.runDao().insertRun(run)
    }

    suspend fun deleteRun(id: Long) {
        database.runDao().deleteRun(id)
    }

    suspend fun clearHistory() {
        database.scanDao().clearAllScans()
    }

    fun getAllItems(): List<IsaacItem> = IsaacItemDatabase.items

    fun searchItems(
        query: String = "",
        qualityFilter: Int? = null,
        poolFilter: String? = null,
        transformationFilter: String? = null
    ): List<IsaacItem> {
        return IsaacItemDatabase.items.filter { item ->
            val matchesQuery = query.isBlank() ||
                    item.name.contains(query, ignoreCase = true) ||
                    item.quote.contains(query, ignoreCase = true) ||
                    item.description.contains(query, ignoreCase = true)

            val matchesQuality = qualityFilter == null || item.quality == qualityFilter
            val matchesPool = poolFilter == null || item.itemPools.any { it.contains(poolFilter, ignoreCase = true) }
            val matchesTrans = transformationFilter == null || item.transformations.any { it.contains(transformationFilter, ignoreCase = true) }

            matchesQuery && matchesQuality && matchesPool && matchesTrans
        }
    }
}
