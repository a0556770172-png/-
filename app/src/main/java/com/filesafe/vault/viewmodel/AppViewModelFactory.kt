package com.filesafe.vault.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.filesafe.vault.AppContainer
import com.filesafe.vault.FileSafeVaultApp
import com.filesafe.vault.crypto.CryptoManager

class AppViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    private val container: AppContainer
        get() = (application as FileSafeVaultApp).container

    private val cryptoManager: CryptoManager by lazy { CryptoManager(application) }

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(OnboardingViewModel::class.java) ->
                OnboardingViewModel(cryptoManager) as T
            modelClass.isAssignableFrom(VaultListViewModel::class.java) ->
                VaultListViewModel(container.repository) as T
            modelClass.isAssignableFrom(AddFileViewModel::class.java) ->
                AddFileViewModel(container.repository, cryptoManager, application) as T
            modelClass.isAssignableFrom(ImportViewModel::class.java) ->
                ImportViewModel(container.repository, cryptoManager, application) as T
            modelClass.isAssignableFrom(ItemDetailsViewModel::class.java) ->
                ItemDetailsViewModel(container.repository, cryptoManager, application) as T
            else -> throw IllegalArgumentException("Unknown ViewModel $modelClass")
        }
    }
}
