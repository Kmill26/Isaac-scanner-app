package com.example.ui.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.db.RunEntity
import com.example.data.db.ScanEntity
import com.example.data.model.ActiveRunSynergy
import com.example.data.model.IsaacItem
import com.example.data.model.IsaacItemDatabase
import com.example.data.model.ScanDetectionResult
import com.example.data.model.SynergyRating
import com.example.data.model.TransformationProgress
import com.example.data.model.XboxPresetScreen
import com.example.data.repository.IsaacRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ScannerUiState(
    val isScanning: Boolean = false,
    val isAutoScanEnabled: Boolean = false,
    val torchEnabled: Boolean = false,
    val zoomLevel: Float = 1.0f,
    val latestScanResult: ScanDetectionResult? = null,
    val scanErrorMessage: String? = null,
    val currentRunItems: List<IsaacItem> = listOf(
        // Default starter items for demonstration
        IsaacItemDatabase.findItemByName("Soy Milk") ?: IsaacItemDatabase.items[0]
    ),
    val compendiumQuery: String = "",
    val compendiumQualityFilter: Int? = null,
    val compendiumPoolFilter: String? = null,
    val compendiumTransformationFilter: String? = null,
    val selectedDetailItem: IsaacItem? = null,
    val testCandidateItem: IsaacItem? = null
)

class ScannerViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: IsaacRepository by lazy {
        val db = AppDatabase.getInstance(application)
        IsaacRepository(db)
    }

    private val _uiState = MutableStateFlow(ScannerUiState())
    val uiState: StateFlow<ScannerUiState> = _uiState.asStateFlow()

    val scanHistory: StateFlow<List<ScanEntity>> = repository.recentScans
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val savedRuns: StateFlow<List<RunEntity>> = repository.savedRuns
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Dynamic Run Calculations
    val activeRunSynergies: StateFlow<List<ActiveRunSynergy>> = MutableStateFlow(emptyList())
    val activeTransformations: StateFlow<List<TransformationProgress>> = MutableStateFlow(emptyList())

    init {
        recalculateRunStats()
    }

    fun setTorchEnabled(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(torchEnabled = enabled)
    }

    fun setZoomLevel(zoom: Float) {
        _uiState.value = _uiState.value.copy(zoomLevel = zoom.coerceIn(1.0f, 5.0f))
    }

    fun toggleAutoScan() {
        val newAutoState = !_uiState.value.isAutoScanEnabled
        _uiState.value = _uiState.value.copy(isAutoScanEnabled = newAutoState)
    }

    fun scanBitmap(bitmap: Bitmap) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isScanning = true, scanErrorMessage = null)
            try {
                val currentNames = _uiState.value.currentRunItems.map { it.name }
                val result = repository.scanImage(bitmap, currentNames)
                _uiState.value = _uiState.value.copy(
                    isScanning = false,
                    latestScanResult = result
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isScanning = false,
                    scanErrorMessage = "Scan error: ${e.localizedMessage ?: "Unknown error"}"
                )
            }
        }
    }

    fun scanXboxPreset(preset: XboxPresetScreen) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isScanning = true, scanErrorMessage = null)

            // Generate an indicative test bitmap representation of TV screen
            val bitmap = createSyntheticConsoleBitmap(preset.itemName, preset.iconEmoji)
            val matched = IsaacItemDatabase.findItemByName(preset.itemName) ?: IsaacItemDatabase.items.first()
            val currentItems = _uiState.value.currentRunItems
            val synergies = IsaacItemDatabase.calculateSynergies(matched, currentItems)
            val isAnti = synergies.any { it.rating == SynergyRating.ANTI_SYNERGY }

            val result = ScanDetectionResult(
                detectedName = matched.name,
                confidence = 0.98f,
                rawGeminiVerdict = "Detected on Xbox ${preset.roomType}: ${matched.name} (${matched.quote}). ${matched.description}",
                matchedItem = matched,
                activeSynergiesWithRun = synergies,
                isAntiSynergyDetected = isAnti
            )

            // Persist scan history
            try {
                repository.scanImage(bitmap, currentItems.map { it.name })
            } catch (_: Exception) {}

            _uiState.value = _uiState.value.copy(
                isScanning = false,
                latestScanResult = result
            )
        }
    }

    private fun createSyntheticConsoleBitmap(itemName: String, emoji: String): Bitmap {
        val bitmap = Bitmap.createBitmap(400, 400, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.rgb(20, 15, 25))

        val paint = Paint().apply {
            color = Color.WHITE
            textSize = 36f
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }

        canvas.drawText("Xbox Screen: $itemName", 200f, 100f, paint)
        paint.textSize = 64f
        canvas.drawText(emoji, 200f, 220f, paint)
        return bitmap
    }

    fun dismissScanResult() {
        _uiState.value = _uiState.value.copy(latestScanResult = null)
    }

    fun addItemToRun(item: IsaacItem) {
        val current = _uiState.value.currentRunItems.toMutableList()
        if (current.none { it.id == item.id }) {
            current.add(item)
            _uiState.value = _uiState.value.copy(
                currentRunItems = current,
                latestScanResult = null
            )
            recalculateRunStats()
        }
    }

    fun removeItemFromRun(item: IsaacItem) {
        val current = _uiState.value.currentRunItems.toMutableList()
        current.removeAll { it.id == item.id }
        _uiState.value = _uiState.value.copy(currentRunItems = current)
        recalculateRunStats()
    }

    fun clearRun() {
        _uiState.value = _uiState.value.copy(currentRunItems = emptyList())
        recalculateRunStats()
    }

    private fun recalculateRunStats() {
        val current = _uiState.value.currentRunItems
        val allSynergies = mutableListOf<ActiveRunSynergy>()

        // Check pairwise synergies for all items in run
        for (i in current.indices) {
            for (j in (i + 1) until current.size) {
                val itemA = current[i]
                val itemB = current[j]
                val synA = itemA.synergies.find { it.partnerItemName.equals(itemB.name, ignoreCase = true) }
                if (synA != null) {
                    allSynergies.add(
                        ActiveRunSynergy(
                            itemA = itemA.name,
                            itemB = itemB.name,
                            rating = synA.rating,
                            title = synA.title,
                            description = synA.description
                        )
                    )
                } else {
                    val synB = itemB.synergies.find { it.partnerItemName.equals(itemA.name, ignoreCase = true) }
                    if (synB != null) {
                        allSynergies.add(
                            ActiveRunSynergy(
                                itemA = itemB.name,
                                itemB = itemA.name,
                                rating = synB.rating,
                                title = synB.title,
                                description = synB.description
                            )
                        )
                    }
                }
            }
        }

        (activeRunSynergies as MutableStateFlow).value = allSynergies
        (activeTransformations as MutableStateFlow).value = IsaacItemDatabase.calculateTransformations(current)
    }

    fun selectDetailItem(item: IsaacItem?) {
        _uiState.value = _uiState.value.copy(selectedDetailItem = item)
    }

    fun setTestCandidateItem(item: IsaacItem?) {
        _uiState.value = _uiState.value.copy(testCandidateItem = item)
    }

    fun updateCompendiumFilters(
        query: String = _uiState.value.compendiumQuery,
        quality: Int? = _uiState.value.compendiumQualityFilter,
        pool: String? = _uiState.value.compendiumPoolFilter,
        trans: String? = _uiState.value.compendiumTransformationFilter
    ) {
        _uiState.value = _uiState.value.copy(
            compendiumQuery = query,
            compendiumQualityFilter = quality,
            compendiumPoolFilter = pool,
            compendiumTransformationFilter = trans
        )
    }

    fun getFilteredCompendiumItems(): List<IsaacItem> {
        val s = _uiState.value
        return repository.searchItems(
            query = s.compendiumQuery,
            qualityFilter = s.compendiumQualityFilter,
            poolFilter = s.compendiumPoolFilter,
            transformationFilter = s.compendiumTransformationFilter
        )
    }

    fun saveCurrentRun(title: String, character: String, notes: String, isWin: Boolean = false) {
        viewModelScope.launch {
            val items = _uiState.value.currentRunItems
            val synergies = activeRunSynergies.value
            val entity = RunEntity(
                runTitle = if (title.isNotBlank()) title else "Xbox $character Run",
                character = if (character.isNotBlank()) character else "Isaac",
                itemsCsv = items.joinToString(",") { it.name },
                synergiesCount = synergies.size,
                winStatus = isWin,
                notes = notes
            )
            repository.saveRun(entity)
        }
    }

    fun deleteRun(id: Long) {
        viewModelScope.launch {
            repository.deleteRun(id)
        }
    }

    fun loadRun(run: RunEntity) {
        val names = run.itemsCsv.split(",").map { it.trim() }
        val items = names.mapNotNull { IsaacItemDatabase.findItemByName(it) }
        _uiState.value = _uiState.value.copy(currentRunItems = items)
        recalculateRunStats()
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }
}
