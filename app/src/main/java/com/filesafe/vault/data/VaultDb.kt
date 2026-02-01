package com.filesafe.vault.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [VaultItemEntity::class], version = 1)
abstract class VaultDb : RoomDatabase() {
    abstract fun vaultDao(): VaultDao

    companion object {
        @Volatile
        private var INSTANCE: VaultDb? = null

        fun get(context: Context): VaultDb {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    VaultDb::class.java,
                    "vault.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
