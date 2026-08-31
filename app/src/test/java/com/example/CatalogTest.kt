package com.example

import com.example.data.model.IsaacItem
import com.example.data.model.IsaacItemDatabase
import com.example.data.model.ItemType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], application = IsaacApp::class)
class CatalogTest {

    @Test
    fun `catalog loads the full bundled collectible + trinket list`() {
        // 721 collectibles + 188 trinkets, all resolvable by name.
        assertEquals(909, IsaacItemDatabase.items.size)
        assertTrue(IsaacItemDatabase.items.all { it.name.isNotBlank() })
        val collectibles = IsaacItemDatabase.items.filter { it.itemType != ItemType.TRINKET }
        assertEquals(721, collectibles.size)
        assertTrue(collectibles.all { it.quality in 0..4 })
        // A trinket resolves by name and carries no tier.
        val trinket = IsaacItemDatabase.findItemByName("Swallowed Penny")
        assertNotNull(trinket)
        assertEquals(ItemType.TRINKET, trinket!!.itemType)
        assertEquals(-1, trinket.quality)
    }

    @Test
    fun `active items get a room-based recharge string`() {
        val d6 = IsaacItemDatabase.findItemByName("D6")
        assertNotNull(d6)
        assertEquals(ItemType.ACTIVE, d6!!.itemType)
        assertNotNull(d6.recharge)
    }

    @Test
    fun `curated synergies are overlaid onto the catalog entry`() {
        val brimstone = IsaacItemDatabase.findItemByName("Brimstone")
        assertNotNull(brimstone)
        assertTrue(brimstone!!.synergies.any { it.partnerItemName.equals("Soy Milk", ignoreCase = true) })
    }

    @Test
    fun `noisy names still resolve, gibberish does not`() {
        assertEquals("Brimstone", IsaacItemDatabase.findItemByName("  the BRIMSTONE ")?.name)
        assertEquals("Brimstone", IsaacItemDatabase.findItemByName("brimstome")?.name) // OCR typo
        assertEquals("Mom's Knife", IsaacItemDatabase.findItemByName("moms knife")?.name)
        assertNull(IsaacItemDatabase.findItemByName("zzzqqq not a real item"))
        assertNull(IsaacItemDatabase.findItemByName(""))
    }

    @Test
    fun `match reports exactness and score`() {
        val exact = IsaacItemDatabase.match("Brimstone")
        assertNotNull(exact)
        assertTrue(exact!!.exact)
        assertEquals(1f, exact.score, 0.0001f)

        val fuzzy = IsaacItemDatabase.match("brimstome")
        assertNotNull(fuzzy)
        assertTrue(!fuzzy!!.exact && fuzzy.score < 1f)
    }

    @Test
    fun `match survives realistic OCR noise on the item banner`() {
        // All-caps banner text (Isaac renders pickup names in caps).
        assertEquals("Brimstone", IsaacItemDatabase.match("BRIMSTONE")?.item?.name)
        // Single-character OCR substitution (O read as Q).
        assertEquals("Brimstone", IsaacItemDatabase.match("BRIMSTQNE")?.item?.name)
        // Leading article + stray whitespace from a two-line banner crop.
        assertEquals("Brimstone", IsaacItemDatabase.match("  the   Brimstone ")?.item?.name)
        // Caps + a trailing garbage token still lands on Sacred Heart.
        assertEquals("Sacred Heart", IsaacItemDatabase.findItemByName("SACRED HEART L")?.name)
        // A short article-prefixed active resolves.
        assertEquals("The D6", IsaacItemDatabase.findItemByName("the d6")?.name)
        // Pure gibberish must not resolve to anything.
        assertNull(IsaacItemDatabase.match("xqzptlk vvv"))
    }

    @Test
    fun `match scores exact above fuzzy`() {
        val exact = IsaacItemDatabase.match("Ipecac")
        val fuzzy = IsaacItemDatabase.match("ipeccac")
        assertNotNull(exact)
        assertNotNull(fuzzy)
        assertTrue(exact!!.exact)
        assertTrue(!fuzzy!!.exact)
        assertTrue(exact.score >= fuzzy.score)
    }

    @Test
    fun `transformations derive counts from catalog data`() {
        val guppyItems: List<IsaacItem> = IsaacItemDatabase.items
            .filter { it.transformations.contains("Guppy") }
            .take(2)
        val progress = IsaacItemDatabase.calculateTransformations(guppyItems)
        val guppy = progress.firstOrNull { it.name == "Guppy" }
        assertNotNull(guppy)
        assertEquals(2, guppy!!.currentCount)
        assertEquals(3, guppy.requiredCount)
    }

    @Test
    fun `xbox presets resolve against the catalog`() {
        val presets = IsaacItemDatabase.getXboxPresets()
        assertEquals(6, presets.size)
        assertTrue(presets.all { IsaacItemDatabase.findItemByName(it.itemName) != null })
    }

    @Test
    fun `tv screen HUD noise is rejected and does not match random items`() {
        assertNull(IsaacItemDatabase.findItemByName("KEYS 05 BOMBS 02"))
        assertNull(IsaacItemDatabase.findItemByName("HIGH SCORE 120"))
        assertNull(IsaacItemDatabase.findItemByName("PRESS START"))
        assertNull(IsaacItemDatabase.findItemByName("GAME OPTIONS"))
        assertNull(IsaacItemDatabase.findItemByName("STAGE 1 BASEMENT"))
        assertNull(IsaacItemDatabase.findItemByName("ROOM 04"))
        assertNull(IsaacItemDatabase.match("KEYS 05 BOMBS 02"))
    }

    @Test
    fun `edge-case item names and aliases resolve accurately`() {
        assertEquals("<3", IsaacItemDatabase.findItemByName("<3")?.name)
        assertEquals("<3", IsaacItemDatabase.findItemByName("less than three")?.name)
        assertEquals("1up!", IsaacItemDatabase.findItemByName("1up!")?.name)
        assertEquals("1up!", IsaacItemDatabase.findItemByName("1-up")?.name)
        assertEquals("20/20", IsaacItemDatabase.findItemByName("20/20")?.name)
        assertEquals("20/20", IsaacItemDatabase.findItemByName("20 20")?.name)
        assertEquals("C Section", IsaacItemDatabase.findItemByName("C-Section")?.name)
        assertEquals("C Section", IsaacItemDatabase.findItemByName("csection")?.name)
        assertEquals("The D6", IsaacItemDatabase.findItemByName("The D6")?.name)
        assertEquals("The D6", IsaacItemDatabase.findItemByName("D6")?.name)
        assertEquals("D100", IsaacItemDatabase.findItemByName("d100")?.name)
        assertEquals("Godhead", IsaacItemDatabase.findItemByName("godhead")?.name)
        assertEquals("Mom's Knife", IsaacItemDatabase.findItemByName("Mom's Knife")?.name)
        assertEquals("Mom's Knife", IsaacItemDatabase.findItemByName("moms knife")?.name)
    }
}
