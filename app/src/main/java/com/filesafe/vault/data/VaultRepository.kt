package com.filesafe.vault.data

import kotlinx.coroutines.flow.Flow

class VaultRepository(private val dao: VaultDao) {
    fun getAllSorted(): Flow<List<VaultItemEntity>> = dao.getAllSorted()

    fun searchByName(query: String): Flow<List<VaultItemEntity>> = dao.searchByName(query)

    suspend fun getById(id: String): VaultItemEntity? = dao.getById(id)

    suspend fun insert(item: VaultItemEntity) = dao.insert(item)

    suspend fun deleteById(id: String) = dao.deleteById(id)
}
