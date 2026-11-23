package com.lockcanvas.app.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ConditionalRule(
    val id: String,
    val name: String,
    val enabled: Boolean = true,
    val condition: RuleCondition,
    val action: RuleAction,
)

@Serializable
sealed class RuleCondition {
    @Serializable
    @SerialName("battery_below")
    data class BatteryBelow(
        val thresholdPercent: Int = 20,
    ) : RuleCondition()

    @Serializable
    @SerialName("is_charging")
    data object IsCharging : RuleCondition()

    @Serializable
    @SerialName("night_time")
    data class NightTime(
        val startHour: Int = 20,
        val endHour: Int = 6,
    ) : RuleCondition()
}

@Serializable
sealed class RuleAction {
    @Serializable
    @SerialName("override_background")
    data class OverrideBackground(
        val colorArgb: Long,
    ) : RuleAction()

    @Serializable
    @SerialName("override_element_text")
    data class OverrideElementText(
        val elementId: String,
        val text: String,
    ) : RuleAction()

    @Serializable
    @SerialName("toggle_element_visibility")
    data class ToggleElementVisibility(
        val elementId: String,
        val visible: Boolean,
    ) : RuleAction()
}
