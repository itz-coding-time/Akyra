package com.getakyra.app.data

import android.util.Log
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class SupabaseRepository {

    private val client = SupabaseClientProvider.client
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val tag = "SupabaseRepository"

    // TODO: Replace with value from authenticated user's profile once Auth is wired in.
    private val devStoreId = "00000000-0000-0000-0000-000000000002"

    // ── Associates ────────────────────────────────────────────────────────────

    suspend fun fetchAssociates(): List<SbAssociate> = try {
        client.from("associates").select().decodeList()
    } catch (e: Exception) {
        Log.e(tag, "fetchAssociates failed: ${e.message}")
        emptyList()
    }

    fun upsertAssociate(associate: Associate) = scope.launch {
        try {
            val sb = SbAssociate(
                storeId = devStoreId,
                name = associate.name,
                role = associate.role,
                currentArchetype = associate.currentArchetype,
                pinCode = associate.pinCode,
                scheduledDays = associate.scheduledDays,
                defaultStartTime = associate.defaultStartTime,
                defaultEndTime = associate.defaultEndTime
            )
            client.from("associates").upsert(sb)
        } catch (e: Exception) {
            Log.e(tag, "upsertAssociate failed: ${e.message}")
        }
    }

    // ── Schedule ──────────────────────────────────────────────────────────────

    fun upsertScheduleEntry(entry: ScheduleEntry) = scope.launch {
        try {
            val sb = SbScheduleEntry(
                storeId = devStoreId,
                associateId = devStoreId, // TODO: resolve real associate UUID
                startTime = entry.startTime,
                endTime = entry.endTime
            )
            client.from("schedule_entries").upsert(sb)
        } catch (e: Exception) {
            Log.e(tag, "upsertScheduleEntry failed: ${e.message}")
        }
    }

    // ── Tasks ─────────────────────────────────────────────────────────────────

    suspend fun fetchTasks(): List<SbTask> = try {
        client.from("tasks").select().decodeList()
    } catch (e: Exception) {
        Log.e(tag, "fetchTasks failed: ${e.message}")
        emptyList()
    }

    fun upsertTask(task: ShiftTask) = scope.launch {
        try {
            val sb = SbTask(
                storeId = devStoreId,
                taskName = task.taskName,
                archetype = task.archetype,
                basePoints = task.basePoints,
                isCompleted = task.isCompleted,
                isPullTask = task.isPullTask,
                pullCategory = task.pullCategory,
                isSticky = task.isSticky,
                priority = task.priority,
                taskDescription = task.taskDescription,
                assignedTo = task.assignedTo,
                completedBy = task.completedBy,
                isTruckTask = task.isTruckTask
            )
            client.from("tasks").upsert(sb)
        } catch (e: Exception) {
            Log.e(tag, "upsertTask failed: ${e.message}")
        }
    }

    // ── Inventory ─────────────────────────────────────────────────────────────

    suspend fun fetchInventory(): List<SbInventoryItem> = try {
        client.from("inventory_items").select().decodeList()
    } catch (e: Exception) {
        Log.e(tag, "fetchInventory failed: ${e.message}")
        emptyList()
    }

    fun upsertInventoryItem(item: InventoryItem) = scope.launch {
        try {
            val sb = SbInventoryItem(
                storeId = devStoreId,
                itemName = item.itemName,
                buildTo = item.buildTo,
                category = item.category,
                amountHave = item.amountHave,
                amountNeeded = item.amountNeeded,
                isPulled = item.isPulled
            )
            client.from("inventory_items").upsert(sb)
        } catch (e: Exception) {
            Log.e(tag, "upsertInventoryItem failed: ${e.message}")
        }
    }

    // ── Table Items ───────────────────────────────────────────────────────────

    suspend fun fetchTableItems(): List<SbTableItem> = try {
        client.from("table_items").select().decodeList()
    } catch (e: Exception) {
        Log.e(tag, "fetchTableItems failed: ${e.message}")
        emptyList()
    }

    fun upsertTableItem(item: TableItem) = scope.launch {
        try {
            val sb = SbTableItem(
                storeId = devStoreId,
                itemName = item.itemName,
                station = item.station,
                isInitialed = item.isInitialed,
                wasteAmount = item.wasteAmount
            )
            client.from("table_items").upsert(sb)
        } catch (e: Exception) {
            Log.e(tag, "upsertTableItem failed: ${e.message}")
        }
    }

    // ── Incidents ─────────────────────────────────────────────────────────────

    fun insertIncident(incident: IncidentLog) = scope.launch {
        try {
            val sb = SbIncident(
                storeId = devStoreId,
                associateId = devStoreId, // TODO: resolve real associate UUID
                description = incident.description,
                category = incident.category,
                timestampMs = incident.timestampMs,
                isStatementGenerated = incident.isStatementGenerated
            )
            client.from("incidents").insert(sb)
        } catch (e: Exception) {
            Log.e(tag, "insertIncident failed: ${e.message}")
        }
    }

    // ── Shift State ───────────────────────────────────────────────────────────

    fun upsertShiftState(state: ShiftState) = scope.launch {
        try {
            val sb = SbShiftState(
                storeId = devStoreId,
                startTimeMs = state.startTimeMs,
                endTimeMs = state.endTimeMs,
                shiftName = state.shiftName,
                isOpen = state.isOpen,
                isTruckNight = state.isTruckNight
            )
            client.from("shift_states").upsert(sb)
        } catch (e: Exception) {
            Log.e(tag, "upsertShiftState failed: ${e.message}")
        }
    }

    companion object {
        @Volatile private var INSTANCE: SupabaseRepository? = null
        fun getInstance(): SupabaseRepository =
            INSTANCE ?: synchronized(this) {
                SupabaseRepository().also { INSTANCE = it }
            }
    }
}
