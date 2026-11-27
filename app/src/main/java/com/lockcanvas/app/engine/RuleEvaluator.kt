package com.lockcanvas.app.engine

import com.lockcanvas.app.model.CanvasDesign
import com.lockcanvas.app.model.ConditionalRule
import com.lockcanvas.app.model.DynamicDataSnapshot
import com.lockcanvas.app.model.RuleAction
import com.lockcanvas.app.model.RuleCondition
import com.lockcanvas.app.model.TextElement

data class EffectiveDesign(
    val design: CanvasDesign,
    val matchedRuleIds: List<String>,
)

object TemplateFormatter {
    fun format(template: String, snapshot: DynamicDataSnapshot): String {
        if (!template.contains('{')) return template
        return template
            .replace("{time}", snapshot.timeText)
            .replace("{date}", snapshot.dateText)
            .replace("{battery}", "${snapshot.batteryPercent}%")
            .replace("{charging_status}", snapshot.chargingLabel)
    }
}

object RuleEvaluator {

    fun evaluate(
        design: CanvasDesign,
        snapshot: DynamicDataSnapshot,
    ): EffectiveDesign {
        var effectiveBgColor = design.backgroundColorArgb
        var effectiveGradientEnabled = design.backgroundGradientEnabled
        val effectiveElements = design.elements.toMutableList()
        val matchedIds = mutableListOf<String>()

        for (rule in design.rules) {
            if (!rule.enabled) continue
            if (!matches(rule.condition, snapshot)) continue

            matchedIds.add(rule.id)
            when (val action = rule.action) {
                is RuleAction.OverrideBackground -> {
                    effectiveBgColor = action.colorArgb
                    effectiveGradientEnabled = false
                }
                is RuleAction.OverrideElementText -> {
                    for (i in effectiveElements.indices) {
                        val el = effectiveElements[i]
                        if (el is TextElement && el.id == action.elementId) {
                            effectiveElements[i] = el.copy(text = action.text)
                        }
                    }
                }
                is RuleAction.ToggleElementVisibility -> {
                    for (i in effectiveElements.indices) {
                        val el = effectiveElements[i]
                        if (el.id == action.elementId) {
                            effectiveElements[i] = el.withVisibility(action.visible)
                        }
                    }
                }
            }
        }

        val effectiveDesign = design.copy(
            backgroundColorArgb = effectiveBgColor,
            backgroundGradientEnabled = effectiveGradientEnabled,
            elements = effectiveElements,
        )
        return EffectiveDesign(
            design = effectiveDesign,
            matchedRuleIds = matchedIds,
        )
    }

    fun matches(
        condition: RuleCondition,
        snapshot: DynamicDataSnapshot,
    ): Boolean = when (condition) {
        is RuleCondition.BatteryBelow -> snapshot.batteryPercent < condition.thresholdPercent
        is RuleCondition.IsCharging -> snapshot.isCharging
        is RuleCondition.NightTime -> {
            val hour = snapshot.hourOfDay.coerceIn(0, 23)
            val start = condition.startHour.coerceIn(0, 23)
            val end = condition.endHour.coerceIn(0, 23)
            if (start == end) {
                true
            } else if (start < end) {
                hour in start until end
            } else {
                hour >= start || hour < end
            }
        }
        is RuleCondition.DateRange -> {
            val today = snapshot.monthOfYear.coerceIn(1, 12) * 100 +
                snapshot.dayOfMonth.coerceIn(1, 31)
            val start = condition.startMonth.coerceIn(1, 12) * 100 +
                condition.startDay.coerceIn(1, 31)
            val end = condition.endMonth.coerceIn(1, 12) * 100 +
                condition.endDay.coerceIn(1, 31)
            if (start <= end) {
                today in start..end
            } else {
                // Range wraps over the year boundary (e.g. Dec 1 - Feb 28).
                today >= start || today <= end
            }
        }
    }

    fun evaluateRule(
        rule: ConditionalRule,
        snapshot: DynamicDataSnapshot,
    ): Boolean = rule.enabled && matches(rule.condition, snapshot)
}
