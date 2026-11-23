package com.lockcanvas.app.ui

import com.lockcanvas.app.model.BatteryGaugeElement
import com.lockcanvas.app.model.CanvasDesign
import com.lockcanvas.app.model.CanvasElement
import com.lockcanvas.app.model.ConditionalRule
import com.lockcanvas.app.model.DynamicDataSnapshot
import com.lockcanvas.app.model.ImageElement
import com.lockcanvas.app.model.PresetDesigns
import com.lockcanvas.app.model.ShapeElement
import com.lockcanvas.app.model.TextElement
import java.util.UUID
import kotlin.math.abs

sealed interface ScreenRoute {
    data object Onboarding : ScreenRoute
    data object Home : ScreenRoute
    data object Editor : ScreenRoute
    data object SettingsInfo : ScreenRoute
}

enum class EditorToolTab(val label: String) {
    ADD("追加"),
    EDIT("編集"),
    BACKGROUND("背景/動き"),
    RULES("条件ルール"),
    LAYERS("レイヤー"),
}

data class SnapResult(
    val xFraction: Float,
    val yFraction: Float,
    val snappedX: Boolean,
    val snappedY: Boolean,
)

data class EditorUiState(
    val design: CanvasDesign = PresetDesigns.defaultDesign(),
    val selectedElementId: String? = null,
    val showSafeAreaGuides: Boolean = true,
    val activeTab: EditorToolTab? = null,
    val undoStack: List<CanvasDesign> = emptyList(),
    val redoStack: List<CanvasDesign> = emptyList(),
    val snapGuideX: Boolean = false,
    val snapGuideY: Boolean = false,
    val hasUnsavedChanges: Boolean = false,
    val simulatorEnabled: Boolean = false,
    val simulatedBatteryPercent: Int = 15,
    val simulatedIsCharging: Boolean = false,
    val simulatedHourOfDay: Int = 22,
) {
    val canUndo: Boolean get() = undoStack.isNotEmpty()
    val canRedo: Boolean get() = redoStack.isNotEmpty()
    val selectedElement: CanvasElement?
        get() = selectedElementId?.let { id -> design.elements.firstOrNull { it.id == id } }

    fun effectiveSnapshot(liveSnapshot: DynamicDataSnapshot): DynamicDataSnapshot {
        if (!simulatorEnabled) return liveSnapshot
        val clampedBattery = simulatedBatteryPercent.coerceIn(0, 100)
        val clampedHour = simulatedHourOfDay.coerceIn(0, 23)
        val isJapanese = java.util.Locale.getDefault().language == java.util.Locale.JAPANESE.language
        val label = if (isJapanese) {
            when {
                simulatedIsCharging && clampedBattery >= 100 -> "充電完了"
                simulatedIsCharging -> "充電中"
                clampedBattery <= 15 -> "バッテリー低下"
                else -> "バッテリー駆動"
            }
        } else {
            when {
                simulatedIsCharging && clampedBattery >= 100 -> "Charged"
                simulatedIsCharging -> "Charging"
                clampedBattery <= 15 -> "Low Battery"
                else -> "On Battery"
            }
        }
        return liveSnapshot.copy(
            timeText = "%02d:00".format(clampedHour),
            hourOfDay = clampedHour,
            batteryPercent = clampedBattery,
            isCharging = simulatedIsCharging,
            chargingLabel = label,
        )
    }
}

object EditorReducer {

    const val MAX_HISTORY_SIZE = 50
    const val DEFAULT_SNAP_THRESHOLD = 0.025f
    const val CENTER_FRACTION = 0.5f

