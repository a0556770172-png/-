package com.filesafe.vault.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.filesafe.vault.util.QrUtils
import com.filesafe.vault.viewmodel.AppViewModelFactory
import com.filesafe.vault.viewmodel.ItemDetailsViewModel
import com.filesafe.vault.viewmodel.OnboardingViewModel

@Composable
fun AccessKeyScreen(factory: AppViewModelFactory, itemId: String, onBack: () -> Unit) {
    val itemViewModel: ItemDetailsViewModel = viewModel(factory = factory)
    val onboardingViewModel: OnboardingViewModel = viewModel(factory = factory)
    val verified = remember { mutableStateOf(false) }
    val adminCode = remember { mutableStateOf("") }
    val accessKey = remember { mutableStateOf<String?>(null) }
    val qr = remember { mutableStateOf<android.graphics.Bitmap?>(null) }

    LaunchedEffect(itemId) {
        itemViewModel.load(itemId)
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Access Key", style = MaterialTheme.typography.headlineMedium)
        if (!verified.value) {
            OutlinedTextField(
                value = adminCode.value,
                onValueChange = { adminCode.value = it },
                label = { Text("Admin code") },
                visualTransformation = PasswordVisualTransformation()
            )
            Button(onClick = {
                if (onboardingViewModel.verify(adminCode.value)) {
                    verified.value = true
                    val key = itemViewModel.accessKeyFor(itemId)
                    accessKey.value = key
                    key?.let { qr.value = QrUtils.generateQr(it) }
                }
            }) {
                Text("Unlock")
            }
        } else {
            accessKey.value?.let { Text(text = it) }
            qr.value?.let { bitmap ->
                Image(bitmap = bitmap.asImageBitmap(), contentDescription = "QR")
            }
        }
        Button(onClick = onBack) { Text("Back") }
    }
}
