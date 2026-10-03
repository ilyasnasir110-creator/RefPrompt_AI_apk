package com.example

import com.example.data.model.PromptDetailLevel
import com.example.data.model.PromptMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun testPromptModes() {
        assertEquals(PromptMode.UNIVERSAL, PromptMode.fromName("Universal"))
        assertEquals(PromptMode.PHOTOREALISTIC, PromptMode.fromName("PHOTOREALISTIC"))
        assertEquals(PromptMode.CINEMATIC, PromptMode.fromName("cinematic"))
        assertEquals(PromptMode.UNIVERSAL, PromptMode.fromName("unknown_mode"))
    }

    @Test
    fun testPromptDetailLevels() {
        assertEquals(PromptDetailLevel.ULTRA_DETAILED, PromptDetailLevel.fromName("Ultra Detailed"))
        assertEquals(PromptDetailLevel.QUICK, PromptDetailLevel.fromName("quick"))
        assertEquals(PromptDetailLevel.DETAILED, PromptDetailLevel.fromName("DETAILED"))
        assertEquals(PromptDetailLevel.ULTRA_DETAILED, PromptDetailLevel.fromName("unknown_level"))
    }
}