    fun snapToCenter(
        xFraction: Float,
        yFraction: Float,
        threshold: Float = DEFAULT_SNAP_THRESHOLD,
    ): SnapResult {
        val clampedX = xFraction.coerceIn(0.02f, 0.98f)
        val clampedY = yFraction.coerceIn(0.02f, 0.98f)
        val snappedX = abs(clampedX - CENTER_FRACTION) <= threshold
        val snappedY = abs(clampedY - CENTER_FRACTION) <= threshold
        return SnapResult(
            xFraction = if (snappedX) CENTER_FRACTION else clampedX,
            yFraction = if (snappedY) CENTER_FRACTION else clampedY,
            snappedX = snappedX,
            snappedY = snappedY,
        )
    }

    fun pushDesignChange(
        state: EditorUiState,
        newDesign: CanvasDesign,
        selectedElementId: String? = state.selectedElementId,
    ): EditorUiState {
        if (newDesign == state.design) {
            return state.copy(selectedElementId = selectedElementId)
        }
        val nextUndo = (state.undoStack + state.design).takeLast(MAX_HISTORY_SIZE)
        return state.copy(
            design = newDesign,
            selectedElementId = selectedElementId,
            undoStack = nextUndo,
            redoStack = emptyList(),
            hasUnsavedChanges = true,
        )
    }

    fun commitGestureSnapshot(
        state: EditorUiState,
        beforeDesign: CanvasDesign,
    ): EditorUiState {
        if (beforeDesign == state.design) {
            return state.copy(snapGuideX = false, snapGuideY = false)
        }
        val nextUndo = (state.undoStack + beforeDesign).takeLast(MAX_HISTORY_SIZE)
        return state.copy(
            undoStack = nextUndo,
            redoStack = emptyList(),
            snapGuideX = false,
            snapGuideY = false,
            hasUnsavedChanges = true,
        )
    }

    fun undo(state: EditorUiState): EditorUiState {
        if (state.undoStack.isEmpty()) return state
        val previous = state.undoStack.last()
        val remainingUndo = state.undoStack.dropLast(1)
        val nextRedo = (state.redoStack + state.design).takeLast(MAX_HISTORY_SIZE)
        val validSelection = state.selectedElementId?.takeIf { id ->
            previous.elements.any { it.id == id }
        }
        return state.copy(
            design = previous,
            selectedElementId = validSelection,
            undoStack = remainingUndo,
            redoStack = nextRedo,
            snapGuideX = false,
            snapGuideY = false,
            hasUnsavedChanges = true,
        )
    }

    fun redo(state: EditorUiState): EditorUiState {
        if (state.redoStack.isEmpty()) return state
        val next = state.redoStack.last()
        val remainingRedo = state.redoStack.dropLast(1)
        val nextUndo = (state.undoStack + state.design).takeLast(MAX_HISTORY_SIZE)
        val validSelection = state.selectedElementId?.takeIf { id ->
            next.elements.any { it.id == id }
        }
        return state.copy(
            design = next,
            selectedElementId = validSelection,
            undoStack = nextUndo,
            redoStack = remainingRedo,
            snapGuideX = false,
            snapGuideY = false,
            hasUnsavedChanges = true,
        )
    }

    fun addElement(
        state: EditorUiState,
        element: CanvasElement,
    ): EditorUiState {
        val nextZ = (state.design.elements.maxOfOrNull { it.zIndex } ?: -1) + 1
        val placed = element.withTransform(zIndex = nextZ)
        val newDesign = state.design.copy(elements = state.design.elements + placed)
        return pushDesignChange(
            state = state,
            newDesign = newDesign,
            selectedElementId = placed.id,
        )
    }

