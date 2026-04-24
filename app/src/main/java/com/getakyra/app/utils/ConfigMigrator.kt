package com.getakyra.app.utils

import com.getakyra.app.data.Associate
import com.getakyra.app.data.InventoryItem
import com.getakyra.app.data.SbProfile
import com.getakyra.app.data.ShiftRepository
import com.getakyra.app.data.ShiftTask
import com.getakyra.app.data.SupabaseRepository
import com.getakyra.app.data.TableItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

object ConfigMigrator {

    // ── JSON schema ───────────────────────────────────────────────────────────

    @Serializable
    data class StoreConfig(
        @SerialName("store_id") val storeId: String = "",
        @SerialName("store_name") val storeName: String = "",
        @SerialName("organization") val organization: String = "",
        val associates: List<AssociateConfig> = emptyList(),
        @SerialName("inventory_items") val inventoryItems: List<InventoryItemConfig> = emptyList(),
        @SerialName("table_items") val tableItems: List<TableItemConfig> = emptyList(),
        val tasks: List<TaskConfig> = emptyList()
    )

    @Serializable
    data class AssociateConfig(
        val name: String,
        val eeid: String,
        @SerialName("current_archetype") val currentArchetype: String = "Float",
        @SerialName("scheduled_days") val scheduledDays: String = "",
        @SerialName("default_start_time") val defaultStartTime: String = "22:00",
        @SerialName("default_end_time") val defaultEndTime: String = "06:30"
    )

    @Serializable
    data class InventoryItemConfig(
        @SerialName("item_name") val itemName: String,
        @SerialName("build_to") val buildTo: String,
        val category: String,
        @SerialName("expected_code_hours") val expectedCodeHours: Int = 24
    )

    @Serializable
    data class TableItemConfig(
        @SerialName("item_name") val itemName: String,
        val station: String,
        @SerialName("expected_code_hours") val expectedCodeHours: Int = 24
    )

    @Serializable
    data class TaskConfig(
        @SerialName("task_name") val taskName: String,
        val archetype: String,
        @SerialName("base_points") val basePoints: Int = 10,
        @SerialName("is_pull_task") val isPullTask: Boolean = false,
        @SerialName("pull_category") val pullCategory: String? = null,
        @SerialName("is_sticky") val isSticky: Boolean = false,
        val priority: String = "Normal",
        @SerialName("task_description") val taskDescription: String? = null,
        @SerialName("is_truck_task") val isTruckTask: Boolean = false
    )

    // ── Migration ─────────────────────────────────────────────────────────────

    suspend fun migrate(
        config: StoreConfig,
        repository: ShiftRepository,
        supabase: SupabaseRepository,
        storeId: String
    ) {
        withContext(Dispatchers.IO) {
            migrateAssociates(config.associates, repository, supabase, storeId)
            migrateInventory(config.inventoryItems, repository)
            migrateTableItems(config.tableItems, repository)
            migrateTasks(config.tasks, repository)
        }
    }

    private suspend fun migrateAssociates(
        configs: List<AssociateConfig>,
        repository: ShiftRepository,
        supabase: SupabaseRepository,
        storeId: String
    ) {
        if (configs.isEmpty()) return
        val existingLocally = repository.getAllAssociates().firstOrNull() ?: emptyList()

        configs.forEach { cfg ->
            // 1. Look up the profile in Supabase by EEID
            var profile = supabase.fetchProfileByEeid(cfg.eeid)

            // 2. If no profile exists, create one with defaults
            if (profile == null) {
                val newProfile = SbProfile(
                    eeid = cfg.eeid,
                    displayName = cfg.name,
                    currentStoreId = storeId,
                    role = "Crew"
                )
                profile = supabase.insertProfile(newProfile)
            }

            val resolvedName = profile?.displayName ?: cfg.name
            val resolvedRole = profile?.role ?: "Crew"
            val resolvedProfileId = profile?.id ?: ""

            // 3. Check if this associate already exists locally (match by name)
            val existingMatch = existingLocally.find { it.name.equals(resolvedName, ignoreCase = true) }

            val associate = Associate(
                id = existingMatch?.id ?: 0,
                name = resolvedName,
                role = resolvedRole,
                currentArchetype = cfg.currentArchetype,
                pinCode = existingMatch?.pinCode ?: "0000",
                scheduledDays = cfg.scheduledDays,
                defaultStartTime = cfg.defaultStartTime,
                defaultEndTime = cfg.defaultEndTime,
                profileId = resolvedProfileId
            )

            // 4. Update locally if already exists, insert if new
            if (existingMatch != null) repository.updateAssociate(associate)
            else repository.insertAssociate(associate)
        }
    }

    private suspend fun migrateInventory(configs: List<InventoryItemConfig>, repository: ShiftRepository) {
        if (configs.isEmpty()) return
        configs.groupBy { it.category }.forEach { (category, items) ->
            repository.clearInventoryByCategory(category)
            repository.insertInventoryItems(items.map { cfg ->
                InventoryItem(
                    itemName = cfg.itemName,
                    buildTo = cfg.buildTo,
                    category = cfg.category,
                    expectedCodeHours = cfg.expectedCodeHours
                )
            })
            if (repository.getTaskByPullCategory(category) == null) {
                val archetype = when (category) {
                    "RTE", "Bakery" -> "POS"
                    "Prep", "Bread" -> "Kitchen"
                    else -> "Float"
                }
                repository.insertTask(
                    ShiftTask(taskName = "$category Pull List", archetype = archetype, isPullTask = true, pullCategory = category, basePoints = 50)
                )
            }
        }
    }

    private suspend fun migrateTableItems(configs: List<TableItemConfig>, repository: ShiftRepository) {
        if (configs.isEmpty()) return
        configs.groupBy { it.station }.forEach { (station, items) ->
            repository.clearTableItemsByStation(station)
            repository.insertTableItems(items.map { cfg ->
                TableItem(itemName = cfg.itemName, station = cfg.station, expectedCodeHours = cfg.expectedCodeHours)
            })
            val flipTaskName = "Midnight Flip: $station"
            val allTasks = repository.getAllTasks().firstOrNull() ?: emptyList()
            if (allTasks.none { it.taskName == flipTaskName }) {
                repository.insertTask(
                    ShiftTask(taskName = flipTaskName, archetype = "Kitchen", priority = "High", isSticky = true)
                )
            }
        }
    }

    private suspend fun migrateTasks(configs: List<TaskConfig>, repository: ShiftRepository) {
        if (configs.isEmpty()) return
        repository.insertTasks(configs.map { cfg ->
            ShiftTask(
                taskName = cfg.taskName,
                archetype = cfg.archetype,
                basePoints = cfg.basePoints,
                isPullTask = cfg.isPullTask,
                pullCategory = cfg.pullCategory,
                isSticky = cfg.isSticky,
                priority = cfg.priority,
                taskDescription = cfg.taskDescription,
                isTruckTask = cfg.isTruckTask
            )
        })
    }
}
