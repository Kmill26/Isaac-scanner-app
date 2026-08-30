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
    fun `catalog loads the full bundled collectible list`() {
        assertEquals(721, IsaacItemDatabase.items.size)
        assertTrue(IsaacItemDatabase.items.all { it.name.isNotBlank() })
        assertTrue(IsaacItemDatabase.items.all { it.quality in 0..4 })
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
}
