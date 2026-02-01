package com.filesafe.vault.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.filesafe.vault.viewmodel.AppViewModelFactory
import com.filesafe.vault.viewmodel.ItemDetailsViewModel

@Composable
fun ItemDetailsScreen(
    factory: AppViewModelFactory,
    itemId: String,
    onBack: () -> Unit,
    onView: () -> Unit,
    onAccessKey: () -> Unit
) {
    val viewModel: ItemDetailsViewModel = viewModel(factory = factory)
    val item by viewModel.item.collectAsState()
    val status by viewModel.status.collectAsState()

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        if (uri != null) viewModel.exportToUri(itemId, uri)
    }

    androidx.compose.runtime.LaunchedEffect(itemId) {
        viewModel.load(itemId)
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = item?.displayName ?: "Loading...", style = MaterialTheme.typography.headlineMedium)
        Text(text = item?.mimeType ?: "")
        Button(onClick = onView, enabled = item != null) { Text("Open") }
        Button(onClick = onAccessKey, enabled = item != null) { Text("Access Key") }
        Button(onClick = { viewModel.forgetKey(itemId) }, enabled = item != null) { Text("Forget Key") }
        Button(onClick = {
            exportLauncher.launch(item?.displayName ?: "export.fsf")
        }, enabled = item != null) { Text("Export FSF") }
        Button(onClick = {
            viewModel.delete(itemId)
            onBack()
        }, enabled = item != null) { Text("Delete") }
        status?.let { Text(text = it) }
        Button(onClick = onBack) { Text("Back") }
    }
}
