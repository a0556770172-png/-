package com.filesafe.vault.viewmodel

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.filesafe.vault.crypto.CryptoManager

class OnboardingViewModel(private val cryptoManager: CryptoManager) : ViewModel() {
    val hasAdmin = mutableStateOf(cryptoManager.hasAdmin())
    val errorMessage = mutableStateOf<String?>(null)

    fun setupAdmin(code: String, confirm: String) {
        if (code.isBlank() || code != confirm) {
            errorMessage.value = "Codes do not match"
            return
        }
        cryptoManager.setupAdmin(code.toCharArray())
        hasAdmin.value = true
        errorMessage.value = null
    }

    fun verify(code: String): Boolean {
        val ok = cryptoManager.verifyAdmin(code.toCharArray())
        if (!ok) {
            errorMessage.value = "Invalid admin code"
        }
        return ok
    }
}
