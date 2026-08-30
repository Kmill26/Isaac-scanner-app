package com.example.data.model

import android.content.Context
import com.example.data.catalog.CatalogLoader
import com.example.data.catalog.NameMatch
import com.example.data.catalog.NameMatcher

/**
 * Public façade over the bundled 721-item Isaac catalog (`assets/isaac_items.json`).
 *
 * Function signatures are unchanged from the previous hardcoded version so existing callers
 * (ScannerViewModel, GeminiVisionService, IsaacRepository, the screens) keep working.
 *
 * [items] is parsed lazily off the caller's thread. [com.example.IsaacApp] calls [warmUp] on a
 * background thread at process start; the `by lazy` guard means any earlier access simply blocks
 * on that same parse instead of racing it.
 */
object IsaacItemDatabase {

    @Volatile
    private var appContext: Context? = null

    /** Called from [com.example.IsaacApp.onCreate]. */
    fun install(context: Context) {
        appContext = context.applicationContext
    }

    /** Touch [items] from a background thread so the first UI access is warm. */
    fun warmUp() {
        items
    }

    val items: List<IsaacItem> by lazy {
        val ctx = appContext
            ?: error("IsaacItemDatabase.install(context) was not called — is IsaacApp registered in the manifest?")
        CatalogLoader.load(ctx).items
    }

    private val byId: Map<Int, IsaacItem> by lazy { items.associateBy { it.id } }

    /** Resolve a catalog id back to its item (used to restore a persisted run). */
    fun itemById(id: Int): IsaacItem? = byId[id]

    private val byNormalizedName: Map<String, IsaacItem> by lazy {
        // A handful of names collide after normalization (e.g. the two Broken Shovel halves,
        // Damocles active vs passive). Keep the first catalog entry for a stable lookup.
        val map = LinkedHashMap<String, IsaacItem>(items.size * 2)
        for (item in items) map.putIfAbsent(NameMatcher.normalize(item.name), item)
        map
    }

    // ---------------------------------------------------------------------------------------------
    // Name matching
    // ---------------------------------------------------------------------------------------------

    /**
     * Resolve a noisy OCR / free-text name to a catalog item, or `null` if nothing clears the bar.
     * Never falls back to an arbitrary item.
     */
    fun findItemByName(query: String): IsaacItem? {
        val cleaned = query.trim()
        if (cleaned.isEmpty()) return null
        byNormalizedName[NameMatcher.normalize(cleaned)]?.let { return it }
        return NameMatcher.match(cleaned, items)?.item
    }

    /** Ranked match with score, for the scan engine. */
    fun match(query: String): NameMatch? {
        val cleaned = query.trim()
        if (cleaned.isEmpty()) return null
        byNormalizedName[NameMatcher.normalize(cleaned)]?.let { return NameMatch(it, 1f, exact = true) }
        return NameMatcher.match(cleaned, items)
    }

    // ---------------------------------------------------------------------------------------------
    // Synergies
    // ---------------------------------------------------------------------------------------------

    fun calculateSynergies(candidate: IsaacItem, currentRun: List<IsaacItem>): List<ActiveRunSynergy> {
        val result = mutableListOf<ActiveRunSynergy>()

        for (item in currentRun) {
            val candidateSynergy = candidate.synergies.find {
                it.partnerItemName.equals(item.name, ignoreCase = true)
            }
            if (candidateSynergy != null) {
                result.add(
                    ActiveRunSynergy(
                        itemA = candidate.name,
                        itemB = item.name,
                        rating = candidateSynergy.rating,
                        title = candidateSynergy.title,
                        description = candidateSynergy.description
                    )
                )
            }

            val inventorySynergy = item.synergies.find {
                it.partnerItemName.equals(candidate.name, ignoreCase = true)
            }
            if (inventorySynergy != null &&
                result.none {
                    it.itemA == item.name && it.itemB == candidate.name ||
                        it.itemA == candidate.name && it.itemB == item.name
                }
            ) {
                result.add(
                    ActiveRunSynergy(
                        itemA = item.name,
                        itemB = candidate.name,
                        rating = inventorySynergy.rating,
                        title = inventorySynergy.title,
                        description = inventorySynergy.description
                    )
                )
            }
        }

        return result
    }

    // ---------------------------------------------------------------------------------------------
    // Transformations — list + per-transformation required counts derived from the catalog itself
    // ---------------------------------------------------------------------------------------------

    private data class TransformationMeta(val required: Int, val reward: String, val emoji: String)

    private val transformationMeta: Map<String, TransformationMeta> by lazy {
        val counts = mutableMapOf<String, Int>()
        for (item in items) {
            for (name in item.transformations) {
                counts[name] = (counts[name] ?: 0) + 1
            }
        }
        counts.mapValues { (name, count) ->
            val flavor = TRANSFORMATION_FLAVOR[name] ?: ("Transform into $name" to "✨")
            // Isaac transformations need 3 items; clamp to what the catalog can actually supply.
            TransformationMeta(
                required = minOf(3, count).coerceAtLeast(1),
                reward = flavor.first,
                emoji = flavor.second
            )
        }
    }

