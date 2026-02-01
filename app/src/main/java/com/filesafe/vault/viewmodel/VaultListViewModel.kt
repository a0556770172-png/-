package com.filesafe.vault.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.filesafe.vault.data.VaultItemEntity
import com.filesafe.vault.data.VaultRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.MutableStateFlow

class VaultListViewModel(private val repository: VaultRepository) : ViewModel() {
    val query = MutableStateFlow("")

    val items: StateFlow<List<VaultItemEntity>> = query
        .flatMapLatest { text ->
            if (text.isBlank()) repository.getAllSorted() else repository.searchByName(text)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}
