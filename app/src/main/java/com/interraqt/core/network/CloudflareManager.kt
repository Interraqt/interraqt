package com.interraqt.core.network

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.UUID
import kotlin.math.max
import kotlin.math.min

object CloudflareManager {
    private const val WORKER_URL = "https://interraqt-uploader.interraqt.workers.dev"
    private const val APP_SECRET = "Interraqt@2026" 
    
    private val client = OkHttpClient()

    // 🚨 Added 'isBanner' boolean to apply custom size rules
    suspend fun uploadImage(context: Context, uri: Uri, isBanner: Boolean = false): String? = withContext(Dispatchers.IO) {
        try {
            // 1. Establish strict limits based on the image type
            val maxBytes = if (isBanner) 2 * 1024 * 1024 else 1 * 1024 * 1024 // 2MB or 1MB limit
            val maxWidth = if (isBanner) 1920 else 1080
            val maxHeight = if (isBanner) 1080 else 1080

            // 2. Decode bounds to safely measure the image without crashing low-memory phones
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }

            // 3. Calculate smart down-sampling ratio
            options.inSampleSize = calculateInSampleSize(options, maxWidth, maxHeight)
            options.inJustDecodeBounds = false // Ready to actually load the pixels now

            // 4. Load the compressed image into memory
            var bitmap = context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
                ?: return@withContext null

            // 5. Precisely scale dimensions to fit our exact max bounds
            val ratio = min(maxWidth.toFloat() / bitmap.width, maxHeight.toFloat() / bitmap.height)
            if (ratio < 1f) {
                val newWidth = (bitmap.width * ratio).toInt()
                val newHeight = (bitmap.height * ratio).toInt()
                val scaledBitmap = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
                if (scaledBitmap != bitmap) {
                    bitmap.recycle() // Free old memory instantly
                    bitmap = scaledBitmap
                }
            }

            // 6. Compress to 90% JPEG Quality
            var quality = 90
            var stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream)
            var bytes = stream.toByteArray()

            // 7. Limit Enforcer: Gently drop quality if file is still somehow over the strict MB limit
            while (bytes.size > maxBytes && quality > 50) {
                stream.reset()
                quality -= 10
                bitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream)
                bytes = stream.toByteArray()
            }
            bitmap.recycle() // Clean up RAM

            // 8. Upload the perfectly compressed file to Cloudflare
            val fileName = "img_${UUID.randomUUID()}.jpg"
            val requestBody = bytes.toRequestBody("image/jpeg".toMediaTypeOrNull())
            
            val request = Request.Builder()
                .url("$WORKER_URL/$fileName")
                .put(requestBody)
                .addHeader("Authorization", "Bearer $APP_SECRET")
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val jsonString = response.body?.string() ?: ""
                return@withContext JSONObject(jsonString).getString("url")
            } else {
                println("Cloudflare Error: ${response.code} - ${response.body?.string()}")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return@withContext null
    }

    // Mathematical formula to safely downsample massive photos
    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val (height: Int, width: Int) = options.outHeight to options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }
}
