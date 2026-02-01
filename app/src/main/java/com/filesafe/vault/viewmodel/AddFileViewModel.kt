package com.filesafe.vault.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.filesafe.vault.crypto.CryptoManager
import com.filesafe.vault.data.VaultItemEntity
import com.filesafe.vault.data.VaultRepository
import com.filesafe.vault.util.FileUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

class AddFileViewModel(
    private val repository: VaultRepository,
    private val cryptoManager: CryptoManager,
    private val application: Application
) : ViewModel() {
    private val _status = MutableStateFlow<String?>(null)
    val status: StateFlow<String?> = _status

    fun addFromUri(uri: Uri, adminCode: String) {
        viewModelScope.launch {
            _status.value = "Encrypting..."
            runCatching {
                withContext(Dispatchers.IO) {
                    if (!cryptoManager.verifyAdmin(adminCode.toCharArray())) {
                        throw IllegalArgumentException("Invalid admin code")
                    }
                    val resolver = application.contentResolver
                    val name = resolver.getType(uri) ?: "application/octet-stream"
                    val displayName = resolver.query(uri, null, null, null, null)?.use { cursor ->
                        val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                        if (cursor.moveToFirst() && nameIndex >= 0) cursor.getString(nameIndex) else "file"
                    } ?: "file"

                    val shaAndSize = resolver.openInputStream(uri)?.use { input ->
                        FileUtils.sha256AndSize(input)
                    } ?: throw IllegalStateException("Unable to read input")

                    val id = UUID.randomUUID().toString()
                    val vaultDir = File(application.filesDir, "vault").apply { mkdirs() }
                    val fsfFile = File(vaultDir, "$id.fsf")

                    resolver.openInputStream(uri)?.use { input ->
                        fsfFile.outputStream().use { output ->
                            val master = cryptoManager.deriveMaster(adminCode.toCharArray())
                            val metadata = cryptoManager.encryptToFsf(
                                input = input,
                                output = output,
                                displayName = displayName,
                                mimeType = name,
                                createdAt = System.currentTimeMillis(),
                                originalSize = shaAndSize.second,
                                sha256 = shaAndSize.first,
                                master = master
                            )
                            cryptoManager.storeWrapKey(id, metadata.wrapKey)
                        }
                    }

                    val entity = VaultItemEntity(
                        id = id,
                        displayName = displayName,
                        mimeType = name,
                        createdAt = System.currentTimeMillis(),
                        size = shaAndSize.second,
                        sha256 = shaAndSize.first,
                        localPath = fsfFile.absolutePath
                    )
                    repository.insert(entity)
                }
            }.onSuccess {
                _status.value = "Added"
            }.onFailure { err ->
                _status.value = "Failed: ${err.message}"
            }
        }
    }
}
