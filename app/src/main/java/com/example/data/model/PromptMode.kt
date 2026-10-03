package com.example.data.model

enum class PromptMode(val displayName: String, val description: String) {
    UNIVERSAL("Universal", "Balanced master prompt suitable for any AI engine"),
    PHOTOREALISTIC("Photorealistic", "Emphasizes authentic camera, sensor, depth, and skin/material fidelity"),
    CINEMATIC("Cinematic", "Focuses on film grade, dramatic lighting, anamorphic ratio, and storytelling mood"),
    PORTRAIT("Portrait", "Optimized for character framing, facial preservation, hair, eyes, and skin textures"),
    PRODUCT("Product", "Studio lighting, clean presentation, material textures, and commercial crispness"),
    ARTISTIC("Artistic", "Expressive brushstrokes, medium characteristics, and creative stylization"),
    ANIME_ILLUSTRATION("Anime / Illustration", "2D/2.5D linework, anime cell shading, vibrant palette, and concept art traits");

    companion object {
        fun fromName(name: String?): PromptMode {
            return entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: UNIVERSAL
        }
    }
}
