package com.example.data.model

data class ImageAnalysisResult(
    val masterPrompt: String,
    val negativePrompt: String,
    val analysis: String,
    val style: String,
    val subject: String,
    val composition: String,
    val lighting: String,
    val colors: String,
    val camera: String,
    val environment: String,
    val isPersonDetected: Boolean = false
)
