package com.getakyra.app.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.getakyra.app.data.Associate
import com.getakyra.app.data.ScheduleEntry
import com.getakyra.app.data.ShiftRepository
import kotlinx.coroutines.launch

class OnboardingViewModel(
    application: Application,
    private val repository: ShiftRepository
) : AndroidViewModel(application) {

    fun completeOnboarding(modName: String, modPin: String, selectedShift: String, onComplete: () -> Unit) {
        viewModelScope.launch {
            val finalPin = if (modPin.isBlank()) "1234" else modPin

            getApplication<Application>()
                .getSharedPreferences("akyra_prefs", Context.MODE_PRIVATE)
                .edit().putString("manager_pin", finalPin).apply()

            repository.insertAssociate(
                Associate(name = modName, role = "Manager", currentArchetype = "MOD", pinCode = finalPin)
            )

            val (start, end) = when (selectedShift) {
                "Morning" -> "06:00" to "16:30"
                "Afternoon" -> "14:00" to "00:30"
                else -> "22:00" to "08:30"
            }
            repository.insertScheduleEntry(
                ScheduleEntry(associateName = modName, startTime = start, endTime = end)
            )

            onComplete()
        }
    }

    companion object {
        fun factoryWithApp(application: Application, repository: ShiftRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T =
                    OnboardingViewModel(application, repository) as T
            }
    }
}
