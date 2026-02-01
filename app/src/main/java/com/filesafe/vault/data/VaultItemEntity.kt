package com.filesafe.vault.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "vault_items")
data class VaultItemEntity(
    @PrimaryKey val id: String,
    val displayName: String,
    val mimeType: String,
    val createdAt: Long,
    val size: Long,
    val sha256: String,
    val localPath: String
)
