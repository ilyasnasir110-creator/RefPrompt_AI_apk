package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PromptHistoryDao {
    @Query("SELECT * FROM prompt_history ORDER BY timestamp DESC")
    fun getAllHistory(): Flow<List<PromptHistoryEntity>>

    @Query("SELECT * FROM prompt_history WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): PromptHistoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: PromptHistoryEntity): Long

    @Delete
    suspend fun delete(entity: PromptHistoryEntity)

    @Query("DELETE FROM prompt_history WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM prompt_history")
    suspend fun clearAll()
}
