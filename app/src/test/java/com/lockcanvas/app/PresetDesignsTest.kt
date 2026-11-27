package com.lockcanvas.app

import com.lockcanvas.app.model.PresetDesigns
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PresetDesignsTest {

    @Test
    fun presetsAreValidAndPlacedInSafeLockScreenBand() {
        val presets = PresetDesigns.all()
        assertEquals(7, presets.size)

        val ids = presets.map { it.id }.toSet()
        assertEquals(7, ids.size)

        for (preset in presets) {
            assertTrue("Preset name should not be blank", preset.name.isNotBlank())
            assertTrue("Preset should have elements", preset.elements.isNotEmpty())
            val elementIds = preset.elements.map { it.id }.toSet()
            assertEquals("Element IDs must be unique in ${preset.name}", preset.elements.size, elementIds.size)

            for (element in preset.elements) {
                assertTrue(
                    "xFraction out of bounds for ${element.id}",
                    element.xFraction in 0.05f..0.95f,
                )
                assertTrue(
                    "yFraction should stay inside lock-screen safe band for ${element.id} (was ${element.yFraction})",
                    element.yFraction in 0.28f..0.80f,
                )
            }
        }
    }
}
