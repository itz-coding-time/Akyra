package com.getakyra.app.data

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class ShiftRepository(private val dao: AkyraDao) {

    // Associates
    fun getAllAssociates(): Flow<List<Associate>> = dao.getAllAssociates()
    suspend fun insertAssociate(associate: Associate) = dao.insertAssociate(associate)
    suspend fun updateAssociate(associate: Associate) = dao.updateAssociate(associate)
    suspend fun deleteAssociate(associate: Associate) = dao.deleteAssociate(associate)

    // Schedule
    fun getSchedule(): Flow<List<ScheduleEntry>> = dao.getSchedule()
    suspend fun insertScheduleEntry(entry: ScheduleEntry) = dao.insertScheduleEntry(entry)
    suspend fun deleteScheduleEntry(entry: ScheduleEntry) = dao.deleteScheduleEntry(entry)
    suspend fun clearSchedule() = dao.clearSchedule()

    // Tasks
    fun getAllTasks(): Flow<List<ShiftTask>> = dao.getAllTasks()
    suspend fun getTaskByPullCategory(category: String): ShiftTask? = dao.getTaskByPullCategory(category)
    suspend fun insertTask(task: ShiftTask) = dao.insertTask(task)
    suspend fun insertTasks(tasks: List<ShiftTask>) = dao.insertTasks(tasks)
    suspend fun updateTask(task: ShiftTask) = dao.updateTask(task)
    suspend fun deleteJitTasks() = dao.deleteJitTasks()
    suspend fun resetStickyAndPullTasks() = dao.resetStickyAndPullTasks()
    suspend fun pullHighPriorityToMod(fromZone: String) = dao.pullHighPriorityToMod(fromZone)

    // Inventory
    fun getInventoryByCategory(category: String): Flow<List<InventoryItem>> = dao.getInventoryByCategory(category)
    suspend fun insertInventoryItem(item: InventoryItem) = dao.insertInventoryItem(item)
    suspend fun insertInventoryItems(items: List<InventoryItem>) = dao.insertInventoryItems(items)
    suspend fun updateInventoryItem(item: InventoryItem) = dao.updateInventoryItem(item)
    suspend fun deleteInventoryItem(item: InventoryItem) = dao.deleteInventoryItem(item)
    suspend fun clearInventoryByCategory(category: String) = dao.clearInventoryByCategory(category)
    suspend fun resetInventoryCounts() = dao.resetInventoryCounts()

    fun getAllInventoryItems(): Flow<Map<String, List<InventoryItem>>> = combine(
        dao.getInventoryByCategory("RTE"),
        dao.getInventoryByCategory("Prep"),
        dao.getInventoryByCategory("Bread"),
        dao.getInventoryByCategory("Bakery")
    ) { rte, prep, bread, bakery ->
        mapOf("RTE" to rte, "Prep" to prep, "Bread" to bread, "Bakery" to bakery)
    }

    // Table Items
    fun getTableItemsByStation(station: String): Flow<List<TableItem>> = dao.getTableItemsByStation(station)
    suspend fun insertTableItems(items: List<TableItem>) = dao.insertTableItems(items)
    suspend fun updateTableItem(item: TableItem) = dao.updateTableItem(item)
    suspend fun deleteTableItem(item: TableItem) = dao.deleteTableItem(item)
    suspend fun clearTableItemsByStation(station: String) = dao.clearTableItemsByStation(station)
    suspend fun resetTableItemsByStation(station: String) = dao.resetTableItemsByStation(station)

    fun getAllTableItems(): Flow<Map<String, List<TableItem>>> = combine(
        dao.getTableItemsByStation("Starter"),
        dao.getTableItemsByStation("Finisher A"),
        dao.getTableItemsByStation("Finisher B")
    ) { starter, finA, finB ->
        mapOf("Starter" to starter, "Finisher A" to finA, "Finisher B" to finB)
    }

    // Incidents
    fun getAllIncidents(): Flow<List<IncidentLog>> = dao.getAllIncidents()
    suspend fun insertIncident(incident: IncidentLog) = dao.insertIncident(incident)

    // Shift State
    fun getActiveShift(): Flow<ShiftState?> = dao.getActiveShift()
    suspend fun updateShiftState(state: ShiftState) = dao.updateShiftState(state)
    suspend fun clearShiftState() = dao.clearShiftState()

    companion object {
        @Volatile private var INSTANCE: ShiftRepository? = null

        fun getInstance(context: Context): ShiftRepository {
            return INSTANCE ?: synchronized(this) {
                val dao = AkyraDatabase.getDatabase(context).akyraDao()
                ShiftRepository(dao).also { INSTANCE = it }
            }
        }
    }
}