    fun calculateTransformations(currentRun: List<IsaacItem>): List<TransformationProgress> {
        return transformationMeta.map { (name, meta) ->
            val owned = currentRun.filter { it.transformations.contains(name) }.map { it.name }.distinct()
            TransformationProgress(
                name = name,
                currentCount = owned.size,
                requiredCount = meta.required,
                itemsOwned = owned,
                rewardEffect = meta.reward,
                emoji = meta.emoji
            )
        }.filter { it.currentCount > 0 }
    }

    private val TRANSFORMATION_FLAVOR: Map<String, Pair<String, String>> = mapOf(
        "Guppy" to ("Transform into Guppy: grants flight and spawns blue flies on every tear hit!" to "🐱"),
        "Seraphim" to ("Transform into Angel: grants flight and +3 Soul Hearts!" to "👼"),
        "Leviathan" to ("Transform into Demon: grants flight, +2 Black Hearts and +0.5 Damage!" to "😈"),
        "Conjoined" to ("Transform into Conjoined: two extra tear-firing heads for a wider spread!" to "👶"),
        "Fun Guy" to ("Transform into Fun Guy: grants +1 Red Heart Container!" to "🍄"),
        "Lord of the Flies" to ("Transform into Beelzebub: grants flight and turns enemy flies friendly!" to "🪰"),
        "Spun" to ("Transform into Spun: +2.0 Damage, +0.15 Speed and spawns random pills!" to "💉"),
        "Mom" to ("Transform into Mom: chance to stomp and stun all enemies on room entry!" to "👠"),
        "Bob" to ("Transform into Bob: leaves a trail of green poison creep!" to "🧪"),
        "Bookworm" to ("Transform into Bookworm: tears have a chance to fire an extra homing tear!" to "📖"),
        "Spider Baby" to ("Transform into Spider Baby: spawns a blue spider on taking damage!" to "🕷️"),
        "Oh Crap" to ("Transform into Oh Crap: leaves damaging brown creep behind you!" to "💩"),
        "Stompy" to ("Transform into Stompy: Isaac grows giant and tears gain knockback!" to "🦶"),
        "Necromancer" to ("Transform into Necromancer: chance to spawn a friendly attack fly on kill!" to "☠️"),
        "Super Bum" to ("Transform into Super Bum: collects nearby pickups and pays out big!" to "💰")
    )

    // ---------------------------------------------------------------------------------------------
    // Xbox presets (debug-gated in a later phase) — item names resolve against the catalog
    // ---------------------------------------------------------------------------------------------

    fun getXboxPresets(): List<XboxPresetScreen> = XBOX_PRESETS.map { preset ->
        val resolved = findItemByName(preset.itemName)
        preset.copy(
            itemName = resolved?.name ?: preset.itemName,
            iconEmoji = resolved?.iconEmoji ?: preset.iconEmoji
        )
    }

    private val XBOX_PRESETS: List<XboxPresetScreen> = listOf(
        XboxPresetScreen(
            title = "Xbox Devil Deal Pedestal",
            roomType = "Devil Room Offering",
            itemName = "Brimstone",
            consolePrompt = "Pedestal showing glowing black horns and blood sigil on Xbox TV screen.",
            iconEmoji = "🩸",
            hint = "Quality 4 • Blood Laser Beam"
        ),
        XboxPresetScreen(
            title = "Xbox Angel Room Pedestal",
            roomType = "Angel Room Statue Reward",
            itemName = "Sacred Heart",
            consolePrompt = "Pedestal showing glowing bleeding winged heart with divine halo on TV.",
            iconEmoji = "❤️",
            hint = "Quality 4 • x2.3 Homing Tears"
        ),
        XboxPresetScreen(
            title = "Xbox Treasure Room Pedestal",
            roomType = "Treasure Room Item",
            itemName = "C Section",
            consolePrompt = "Pedestal showing umbilical baby fetus icon in glass on console display.",
            iconEmoji = "👶",
            hint = "Quality 4 • Piercing Ghost Fetuses"
        ),
        XboxPresetScreen(
            title = "Xbox Secret Room Pedestal",
            roomType = "Secret Room Discovery",
            itemName = "Rock Bottom",
            consolePrompt = "Pedestal showing gray jagged bottom rock with upward arrow on Xbox screen.",
            iconEmoji = "🪨",
            hint = "Quality 4 • Stat Decreases Locked"
        ),
        XboxPresetScreen(
            title = "Xbox Boss Rush Pedestal",
            roomType = "Boss Rush Choice",
            itemName = "Soy Milk",
            consolePrompt = "Pedestal showing white carton of soy milk with tears aura on TV.",
            iconEmoji = "🥛",
            hint = "Quality 2 • x5.5 Fire Rate"
        ),
        XboxPresetScreen(
            title = "Xbox Item Room Pedestal",
            roomType = "Treasure Room Item",
            itemName = "Ipecac",
            consolePrompt = "Pedestal showing small green bottle of explosive vomit medicine on Xbox.",
            iconEmoji = "🤢",
            hint = "Quality 4 • +40 Explosive Spit"
        )
    )
}
