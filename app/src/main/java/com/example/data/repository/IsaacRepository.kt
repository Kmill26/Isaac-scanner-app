package com.example.data.repository

import android.graphics.Bitmap
import com.example.data.db.AppDatabase
import com.example.data.db.RunEntity
import com.example.data.db.ScanEntity
import com.example.data.gemini.GeminiVisionService
import com.example.data.model.IsaacItem
import com.example.data.model.IsaacItemDatabase
import com.example.data.model.ScanDetectionResult
import kotlinx.coroutines.flow.Flow

class IsaacRepository(
    private val database: AppDatabase,
    private val geminiService: GeminiVisionService = GeminiVisionService()
) {
    val recentScans: Flow<List<ScanEntity>> = database.scanDao().getRecentScans()
    val savedRuns: Flow<List<RunEntity>> = database.runDao().getAllSavedRuns()

    suspend fun scanImage(bitmap: Bitmap, currentRunItemNames: List<String>): ScanDetectionResult {
        val result = geminiService.scanItemFromImage(bitmap, currentRunItemNames)

        // Save into scan history
        database.scanDao().insertScan(
            ScanEntity(
                itemName = result.detectedName,
                itemQuality = result.matchedItem?.quality ?: 0,
                confidence = result.confidence,
                verdict = result.rawGeminiVerdict
            )
        )

        return result
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
