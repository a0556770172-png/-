package com.filesafe.vault.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.filesafe.vault.viewmodel.AppViewModelFactory
import com.filesafe.vault.viewmodel.VaultListViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultListScreen(
    factory: AppViewModelFactory,
    onAdd: () -> Unit,
    onImport: () -> Unit,
    onOpen: (String) -> Unit
) {
    val viewModel: VaultListViewModel = viewModel(factory = factory)
    val items by viewModel.items.collectAsState()

    Scaffold { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Text(text = "Vault", style = MaterialTheme.typography.headlineLarge)
            OutlinedTextField(
                value = viewModel.query.collectAsState().value,
                onValueChange = { viewModel.query.value = it },
                label = { Text("Search") },
                modifier = Modifier.padding(vertical = 12.dp)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onAdd) { Text("Add") }
                Button(onClick = onImport) { Text("Import") }
            }
            LazyColumn(modifier = Modifier.padding(top = 16.dp)) {
                items(items) { item ->
                    Card(
                        modifier = Modifier
                            .padding(vertical = 6.dp)
                            .clickable { onOpen(item.id) }
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(text = item.displayName, style = MaterialTheme.typography.titleMedium)
                            Text(text = item.mimeType, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}
