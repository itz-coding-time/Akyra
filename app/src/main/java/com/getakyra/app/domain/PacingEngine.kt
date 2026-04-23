package com.getakyra.app.domain

import com.getakyra.app.data.Associate
import com.getakyra.app.data.ScheduleEntry
import com.getakyra.app.data.ShiftTask
import com.getakyra.app.utils.TimeUtils

enum class PacingStatus { NO_TASKS, BEHIND_PACE, ON_TRACK, AHEAD_OF_PACE }

data class PacingResult(
    val status: PacingStatus,
    val taskPct: Float,
    val timePct: Float,
    val completed: Int,
    val total: Int
)

object PacingEngine {

    fun calculatePacing(tasks: List<ShiftTask>, timeElapsedPct: Float): PacingResult {
        val total = tasks.size
        val completed = tasks.count { it.isCompleted }
        val taskPct = if (total > 0) completed.toFloat() / total else 1f

        // If taskPct - timePct < -0.05 → BEHIND (fell behind target by >5%)
        // If diff in -0.05..0.15 → ON TRACK (within acceptable variance)
        // Else → AHEAD
        val diff = taskPct - timeElapsedPct
        val status = when {
            total == 0 -> PacingStatus.NO_TASKS
            diff < -0.05f -> PacingStatus.BEHIND_PACE
            diff <= 0.15f -> PacingStatus.ON_TRACK
            else -> PacingStatus.AHEAD_OF_PACE
        }

        return PacingResult(
            status = status,
            taskPct = taskPct,
            timePct = timeElapsedPct,
            completed = completed,
            total = total
        )
    }

    fun calculateSmartGlobalTimePct(
        associates: List<Associate>,
        scheduleEntries: List<ScheduleEntry>,
        globalFallback: Float
    ): Float {
        val progresses = associates.mapNotNull { assoc ->
            scheduleEntries.find { it.associateName.equals(assoc.name, ignoreCase = true) }
                ?.let { sched -> TimeUtils.calculateAssociateShiftProgress(sched.startTime, sched.endTime) }
        }
        return if (progresses.isNotEmpty()) progresses.average().toFloat() else globalFallback
    }

    fun calculateZoneTimePct(
        zoneArchetype: String,
        associates: List<Associate>,
        scheduleEntries: List<ScheduleEntry>,
        globalFallback: Float
    ): Float {
        val zoneAssocs = associates.filter { it.currentArchetype == zoneArchetype }
        val progresses = zoneAssocs.mapNotNull { assoc ->
            scheduleEntries.find { it.associateName.equals(assoc.name, ignoreCase = true) }
                ?.let { sched -> TimeUtils.calculateAssociateShiftProgress(sched.startTime, sched.endTime) }
        }
        return if (progresses.isNotEmpty()) progresses.average().toFloat() else globalFallback
    }

    fun calculateGlobalFallbackPct(
        shiftStartMs: Long,
        shiftEndMs: Long,
        currentTimeMs: Long
    ): Float {
        if (shiftEndMs <= shiftStartMs) return 0f
        val totalMs = shiftEndMs - shiftStartMs
        val passedMs = currentTimeMs - shiftStartMs
        return (passedMs.toFloat() / totalMs).coerceIn(0f, 1f)
    }
}
