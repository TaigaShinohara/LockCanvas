package com.lockcanvas.app

import com.lockcanvas.app.engine.RuleEvaluator
import com.lockcanvas.app.engine.TemplateFormatter
import com.lockcanvas.app.model.CanvasDesign
import com.lockcanvas.app.model.ConditionalRule
import com.lockcanvas.app.model.DynamicDataSnapshot
import com.lockcanvas.app.model.RuleAction
import com.lockcanvas.app.model.RuleCondition
import com.lockcanvas.app.model.TextElement
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RuleEvaluatorTest {

    @Test
    fun templateFormatterSubstitutesAllTokens() {
        val snapshot = DynamicDataSnapshot.sample(
            timeText = "21:15",
            dateText = "Mon, Nov 23",
            hourOfDay = 21,
            batteryPercent = 64,
            isCharging = true,
            chargingLabel = "Charging",
        )
        val result = TemplateFormatter.format(
            "{date} | {time} | {battery} ({charging_status})",
            snapshot,
        )
        assertEquals("Mon, Nov 23 | 21:15 | 64% (Charging)", result)
    }

    @Test
    fun batteryBelowRuleTriggersOnlyStrictlyBelowThreshold() {
        val design = CanvasDesign(
            id = "d1",
            name = "Test",
            backgroundColorArgb = 0xFF000000,
            backgroundGradientEnabled = true,
            rules = listOf(
                ConditionalRule(
                    id = "low_bg",
                    name = "Low Battery BG",
                    enabled = true,
                    condition = RuleCondition.BatteryBelow(20),
                    action = RuleAction.OverrideBackground(0xFF440000),
                ),
            ),
        )

        val atThreshold = RuleEvaluator.evaluate(design, DynamicDataSnapshot.sample(batteryPercent = 20))
        assertTrue(atThreshold.matchedRuleIds.isEmpty())
        assertEquals(0xFF000000, atThreshold.design.backgroundColorArgb)
        assertTrue(atThreshold.design.backgroundGradientEnabled)

        val belowThreshold = RuleEvaluator.evaluate(design, DynamicDataSnapshot.sample(batteryPercent = 19))
        assertEquals(listOf("low_bg"), belowThreshold.matchedRuleIds)
        assertEquals(0xFF440000, belowThreshold.design.backgroundColorArgb)
        assertFalse(belowThreshold.design.backgroundGradientEnabled)
    }

    @Test
    fun chargingRuleTogglesVisibilityAndOverridesText() {
        val design = CanvasDesign(
            id = "d2",
            name = "Charging Test",
            elements = listOf(
                TextElement(id = "t1", text = "Idle", visible = false),
            ),
            rules = listOf(
                ConditionalRule(
                    id = "show_on_charge",
                    name = "Show",
                    enabled = true,
                    condition = RuleCondition.IsCharging,
                    action = RuleAction.ToggleElementVisibility("t1", true),
                ),
                ConditionalRule(
                    id = "text_on_charge",
                    name = "Text",
                    enabled = true,
                    condition = RuleCondition.IsCharging,
                    action = RuleAction.OverrideElementText("t1", "Powering Up"),
                ),
            ),
        )

        val notCharging = RuleEvaluator.evaluate(design, DynamicDataSnapshot.sample(isCharging = false))
        val elNotCharging = notCharging.design.elements.first() as TextElement
        assertFalse(elNotCharging.visible)
        assertEquals("Idle", elNotCharging.text)

        val charging = RuleEvaluator.evaluate(design, DynamicDataSnapshot.sample(isCharging = true))
        val elCharging = charging.design.elements.first() as TextElement
        assertTrue(elCharging.visible)
        assertEquals("Powering Up", elCharging.text)
        assertEquals(listOf("show_on_charge", "text_on_charge"), charging.matchedRuleIds)
    }

    @Test
    fun nightTimeRuleHandlesMidnightWrapAround() {
        val cond = RuleCondition.NightTime(startHour = 20, endHour = 6)

        assertTrue(RuleEvaluator.matches(cond, DynamicDataSnapshot.sample(hourOfDay = 20)))
        assertTrue(RuleEvaluator.matches(cond, DynamicDataSnapshot.sample(hourOfDay = 23)))
        assertTrue(RuleEvaluator.matches(cond, DynamicDataSnapshot.sample(hourOfDay = 0)))
        assertTrue(RuleEvaluator.matches(cond, DynamicDataSnapshot.sample(hourOfDay = 5)))
        assertFalse(RuleEvaluator.matches(cond, DynamicDataSnapshot.sample(hourOfDay = 6)))
        assertFalse(RuleEvaluator.matches(cond, DynamicDataSnapshot.sample(hourOfDay = 14)))
    }

    @Test
    fun disabledRuleIsIgnored() {
        val design = CanvasDesign(
            id = "d3",
            name = "Disabled",
            backgroundColorArgb = 0xFF111111,
            rules = listOf(
                ConditionalRule(
                    id = "disabled_rule",
                    name = "Disabled",
                    enabled = false,
                    condition = RuleCondition.IsCharging,
                    action = RuleAction.OverrideBackground(0xFF999999),
                ),
            ),
        )

        val result = RuleEvaluator.evaluate(design, DynamicDataSnapshot.sample(isCharging = true))
        assertTrue(result.matchedRuleIds.isEmpty())
        assertEquals(0xFF111111, result.design.backgroundColorArgb)
    }
}
