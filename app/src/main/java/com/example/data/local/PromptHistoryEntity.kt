package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "prompt_history")
data class PromptHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val imagePath: String,
    val timestamp: Long = System.currentTimeMillis(),
    val masterPrompt: String,
    val negativePrompt: String,
    val mode: String,
    val detailLevel: String,
    val subject: String = "",
    val composition: String = "",
    val lighting: String = "",
    val colors: String = "",
    val camera: String = "",
    val environment: String = "",
    val style: String = "",
    val analysis: String = ""
)
