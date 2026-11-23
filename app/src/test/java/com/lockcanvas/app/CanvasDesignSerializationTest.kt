package com.lockcanvas.app

import com.lockcanvas.app.data.LockCanvasJson
import com.lockcanvas.app.model.AnimationStyle
import com.lockcanvas.app.model.BatteryGaugeElement
import com.lockcanvas.app.model.BatteryGaugeStyle
import com.lockcanvas.app.model.CanvasDesign
import com.lockcanvas.app.model.ConditionalRule
import com.lockcanvas.app.model.ImageElement
import com.lockcanvas.app.model.PresetDesigns
import com.lockcanvas.app.model.RuleAction
import com.lockcanvas.app.model.RuleCondition
import com.lockcanvas.app.model.ShapeElement
import com.lockcanvas.app.model.ShapeType
import com.lockcanvas.app.model.TextElement
import kotlinx.serialization.encodeToString
import org.junit.Assert.assertEquals
import org.junit.Test

class CanvasDesignSerializationTest {

    @Test
    fun roundTripAllElementAndRuleTypes() {
        val original = CanvasDesign(
            id = "design_full_test",
            name = "Full Round-Trip",
            backgroundColorArgb = 0xFF101828,
            secondaryBackgroundColorArgb = 0xFF1D2939,
            backgroundGradientEnabled = true,
            animationStyle = AnimationStyle.FLOATING_PARTICLES,
            elements = listOf(
                TextElement(
                    id = "el_text",
                    xFraction = 0.5f,
                    yFraction = 0.45f,
                    text = "{time} {battery}",
                    colorArgb = 0xFFFFFFFF,
                    fontSizeSp = 28f,
                    bold = true,
                    dynamicTemplate = true,
                ),
                BatteryGaugeElement(
                    id = "el_gauge",
                    xFraction = 0.5f,
                    yFraction = 0.55f,
                    style = BatteryGaugeStyle.CIRCULAR,
                    widthFraction = 0.4f,
                    heightFraction = 0.2f,
                    activeColorArgb = 0xFF00F5D4,
                    trackColorArgb = 0x33FFFFFF,
                    textColorArgb = 0xFFFFFFFF,
                    showLabel = true,
                ),
                ShapeElement(
                    id = "el_shape",
                    xFraction = 0.5f,
                    yFraction = 0.5f,
                    shapeType = ShapeType.PILL,
                    widthFraction = 0.7f,
                    heightFraction = 0.1f,
                    fillColorArgb = 0x22FFFFFF,
                    strokeColorArgb = 0x66FFFFFF,
                    strokeWidthDp = 2f,
                    cornerRadiusDp = 24f,
                ),
                ImageElement(
                    id = "el_image",
                    xFraction = 0.5f,
                    yFraction = 0.65f,
                    uriString = "content://media/picker/0/photo/1",
                    widthFraction = 0.3f,
                    heightFraction = 0.18f,
                    cornerRadiusDp = 16f,
                ),
            ),
            rules = listOf(
                ConditionalRule(
                    id = "r1",
                    name = "Low Battery",
                    enabled = true,
                    condition = RuleCondition.BatteryBelow(20),
                    action = RuleAction.OverrideBackground(0xFF330000),
                ),
                ConditionalRule(
                    id = "r2",
                    name = "Charging",
                    enabled = true,
                    condition = RuleCondition.IsCharging,
                    action = RuleAction.OverrideElementText("el_text", "CHARGING"),
                ),
                ConditionalRule(
                    id = "r3",
                    name = "Night",
                    enabled = false,
                    condition = RuleCondition.NightTime(startHour = 22, endHour = 5),
                    action = RuleAction.ToggleElementVisibility("el_image", false),
                ),
            ),
            updatedAtEpochMs = 1_750_000_000_000L,
        )

        val encoded = LockCanvasJson.encodeToString(original)
        val decoded = LockCanvasJson.decodeFromString<CanvasDesign>(encoded)

        assertEquals(original, decoded)
    }

    @Test
    fun roundTripAllPresets() {
        val presets = PresetDesigns.all()
        val encoded = LockCanvasJson.encodeToString(presets)
        val decoded = LockCanvasJson.decodeFromString<List<CanvasDesign>>(encoded)
        assertEquals(presets, decoded)
    }
}
