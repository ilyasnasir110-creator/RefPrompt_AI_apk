package com.example.data.model

enum class PromptDetailLevel(val displayName: String, val description: String) {
    QUICK("Quick", "Concise, punchy key-descriptors for fast generation"),
    DETAILED("Detailed", "Comprehensive visual breakdown covering major visual vectors"),
    ULTRA_DETAILED("Ultra Detailed", "Maximum useful visual information without repetition for exact replica");

    companion object {
        fun fromName(name: String?): PromptDetailLevel {
            return entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: ULTRA_DETAILED
        }
    }
}
