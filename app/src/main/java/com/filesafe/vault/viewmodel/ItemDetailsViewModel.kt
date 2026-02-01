package com.filesafe.vault.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.filesafe.vault.crypto.CryptoManager
import com.filesafe.vault.data.VaultItemEntity
import com.filesafe.vault.data.VaultRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class ItemDetailsViewModel(
    private val repository: VaultRepository,
    private val cryptoManager: CryptoManager,
    private val application: Application
) : ViewModel() {
    private val _item = MutableStateFlow<VaultItemEntity?>(null)
    val item: StateFlow<VaultItemEntity?> = _item

    private val _status = MutableStateFlow<String?>(null)
    val status: StateFlow<String?> = _status

    fun load(id: String) {
        viewModelScope.launch {
            _item.value = repository.getById(id)
        }
    }

    fun delete(id: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val entity = repository.getById(id)
                entity?.let {
                    File(it.localPath).delete()
                    repository.deleteById(id)
                    cryptoManager.forgetWrapKey(id)
                }
            }
        }
    }

    fun exportToUri(id: String, uri: Uri) {
        viewModelScope.launch {
            _status.value = "Exporting..."
            runCatching {
                withContext(Dispatchers.IO) {
                    val entity = repository.getById(id) ?: return@withContext
                    application.contentResolver.openOutputStream(uri)?.use { output ->
                        File(entity.localPath).inputStream().use { input ->
                            input.copyTo(output)
                        }
                    }
                }
            }.onSuccess { _status.value = "Exported" }
                .onFailure { _status.value = "Failed: ${it.message}" }
        }
    }

    fun accessKeyFor(id: String): String? {
        val wrapKey = cryptoManager.loadWrapKey(id) ?: return null
        return cryptoManager.buildAccessKey(wrapKey)
    }

    fun forgetKey(id: String) {
        cryptoManager.forgetWrapKey(id)
    }

    fun decryptToTemp(id: String): File? {
        val entity = _item.value ?: return null
        val wrapKey = cryptoManager.loadWrapKey(id) ?: return null
        val cacheDir = File(application.cacheDir, "view/$id").apply { mkdirs() }
        val outFile = File(cacheDir, entity.displayName)
        File(entity.localPath).inputStream().use { input ->
            outFile.outputStream().use { output ->
                cryptoManager.decryptFsf(input, output, wrapKey)
            }
        }
        return outFile
    }
}
