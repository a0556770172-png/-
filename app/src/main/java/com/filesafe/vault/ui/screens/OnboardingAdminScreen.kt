package com.filesafe.vault.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.filesafe.vault.viewmodel.OnboardingViewModel

@Composable
fun OnboardingAdminScreen(viewModel: OnboardingViewModel, onComplete: () -> Unit) {
    val adminCode = remember { mutableStateOf("") }
    val confirm = remember { mutableStateOf("") }
    val error = viewModel.errorMessage

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Set Admin Code", style = MaterialTheme.typography.headlineMedium)
        OutlinedTextField(
            value = adminCode.value,
            onValueChange = { adminCode.value = it },
            label = { Text("Admin code") },
            visualTransformation = PasswordVisualTransformation()
        )
        OutlinedTextField(
            value = confirm.value,
            onValueChange = { confirm.value = it },
            label = { Text("Confirm admin code") },
            visualTransformation = PasswordVisualTransformation()
        )
        Button(onClick = {
            viewModel.setupAdmin(adminCode.value, confirm.value)
            if (viewModel.hasAdmin.value) onComplete()
        }) {
            Text("Continue")
        }
        error.value?.let { Text(text = it, color = MaterialTheme.colorScheme.error) }
    }
}
