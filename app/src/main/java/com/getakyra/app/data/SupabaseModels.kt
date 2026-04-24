package com.getakyra.app.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SbAssociate(
    val id: String = "",
    @SerialName("store_id") val storeId: String = "",
    val name: String = "",
    val role: String = "",
    @SerialName("current_archetype") val currentArchetype: String = "Float",
    @SerialName("pin_code") val pinCode: String? = null,
    @SerialName("scheduled_days") val scheduledDays: String = "",
    @SerialName("default_start_time") val defaultStartTime: String = "22:00",
    @SerialName("default_end_time") val defaultEndTime: String = "06:30",
    @SerialName("profile_id") val profileId: String = ""
)

@Serializable
data class SbScheduleEntry(
    val id: String = "",
    @SerialName("store_id") val storeId: String = "",
    @SerialName("associate_id") val associateId: String = "",
    @SerialName("shift_date") val shiftDate: String = "",
    @SerialName("start_time") val startTime: String = "",
    @SerialName("end_time") val endTime: String = ""
)

@Serializable
data class SbTask(
    val id: String = "",
    @SerialName("store_id") val storeId: String = "",
    @SerialName("task_name") val taskName: String = "",
    val archetype: String = "",
    @SerialName("base_points") val basePoints: Int = 10,
    @SerialName("is_completed") val isCompleted: Boolean = false,
    @SerialName("is_pull_task") val isPullTask: Boolean = false,
    @SerialName("pull_category") val pullCategory: String? = null,
    @SerialName("is_sticky") val isSticky: Boolean = false,
    val priority: String = "Normal",
    @SerialName("task_description") val taskDescription: String? = null,
    @SerialName("assigned_to") val assignedTo: String? = null,
    @SerialName("completed_by") val completedBy: String? = null,
    @SerialName("is_truck_task") val isTruckTask: Boolean = false
)

@Serializable
data class SbInventoryItem(
    val id: String = "",
    @SerialName("store_id") val storeId: String = "",
    @SerialName("item_name") val itemName: String = "",
    @SerialName("build_to") val buildTo: String = "",
    val category: String = "",
    @SerialName("amount_have") val amountHave: Int? = null,
    @SerialName("amount_needed") val amountNeeded: Int? = null,
    @SerialName("is_pulled") val isPulled: Boolean = false,
    @SerialName("expected_code_hours") val expectedCodeHours: Int = 24
)

@Serializable
data class SbTableItem(
    val id: String = "",
    @SerialName("store_id") val storeId: String = "",
    @SerialName("item_name") val itemName: String = "",
    val station: String = "",
    @SerialName("is_initialed") val isInitialed: Boolean = true,
    @SerialName("waste_amount") val wasteAmount: String? = null,
    @SerialName("expected_code_hours") val expectedCodeHours: Int = 24
)

@Serializable
data class SbIncident(
    val id: String = "",
    @SerialName("store_id") val storeId: String = "",
    @SerialName("associate_id") val associateId: String = "",
    val description: String = "",
    val category: String = "",
    @SerialName("timestamp_ms") val timestampMs: Long = 0L,
    @SerialName("is_statement_generated") val isStatementGenerated: Boolean = false
)

@Serializable
data class SbProfile(
    val id: String = "",
    @SerialName("org_id") val orgId: String = "",
    @SerialName("current_store_id") val currentStoreId: String = "",
    val eeid: String = "",
    val role: String = "",
    @SerialName("display_name") val displayName: String = "",
    @SerialName("auth_uid") val authUid: String = ""
)

@Serializable
data class SbAuthSession(
    @SerialName("access_token") val accessToken: String = "",
    @SerialName("refresh_token") val refreshToken: String = "",
    @SerialName("user_id") val userId: String = ""
)

@Serializable
data class SbShiftState(
    val id: String = "",
    @SerialName("store_id") val storeId: String = "",
    @SerialName("start_time_ms") val startTimeMs: Long = 0L,
    @SerialName("end_time_ms") val endTimeMs: Long = 0L,
    @SerialName("shift_name") val shiftName: String = "",
    @SerialName("is_open") val isOpen: Boolean = false,
    @SerialName("is_truck_night") val isTruckNight: Boolean = false
)
