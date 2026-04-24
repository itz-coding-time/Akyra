package com.getakyra.app.data

import android.util.Log
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.builtin.Email
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class SupabaseRepository {

    private val client = SupabaseClientProvider.client
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val tag = "SupabaseRepository"

    private var currentStoreIdCache: String? = null

    suspend fun currentStoreId(): String {
        currentStoreIdCache?.let { return it }
        val authUid = currentAuthUid() ?: return FALLBACK_DEV_STORE_ID
        val profile = try {
            client.from("profiles").select {
                filter { eq("auth_uid", authUid) }
            }.decodeSingleOrNull<SbProfile>()
        } catch (e: Exception) {
            Log.e(tag, "currentStoreId lookup failed: ${e.message}")
            null
        }
        val storeId = profile?.currentStoreId?.takeIf { it.isNotBlank() }
            ?: FALLBACK_DEV_STORE_ID
        currentStoreIdCache = storeId
        return storeId
    }

    fun clearStoreIdCache() {
        currentStoreIdCache = null
    }

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
                storeId = currentStoreId(),
                name = associate.name,
                role = associate.role,
                currentArchetype = associate.currentArchetype,
                pinCode = associate.pinCode,
                scheduledDays = associate.scheduledDays,
                defaultStartTime = associate.defaultStartTime,
                defaultEndTime = associate.defaultEndTime,
                profileId = associate.profileId ?: ""
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
                storeId = currentStoreId(),
                associateId = currentStoreId(), // TODO: resolve real associate UUID
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
                storeId = currentStoreId(),
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
                storeId = currentStoreId(),
                itemName = item.itemName,
                buildTo = item.buildTo,
                category = item.category,
                amountHave = item.amountHave,
                amountNeeded = item.amountNeeded,
                isPulled = item.isPulled,
                expectedCodeHours = item.expectedCodeHours
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
                storeId = currentStoreId(),
                itemName = item.itemName,
                station = item.station,
                isInitialed = item.isInitialed,
                wasteAmount = item.wasteAmount,
                expectedCodeHours = item.expectedCodeHours
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
                storeId = currentStoreId(),
                associateId = currentStoreId(), // TODO: resolve real associate UUID
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
                storeId = currentStoreId(),
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

    // ── Auth ─────────────────────────────────────────────────────────────────

    suspend fun signInWithEeidAndPin(eeid: String, pin: String): SbProfile? {
        return try {
            // Step 1: Look up profile by EEID to get auth_uid / email
            val profile = fetchProfileByEeid(eeid) ?: run {
                Log.e(tag, "signIn failed: no profile for EEID $eeid")
                return null
            }

            if (profile.authUid.isBlank()) {
                Log.e(tag, "signIn failed: profile has no auth_uid; needs first-time setup")
                return null
            }

            // Step 2: Sign in via Supabase Auth using synthesized email
            val syntheticEmail = "$eeid@akyra.internal"
            client.auth.signInWith(Email) {
                email = syntheticEmail
                password = pin
            }

            profile
        } catch (e: Exception) {
            Log.e(tag, "signInWithEeidAndPin failed: ${e.message}")
            null
        }
    }

    suspend fun registerAuthForProfile(eeid: String, pin: String): SbProfile? {
        return try {
            // Step 1: Find the profile
            val profile = fetchProfileByEeid(eeid) ?: run {
                Log.e(tag, "registerAuth failed: no profile for EEID $eeid")
                return null
            }

            if (profile.authUid.isNotBlank()) {
                Log.e(tag, "registerAuth failed: profile already has auth_uid")
                return null
            }

            // Step 2: Create Supabase Auth user with synthesized email
            val syntheticEmail = "$eeid@akyra.internal"
            client.auth.signUpWith(Email) {
                email = syntheticEmail
                password = pin
            }

            // Step 3: Grab the new auth user's id
            val newAuthUid = client.auth.currentUserOrNull()?.id ?: run {
                Log.e(tag, "registerAuth failed: no current user after signup")
                return null
            }

            // Step 4: Update the profile row with auth_uid
            client.from("profiles").update({ set("auth_uid", newAuthUid) }) {
                filter { eq("eeid", eeid) }
            }

            profile.copy(authUid = newAuthUid)
        } catch (e: Exception) {
            Log.e(tag, "registerAuthForProfile failed: ${e.message}")
            null
        }
    }

    suspend fun signOut() = try {
        client.auth.signOut()
        clearStoreIdCache()
    } catch (e: Exception) {
        Log.e(tag, "signOut failed: ${e.message}")
    }

    fun currentAuthUid(): String? = try {
        client.auth.currentUserOrNull()?.id
    } catch (e: Exception) {
        null
    }

    // ── Profiles ──────────────────────────────────────────────────────────────

    suspend fun fetchProfileByEeid(eeid: String): SbProfile? = try {
        client.from("profiles").select {
            filter { eq("eeid", eeid) }
        }.decodeSingleOrNull()
    } catch (e: Exception) {
        Log.e(tag, "fetchProfileByEeid failed: ${e.message}")
        null
    }

    suspend fun insertProfile(profile: SbProfile): SbProfile? = try {
        client.from("profiles").insert(profile) {
            select()
        }.decodeSingleOrNull()
    } catch (e: Exception) {
        Log.e(tag, "insertProfile failed: ${e.message}")
        null
    }

    companion object {
        private const val FALLBACK_DEV_STORE_ID = "00000000-0000-0000-0000-000000000002"

        @Volatile private var INSTANCE: SupabaseRepository? = null
        fun getInstance(): SupabaseRepository =
            INSTANCE ?: synchronized(this) {
                SupabaseRepository().also { INSTANCE = it }
            }
    }
}
