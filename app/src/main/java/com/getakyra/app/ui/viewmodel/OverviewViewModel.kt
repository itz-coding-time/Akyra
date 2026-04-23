package com.getakyra.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.getakyra.app.data.Associate
import com.getakyra.app.data.ScheduleEntry
import com.getakyra.app.data.ShiftRepository
import com.getakyra.app.domain.PacingEngine
import com.getakyra.app.domain.PacingResult
import com.getakyra.app.domain.PacingStatus
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class OverviewViewModel(private val repository: ShiftRepository) : ViewModel() {

    private val _currentTime = MutableStateFlow(System.currentTimeMillis())

    private val tasksFlow = repository.getAllTasks()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    private val associatesFlow = repository.getAllAssociates()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    private val scheduleFlow = repository.getSchedule()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    private val shiftFlow = repository.getActiveShift()
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val isShiftActive: StateFlow<Boolean> = shiftFlow
        .map { it?.isOpen == true }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val scheduleEntries: StateFlow<List<ScheduleEntry>> = scheduleFlow

    val associates: StateFlow<List<Associate>> = associatesFlow

    val activeMods: StateFlow<List<Associate>> = associatesFlow
        .map { list -> list.filter { it.currentArchetype == "MOD" } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val globalPacing: StateFlow<PacingResult> = combine(
        tasksFlow, associatesFlow, scheduleFlow, shiftFlow, _currentTime
    ) { tasks, associates, schedule, shift, time ->
        val fallback = if (shift?.isOpen == true && shift.endTimeMs > shift.startTimeMs) {
            PacingEngine.calculateGlobalFallbackPct(shift.startTimeMs, shift.endTimeMs, time)
        } else 0f
        val smartTimePct = PacingEngine.calculateSmartGlobalTimePct(associates, schedule, fallback)
        PacingEngine.calculatePacing(tasks, smartTimePct)
    }.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        PacingResult(PacingStatus.NO_TASKS, 1f, 0f, 0, 0)
    )

    val zonePacing: StateFlow<Map<String, PacingResult>> = combine(
        tasksFlow, associatesFlow, scheduleFlow, shiftFlow, _currentTime
    ) { tasks, associates, schedule, shift, time ->
        val fallback = if (shift?.isOpen == true && shift.endTimeMs > shift.startTimeMs) {
            PacingEngine.calculateGlobalFallbackPct(shift.startTimeMs, shift.endTimeMs, time)
        } else 0f
        val smartGlobal = PacingEngine.calculateSmartGlobalTimePct(associates, schedule, fallback)

        listOf("Kitchen", "POS", "Float").associateWith { archetype ->
            val zoneTasks = tasks.filter { it.archetype == archetype }
            val zoneTimePct = PacingEngine.calculateZoneTimePct(archetype, associates, schedule, smartGlobal)
            PacingEngine.calculatePacing(zoneTasks, zoneTimePct)
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    init {
        viewModelScope.launch {
            while (true) {
                delay(60_000L)
                _currentTime.value = System.currentTimeMillis()
            }
        }
    }

    companion object {
        fun factory(repository: ShiftRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    OverviewViewModel(repository) as T
            }
    }
}
