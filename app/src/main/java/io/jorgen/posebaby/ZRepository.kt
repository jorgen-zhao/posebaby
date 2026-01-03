package io.jorgen.posebaby

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

/**
 * Repository to analyze scene using GLM-4.5v model.
 * Note: We are mocking the "GenerativeModel" class structure to match the user's request pattern,
 * while implementing the actual network call using OkHttp to ensure reliability without
 * full documentation of the Zai SDK internal classes.
 */
class ZRepository(private val apiKey: String = BuildConfig.ZHIPU_API_KEY) {

    // Placeholder for the requested GenerativeModel class
    class GenerativeModel(val modelName: String, val apiKey: String) {
        
        private val client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        suspend fun generateContent(prompt: String, bitmap: Bitmap): String = withContext(Dispatchers.IO) {
            val base64Image = encodeImage(bitmap)
            
            // Construct payload for GLM-4v (Zhipu AI style)
            val jsonBody = JSONObject().apply {
                put("model", modelName)
                put("messages", JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "user")
                        put("content", JSONArray().apply {
                            put(JSONObject().apply {
                                put("type", "text")
                                put("text", prompt)
                            })
                            put(JSONObject().apply {
                                put("type", "image_url")
                                put("image_url", JSONObject().apply {
                                    put("url", "data:image/jpeg;base64,$base64Image")
                                })
                            })
                        })
                    })
                })
                put("temperature", 0.7)
            }

            val request = Request.Builder()
                .url("https://open.bigmodel.cn/api/paas/v4/chat/completions") // Zhipu AI Endpoint
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
                .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                .build()

            try {
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        return@use "Error: ${response.code} ${response.message}"
                    }
                    val body = response.body?.string() ?: return@use "{}"
                    
                    // Parse response to get content
                    val jsonResponse = JSONObject(body)
                    val content = jsonResponse.getJSONArray("choices")
                        .getJSONObject(0)
                        .getJSONObject("message")
                        .getString("content")
                    
                    // Clean content (remove markdown json blocks if present)
                    content.replace("```json", "").replace("```", "").trim()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                "{}"
            }
        }

        private fun encodeImage(bitmap: Bitmap): String {
            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
            val bytes = outputStream.toByteArray()
            return Base64.encodeToString(bytes, Base64.NO_WRAP)
        }
    }

    // Initialize using key from constructor
    private val generativeModel = GenerativeModel(
        modelName = "glm-4.5v",
        apiKey = apiKey
    )
    
    init {
        val maskedKey = if (apiKey.length > 8) "${apiKey.take(4)}...${apiKey.takeLast(4)}" else apiKey
        android.util.Log.d("ZRepository", "Initialized with API Key: $maskedKey")
    }

    // Removed duplicate data class PoseSuggestion


    suspend fun analyzeScene(bitmap: Bitmap): List<PoseSuggestion> {
        val prompt = """
            You are a master photography director. I will send you a photo of a scene (and potential subject).
            1. Analyze the lighting, background, and mood.
            2. Brainstorm 4 distinct, creative posing ideas that fit this scene perfectly.
            3. For each idea, map it to one of these broad technical categories: 
               ['standing_straight', 'leaning_left', 'leaning_right', 'sitting_casual', 'crouching', 'walking_away', 'arms_crossed', 'hands_in_pockets'].
            4. Output strictly valid JSON format:
            {
              "suggestions": [
                {
                  "id": 1,
                  "title": "靠墙慵懒风",
                  "description": "让模特背靠左边的红砖墙，右腿微曲，眼神看向右下方。",
                  "photographer_tip": "使用大光圈虚化背景，对焦在眼睛。",
                  "technical_category": "leaning_left",
                  "difficulty": "Easy"
                }
              ]
            }
        """.trimIndent()
        
        return try {
            val response = generativeModel.generateContent(prompt, bitmap)
            Log.d("ZRepository", "Raw API Response: $response")
            
            if (response.startsWith("Error:") || !response.trim().startsWith("{")) {
                Log.e("ZRepository", "API returned non-JSON error: $response")
                return emptyList()
            }
            
            val jsonObject = JSONObject(response)
            val jsonArray = jsonObject.optJSONArray("suggestions") ?: return emptyList()
            val suggestions = mutableListOf<PoseSuggestion>()
            
            for (i in 0 until jsonArray.length()) {
                val item = jsonArray.getJSONObject(i)
                suggestions.add(
                    PoseSuggestion(
                        id = item.optInt("id"),
                        title = item.optString("title"),
                        description = item.optString("description"),
                        photographerTip = item.optString("photographer_tip"),
                        technicalCategory = item.optString("technical_category"),
                        difficulty = item.optString("difficulty")
                    )
                )
            }
            suggestions
        } catch (e: Exception) {
            Log.e("ZRepository", "Failed to parse API response", e)
            emptyList()
        }
    }

    data class PoseSuggestion(
        val id: Int,
        val title: String,
        val description: String,
        val photographerTip: String,
        val technicalCategory: String,
        val difficulty: String
    )
}
