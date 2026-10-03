package com.example.data.local

import kotlinx.coroutines.flow.Flow

class PromptRepository(private val dao: PromptHistoryDao) {
    val allHistory: Flow<List<PromptHistoryEntity>> = dao.getAllHistory()

    suspend fun getById(id: Long): PromptHistoryEntity? = dao.getById(id)

    suspend fun savePrompt(entity: PromptHistoryEntity): Long = dao.insert(entity)

    suspend fun delete(entity: PromptHistoryEntity) = dao.delete(entity)

    suspend fun deleteById(id: Long) = dao.deleteById(id)

    suspend fun clearAll() = dao.clearAll()
}