    fun duplicateElement(
        state: EditorUiState,
        elementId: String,
        newId: String = "el_${UUID.randomUUID().toString().take(8)}",
    ): EditorUiState {
        val source = state.design.elements.firstOrNull { it.id == elementId } ?: return state
        val nextZ = (state.design.elements.maxOfOrNull { it.zIndex } ?: 0) + 1
        val offsetX = (source.xFraction + 0.035f).coerceIn(0.05f, 0.95f)
        val offsetY = (source.yFraction + 0.035f).coerceIn(0.05f, 0.95f)
        val copy: CanvasElement = when (source) {
            is TextElement -> source.copy(id = newId, xFraction = offsetX, yFraction = offsetY, zIndex = nextZ)
            is BatteryGaugeElement -> source.copy(id = newId, xFraction = offsetX, yFraction = offsetY, zIndex = nextZ)
            is ShapeElement -> source.copy(id = newId, xFraction = offsetX, yFraction = offsetY, zIndex = nextZ)
            is ImageElement -> source.copy(id = newId, xFraction = offsetX, yFraction = offsetY, zIndex = nextZ)
        }
        val newDesign = state.design.copy(elements = state.design.elements + copy)
        return pushDesignChange(
            state = state,
            newDesign = newDesign,
            selectedElementId = newId,
        )
    }

    fun deleteElement(
        state: EditorUiState,
        elementId: String,
    ): EditorUiState {
        if (state.design.elements.none { it.id == elementId }) return state
        val remaining = state.design.elements.filterNot { it.id == elementId }
        val nextSelected = if (state.selectedElementId == elementId) null else state.selectedElementId
        return pushDesignChange(
            state = state,
            newDesign = state.design.copy(elements = remaining),
            selectedElementId = nextSelected,
        )
    }

    fun toggleVisibility(
        state: EditorUiState,
        elementId: String,
    ): EditorUiState = updateElement(state, elementId, recordHistory = true) {
        it.withVisibility(!it.visible)
    }

    fun bringForward(
        state: EditorUiState,
        elementId: String,
    ): EditorUiState {
        val sorted = state.design.elements.sortedBy { it.zIndex }.toMutableList()
        val idx = sorted.indexOfFirst { it.id == elementId }
        if (idx < 0 || idx == sorted.lastIndex) return state
        val item = sorted.removeAt(idx)
        sorted.add(idx + 1, item)
        val reindexed = sorted.mapIndexed { index, element ->
            element.withTransform(zIndex = index)
        }
        return pushDesignChange(state, state.design.copy(elements = reindexed))
    }

    fun sendBackward(
        state: EditorUiState,
        elementId: String,
    ): EditorUiState {
        val sorted = state.design.elements.sortedBy { it.zIndex }.toMutableList()
        val idx = sorted.indexOfFirst { it.id == elementId }
        if (idx <= 0) return state
        val item = sorted.removeAt(idx)
        sorted.add(idx - 1, item)
        val reindexed = sorted.mapIndexed { index, element ->
            element.withTransform(zIndex = index)
        }
        return pushDesignChange(state, state.design.copy(elements = reindexed))
    }

    fun updateElement(
        state: EditorUiState,
        elementId: String,
        recordHistory: Boolean = true,
        transform: (CanvasElement) -> CanvasElement,
    ): EditorUiState {
        var changed = false
        val updated = state.design.elements.map { el ->
            if (el.id == elementId) {
                val next = transform(el)
                if (next != el) changed = true
                next
            } else {
                el
            }
        }
        if (!changed) return state
        val newDesign = state.design.copy(elements = updated)
        return if (recordHistory) {
            pushDesignChange(state, newDesign)
        } else {
            state.copy(design = newDesign, hasUnsavedChanges = true)
        }
    }

    fun addRule(
        state: EditorUiState,
        rule: ConditionalRule,
    ): EditorUiState {
        val newDesign = state.design.copy(rules = state.design.rules + rule)
        return pushDesignChange(state, newDesign)
    }

    fun toggleRule(
        state: EditorUiState,
        ruleId: String,
    ): EditorUiState {
        val updated = state.design.rules.map { rule ->
            if (rule.id == ruleId) rule.copy(enabled = !rule.enabled) else rule
        }
        return pushDesignChange(state, state.design.copy(rules = updated))
    }

    fun deleteRule(
        state: EditorUiState,
        ruleId: String,
    ): EditorUiState {
        val updated = state.design.rules.filterNot { it.id == ruleId }
        return pushDesignChange(state, state.design.copy(rules = updated))
    }
}
