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
import java.util.UUID

class ImportViewModel(
    private val repository: VaultRepository,
    private val cryptoManager: CryptoManager,
    private val application: Application
) : ViewModel() {
    private val _status = MutableStateFlow<String?>(null)
    val status: StateFlow<String?> = _status

    fun importFromUri(uri: Uri, accessKeyText: String) {
        viewModelScope.launch {
            _status.value = "Importing..."
            runCatching {
                withContext(Dispatchers.IO) {
                    val resolver = application.contentResolver
                    val id = UUID.randomUUID().toString()
                    val vaultDir = File(application.filesDir, "vault").apply { mkdirs() }
                    val fsfFile = File(vaultDir, "$id.fsf")

                    resolver.openInputStream(uri)?.use { input ->
                        fsfFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    } ?: throw IllegalStateException("Unable to read FSF")

                    fsfFile.inputStream().use { input ->
                        val header = cryptoManager.parseHeader(input)
                        val meta = cryptoManager.parseHeaderMetadata(header.headerJson)
                        val wrapKey = cryptoManager.buildWrapKeyFromAccess(accessKeyText)
                        cryptoManager.storeWrapKey(id, wrapKey)
                        repository.insert(
                            VaultItemEntity(
                                id = id,
                                displayName = meta.originalName,
                                mimeType = meta.mimeType,
                                createdAt = meta.createdAt,
                                size = meta.originalSize,
                                sha256 = meta.sha256,
                                localPath = fsfFile.absolutePath
                            )
                        )
                    }
                }
            }.onSuccess {
                _status.value = "Imported"
            }.onFailure { err ->
                _status.value = "Failed: ${err.message}"
            }
        }
    }
}
