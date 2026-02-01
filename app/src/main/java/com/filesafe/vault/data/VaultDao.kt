package com.filesafe.vault.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface VaultDao {
    @Query("SELECT * FROM vault_items ORDER BY createdAt DESC")
    fun getAllSorted(): Flow<List<VaultItemEntity>>

    @Query("SELECT * FROM vault_items WHERE displayName LIKE '%' || :query || '%' ORDER BY createdAt DESC")
    fun searchByName(query: String): Flow<List<VaultItemEntity>>

    @Query("SELECT * FROM vault_items WHERE id = :id")
    suspend fun getById(id: String): VaultItemEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: VaultItemEntity)

    @Query("DELETE FROM vault_items WHERE id = :id")
    suspend fun deleteById(id: String)
}
