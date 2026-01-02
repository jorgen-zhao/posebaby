package io.jorgen.posebaby

import android.graphics.Bitmap
import android.util.Base64
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
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

/**
 * Result containing the generated image URL and its size.
 */
data class ImageResult(
    val url: String,
    val width: Int,
    val height: Int
)

/**
 * Repository for interacting with Doubao (Volcano Engine) API.
 * Handles image generation using doubao-seedream-4-5-251128 model.
 * 
 * API Reference: https://ark.cn-beijing.volces.com/api/v3/images/generations
 */
class DoubaoRepository(private val apiKey: String) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(120, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .build()

    /**
     * Generate a reference image combining user's photo with a pose prompt.
     * Returns a 2048x2048 image that can be split into 9 parts (3x3 grid).
     * 
     * @param prompt The pose description prompt
     * @param bitmap The user's captured/uploaded image
     * @return Flow emitting ImageResult with URL and size, or null on error
     */
    fun generateReferenceImage(prompt: String, bitmap: Bitmap): Flow<ImageResult?> = flow {
        try {
            android.util.Log.d("DoubaoRepository", "Generating image with prompt: $prompt")
            
            val result = withContext(Dispatchers.IO) {
                // Encode bitmap to base64
                val base64Image = encodeImage(bitmap)
                
                // Construct the request payload for Doubao
                val jsonBody = JSONObject().apply {
                    put("model", "doubao-seedream-4-5-251128")
                    put("prompt", prompt)
                    put("image", "data:image/jpeg;base64,$base64Image")
                    put("response_format", "url")
                    put("stream", false)
                    put("watermark", false)
                }

                val request = Request.Builder()
                    .url("https://ark.cn-beijing.volces.com/api/v3/images/generations")
                    .addHeader("Authorization", "Bearer $apiKey")
                    .addHeader("Content-Type", "application/json")
                    .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        android.util.Log.e("DoubaoRepository", "API error: ${response.code} ${response.message}")
                        android.util.Log.e("DoubaoRepository", "Response body: ${response.body?.string()}")
                        return@use null
                    }

                    val body = response.body?.string() ?: return@use null
                    android.util.Log.d("DoubaoRepository", "API response: $body")

                    // Parse the response
                    // {"data":[{"url":"xxx","size":"2048x2048"}],...}
                    val jsonResponse = JSONObject(body)
                    val dataArray = jsonResponse.optJSONArray("data")
                    
                    if (dataArray != null && dataArray.length() > 0) {
                        val item = dataArray.getJSONObject(0)
                        val url = item.optString("url")
                        val sizeStr = item.optString("size", "2048x2048")
                        
                        // Parse size "2048x2048" -> width, height
                        val sizeParts = sizeStr.split("x")
                        val width = sizeParts.getOrNull(0)?.toIntOrNull() ?: 2048
                        val height = sizeParts.getOrNull(1)?.toIntOrNull() ?: 2048
                        
                        android.util.Log.d("DoubaoRepository", "Generated image: $url, size: ${width}x${height}")
                        ImageResult(url, width, height)
                    } else {
                        android.util.Log.e("DoubaoRepository", "No data in response")
                        null
                    }
                }
            }

            emit(result)

        } catch (e: Exception) {
            android.util.Log.e("DoubaoRepository", "Error generating image", e)
            emit(null)
        }
    }.flowOn(Dispatchers.IO)

    private fun encodeImage(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
        val bytes = outputStream.toByteArray()
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }
}
