package com.getakyra.app.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.getakyra.app.data.ShiftRepository
import com.getakyra.app.data.SupabaseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class OnboardingViewModel(
    application: Application,
    private val repository: ShiftRepository,
    private val supabase: SupabaseRepository
) : AndroidViewModel(application) {

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun completeOnboarding(eeid: String, pin: String, onComplete: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            val finalPin = if (pin.isBlank()) "1234" else pin

            getApplication<Application>()
                .getSharedPreferences("akyra_prefs", Context.MODE_PRIVATE)
                .edit().putString("manager_pin", finalPin).apply()

            val registered = supabase.registerAuthForProfile(eeid, finalPin)
            if (registered != null) {
                val signedIn = supabase.signInWithEeidAndPin(eeid, finalPin)
                if (signedIn != null) {
                    _isLoading.value = false
                    onComplete()
                } else {
                    _error.value = "Registration succeeded but sign-in failed. Please try logging in."
                    _isLoading.value = false
                }
            } else {
                _error.value = "Could not complete setup. Check your EEID and try again."
                _isLoading.value = false
            }
        }
    }

    fun clearError() {
        _error.value = null
    }

    companion object {
        fun factoryWithApp(
            application: Application,
            repository: ShiftRepository,
            supabase: SupabaseRepository
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T =
                    OnboardingViewModel(application, repository, supabase) as T
            }
    }
}
