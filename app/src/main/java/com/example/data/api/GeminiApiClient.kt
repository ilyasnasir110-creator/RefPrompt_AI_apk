package com.example.data.api

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import com.example.BuildConfig
import com.example.data.model.ImageAnalysisResult
import com.example.data.model.PromptDetailLevel
import com.example.data.model.PromptMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.concurrent.TimeUnit

class GeminiApiClient {

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    companion object {
        private const val TAG = "GeminiApiClient"
        // High-capacity models with fallback order
        private val FALLBACK_MODELS = listOf(
            "gemini-3.5-flash",
            "gemini-3.8-flash",
            "gemini-flash-latest",
            "gemini-3.1-flash-lite-preview",
            "gemini-3.1-pro-preview"
        )
        private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models"
        const val DEFAULT_PERMANENT_KEY = "AQ.Ab8RN6Ke8hIvfsNt8Sdo8PpmIX-CWnVkb-lhuT1lHDHf2_NVhA"

        const val SYSTEM_INSTRUCTION =
            "You are an expert visual analyst and AI image-prompt engineer. Analyze the supplied reference image carefully. " +
            "Your goal is to describe the visible visual information with enough precision that another image-generation model " +
            "can reproduce the image as closely as possible when given the original reference image together with your generated prompt. " +
            "Prioritize observable details over assumptions. Never invent details that cannot reasonably be seen. " +
            "Preserve composition, subject identity, pose, environment, lighting, color relationships, camera perspective, textures, " +
            "and overall visual character. Make the final master prompt clear, natural, highly descriptive, and optimized for image generation."
    }

