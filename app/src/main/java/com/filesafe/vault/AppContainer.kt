package com.filesafe.vault

import android.content.Context
import com.filesafe.vault.data.VaultDb
import com.filesafe.vault.data.VaultRepository

class AppContainer(context: Context) {
    private val db = VaultDb.get(context)
    val repository = VaultRepository(db.vaultDao())
}
