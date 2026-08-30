package com.example.data.model

enum class ItemType(val label: String) {
    PASSIVE("Passive Item"),
    ACTIVE("Active Item"),
    FAMILIAR("Familiar"),
    TRINKET("Trinket")
}

/** Where a [ScanDetectionResult] / scan outcome came from. */
enum class ScanSource {
    /** On-device ML Kit OCR matched against the bundled catalog. Fully offline. */
    OCR,

    /** Gemini vision identified the item (primary path when an API key is configured). */
    GEMINI,

    /** Gemini produced an on-demand "should I take this?" verdict for an already-matched item. */
    GEMINI_VERDICT
}

enum class SynergyRating(val title: String, val emoji: String) {
    GOD_TIER("God Tier Synergy", "🔥"),
    EXCELLENT("Excellent Synergy", "✨"),
    GOOD("Good Synergy", "👍"),
    SITUATIONAL("Situational", "⚖️"),
    ANTI_SYNERGY("Anti-Synergy Warning", "⚠️")
}

data class SynergyInfo(
    val partnerItemName: String,
    val rating: SynergyRating,
    val title: String,
    val description: String
)

data class IsaacItem(
    val id: Int,
    val name: String,
    val quote: String,
    val description: String,
    val quality: Int, // 0 to 4 for collectibles; -1 for trinkets (no tier)
    val itemType: ItemType,
    val recharge: String? = null,
    val itemPools: List<String> = emptyList(),
    val transformations: List<String> = emptyList(),
    val stats: Map<String, String> = emptyMap(),
    val iconEmoji: String = "📦",
    val synergies: List<SynergyInfo> = emptyList(),
    val dlc: String = "Repentance"
)

data class ActiveRunSynergy(
    val itemA: String,
    val itemB: String,
    val rating: SynergyRating,
    val title: String,
    val description: String
)

data class TransformationProgress(
    val name: String,
    val currentCount: Int,
    val requiredCount: Int = 3,
    val itemsOwned: List<String>,
    val rewardEffect: String,
    val emoji: String
)

data class ScanDetectionResult(
    val detectedName: String,
    val confidence: Float,
    val rawGeminiVerdict: String,
    val matchedItem: IsaacItem?,
    val activeSynergiesWithRun: List<ActiveRunSynergy>,
    val isAntiSynergyDetected: Boolean,
    val source: ScanSource = ScanSource.OCR,
    val scanTimestamp: Long = System.currentTimeMillis()
)

data class XboxPresetScreen(
    val title: String,
    val roomType: String,
    val itemName: String,
    val consolePrompt: String,
    val iconEmoji: String,
    val hint: String
)
