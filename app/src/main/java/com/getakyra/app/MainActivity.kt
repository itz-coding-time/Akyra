package com.getakyra.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.getakyra.app.data.SbProfile
import com.getakyra.app.data.SupabaseRepository
import com.getakyra.app.features.auth.ui.LoginScreen
import com.getakyra.app.ui.theme.AkyraTheme
import com.getakyra.app.ui.viewmodel.AuthViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val repository = (application as AkyraApp).repository
        val supabase = SupabaseRepository.getInstance()

        setContent {
            val authViewModel: AuthViewModel = viewModel(factory = AuthViewModel.factory(supabase))
            var signedInProfile by remember { mutableStateOf<SbProfile?>(null) }
            var skipToOnboarding by remember { mutableStateOf(false) }

            AkyraTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (signedInProfile == null && !skipToOnboarding) {
                        LoginScreen(
                            viewModel = authViewModel,
                            onSignedIn = { signedInProfile = it },
                            onNeedsOnboarding = { skipToOnboarding = true }
                        )
                    } else {
                        AkyraAppNavigation(repository = repository)
                    }
                }
            }
        }
    }
}
