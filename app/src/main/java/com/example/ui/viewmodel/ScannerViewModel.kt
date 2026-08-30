package com.example.ui.viewmodel

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.db.RunEntity
import com.example.data.db.ScanEntity
import com.example.data.model.ActiveRunSynergy
import com.example.data.model.IsaacItem
import com.example.data.model.IsaacItemDatabase
import com.example.data.model.ScanDetectionResult
import com.example.data.model.ScanSource
import com.example.data.model.SynergyRating
import com.example.data.model.TransformationProgress
import com.example.data.model.XboxPresetScreen
import com.example.data.prefs.RunStore
import com.example.data.repository.IsaacRepository
import com.example.data.scan.ScanException
import com.example.data.scan.ScanOutcome
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ScannerUiState(
    val isScanning: Boolean = false,
    val latestScanResult: ScanDetectionResult? = null,
    val scanErrorMessage: String? = null,
    /** True while an on-demand Gemini "should I take this?" verdict is being fetched. */
    val isLoadingVerdict: Boolean = false,
    /** Whether a real Gemini key is configured. Drives visibility of every AI affordance. */
    val aiAvailable: Boolean = false,
    val currentRunItems: List<IsaacItem> = emptyList(),
    /**
     * On a cold start with a persisted, non-empty current run this is that run's size. The UI
     * shows a "Resume run (N) / Start fresh" banner; it is cleared once the user picks either way.
     */
    val resumableRunCount: Int = 0,
    val compendiumQuery: String = "",
    val compendiumQualityFilter: Int? = null,
    val compendiumPoolFilter: String? = null,
    val compendiumTransformationFilter: String? = null
)

class ScannerViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: IsaacRepository by lazy {
        val db = AppDatabase.getInstance(application)
        IsaacRepository(db)
    }

    private val runStore = RunStore(application)

    /** Items from a persisted run awaiting the user's Resume/Start-fresh choice. */
    private var pendingResumeItems: List<IsaacItem> = emptyList()

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
        _uiState.value = _uiState.value.copy(aiAvailable = repository.geminiConfigured)
        recalculateRunStats()
        restorePersistedRun()
    }

    /** Read the persisted current run once at startup and offer it as a resumable banner. */
    private fun restorePersistedRun() {
        viewModelScope.launch {
            val ids = runCatching { runStore.currentRunItemIds.first() }.getOrDefault(emptyList())
            if (ids.isEmpty()) return@launch
            val items = ids.mapNotNull { IsaacItemDatabase.itemById(it) }
            if (items.isEmpty()) {
                runStore.save(emptyList())
                return@launch
            }
            pendingResumeItems = items
            _uiState.value = _uiState.value.copy(resumableRunCount = items.size)
        }
    }

    /** User tapped "Resume run" — adopt the persisted items as the active run. */
    fun resumePersistedRun() {
        if (pendingResumeItems.isEmpty()) {
            _uiState.value = _uiState.value.copy(resumableRunCount = 0)
            return
        }
        _uiState.value = _uiState.value.copy(
            currentRunItems = pendingResumeItems,
            resumableRunCount = 0
        )
        pendingResumeItems = emptyList()
        recalculateRunStats()
        persistCurrentRun()
    }

    /** User tapped "Start fresh" — drop the persisted run. */
    fun discardPersistedRun() {
        pendingResumeItems = emptyList()
        _uiState.value = _uiState.value.copy(resumableRunCount = 0)
        viewModelScope.launch { runStore.save(emptyList()) }
    }

    private fun persistCurrentRun() {
        val ids = _uiState.value.currentRunItems.map { it.id }
        viewModelScope.launch { runStore.save(ids) }
    }

    /** Surface a camera-capture failure (raised from the viewfinder before a scan starts). */
    fun reportScanError(message: String) {
        _uiState.value = _uiState.value.copy(isScanning = false, scanErrorMessage = message)
    }

    /** Alias used by the camera `onError` callback wiring. */
    fun onCaptureError(message: String) = reportScanError(message)

    fun dismissScanError() {
        _uiState.value = _uiState.value.copy(scanErrorMessage = null)
    }

    // ---------------------------------------------------------------------------------------------
    // Scan flow
    // ---------------------------------------------------------------------------------------------

    fun scanBitmap(bitmap: Bitmap) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isScanning = true,
                scanErrorMessage = null,
                latestScanResult = null
            )

            val outcome = try {
                val runNames = _uiState.value.currentRunItems.map { it.name }
                repository.identify(bitmap, runNames)
            } catch (t: Throwable) {
                _uiState.value = _uiState.value.copy(
                    isScanning = false,
                    latestScanResult = null,
                    scanErrorMessage = "Scan failed: ${t.localizedMessage ?: "unexpected error"}"
                )
                return@launch
            }

            _uiState.value = when (outcome) {
                is ScanOutcome.Identified -> _uiState.value.copy(
                    isScanning = false,
                    scanErrorMessage = null,
                    latestScanResult = outcome.toResult()
                )

                is ScanOutcome.Unrecognized -> _uiState.value.copy(
                    isScanning = false,
                    latestScanResult = null,
                    scanErrorMessage = "Read \"${outcome.rawText.take(60).trim()}\" but it's not a known " +
                        "item — line the box up with just the item-name banner and rescan."
                )

                is ScanOutcome.NeedCloserLook -> _uiState.value.copy(
                    isScanning = false,
                    latestScanResult = null,
                    scanErrorMessage = outcome.message
                )

                is ScanOutcome.Failed -> _uiState.value.copy(
                    isScanning = false,
                    latestScanResult = null,
                    scanErrorMessage = messageFor(outcome.error)
                )
            }
        }
    }

    private fun messageFor(error: ScanException): String = when (error) {
        is ScanException.NoApiKey ->
            "Couldn't read the name on screen. Add a Gemini API key (see README) to enable AI " +
                "recognition of item art."
        else -> error.message
    }

    private fun ScanOutcome.Identified.toResult(): ScanDetectionResult = ScanDetectionResult(
        detectedName = item.name,
        confidence = confidence,
        rawGeminiVerdict = verdict ?: item.description,
        matchedItem = item,
        activeSynergiesWithRun = activeSynergiesWithRun,
        isAntiSynergyDetected = isAntiSynergyDetected,
        source = source
    )

    /**
     * On-demand run-aware "should I take this?" verdict for the currently shown result. No-op
     * unless a real Gemini key is configured and the result has a matched catalog item. Failures
     * surface as [ScannerUiState.scanErrorMessage] and never wipe the result card.
     */
    fun requestAiVerdict() {
        val result = _uiState.value.latestScanResult ?: return
        val item = result.matchedItem ?: return
        if (!_uiState.value.aiAvailable || _uiState.value.isLoadingVerdict) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingVerdict = true)
            try {
                val runNames = _uiState.value.currentRunItems.map { it.name }
                val verdict = repository.verdictFor(item.name, runNames)
                val current = _uiState.value.latestScanResult
                if (current?.matchedItem?.id == item.id) {
                    _uiState.value = _uiState.value.copy(
                        isLoadingVerdict = false,
                        latestScanResult = current.copy(
                            rawGeminiVerdict = verdict,
                            source = ScanSource.GEMINI_VERDICT
                        )
                    )
                } else {
                    _uiState.value = _uiState.value.copy(isLoadingVerdict = false)
                }
            } catch (e: ScanException) {
                _uiState.value = _uiState.value.copy(
                    isLoadingVerdict = false,
                    scanErrorMessage = e.message
                )
            } catch (t: Throwable) {
                _uiState.value = _uiState.value.copy(
                    isLoadingVerdict = false,
                    scanErrorMessage = "AI verdict failed: ${t.localizedMessage ?: "unexpected error"}"
                )
            }
        }
    }

    fun scanXboxPreset(preset: XboxPresetScreen) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isScanning = true, scanErrorMessage = null)

            val matched = IsaacItemDatabase.findItemByName(preset.itemName)
            if (matched == null) {
                // Preset item isn't in the catalog — skip it rather than fabricate a result.
                _uiState.value = _uiState.value.copy(
                    isScanning = false,
                    scanErrorMessage = "Preset \"${preset.itemName}\" isn't in the item catalog."
                )
                return@launch
            }

            val currentItems = _uiState.value.currentRunItems
            val synergies = IsaacItemDatabase.calculateSynergies(matched, currentItems)
            val isAnti = synergies.any { it.rating == SynergyRating.ANTI_SYNERGY }

            val result = ScanDetectionResult(
                detectedName = matched.name,
                confidence = 0.98f,
                rawGeminiVerdict = "Xbox ${preset.roomType}: ${matched.name}. ${matched.description}",
                matchedItem = matched,
                activeSynergiesWithRun = synergies,
                isAntiSynergyDetected = isAnti,
                source = ScanSource.OCR
            )

            _uiState.value = _uiState.value.copy(
                isScanning = false,
                latestScanResult = result
            )
        }
    }

    fun dismissScanResult() {
        _uiState.value = _uiState.value.copy(latestScanResult = null)
    }

    /** Dismiss whatever the scanner is showing (result or error) so the shutter is ready again. */
    fun rescan() {
        _uiState.value = _uiState.value.copy(
            latestScanResult = null,
            scanErrorMessage = null,
            isScanning = false
        )
    }

    // ---------------------------------------------------------------------------------------------
    // Run inventory
    // ---------------------------------------------------------------------------------------------

    fun addItemToRun(item: IsaacItem) {
        val current = _uiState.value.currentRunItems.toMutableList()
        if (current.none { it.id == item.id }) {
            current.add(item)
            _uiState.value = _uiState.value.copy(
                currentRunItems = current,
                latestScanResult = null
            )
            recalculateRunStats()
            persistCurrentRun()
        }
    }

    fun removeItemFromRun(item: IsaacItem) {
        val current = _uiState.value.currentRunItems.toMutableList()
        current.removeAll { it.id == item.id }
        _uiState.value = _uiState.value.copy(currentRunItems = current)
        recalculateRunStats()
        persistCurrentRun()
    }

    fun clearRun() {
        _uiState.value = _uiState.value.copy(currentRunItems = emptyList())
        recalculateRunStats()
        persistCurrentRun()
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
        _uiState.value = _uiState.value.copy(
            currentRunItems = items,
            resumableRunCount = 0
        )
        pendingResumeItems = emptyList()
        recalculateRunStats()
        persistCurrentRun()
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }
}
