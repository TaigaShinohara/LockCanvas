package com.lockcanvas.app.ui

import com.lockcanvas.app.model.CanvasDesign
import com.lockcanvas.app.model.ConditionalRule
import com.lockcanvas.app.model.RuleAction
import com.lockcanvas.app.model.RuleCondition
import com.lockcanvas.app.model.ShapeElement
import com.lockcanvas.app.model.TextElement
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EditorReducerTest {

    private fun baseState(): EditorUiState {
        val design = CanvasDesign(
            id = "test_design",
            name = "Test",
            elements = listOf(
                ShapeElement(id = "el_a", zIndex = 0),
                TextElement(id = "el_b", text = "Hello", zIndex = 5),
                TextElement(id = "el_c", text = "World", zIndex = 10),
            ),
        )
        return EditorUiState(design = design)
    }

    @Test
    fun undoAndRedoStackBehavesPredictably() {
        val initial = baseState()
        assertFalse(initial.canUndo)
        assertFalse(initial.canRedo)

        val added = EditorReducer.addElement(initial, TextElement(id = "el_new", text = "New"))
        assertTrue(added.canUndo)
        assertFalse(added.canRedo)
        assertEquals(4, added.design.elements.size)

        val undone = EditorReducer.undo(added)
        assertFalse(undone.canUndo)
        assertTrue(undone.canRedo)
        assertEquals(3, undone.design.elements.size)
        assertNull(undone.selectedElementId)

        val redone = EditorReducer.redo(undone)
        assertTrue(redone.canUndo)
        assertFalse(redone.canRedo)
        assertEquals(4, redone.design.elements.size)

        // New mutation after undo clears redo stack
        val afterUndoThenEdit = EditorReducer.deleteElement(undone, "el_a")
        assertFalse(afterUndoThenEdit.canRedo)
        assertEquals(2, afterUndoThenEdit.design.elements.size)
    }

    @Test
    fun addDuplicateAndDeleteElementWorkCorrectly() {
        val initial = baseState()

        val afterAdd = EditorReducer.addElement(initial, TextElement(id = "el_d", text = "Added"))
        val addedEl = afterAdd.design.elements.first { it.id == "el_d" }
        assertEquals("el_d", afterAdd.selectedElementId)
        assertEquals(11, addedEl.zIndex)

        val afterDup = EditorReducer.duplicateElement(afterAdd, "el_d", newId = "el_d_copy")
        val dupEl = afterDup.design.elements.first { it.id == "el_d_copy" }
        assertEquals("el_d_copy", afterDup.selectedElementId)
        assertEquals(12, dupEl.zIndex)
        assertTrue(dupEl.xFraction > addedEl.xFraction)
        assertTrue(dupEl.yFraction > addedEl.yFraction)

        val afterDeleteSelected = EditorReducer.deleteElement(afterDup, "el_d_copy")
        assertNull(afterDeleteSelected.selectedElementId)
        assertFalse(afterDeleteSelected.design.elements.any { it.id == "el_d_copy" })
    }

    @Test
    fun bringForwardAndSendBackwardReorderZIndices() {
        val initial = baseState()

        // Order initially: el_a (0), el_b (5), el_c (10)
        val brought = EditorReducer.bringForward(initial, "el_a")
        val orderedAfterBring = brought.design.elements.sortedBy { it.zIndex }.map { it.id }
        assertEquals(listOf("el_b", "el_a", "el_c"), orderedAfterBring)

        // Bringing top-most element forward is a no-op
        val broughtTop = EditorReducer.bringForward(brought, "el_c")
        assertEquals(brought.design, broughtTop.design)

        val sentBack = EditorReducer.sendBackward(brought, "el_c")
        val orderedAfterSend = sentBack.design.elements.sortedBy { it.zIndex }.map { it.id }
        assertEquals(listOf("el_b", "el_c", "el_a"), orderedAfterSend)

        // Sending bottom-most element backward is a no-op
        val sentBottom = EditorReducer.sendBackward(sentBack, "el_b")
        assertEquals(sentBack.design, sentBottom.design)
    }

    @Test
    fun ruleActionsCanTargetExistingElementsAndCanBeToggledOrDeleted() {
        val initial = baseState()
        val rule = ConditionalRule(
            id = "rule_1",
            name = "低電力時",
            enabled = true,
            condition = RuleCondition.BatteryBelow(thresholdPercent = 20),
            action = RuleAction.ToggleElementVisibility(
                elementId = "el_b",
                visible = false,
            ),
        )

        val withRule = EditorReducer.addRule(initial, rule)
        assertEquals("el_b", (withRule.design.rules.single().action as RuleAction.ToggleElementVisibility).elementId)
        assertTrue(withRule.hasUnsavedChanges)

        val disabled = EditorReducer.toggleRule(withRule, "rule_1")
        assertFalse(disabled.design.rules.single().enabled)

        val deleted = EditorReducer.deleteRule(disabled, "rule_1")
        assertTrue(deleted.design.rules.isEmpty())
    }

    @Test
    fun snapToCenterSnapsWithinThresholdAndPreservesOutside() {
        val bothSnapped = EditorReducer.snapToCenter(0.51f, 0.485f)
        assertTrue(bothSnapped.snappedX)
        assertTrue(bothSnapped.snappedY)
        assertEquals(0.5f, bothSnapped.xFraction, 0.0001f)
        assertEquals(0.5f, bothSnapped.yFraction, 0.0001f)

        val onlyXSnapped = EditorReducer.snapToCenter(0.49f, 0.72f)
        assertTrue(onlyXSnapped.snappedX)
        assertFalse(onlyXSnapped.snappedY)
        assertEquals(0.5f, onlyXSnapped.xFraction, 0.0001f)
        assertEquals(0.72f, onlyXSnapped.yFraction, 0.0001f)

        val noneSnapped = EditorReducer.snapToCenter(0.25f, 0.80f)
        assertFalse(noneSnapped.snappedX)
        assertFalse(noneSnapped.snappedY)
        assertEquals(0.25f, noneSnapped.xFraction, 0.0001f)
        assertEquals(0.80f, noneSnapped.yFraction, 0.0001f)
    }
}
