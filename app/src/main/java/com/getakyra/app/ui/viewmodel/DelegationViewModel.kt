package com.getakyra.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.getakyra.app.data.Associate
import com.getakyra.app.data.ShiftRepository
import com.getakyra.app.data.ShiftTask
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DelegationViewModel(private val repository: ShiftRepository) : ViewModel() {

    val associates: StateFlow<List<Associate>> = repository.getAllAssociates()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val tasksByArchetype: StateFlow<Map<String, List<ShiftTask>>> = repository.getAllTasks()
        .map { tasks -> tasks.groupBy { it.archetype } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    val shiftState = repository.getActiveShift()
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val schedule = repository.getSchedule()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun reassignAssociate(associate: Associate, newArchetype: String) {
        viewModelScope.launch {
            repository.updateAssociate(associate.copy(currentArchetype = newArchetype))
        }
    }

    fun reassignTask(task: ShiftTask, newArchetype: String) {
        viewModelScope.launch {
            repository.updateTask(task.copy(archetype = newArchetype))
        }
    }

    fun completeTaskAsMod(task: ShiftTask) {
        viewModelScope.launch {
            repository.updateTask(task.copy(isCompleted = true, completedBy = "MOD"))
        }
    }

    fun completeTaskAsAssociate(task: ShiftTask, associateName: String) {
        viewModelScope.launch {
            repository.updateTask(task.copy(isCompleted = true, completedBy = associateName))
        }
    }

    fun pullHighPriorityToMod(fromZone: String) {
        viewModelScope.launch {
            repository.pullHighPriorityToMod(fromZone)
        }
    }

    companion object {
        fun factory(repository: ShiftRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    DelegationViewModel(repository) as T
            }
    }
}
