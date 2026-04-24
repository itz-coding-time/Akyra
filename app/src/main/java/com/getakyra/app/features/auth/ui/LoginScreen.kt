package com.getakyra.app.features.auth.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.getakyra.app.data.SbProfile
import com.getakyra.app.ui.viewmodel.AuthViewModel

@Composable
fun LoginScreen(
    viewModel: AuthViewModel,
    onSignedIn: (SbProfile) -> Unit,
    onNeedsOnboarding: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    var eeid by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }

    LaunchedEffect(state) {
        when (state) {
            is AuthViewModel.AuthState.Success ->
                onSignedIn((state as AuthViewModel.AuthState.Success).profile)
            is AuthViewModel.AuthState.RedirectToOnboarding ->
                onNeedsOnboarding()
            else -> {}
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Akyra", style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(32.dp))

        OutlinedTextField(
            value = eeid,
            onValueChange = { eeid = it.filter { c -> c.isDigit() } },
            label = { Text("EEID") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = pin,
            onValueChange = { pin = it.filter { c -> c.isDigit() }.take(6) },
            label = { Text("PIN") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(24.dp))

        when (val s = state) {
            is AuthViewModel.AuthState.Loading -> {
                CircularProgressIndicator()
            }
            is AuthViewModel.AuthState.Error -> {
                Text(s.message, color = MaterialTheme.colorScheme.error)
                Spacer(Modifier.height(8.dp))
                Button(onClick = { viewModel.signIn(eeid, pin) }) { Text("Sign In") }
            }
            else -> {
                Button(
                    onClick = { viewModel.signIn(eeid, pin) },
                    enabled = eeid.isNotBlank() && pin.length >= 4,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Sign In")
                }
            }
        }
    }
}
