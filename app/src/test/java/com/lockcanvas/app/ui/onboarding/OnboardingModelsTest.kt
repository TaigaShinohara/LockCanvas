package com.lockcanvas.app.ui.onboarding

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OnboardingModelsTest {

    @Test
    fun pageOrder_has6PagesEndingAtWallpaperSetup() {
        val freePages = OnboardingPageOrder.buildPages(OnboardingGoal.FREE_LAYOUT)
        val dynamicPages = OnboardingPageOrder.buildPages(OnboardingGoal.DYNAMIC_INFO)

        assertEquals(6, freePages.size)
        assertEquals(6, dynamicPages.size)
        // One value page, chosen by goal
        assertEquals(OnboardingPageRole.VALUE_SAFE_AREA, freePages[3])
        assertEquals(OnboardingPageRole.VALUE_DYNAMIC, dynamicPages[3])
        // Choice must always come immediately before Match Result
        assertEquals(OnboardingPageRole.GOAL_CHOICE, freePages[1])
        assertEquals(OnboardingPageRole.MATCH_RESULT, freePages[2])
        // The flow must end at the wallpaper setup page (the first success)
        assertEquals(OnboardingPageRole.TRY_DRAG, freePages[4])
        assertEquals(OnboardingPageRole.CONNECT, freePages[5])
    }

    @Test
    fun windowDots_limitsToFiveAndMarksOverflowEdges() {
        val startDots = WindowDotCalculator.compute(currentPage = 0, totalPages = 12)
        assertEquals(5, startDots.size)
        assertTrue(startDots[0].isCurrent)
        assertFalse(startDots[0].isEdgeSmall)
        assertTrue(startDots[4].isEdgeSmall)

        val midDots = WindowDotCalculator.compute(currentPage = 6, totalPages = 12)
        assertEquals(5, midDots.size)
        assertEquals(6, midDots[2].pageIndex)
        assertTrue(midDots[2].isCurrent)
        assertTrue(midDots[0].isEdgeSmall)
        assertTrue(midDots[4].isEdgeSmall)

        val endDots = WindowDotCalculator.compute(currentPage = 11, totalPages = 12)
        assertEquals(5, endDots.size)
        assertTrue(endDots[4].isCurrent)
        assertTrue(endDots[0].isEdgeSmall)
        assertFalse(endDots[4].isEdgeSmall)
    }

    @Test
    fun praiseRotator_neverRepeatsConsecutively() {
        val rotator = PraiseRotator()
        val first = rotator.nextGoalPraise()
        val second = rotator.nextGoalPraise()
        assertNotEquals(first, second)
    }
}