    suspend fun analyzeImageAndGeneratePrompt(
        imageFile: File,
        mode: PromptMode,
        detailLevel: PromptDetailLevel,
        customApiKey: String?
    ): Result<ImageAnalysisResult> = withContext(Dispatchers.IO) {
        try {
            val candidateKeys = resolveCandidateKeys(customApiKey)
            if (candidateKeys.isEmpty()) {
                return@withContext Result.failure(
                    MissingApiKeyException(
                        "Gemini API Key is not configured. Please add your GEMINI_API_KEY in the AI Studio Secrets panel or enter it in Settings."
                    )
                )
            }

            // Downscale image if necessary to prevent oversized payloads (max 1280px width/height)
            val base64Image = prepareImageBase64(imageFile)
            val promptRequestText = buildUserPrompt(mode, detailLevel)
            val payload = buildJsonPayload(promptRequestText, base64Image)

            var lastError = "Analysis failed. Please try again."

            for (apiKey in candidateKeys) {
                for (model in FALLBACK_MODELS) {
                    val url = "$BASE_URL/$model:generateContent?key=$apiKey"
                    val request = Request.Builder()
                        .url(url)
                        .post(payload.toRequestBody("application/json".toMediaType()))
                        .build()

                    try {
                        val response = client.newCall(request).execute()
                        val responseBody = response.body?.string() ?: ""

                        if (response.isSuccessful) {
                            val analysisResult = parseGeminiResponse(responseBody, mode, detailLevel)
                            return@withContext Result.success(analysisResult)
                        }

                        Log.w(TAG, "API call for model $model failed (${response.code}): $responseBody")
                        lastError = parseApiErrorMessage(responseBody, response.code)

                        // If 400 with invalid key, switch to next key immediately
                        if (response.code == 400 && responseBody.contains("API_KEY_INVALID", ignoreCase = true)) {
                            break
                        }

                        // For 503 (high demand), 429 (rate limit), 404 (model not found), or 500 (internal error),
                        // continue to next model in the fallback list after a tiny delay
                        if (response.code == 503 || response.code == 429 || response.code == 404 || response.code >= 500) {
                            kotlinx.coroutines.delay(400)
                            continue
                        } else {
                            // Other non-transient error for this model, continue to try next model
                            continue
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Exception calling model $model: ${e.message}")
                        lastError = e.message ?: "Network error connecting to AI service."
                    }
                }
            }

            Result.failure(Exception(lastError))
        } catch (e: Exception) {
            Log.e(TAG, "Error generating prompt", e)
            Result.failure(e)
        }
    }

    private fun resolveCandidateKeys(customApiKey: String?): List<String> {
        val keys = mutableListOf<String>()
        if (!customApiKey.isNullOrBlank()) {
            keys.add(customApiKey.trim())
        }
        val buildKey = try {
            BuildConfig.GEMINI_API_KEY.trim()
        } catch (_: Exception) {
            ""
        }
        if (buildKey.isNotBlank() && buildKey != "MY_GEMINI_API_KEY" && !keys.contains(buildKey)) {
            keys.add(buildKey)
        }
        if (DEFAULT_PERMANENT_KEY.isNotBlank() && !keys.contains(DEFAULT_PERMANENT_KEY)) {
            keys.add(DEFAULT_PERMANENT_KEY)
        }
        return keys
    }

    private fun prepareImageBase64(file: File): String {
        val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, boundsOptions)
        val origWidth = boundsOptions.outWidth
        val origHeight = boundsOptions.outHeight

        val maxDimension = 1536
        var inSampleSize = 1
        if (origWidth > maxDimension || origHeight > maxDimension) {
            val halfHeight = origHeight / 2
            val halfWidth = origWidth / 2
            while ((halfHeight / inSampleSize) >= maxDimension || (halfWidth / inSampleSize) >= maxDimension) {
                inSampleSize *= 2
            }
        }

        val decodeOptions = BitmapFactory.Options().apply {
            this.inSampleSize = inSampleSize
            inPreferredConfig = Bitmap.Config.RGB_565
        }
        val bitmap = BitmapFactory.decodeFile(file.absolutePath, decodeOptions)
            ?: throw IllegalStateException("Could not read image file")

        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
        val byteArray = outputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.NO_WRAP)
    }

    private fun buildUserPrompt(mode: PromptMode, detailLevel: PromptDetailLevel): String {
        val modeInstruction = when (mode) {
            PromptMode.UNIVERSAL -> "Synthesize a balanced master prompt that translates effortlessly into any major generative model."
            PromptMode.PHOTOREALISTIC -> "Emphasize photorealistic camera optics, natural skin pores, realistic depth of field, real-world bounce lighting, and camera sensor texture."
            PromptMode.CINEMATIC -> "Emphasize cinematic lighting, anamorphic framing, film color grading, dramatic chiaroscuro contrast, volumetric haze, and narrative composition."
            PromptMode.PORTRAIT -> "Focus heavily on the subject's face, gaze, expression, hair strands, facial structure, skin undertones, neck/shoulders, and personal identity preservation."
            PromptMode.PRODUCT -> "Focus on studio product photography, pristine clean reflections, material surface finishes, accurate specular highlights, and crisp commercial presentation."
            PromptMode.ARTISTIC -> "Focus on artistic medium nuances, painterly brush textures, creative color choices, expressive mark-making, and stylized mood."
            PromptMode.ANIME_ILLUSTRATION -> "Focus on clean 2D/2.5D linework, cell shading, anime aesthetics, dynamic illustrative perspective, vibrant color palettes, and digital art rendering."
        }

        val detailInstruction = when (detailLevel) {
            PromptDetailLevel.QUICK -> "Provide a punchy, concise prompt focusing directly on the essential visual anchors without unnecessary filler."
            PromptDetailLevel.DETAILED -> "Provide a thorough and rich description covering subject, setting, lighting, colors, and camera angle."
            PromptDetailLevel.ULTRA_DETAILED -> "Provide an ultra-detailed, highly comprehensive master prompt with complete nuance: subject details, clothing materials, physical features, micro-expressions, exact camera perspective, depth layers, lighting vectors, and color grading. Eliminate generic fluff and maximize actionable visual detail."
        }

        return """
            Analyze this uploaded reference image in detail.
            Target Mode: ${mode.displayName} ($modeInstruction)
            Detail Level: ${detailLevel.displayName} ($detailInstruction)

            Identity & Reference Preservation Rule:
            If the image contains a person, you MUST explicitly describe their physical identity (facial structure, hair, skin tone, facial proportions, expression, clothing, pose) and include the following reference preservation clause inside the masterPrompt:
            "Use the provided reference image as the primary visual identity and composition reference. Preserve the subject's recognizable facial structure, proportions, hairstyle, and overall appearance while reproducing the described scene."
            If the image does NOT contain a person, adapt the prompt to preserve the key object, animal, architecture, or environment from the reference without mentioning human facial structure.

            Negative Prompt:
            Provide a targeted negative prompt containing unwanted artifacts, distortions, or traits relevant to this specific image type (e.g. malformed hands, distorted anatomy, text, watermarks, bad reflections, overexposure).

            Return ONLY a valid JSON object strictly matching this schema:
            {
              "analysis": "Brief 2-3 sentence overview of what is seen in the image",
              "masterPrompt": "The complete, standalone, natural English master prompt for recreating this image with an AI generator",
              "negativePrompt": "Comma-separated negative prompt terms tailored to this image",
              "style": "Visual style, e.g., Photorealistic editorial portrait, Cinematic film still, Digital 3D render",
              "subject": "Detailed breakdown of the main subject, people/objects, pose, clothing, expression",
              "composition": "Framing, camera angle, focal subject placement, foreground, background, depth",
              "lighting": "Direction of light, quality (hard/soft), light source, shadows, highlights, ambient bounce",
              "colors": "Dominant color palette, contrast, saturation, color grading, mood tones",
              "camera": "Estimated lens focal length, aperture/depth of field, focus plane, bokeh, camera type/style",
              "environment": "Location setting, background elements, interior/exterior, weather, time of day"
            }
        """.trimIndent()
    }

    private fun buildJsonPayload(promptText: String, base64Image: String): String {
        val root = JSONObject()

        val systemInstructionObj = JSONObject()
        val sysParts = JSONArray()
        sysParts.put(JSONObject().put("text", SYSTEM_INSTRUCTION))
        systemInstructionObj.put("parts", sysParts)
        root.put("systemInstruction", systemInstructionObj)

        val contents = JSONArray()
        val contentObj = JSONObject()
        val parts = JSONArray()

        parts.put(JSONObject().put("text", promptText))

        val inlineDataObj = JSONObject()
        inlineDataObj.put("mimeType", "image/jpeg")
        inlineDataObj.put("data", base64Image)
        parts.put(JSONObject().put("inlineData", inlineDataObj))

        contentObj.put("parts", parts)
        contents.put(contentObj)
        root.put("contents", contents)

        val generationConfig = JSONObject()
        generationConfig.put("responseMimeType", "application/json")
        generationConfig.put("temperature", 0.3)
        root.put("generationConfig", generationConfig)

        return root.toString()
    }

    private fun parseGeminiResponse(
        rawResponse: String,
        mode: PromptMode,
        detailLevel: PromptDetailLevel
    ): ImageAnalysisResult {
        try {
            val root = JSONObject(rawResponse)
            val candidates = root.optJSONArray("candidates")
            if (candidates == null || candidates.length() == 0) {
                throw IllegalStateException("No candidate returned by Gemini model")
            }

            val firstCandidate = candidates.getJSONObject(0)
            val content = firstCandidate.optJSONObject("content")
            val parts = content?.optJSONArray("parts")
            val text = parts?.optJSONObject(0)?.optString("text", "") ?: ""

            if (text.isBlank()) {
                throw IllegalStateException("Empty text content in model response")
            }

            // Extract JSON substring if wrapped in markdown ```json ... ```
            val cleanedJson = extractJsonString(text)
            val json = JSONObject(cleanedJson)

            val masterPrompt = json.optString("masterPrompt", "").trim()
            val negativePrompt = json.optString("negativePrompt", "").trim()
            val analysis = json.optString("analysis", "").trim()
            val style = json.optString("style", mode.displayName).trim()
            val subject = json.optString("subject", "").trim()
            val composition = json.optString("composition", "").trim()
            val lighting = json.optString("lighting", "").trim()
            val colors = json.optString("colors", "").trim()
            val camera = json.optString("camera", "").trim()
            val environment = json.optString("environment", "").trim()

            val isPerson = subject.contains("person", ignoreCase = true) ||
                    subject.contains("man", ignoreCase = true) ||
                    subject.contains("woman", ignoreCase = true) ||
                    subject.contains("face", ignoreCase = true) ||
                    subject.contains("portrait", ignoreCase = true) ||
                    masterPrompt.contains("facial structure", ignoreCase = true)

            return ImageAnalysisResult(
                masterPrompt = if (masterPrompt.isNotEmpty()) masterPrompt else text,
                negativePrompt = if (negativePrompt.isNotEmpty()) negativePrompt else "blurry, distorted anatomy, bad proportions, low resolution, artifacts, watermark",
                analysis = if (analysis.isNotEmpty()) analysis else "Visual reference analyzed for high-fidelity reproduction.",
                style = style,
                subject = subject,
                composition = composition,
                lighting = lighting,
                colors = colors,
                camera = camera,
                environment = environment,
                isPersonDetected = isPerson
            )
        } catch (e: Exception) {
            Log.w(TAG, "Failed parsing structured JSON from response, attempting lenient extraction: ${e.message}")
            return fallbackParse(rawResponse, mode)
        }
    }

    private fun extractJsonString(text: String): String {
        var str = text.trim()
        if (str.startsWith("```json")) {
            str = str.substring(7)
        } else if (str.startsWith("```")) {
            str = str.substring(3)
        }
        if (str.endsWith("```")) {
            str = str.substring(0, str.length - 3)
        }
        str = str.trim()
        val startIndex = str.indexOf('{')
        val endIndex = str.lastIndexOf('}')
        if (startIndex != -1 && endIndex != -1 && endIndex > startIndex) {
            return str.substring(startIndex, endIndex + 1)
        }
        return str
    }

    private fun fallbackParse(rawResponse: String, mode: PromptMode): ImageAnalysisResult {
        // Attempt to extract text from candidates
        var extractedText = ""
        try {
            val root = JSONObject(rawResponse)
            val candidates = root.optJSONArray("candidates")
            val content = candidates?.optJSONObject(0)?.optJSONObject("content")
            extractedText = content?.optJSONArray("parts")?.optJSONObject(0)?.optString("text", "") ?: ""
        } catch (_: Exception) {
            extractedText = rawResponse
        }

        return ImageAnalysisResult(
            masterPrompt = extractedText.ifBlank { "High detail reproduction of reference image in ${mode.displayName} style." },
            negativePrompt = "blurry, low resolution, distorted anatomy, bad proportions, bad hands, artifacts, text, watermark",
            analysis = "Reference image visual structure analyzed.",
            style = mode.displayName,
            subject = "Reference image focal elements",
            composition = "Matches reference framing and angle",
            lighting = "Balanced illumination matching reference",
            colors = "Color harmony derived from reference",
            camera = "Sharp focal plane and natural depth of field",
            environment = "Faithful environment recreation matching reference",
            isPersonDetected = false
        )
    }

    private fun parseApiErrorMessage(body: String, statusCode: Int): String {
        return try {
            val json = JSONObject(body)
            val error = json.optJSONObject("error")
            val message = error?.optString("message") ?: ""
            if (message.isNotEmpty()) {
                when {
                    statusCode == 400 && message.contains("API_KEY_INVALID", ignoreCase = true) ->
                        "Invalid Gemini API Key. Please verify the key in Settings or AI Studio Secrets."
                    statusCode == 503 || message.contains("high demand", ignoreCase = true) ->
                        "The AI model is currently experiencing high demand. Please tap Generate again in a few seconds."
                    statusCode == 404 ->
                        "The AI model is unavailable or rate-limited. Please try again shortly."
                    statusCode == 429 ->
                        "Gemini API rate limit reached. Please wait a moment and try again."
                    else -> "API Error: $message"
                }
            } else {
                "Server returned status code $statusCode. Please try again."
            }
        } catch (_: Exception) {
            "Server returned error code $statusCode."
        }
    }
}

class MissingApiKeyException(message: String) : Exception(message)
