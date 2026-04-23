package com.getakyra.app.features.hud.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.getakyra.app.domain.PacingResult
import com.getakyra.app.domain.PacingStatus
import com.getakyra.app.ui.viewmodel.OverviewViewModel

@Composable
fun OverviewScreen(viewModel: OverviewViewModel) {
    val isShiftActive by viewModel.isShiftActive.collectAsState()
    val globalPacing by viewModel.globalPacing.collectAsState()
    val zonePacing by viewModel.zonePacing.collectAsState()
    val activeMods by viewModel.activeMods.collectAsState()
    val scheduleEntries by viewModel.scheduleEntries.collectAsState()
    val associates by viewModel.associates.collectAsState()

    // Count how many associates have matching schedule entries (for subtext)
    val scheduledAssocCount = associates.count { assoc ->
        scheduleEntries.any { it.associateName.equals(assoc.name, ignoreCase = true) }
    }

    Column(modifier = Modifier.padding(16.dp).fillMaxSize()) {
        Text(
            "Shift Overview",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(16.dp))

        if (!isShiftActive) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
            ) {
                Text(
                    "⚠️ NO ACTIVE SHIFT. Pacing calculation disabled.",
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    fontWeight = FontWeight.Bold
                )
            }
        } else {
            val globalSubtext = if (scheduledAssocCount > 0)
                "Averaged across $scheduledAssocCount active schedules"
            else
                "Based on MOD Shift Duration"

            PacingCardUI(
                title = "GLOBAL PROGRESS",
                pacingResult = globalPacing,
                isGlobal = true,
                subtext = globalSubtext
            )

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 8.dp)
            ) {
                Text("Active MOD(s): ", style = MaterialTheme.typography.labelSmall)
                if (activeMods.isEmpty()) {
                    Text(
                        "Unassigned",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                activeMods.forEach { mod ->
                    Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(14.dp))
                    Text(
                        mod.name,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text("Zone Pacing", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                val archetypes = listOf("Kitchen", "POS", "Float")
                items(archetypes) { archetype ->
                    val result = zonePacing[archetype]
                    val zoneAssocs = associates.filter { it.currentArchetype == archetype }
                    val hasZoneSchedule = zoneAssocs.any { assoc ->
                        scheduleEntries.any { it.associateName.equals(assoc.name, ignoreCase = true) }
                    }
                    val zoneSubtext = if (hasZoneSchedule) {
                        "Tracking pacing for ${zoneAssocs.joinToString { it.name }}'s schedule"
                    } else {
                        "Using global pacing (No schedule found)"
                    }

                    if (result != null) {
                        PacingCardUI(
                            title = "${archetype.uppercase()} ZONE",
                            pacingResult = result,
                            isGlobal = false,
                            subtext = zoneSubtext
                        )
                    }
                }

                if (scheduleEntries.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "Scheduled Shift",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                scheduleEntries.forEach { entry ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            entry.associateName,
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        Text(
                                            "${entry.startTime} - ${entry.endTime}",
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PacingCardUI(
    title: String,
    pacingResult: PacingResult,
    isGlobal: Boolean,
    subtext: String
) {
    val (pacingText, pacingColor) = when (pacingResult.status) {
        PacingStatus.NO_TASKS -> "NO TASKS" to Color.Gray
        PacingStatus.BEHIND_PACE -> "BEHIND PACE" to Color(0xFFE53935)
        PacingStatus.ON_TRACK -> "ON TRACK" to Color(0xFF4CAF50)
        PacingStatus.AHEAD_OF_PACE -> "AHEAD OF PACE" to Color(0xFF2196F3)
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isGlobal) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(if (isGlobal) 0.dp else 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(title, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Text(
                        subtext,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isGlobal)
                            MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        else
                            Color.Gray
                    )
                }
                Surface(color = pacingColor.copy(alpha = 0.2f), shape = MaterialTheme.shapes.small) {
                    Text(
                        pacingText,
                        style = MaterialTheme.typography.labelSmall,
                        color = pacingColor,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        fontWeight = FontWeight.Black
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Tasks: ${pacingResult.completed}/${pacingResult.total}", style = MaterialTheme.typography.bodySmall)
                Text(
                    "${(pacingResult.taskPct * 100).toInt()}%",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold
                )
            }
            LinearProgressIndicator(
                progress = { pacingResult.taskPct },
                modifier = Modifier.fillMaxWidth().height(8.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Expected Time Target", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                Text(
                    "${(pacingResult.timePct * 100).toInt()}%",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray
                )
            }
            LinearProgressIndicator(
                progress = { pacingResult.timePct },
                modifier = Modifier.fillMaxWidth().height(4.dp),
                color = Color.Gray,
                trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
            )
        }
    }
}
