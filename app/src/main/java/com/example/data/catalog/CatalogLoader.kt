package com.example.data.catalog

import android.content.Context
import com.example.data.model.IsaacItem
import com.example.data.model.ItemType
import com.example.data.model.SynergyInfo
import com.squareup.moshi.Moshi

/**
 * Parses `assets/isaac_items.json` and maps it onto the app's [IsaacItem] model.
 *
 * The file is ~436 KB; a full parse + map is ~50-80 ms on a mid device. Callers should
 * run this off the main thread (see [com.example.IsaacApp]); [IsaacItemDatabase] warms it
 * on a background thread at startup and any stray main-thread access blocks on that.
 */
object CatalogLoader {

    private const val ASSET_NAME = "isaac_items.json"

    data class Catalog(
        val items: List<IsaacItem>,
        val version: String
    )

    fun load(context: Context): Catalog {
        val moshi = Moshi.Builder().build()
        val adapter = moshi.adapter(CatalogDto::class.java)
        val json = context.applicationContext.assets.open(ASSET_NAME)
            .bufferedReader(Charsets.UTF_8)
            .use { it.readText() }
        val dto = adapter.fromJson(json) ?: error("Could not parse $ASSET_NAME")

        val items = dto.collectibles.map { it.toIsaacItem(CuratedSynergies.byName[it.name.trim().lowercase()]) }
        return Catalog(items = items, version = dto.version)
    }

    private fun CollectibleDto.toIsaacItem(curated: List<SynergyInfo>?): IsaacItem {
        val resolvedType = when (type.trim().lowercase()) {
            "active" -> ItemType.ACTIVE
            "familiar" -> ItemType.FAMILIAR
            else -> ItemType.PASSIVE
        }
        val resolvedRecharge = rechargeTime?.let { if (it <= 0) "Single Use" else "$it Rooms" }
        val emoji = when (resolvedType) {
            ItemType.ACTIVE -> "✨"
            ItemType.FAMILIAR -> "🐣"
            ItemType.TRINKET -> "🪬"
            ItemType.PASSIVE -> "🔮"
        }
        return IsaacItem(
            id = id,
            name = name,
            quote = quote?.trim().orEmpty(),
            description = description,
            quality = (quality ?: 0).coerceIn(0, 4),
            itemType = resolvedType,
            recharge = resolvedRecharge,
            itemPools = itemPools,
            transformations = transformations,
            stats = emptyMap(),
            iconEmoji = emoji,
            synergies = curated ?: emptyList(),
            dlc = dlc
        )
    }
}
