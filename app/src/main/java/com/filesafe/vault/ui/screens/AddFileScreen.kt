package com.filesafe.vault.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.filesafe.vault.viewmodel.AddFileViewModel
import com.filesafe.vault.viewmodel.AppViewModelFactory

@Composable
fun AddFileScreen(factory: AppViewModelFactory, onDone: () -> Unit) {
    val viewModel: AddFileViewModel = viewModel(factory = factory)
    val adminCode = remember { mutableStateOf("") }
    val picked = remember { mutableStateOf<Uri?>(null) }
    val status by viewModel.status.collectAsState()

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        picked.value = uri
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Add File", style = MaterialTheme.typography.headlineMedium)
        Button(onClick = { launcher.launch(arrayOf("*/*")) }) { Text("Pick File") }
        picked.value?.let { Text(text = "Selected: $it") }
        OutlinedTextField(
            value = adminCode.value,
            onValueChange = { adminCode.value = it },
            label = { Text("Admin code") },
            visualTransformation = PasswordVisualTransformation()
        )
        Button(onClick = {
            val uri = picked.value ?: return@Button
            viewModel.addFromUri(uri, adminCode.value)
        }) {
            Text("Encrypt & Save")
        }
        status?.let { Text(text = it) }
        Button(onClick = onDone) { Text("Back") }
    }
}
