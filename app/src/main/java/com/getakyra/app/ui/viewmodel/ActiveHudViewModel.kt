package com.getakyra.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.getakyra.app.data.Associate
import com.getakyra.app.data.IncidentLog
import com.getakyra.app.data.InventoryItem
import com.getakyra.app.data.ShiftRepository
import com.getakyra.app.data.ShiftState
import com.getakyra.app.data.ShiftTask
import com.getakyra.app.data.TableItem
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ActiveHudViewModel(private val repository: ShiftRepository) : ViewModel() {

    val shiftState: StateFlow<ShiftState?> = repository.getActiveShift()
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val associates: StateFlow<List<Associate>> = repository.getAllAssociates()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val tasks: StateFlow<Map<String, List<ShiftTask>>> = repository.getAllTasks()
        .map { allTasks ->
            val pending = allTasks.filter { !it.isCompleted }
            val sorted = pending.sortedWith(
                compareBy<ShiftTask> { if (it.assignedTo != null) 0 else 1 }
                    .thenBy { it.assignedTo ?: "" }
                    .thenBy { if (it.assignedTo != null) it.id else 0 }
                    .thenBy { when (it.priority) { "High" -> 1; "Normal" -> 2; "Low" -> 3; else -> 2 } }
                    .thenBy { it.id }
            )
            sorted.groupBy { it.archetype }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    val inventoryByCategory: StateFlow<Map<String, List<InventoryItem>>> =
        repository.getAllInventoryItems()
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    val tableItems: StateFlow<Map<String, List<TableItem>>> =
        repository.getAllTableItems()
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    val schedule = repository.getSchedule()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun validateAssociatePin(pin: String): Associate? =
        associates.value.find { it.pinCode == pin }

    fun completeTask(task: ShiftTask, completedByName: String) {
        viewModelScope.launch {
            repository.updateTask(task.copy(isCompleted = true, completedBy = completedByName))
        }
    }

    fun addTask(taskName: String, archetype: String, priority: String, isSticky: Boolean) {
        viewModelScope.launch {
            repository.insertTask(ShiftTask(taskName = taskName, archetype = archetype, isSticky = isSticky, priority = priority))
        }
    }

    fun reassignTaskToMod(task: ShiftTask) {
        viewModelScope.launch {
            repository.updateTask(task.copy(archetype = "MOD", assignedTo = "MOD"))
        }
    }

    fun delegateTask(task: ShiftTask, assignedTo: String) {
        viewModelScope.launch {
            repository.updateTask(task.copy(assignedTo = assignedTo))
        }
    }

    fun pullHighPriorityToMod(fromZone: String) {
        viewModelScope.launch {
            repository.pullHighPriorityToMod(fromZone)
        }
    }

    fun pullInventory(item: InventoryItem, have: Int, needed: Int) {
        viewModelScope.launch {
            repository.updateInventoryItem(item.copy(amountHave = have, amountNeeded = needed))
        }
    }

    fun toggleInventoryItemPulled(item: InventoryItem, isPulled: Boolean) {
        viewModelScope.launch {
            repository.updateInventoryItem(item.copy(isPulled = isPulled))
        }
    }

    fun completePullTask(pullTask: ShiftTask) {
        viewModelScope.launch {
            repository.updateTask(pullTask.copy(isCompleted = true))
        }
    }

    fun createPullExecutionTask(pullTask: ShiftTask) {
        val category = pullTask.pullCategory ?: return
        viewModelScope.launch {
            repository.updateTask(pullTask.copy(isCompleted = true))
            repository.insertTask(
                ShiftTask(
                    taskName = "$category Pull Execution",
                    archetype = pullTask.archetype,
                    isPullTask = true,
                    pullCategory = category,
                    priority = pullTask.priority,
                    isSticky = false
                )
            )
        }
    }

    fun updateTableItem(item: TableItem) {
        viewModelScope.launch {
            repository.updateTableItem(item)
        }
    }

    fun submitTruckManifest(counts: List<Triple<String, String, String>>, truckTask: ShiftTask) {
        // counts = list of (title, archetype, countStr)
        viewModelScope.launch {
            val newTasks = counts.mapNotNull { (title, arch, countStr) ->
                val count = countStr.toIntOrNull() ?: 0
                if (count > 0) ShiftTask(
                    taskName = "Put Away $title ($count Cubes)",
                    archetype = arch,
                    priority = "High",
                    isTruckTask = true
                ) else null
            }
            if (newTasks.isNotEmpty()) repository.insertTasks(newTasks)
            repository.updateTask(truckTask.copy(isCompleted = true, completedBy = "MOD"))
        }
    }

    fun submitTableFlip(station: String, items: List<TableItem>, flipTask: ShiftTask) {
        viewModelScope.launch {
            val wastedItems = items.filter { !it.isInitialed }
            if (wastedItems.isNotEmpty()) {
                val wasteLog = StringBuilder("Waste Report:\n")
                wastedItems.forEach { wasteLog.append("- ${it.itemName}: ${it.wasteAmount ?: "Unspecified"}\n") }
                repository.insertIncident(
                    IncidentLog(
                        associateId = -1,
                        category = "Midnight Waste: $station",
                        description = wasteLog.toString().trim(),
                        timestampMs = System.currentTimeMillis()
                    )
                )
                repository.insertTask(
                    ShiftTask(
                        taskName = "Record $station Waste",
                        archetype = "MOD",
                        priority = "High",
                        taskDescription = wasteLog.toString().trim(),
                        isSticky = false
                    )
                )
            }
            repository.resetTableItemsByStation(station)
            repository.updateTask(flipTask.copy(isCompleted = true))
        }
    }

    companion object {
        fun factory(repository: ShiftRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    ActiveHudViewModel(repository) as T
            }
    }
}
