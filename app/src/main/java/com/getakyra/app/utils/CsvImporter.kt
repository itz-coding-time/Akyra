package com.getakyra.app.utils

import com.getakyra.app.data.Associate
import com.getakyra.app.data.InventoryItem
import com.getakyra.app.data.ShiftRepository
import com.getakyra.app.data.ShiftTask
import com.getakyra.app.data.TableItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import java.io.InputStream

object CsvImporter {

    suspend fun importCsvStream(inputStream: InputStream, category: String, repository: ShiftRepository) {
        withContext(Dispatchers.IO) {
            val itemsToInsert = mutableListOf<InventoryItem>()
            try {
                inputStream.bufferedReader().useLines { lines ->
                    lines.drop(1).forEach { line ->
                        val tokens = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)".toRegex())
                        if (tokens.size >= 2) {
                            itemsToInsert.add(InventoryItem(itemName = tokens[0].trim().replace("\"", ""), buildTo = tokens[1].trim(), category = category))
                        }
                    }
                }
                if (itemsToInsert.isNotEmpty()) {
                    repository.clearInventoryByCategory(category)
                    repository.insertInventoryItems(itemsToInsert)

                    if (repository.getTaskByPullCategory(category) == null) {
                        val defaultArchetype = when (category) {
                            "RTE", "Bakery" -> "POS"
                            "Prep", "Bread" -> "Kitchen"
                            else -> "Float"
                        }
                        repository.insertTask(ShiftTask(taskName = "$category Pull List", archetype = defaultArchetype, isPullTask = true, pullCategory = category, basePoints = 50))
                    }
                }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    suspend fun importTaskChecklist(inputStream: InputStream, repository: ShiftRepository) {
        withContext(Dispatchers.IO) {
            val tasksToInsert = mutableListOf<ShiftTask>()
            try {
                inputStream.bufferedReader().useLines { lines ->
                    lines.drop(1).forEach { line ->
                        val tokens = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)".toRegex()).map { it.trim().replace("\"", "") }

                        if (tokens.size >= 2) {
                            val taskName = tokens[0]
                            val archetype = tokens[1]

                            val rawPriority = tokens.getOrNull(2) ?: "2"
                            val safePriority = when (rawPriority) {
                                "1", "High", "high" -> "High"
                                "3", "Low", "low" -> "Low"
                                else -> "Normal"
                            }

                            val rawSticky = tokens.getOrNull(3) ?: "0"
                            val isSticky = rawSticky == "1" || rawSticky.equals("true", ignoreCase = true)

                            tasksToInsert.add(
                                ShiftTask(
                                    taskName = taskName,
                                    archetype = archetype,
                                    priority = safePriority,
                                    isSticky = isSticky,
                                    basePoints = 10,
                                    isPullTask = false
                                )
                            )
                        }
                    }
                }
                if (tasksToInsert.isNotEmpty()) repository.insertTasks(tasksToInsert)
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    suspend fun importTableFlipStream(inputStream: InputStream, station: String, repository: ShiftRepository) {
        withContext(Dispatchers.IO) {
            val itemsToInsert = mutableListOf<TableItem>()
            try {
                inputStream.bufferedReader().useLines { lines ->
                    lines.drop(1).forEach { line ->
                        val itemName = line.trim().replace("\"", "")
                        if (itemName.isNotBlank()) {
                            itemsToInsert.add(TableItem(itemName = itemName, station = station))
                        }
                    }
                }
                if (itemsToInsert.isNotEmpty()) {
                    repository.clearTableItemsByStation(station)
                    repository.insertTableItems(itemsToInsert)

                    val taskName = "Midnight Flip: $station"
                    val tasks = repository.getAllTasks().firstOrNull() ?: emptyList()
                    if (tasks.none { it.taskName == taskName }) {
                        repository.insertTask(ShiftTask(taskName = taskName, archetype = "Kitchen", priority = "High", isSticky = true))
                    }
                }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    suspend fun importRoster(inputStream: InputStream, repository: ShiftRepository) {
        withContext(Dispatchers.IO) {
            val associatesToInsert = mutableListOf<Associate>()
            try {
                inputStream.bufferedReader().useLines { lines ->
                    lines.drop(1).forEach { line ->
                        val tokens = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)".toRegex()).map { it.trim().replace("\"", "") }
                        if (tokens.size >= 3) {
                            associatesToInsert.add(
                                Associate(
                                    name = tokens[0],
                                    role = tokens[1],
                                    currentArchetype = tokens[2],
                                    scheduledDays = tokens.getOrNull(3) ?: "",
                                    defaultStartTime = tokens.getOrNull(4) ?: "22:00",
                                    defaultEndTime = tokens.getOrNull(5) ?: "06:30",
                                    pinCode = tokens.getOrNull(6).takeIf { !it.isNullOrBlank() }
                                )
                            )
                        }
                    }
                }
                if (associatesToInsert.isNotEmpty()) {
                    val existingAssocs = repository.getAllAssociates().firstOrNull() ?: emptyList()
                    associatesToInsert.forEach { newAssoc ->
                        val existingMatch = existingAssocs.find { it.name.equals(newAssoc.name, ignoreCase = true) }
                        if (existingMatch != null) {
                            repository.updateAssociate(newAssoc.copy(id = existingMatch.id))
                        } else {
                            repository.insertAssociate(newAssoc)
                        }
                    }
                }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }
}
