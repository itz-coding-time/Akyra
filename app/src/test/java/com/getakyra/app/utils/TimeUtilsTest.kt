package com.getakyra.app.utils

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class TimeUtilsTest {

    private fun msForTime(hour: Int, minute: Int): Long {
        return Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    // --- isAssociateOnClock: normal (same-day) shift ---

    @Test
    fun `day shift - associate is on clock during hours`() {
        val result = TimeUtils.isAssociateOnClock("09:00", "17:00", msForTime(12, 0))
        assertTrue(result)
    }

    @Test
    fun `day shift - associate is off clock before shift`() {
        val result = TimeUtils.isAssociateOnClock("09:00", "17:00", msForTime(7, 0))
        assertFalse(result)
    }

    @Test
    fun `day shift - associate is off clock after shift`() {
        val result = TimeUtils.isAssociateOnClock("09:00", "17:00", msForTime(18, 0))
        assertFalse(result)
    }

    @Test
    fun `day shift - associate is on clock at exact start time`() {
        val result = TimeUtils.isAssociateOnClock("09:00", "17:00", msForTime(9, 0))
        assertTrue(result)
    }

    @Test
    fun `day shift - associate is on clock at exact end time`() {
        val result = TimeUtils.isAssociateOnClock("09:00", "17:00", msForTime(17, 0))
        assertTrue(result)
    }

    // --- isAssociateOnClock: overnight shift (crosses midnight) ---

    @Test
    fun `overnight shift - on clock before midnight`() {
        val result = TimeUtils.isAssociateOnClock("22:00", "06:30", msForTime(23, 30))
        assertTrue(result)
    }

    @Test
    fun `overnight shift - on clock after midnight`() {
        val result = TimeUtils.isAssociateOnClock("22:00", "06:30", msForTime(3, 0))
        assertTrue(result)
    }

    @Test
    fun `overnight shift - off clock mid-afternoon`() {
        val result = TimeUtils.isAssociateOnClock("22:00", "06:30", msForTime(14, 0))
        assertFalse(result)
    }

    @Test
    fun `overnight shift - on clock at start time`() {
        val result = TimeUtils.isAssociateOnClock("22:00", "06:30", msForTime(22, 0))
        assertTrue(result)
    }

    @Test
    fun `overnight shift - on clock at end time`() {
        val result = TimeUtils.isAssociateOnClock("22:00", "06:30", msForTime(6, 30))
        assertTrue(result)
    }
}
