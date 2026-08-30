package com.example.data.catalog

import com.squareup.moshi.JsonClass

/**
 * Moshi DTOs for the bundled `assets/isaac_items.json` catalog
 * (721 collectibles, 188 trinkets). Codegen adapters via KSP.
 */
@JsonClass(generateAdapter = true)
data class CatalogDto(
    val version: String = "",
    val generatedAt: String = "",
    val collectibles: List<CollectibleDto> = emptyList(),
    val trinkets: List<TrinketDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class CollectibleDto(
    val id: Int,
    val name: String,
    val quality: Int? = null,
    val type: String = "passive",
    val rechargeTime: Int? = null,
    val itemPools: List<String> = emptyList(),
    val description: String = "",
    val quote: String? = null,
    val transformations: List<String> = emptyList(),
    val dlc: String = "Repentance",
    val tags: List<String> = emptyList()
)

@JsonClass(generateAdapter = true)
data class TrinketDto(
    val id: Int,
    val name: String,
    val description: String = "",
    val quote: String? = null
)
