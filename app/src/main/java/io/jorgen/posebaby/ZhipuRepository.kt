package io.jorgen.posebaby

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Repository for interacting with Zhipu AI API.
 * Handles image generation using CogView-4 model.
 * 
 * Based on official documentation:
 * https://docs.bigmodel.cn/cn/guide/models/image-generation/cogview-4#java
 * 
 * Implementation uses direct REST API calls via OkHttp for maximum compatibility.
 */
class ZhipuRepository(private val apiKey: String) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    /**
     * Generate a reference image based on the given prompt.
     * 
     * @param prompt The detailed prompt describing the desired image
     * @return Flow emitting the URL of the generated image, or null on error
     */
    fun generateReferenceImage(prompt: String): Flow<String?> = flow {
        try {
            android.util.Log.d("ZhipuRepository", "Generating image with prompt: $prompt")
            android.util.Log.d("ZhipuRepository", "Using API key: ${apiKey.take(10)}...")
            
            val imageUrl = withContext(Dispatchers.IO) {
                // Construct the request payload for CogView-4
                val jsonBody = JSONObject().apply {
                    put("model", "cogview-4")
                    put("prompt", prompt)
                    put("size", "1024x1024")
                }

                val request = Request.Builder()
                    .url("https://open.bigmodel.cn/api/paas/v4/images/generations")
                    .addHeader("Authorization", "Bearer $apiKey")
                    .addHeader("Content-Type", "application/json")
                    .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        android.util.Log.e("ZhipuRepository", "API error: ${response.code} ${response.message}")
                        android.util.Log.e("ZhipuRepository", "Response body: ${response.body?.string()}")
                        return@use null
                    }

                    val body = response.body?.string() ?: return@use null
                    android.util.Log.d("ZhipuRepository", "API response: $body")

                    // Parse the response
                    val jsonResponse = JSONObject(body)
                    
                    // The response structure should be:
                    // { "data": [{ "url": "..." }] }
                    val dataArray = jsonResponse.optJSONArray("data")
                    if (dataArray != null && dataArray.length() > 0) {
                        val firstImage = dataArray.getJSONObject(0)
                        firstImage.optString("url")
                    } else {
                        android.util.Log.e("ZhipuRepository", "No data array in response")
                        null
                    }
                }
            }

            if (imageUrl != null && imageUrl.isNotEmpty()) {
                android.util.Log.d("ZhipuRepository", "Image generated successfully: $imageUrl")
                emit(imageUrl)
            } else {
                android.util.Log.e("ZhipuRepository", "Failed to extract image URL from response")
                emit(null)
            }

        } catch (e: Exception) {
            android.util.Log.e("ZhipuRepository", "Error generating image", e)
            emit(null)
        }
    }.flowOn(Dispatchers.IO)
}
