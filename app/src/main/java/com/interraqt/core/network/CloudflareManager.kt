package com.interraqt.core.network

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.UUID

object CloudflareManager {
    // 🚨 Replace with your actual Cloudflare Worker URL
    private const val WORKER_URL = "https://interraqt-uploader.your-username.workers.dev"
    
    // 🚨 Replace with the exact APP_SECRET you put in the Cloudflare Dashboard
    private const val APP_SECRET = "InterraqtSecretUploadKey2026" 
    
    private val client = OkHttpClient()

    suspend fun uploadImage(context: Context, uri: Uri): String? = withContext(Dispatchers.IO) {
        try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val bytes = inputStream?.readBytes()
            inputStream?.close()

            if (bytes != null) {
                // Generates a unique filename (e.g., img_123e4567-e89b.jpg)
                val fileName = "img_${UUID.randomUUID()}.jpg"
                
                val requestBody = bytes.toRequestBody("image/jpeg".toMediaTypeOrNull())
                
                // Sends the file securely to your Cloudflare Worker
                val request = Request.Builder()
                    .url("$WORKER_URL/$fileName")
                    .put(requestBody)
                    .addHeader("Authorization", "Bearer $APP_SECRET")
                    .build()

                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    val jsonString = response.body?.string() ?: ""
                    // Parses the {"url": "https://cdn.interraqt.com/..."} response from the Worker
                    return@withContext JSONObject(jsonString).getString("url")
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return@withContext null
    }
}
