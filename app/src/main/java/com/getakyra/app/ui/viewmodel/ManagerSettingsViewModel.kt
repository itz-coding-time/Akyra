package com.getakyra.app.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.getakyra.app.data.Associate
import com.getakyra.app.data.IncidentLog
import com.getakyra.app.data.InventoryItem
import com.getakyra.app.data.ScheduleEntry
import com.getakyra.app.data.ShiftRepository
import com.getakyra.app.data.ShiftTask
import com.getakyra.app.data.TableItem
import com.getakyra.app.domain.ShiftLifecycleManager
import com.getakyra.app.features.docupro.StatementGenerator
import com.getakyra.app.utils.CsvImporter
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.InputStream

class ManagerSettingsViewModel(
    application: Application,
    private val repository: ShiftRepository
) : AndroidViewModel(application) {

    private val lifecycleManager = ShiftLifecycleManager(repository)
    private var statementTemplate: String = ""

    val associates: StateFlow<List<Associate>> = repository.getAllAssociates()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val schedule: StateFlow<List<ScheduleEntry>> = repository.getSchedule()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val incidents: StateFlow<List<IncidentLog>> = repository.getAllIncidents()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val inventoryItems: StateFlow<Map<String, List<InventoryItem>>> =
        repository.getAllInventoryItems()
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    val tableItems: StateFlow<Map<String, List<TableItem>>> =
        repository.getAllTableItems()
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    val shiftState = repository.getActiveShift()
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val allTasks: StateFlow<List<ShiftTask>> = repository.getAllTasks()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    init {
        statementTemplate = StatementGenerator.loadTemplate(application)
    }

    fun validateManagerPin(pin: String): Boolean {
        val prefs = getApplication<Application>()
            .getSharedPreferences("akyra_prefs", Context.MODE_PRIVATE)
        val customPin = prefs.getString("manager_pin", MASTER_PIN) ?: MASTER_PIN
        return pin == customPin || pin == MASTER_PIN
    }

    // --- Shift Lifecycle ---

    fun openShift(
        shiftName: String,
        isTruckNight: Boolean,
        startMs: Long,
        endMs: Long,
        modName: String,
        startStr: String,
        endStr: String
    ) {
        viewModelScope.launch {
            repository.clearSchedule()

            // Add MOD's schedule entry
            repository.insertScheduleEntry(ScheduleEntry(associateName = modName, startTime = startStr, endTime = endStr))

            // Auto-draft associates scheduled for today
            val today = java.text.SimpleDateFormat("EEE", java.util.Locale.getDefault()).format(java.util.Date())
            associates.value.forEach { assoc ->
                if (assoc.currentArchetype != "MOD" && assoc.scheduledDays.contains(today, ignoreCase = true)) {
                    repository.insertScheduleEntry(
                        ScheduleEntry(associateName = assoc.name, startTime = assoc.defaultStartTime, endTime = assoc.defaultEndTime)
                    )
                }
            }

            lifecycleManager.openShift(shiftName, isTruckNight, startMs, endMs)
        }
    }

    fun closeShift() {
        viewModelScope.launch {
            lifecycleManager.closeShift(allTasks.value, associates.value)
        }
    }

    // --- Associates ---

    fun addAssociate(name: String, role: String, archetype: String, pin: String?) {
        viewModelScope.launch {
            repository.insertAssociate(Associate(name = name, role = role, currentArchetype = archetype, pinCode = pin))
        }
    }

    fun insertAssociate(associate: Associate) {
        viewModelScope.launch { repository.insertAssociate(associate) }
    }

    fun updateAssociate(associate: Associate) {
        viewModelScope.launch { repository.updateAssociate(associate) }
    }

    fun deleteAssociate(associate: Associate) {
        viewModelScope.launch { repository.deleteAssociate(associate) }
    }

    // --- Schedule ---

    fun upsertScheduleEntry(name: String, start: String, end: String) {
        viewModelScope.launch {
            repository.insertScheduleEntry(ScheduleEntry(associateName = name, startTime = start, endTime = end))
        }
    }

    fun insertScheduleEntry(entry: ScheduleEntry) {
        viewModelScope.launch { repository.insertScheduleEntry(entry) }
    }

    fun deleteScheduleEntry(entry: ScheduleEntry) {
        viewModelScope.launch { repository.deleteScheduleEntry(entry) }
    }

    // --- Inventory ---

    fun insertInventoryItem(item: InventoryItem) {
        viewModelScope.launch { repository.insertInventoryItem(item) }
    }

    fun updateInventoryItem(item: InventoryItem) {
        viewModelScope.launch { repository.updateInventoryItem(item) }
    }

    fun deleteInventoryItem(item: InventoryItem) {
        viewModelScope.launch { repository.deleteInventoryItem(item) }
    }

    fun ensurePullTaskExists(category: String) {
        viewModelScope.launch {
            if (repository.getTaskByPullCategory(category) == null) {
                val defaultArchetype = when (category) {
                    "RTE", "Bakery" -> "POS"
                    "Prep", "Bread" -> "Kitchen"
                    else -> "Float"
                }
                repository.insertTask(ShiftTask(taskName = "$category Pull List", archetype = defaultArchetype, isPullTask = true, pullCategory = category, basePoints = 50))
            }
        }
    }

    // --- Table Items ---

    fun insertTableItems(items: List<TableItem>) {
        viewModelScope.launch { repository.insertTableItems(items) }
    }

    fun updateTableItem(item: TableItem) {
        viewModelScope.launch { repository.updateTableItem(item) }
    }

    fun deleteTableItem(item: TableItem) {
        viewModelScope.launch { repository.deleteTableItem(item) }
    }

    fun ensureMidnightFlipTaskExists(station: String) {
        viewModelScope.launch {
            val taskName = "Midnight Flip: $station"
            val existing = allTasks.value
            if (existing.none { it.taskName == taskName }) {
                repository.insertTask(ShiftTask(taskName = taskName, archetype = "Kitchen", priority = "High", isSticky = true))
            }
        }
    }

    // --- Incidents & Statements ---

    fun logIncident(associateId: Int, category: String, description: String) {
        viewModelScope.launch {
            repository.insertIncident(
                IncidentLog(associateId = associateId, category = category, description = description, timestampMs = System.currentTimeMillis())
            )
        }
    }

    fun markStatementGenerated(incident: IncidentLog) {
        viewModelScope.launch {
            repository.insertIncident(incident.copy(isStatementGenerated = true))
        }
    }

    fun generateStatement(associate: Associate, incident: IncidentLog): String {
        return StatementGenerator.generate(statementTemplate, associate, incident)
    }

    // --- CSV Operations ---

    suspend fun importInventoryCsv(inputStream: InputStream, category: String) {
        CsvImporter.importCsvStream(inputStream, category, repository)
    }

    suspend fun importTableFlipCsv(inputStream: InputStream, station: String) {
        CsvImporter.importTableFlipStream(inputStream, station, repository)
    }

    suspend fun importTaskChecklist(inputStream: InputStream) {
        CsvImporter.importTaskChecklist(inputStream, repository)
    }

    suspend fun importRoster(inputStream: InputStream) {
        CsvImporter.importRoster(inputStream, repository)
    }

    companion object {
        // TODO: Replace with BuildConfig or env var before production
        private const val MASTER_PIN = "1234"

        fun factory(repository: ShiftRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                    val app = repository::class.java.classLoader?.let {
                        // Factory is called with the Application context from ViewModelProvider
                        throw IllegalStateException("Use AndroidViewModelFactory or provide Application")
                    }
                    throw IllegalStateException("Use factoryWithApp")
                }
            }

        fun factoryWithApp(application: Application, repository: ShiftRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T =
                    ManagerSettingsViewModel(application, repository) as T
            }
    }
}
