package com.getakyra.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.getakyra.app.data.SbProfile
import com.getakyra.app.data.SupabaseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel(
    private val supabase: SupabaseRepository
) : ViewModel() {

    sealed class AuthState {
        object Idle : AuthState()
        object Loading : AuthState()
        object RedirectToOnboarding : AuthState()
        data class Success(val profile: SbProfile) : AuthState()
        data class Error(val message: String) : AuthState()
    }

    private val _state = MutableStateFlow<AuthState>(AuthState.Idle)
    val state: StateFlow<AuthState> = _state.asStateFlow()

    fun signIn(eeid: String, pin: String) {
        viewModelScope.launch {
            _state.value = AuthState.Loading
            val profile = supabase.fetchProfileByEeid(eeid)
            when {
                profile == null -> {
                    _state.value = AuthState.Error("No account found for EEID $eeid")
                }
                profile.authUid.isBlank() -> {
                    _state.value = AuthState.RedirectToOnboarding
                }
                else -> {
                    val signedIn = supabase.signInWithEeidAndPin(eeid, pin)
                    _state.value = if (signedIn != null) AuthState.Success(signedIn)
                    else AuthState.Error("Invalid PIN")
                }
            }
        }
    }

    fun completeRegistration(eeid: String, pin: String) {
        viewModelScope.launch {
            _state.value = AuthState.Loading
            val registered = supabase.registerAuthForProfile(eeid, pin)
            if (registered != null) {
                val signedIn = supabase.signInWithEeidAndPin(eeid, pin)
                _state.value = if (signedIn != null) AuthState.Success(signedIn)
                else AuthState.Error("Registration succeeded but sign-in failed")
            } else {
                _state.value = AuthState.Error("Could not complete first-time setup")
            }
        }
    }

    fun reset() {
        _state.value = AuthState.Idle
    }

    companion object {
        fun factory(supabase: SupabaseRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    AuthViewModel(supabase) as T
            }
    }
}
