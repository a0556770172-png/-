package com.filesafe.vault.ui.screens

import android.graphics.BitmapFactory
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.filesafe.vault.util.QrUtils
import com.filesafe.vault.viewmodel.AppViewModelFactory
import com.filesafe.vault.viewmodel.ImportViewModel

@Composable
fun ImportFsfScreen(factory: AppViewModelFactory, onDone: () -> Unit) {
    val viewModel: ImportViewModel = viewModel(factory = factory)
    val picked = remember { mutableStateOf<Uri?>(null) }
    val accessKey = remember { mutableStateOf("") }
    val status by viewModel.status.collectAsState()
    val context = LocalContext.current

    val fileLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        picked.value = uri
    }
    val qrImageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            val resolver = context.contentResolver
            resolver.openInputStream(uri)?.use { input ->
                val bitmap = BitmapFactory.decodeStream(input)
                val text = QrUtils.decodeQr(bitmap)
                if (text != null) {
                    accessKey.value = text
                }
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Import FSF", style = MaterialTheme.typography.headlineMedium)
        Button(onClick = { fileLauncher.launch(arrayOf("*/*")) }) { Text("Pick .fsf") }
        picked.value?.let { Text(text = "Selected: $it") }
        OutlinedTextField(
            value = accessKey.value,
            onValueChange = { accessKey.value = it },
            label = { Text("Access key") }
        )
        Button(onClick = { qrImageLauncher.launch("image/*") }) { Text("Scan QR from Image") }
        Button(onClick = {
            val uri = picked.value ?: return@Button
            viewModel.importFromUri(uri, accessKey.value)
        }) {
            Text("Import")
        }
        status?.let { Text(text = it) }
        Button(onClick = onDone) { Text("Back") }
    }
}
