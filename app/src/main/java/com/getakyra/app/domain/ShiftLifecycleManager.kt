package com.getakyra.app.domain

import com.getakyra.app.data.Associate
import com.getakyra.app.data.IncidentLog
import com.getakyra.app.data.ShiftRepository
import com.getakyra.app.data.ShiftState
import com.getakyra.app.data.ShiftTask

class ShiftLifecycleManager(private val repository: ShiftRepository) {

    suspend fun openShift(
        shiftName: String,
        isTruckNight: Boolean,
        startMs: Long,
        endMs: Long
    ) {
        repository.updateShiftState(
            ShiftState(
                startTimeMs = startMs,
                endTimeMs = endMs,
                shiftName = shiftName,
                isOpen = true,
                isTruckNight = isTruckNight
            )
        )

        if (isTruckNight) {
            repository.insertTask(
                ShiftTask(
                    taskName = "Receive & Count Truck",
                    archetype = "MOD",
                    assignedTo = "MOD",
                    priority = "High",
                    isTruckTask = true,
                    taskDescription = "Count cubes and log the split for Ambient zones."
                )
            )
        }

        repository.insertIncident(
            IncidentLog(
                associateId = -1,
                category = "Shift Opened",
                description = "Shift '$shiftName' started.",
                timestampMs = System.currentTimeMillis()
            )
        )
    }

    suspend fun closeShift(tasks: List<ShiftTask>, associates: List<Associate>) {
        val reportBuilder = StringBuilder()
        reportBuilder.append("Total Tasks Completed: ${tasks.count { it.isCompleted }} / ${tasks.size}\n\n")

        listOf("Kitchen", "POS", "Float", "MOD").forEach { arch ->
            val archDone = tasks.count { it.archetype == arch && it.isCompleted }
            val archTotal = tasks.count { it.archetype == arch }
            val archAssocs = associates.filter { it.currentArchetype == arch }.joinToString { it.name }
            val assignedStr = if (archAssocs.isEmpty()) "Unmanned" else archAssocs
            reportBuilder.append("[$arch ZONE]\nCompleted: $archDone/$archTotal\nAssigned: $assignedStr\n\n")
        }

        val missedTasks = tasks.filter { !it.isCompleted }
        if (missedTasks.isNotEmpty()) {
            reportBuilder.append("--- MISSED TASKS (FELL OFF) ---\n")
            missedTasks.forEach { task ->
                reportBuilder.append("- [${task.archetype}] ${task.taskName} (${task.priority} Priority)\n")
            }
        }

        repository.insertIncident(
            IncidentLog(
                associateId = -1,
                category = "End of Shift Report",
                description = reportBuilder.toString().trim(),
                timestampMs = System.currentTimeMillis()
            )
        )
        repository.clearShiftState()
        repository.deleteJitTasks()
        repository.resetStickyAndPullTasks()
        repository.resetInventoryCounts()
    }
}
