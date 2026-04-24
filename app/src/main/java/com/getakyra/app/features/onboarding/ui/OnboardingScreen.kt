package com.getakyra.app.features.onboarding.ui

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.getakyra.app.ui.viewmodel.OnboardingViewModel

@Composable
fun OnboardingScreen(viewModel: OnboardingViewModel, onComplete: () -> Unit) {
    var step by remember { mutableIntStateOf(0) }
    var eeid by remember { mutableStateOf("") }
    var modPin by remember { mutableStateOf("") }

    val error by viewModel.error.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AnimatedContent(
                targetState = step,
                transitionSpec = { slideInHorizontally { width -> width } + fadeIn() togetherWith slideOutHorizontally { width -> -width } + fadeOut() },
                label = "Onboarding"
            ) { targetStep ->
                when (targetStep) {
                    0 -> {
                        // STEP 1: WELCOME
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.RocketLaunch, contentDescription = null, modifier = Modifier.size(80.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.height(24.dp))
                            Text("Akyra", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                            Text("Your shift, made easier.", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(32.dp))
                            Text("An Android Operations Engine\nDesigned by CaseSmarts LLC.", style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.secondary)
                            Spacer(Modifier.height(48.dp))
                            Button(onClick = { step++ }, modifier = Modifier.fillMaxWidth()) { Text("Begin Setup") }
                        }
                    }
                    1 -> {
                        // STEP 2: EEID
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                            Text("Command Authentication", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(16.dp))
                            Text("Enter your Employee ID to register this device.", style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
                            Spacer(Modifier.height(24.dp))
                            OutlinedTextField(
                                value = eeid,
                                onValueChange = { eeid = it.filter { c -> c.isDigit() } },
                                label = { Text("Employee ID (EEID)") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )
                            Spacer(Modifier.height(32.dp))
                            Button(onClick = { if (eeid.isNotBlank()) step++ }, modifier = Modifier.fillMaxWidth(), enabled = eeid.isNotBlank()) { Text("Next") }
                        }
                    }
                    2 -> {
                        // STEP 3: PIN
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                            Text("Gateway Security", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(16.dp))
                            Text("Set a PIN to lock the Manager Settings.", style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
                            Text("If you ever forget this, the master fallback is always '1234'.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 8.dp))
                            Spacer(Modifier.height(24.dp))
                            OutlinedTextField(
                                value = modPin,
                                onValueChange = { if (it.length <= 8) modPin = it },
                                label = { Text("Enter 4-Digit PIN") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                visualTransformation = PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword)
                            )
                            Spacer(Modifier.height(32.dp))
                            Button(onClick = { step++ }, modifier = Modifier.fillMaxWidth()) {
                                Text(if (modPin.isBlank()) "Skip (Use 1234)" else "Set PIN & Continue")
                            }
                        }
                    }
                    3 -> {
                        // STEP 4: FINISH
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(80.dp), tint = Color(0xFF4CAF50))
                            Spacer(Modifier.height(24.dp))
                            Text("Almost There", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(16.dp))
                            Text("Tap below to create your account and launch Akyra.", style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
                            Spacer(Modifier.height(48.dp))

                            if (isLoading) {
                                CircularProgressIndicator()
                            } else {
                                error?.let { msg ->
                                    Text(msg, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
                                    Spacer(Modifier.height(16.dp))
                                }
                                Button(
                                    onClick = {
                                        viewModel.clearError()
                                        viewModel.completeOnboarding(eeid, modPin, onComplete)
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) { Text("Launch Akyra") }
                            }
                        }
                    }
                }
            }
        }
    }
}
