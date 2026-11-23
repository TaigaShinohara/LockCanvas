package com.lockcanvas.app.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class AnimationStyle {
    NONE,
    FLOATING_PARTICLES,
    PULSE_GLOW,
}

@Serializable
enum class BatteryGaugeStyle {
    BAR,
    CIRCULAR,
}

@Serializable
enum class ShapeType {
    RECTANGLE,
    CIRCLE,
    PILL,
}

@Serializable
sealed class CanvasElement {
    abstract val id: String
    abstract val xFraction: Float
    abstract val yFraction: Float
    abstract val scale: Float
    abstract val rotationDegrees: Float
    abstract val alpha: Float
    abstract val zIndex: Int
    abstract val visible: Boolean

    fun withPosition(xFraction: Float, yFraction: Float): CanvasElement = when (this) {
        is TextElement -> copy(xFraction = xFraction, yFraction = yFraction)
        is BatteryGaugeElement -> copy(xFraction = xFraction, yFraction = yFraction)
        is ShapeElement -> copy(xFraction = xFraction, yFraction = yFraction)
        is ImageElement -> copy(xFraction = xFraction, yFraction = yFraction)
    }

    fun withVisibility(visible: Boolean): CanvasElement = when (this) {
        is TextElement -> copy(visible = visible)
        is BatteryGaugeElement -> copy(visible = visible)
        is ShapeElement -> copy(visible = visible)
        is ImageElement -> copy(visible = visible)
    }

    fun withTransform(
        scale: Float = this.scale,
        rotationDegrees: Float = this.rotationDegrees,
        alpha: Float = this.alpha,
        zIndex: Int = this.zIndex,
    ): CanvasElement = when (this) {
        is TextElement -> copy(
            scale = scale,
            rotationDegrees = rotationDegrees,
            alpha = alpha,
            zIndex = zIndex,
        )
        is BatteryGaugeElement -> copy(
            scale = scale,
            rotationDegrees = rotationDegrees,
            alpha = alpha,
            zIndex = zIndex,
        )
        is ShapeElement -> copy(
            scale = scale,
            rotationDegrees = rotationDegrees,
            alpha = alpha,
            zIndex = zIndex,
        )
        is ImageElement -> copy(
            scale = scale,
            rotationDegrees = rotationDegrees,
            alpha = alpha,
            zIndex = zIndex,
        )
    }
}

@Serializable
@SerialName("text")
data class TextElement(
    override val id: String,
    override val xFraction: Float = 0.5f,
    override val yFraction: Float = 0.5f,
    override val scale: Float = 1f,
    override val rotationDegrees: Float = 0f,
    override val alpha: Float = 1f,
    override val zIndex: Int = 10,
    override val visible: Boolean = true,
    val text: String = "LockCanvas",
    val colorArgb: Long = 0xFFFFFFFF,
    val fontSizeSp: Float = 24f,
    val bold: Boolean = false,
    val dynamicTemplate: Boolean = true,
) : CanvasElement()

@Serializable
@SerialName("battery_gauge")
data class BatteryGaugeElement(
    override val id: String,
    override val xFraction: Float = 0.5f,
    override val yFraction: Float = 0.56f,
    override val scale: Float = 1f,
    override val rotationDegrees: Float = 0f,
    override val alpha: Float = 1f,
    override val zIndex: Int = 10,
    override val visible: Boolean = true,
    val style: BatteryGaugeStyle = BatteryGaugeStyle.BAR,
    val widthFraction: Float = 0.58f,
    val heightFraction: Float = 0.045f,
    val activeColorArgb: Long = 0xFF34D399,
    val trackColorArgb: Long = 0x33FFFFFF,
    val textColorArgb: Long = 0xFFFFFFFF,
    val showLabel: Boolean = true,
) : CanvasElement()

@Serializable
@SerialName("shape")
data class ShapeElement(
    override val id: String,
    override val xFraction: Float = 0.5f,
    override val yFraction: Float = 0.52f,
    override val scale: Float = 1f,
    override val rotationDegrees: Float = 0f,
    override val alpha: Float = 1f,
    override val zIndex: Int = 0,
    override val visible: Boolean = true,
    val shapeType: ShapeType = ShapeType.RECTANGLE,
    val widthFraction: Float = 0.76f,
    val heightFraction: Float = 0.16f,
    val fillColorArgb: Long = 0x22FFFFFF,
    val strokeColorArgb: Long = 0x55FFFFFF,
    val strokeWidthDp: Float = 1.5f,
    val cornerRadiusDp: Float = 24f,
) : CanvasElement()

@Serializable
@SerialName("image")
data class ImageElement(
    override val id: String,
    override val xFraction: Float = 0.5f,
    override val yFraction: Float = 0.5f,
    override val scale: Float = 1f,
    override val rotationDegrees: Float = 0f,
    override val alpha: Float = 1f,
    override val zIndex: Int = 5,
    override val visible: Boolean = true,
    val uriString: String = "",
    val widthFraction: Float = 0.36f,
    val heightFraction: Float = 0.20f,
    val cornerRadiusDp: Float = 18f,
) : CanvasElement()

@Serializable
data class CanvasDesign(
    val id: String,
    val name: String,
    val backgroundColorArgb: Long = 0xFF0B1020,
    val secondaryBackgroundColorArgb: Long = 0xFF16223D,
    val backgroundGradientEnabled: Boolean = true,
    val animationStyle: AnimationStyle = AnimationStyle.NONE,
    val elements: List<CanvasElement> = emptyList(),
    val rules: List<ConditionalRule> = emptyList(),
    val updatedAtEpochMs: Long = 0L,
)
