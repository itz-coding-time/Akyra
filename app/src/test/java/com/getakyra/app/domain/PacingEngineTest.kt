package com.getakyra.app.domain

import com.getakyra.app.data.ShiftTask
import org.junit.Assert.assertEquals
import org.junit.Test

class PacingEngineTest {

    private fun task(completed: Boolean) = ShiftTask(taskName = "T", archetype = "Float", isCompleted = completed)

    @Test
    fun `empty task list returns NO_TASKS`() {
        val result = PacingEngine.calculatePacing(emptyList(), 0.5f)
        assertEquals(PacingStatus.NO_TASKS, result.status)
        assertEquals(0, result.total)
    }

    @Test
    fun `zero elapsed and zero completed is ON_TRACK`() {
        val tasks = listOf(task(false), task(false))
        val result = PacingEngine.calculatePacing(tasks, 0.0f)
        // diff = 0.0 - 0.0 = 0.0, within -0.05..0.15
        assertEquals(PacingStatus.ON_TRACK, result.status)
    }

    @Test
    fun `no tasks completed but time at 10 percent is BEHIND_PACE`() {
        val tasks = listOf(task(false), task(false), task(false))
        val result = PacingEngine.calculatePacing(tasks, 0.1f)
        // taskPct=0.0, diff=-0.1 < -0.05
        assertEquals(PacingStatus.BEHIND_PACE, result.status)
    }

    @Test
    fun `half completed at half elapsed is ON_TRACK`() {
        val tasks = listOf(task(true), task(false))
        val result = PacingEngine.calculatePacing(tasks, 0.5f)
        // taskPct=0.5, diff=0.0, in -0.05..0.15
        assertEquals(PacingStatus.ON_TRACK, result.status)
        assertEquals(1, result.completed)
        assertEquals(2, result.total)
    }

    @Test
    fun `all done at 50 percent elapsed is AHEAD_OF_PACE`() {
        val tasks = listOf(task(true), task(true), task(true))
        val result = PacingEngine.calculatePacing(tasks, 0.5f)
        // taskPct=1.0, diff=0.5 > 0.15
        assertEquals(PacingStatus.AHEAD_OF_PACE, result.status)
    }

    @Test
    fun `80 percent done at 60 percent elapsed is AHEAD_OF_PACE`() {
        val tasks = (1..5).map { i -> task(i <= 4) }
        val result = PacingEngine.calculatePacing(tasks, 0.6f)
        // taskPct=0.8, diff=0.2 > 0.15
        assertEquals(PacingStatus.AHEAD_OF_PACE, result.status)
    }

    @Test
    fun `calculateGlobalFallbackPct returns 0 for zero-duration shift`() {
        val pct = PacingEngine.calculateGlobalFallbackPct(
            shiftStartMs = 1000L,
            shiftEndMs = 1000L,
            currentTimeMs = 1000L
        )
        assertEquals(0f, pct)
    }

    @Test
    fun `calculateGlobalFallbackPct returns 0 5 at midpoint`() {
        val start = 0L
        val end = 1000L
        val current = 500L
        val pct = PacingEngine.calculateGlobalFallbackPct(start, end, current)
        assertEquals(0.5f, pct, 0.001f)
    }

    @Test
    fun `calculateGlobalFallbackPct clamps to 1 past end`() {
        val pct = PacingEngine.calculateGlobalFallbackPct(0L, 100L, 200L)
        assertEquals(1f, pct)
    }

    @Test
    fun `calculateGlobalFallbackPct clamps to 0 before start`() {
        val pct = PacingEngine.calculateGlobalFallbackPct(500L, 1000L, 100L)
        assertEquals(0f, pct)
    }
}
