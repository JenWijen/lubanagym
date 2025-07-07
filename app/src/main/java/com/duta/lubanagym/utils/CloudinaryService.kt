package com.duta.lubanagym.utils

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.ByteArrayOutputStream
import kotlin.coroutines.resume

class CloudinaryService {

    companion object {
        private var isInitialized = false

        fun initialize(context: Context) {
            if (!isInitialized) {
                val config = mapOf(
                    "cloud_name" to "dh4as81sp", // Ganti dengan cloud name Anda
                    "api_key" to "171962878274666",       // Ganti dengan API key Anda
                    "api_secret" to "GOpjDMN1buu9bQIbkGSQm9IazVM"  // Ganti dengan API secret Anda
                )
                MediaManager.init(context, config)
                isInitialized = true
            }
        }
    }

    suspend fun uploadImage(uri: Uri, folder: String = "lubana_gym"): Result<String> {
        return suspendCancellableCoroutine { continuation ->
            MediaManager.get().upload(uri)
                .option("folder", folder)
                .option("resource_type", "image")
                .option("quality", "auto")
                .option("fetch_format", "auto")
                .callback(object : UploadCallback {
                    override fun onStart(requestId: String) {
                        // Upload started
                    }

                    override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {
                        // Upload progress
                    }

                    override fun onSuccess(requestId: String, resultData: Map<*, *>) {
                        val imageUrl = resultData["secure_url"] as? String
                        if (imageUrl != null) {
                            continuation.resume(Result.success(imageUrl))
                        } else {
                            continuation.resume(Result.failure(Exception("Failed to get image URL")))
                        }
                    }

                    override fun onError(requestId: String, error: ErrorInfo) {
                        continuation.resume(Result.failure(Exception(error.description)))
                    }

                    override fun onReschedule(requestId: String, error: ErrorInfo) {
                        continuation.resume(Result.failure(Exception("Upload rescheduled: ${error.description}")))
                    }
                })
                .dispatch()
        }
    }

    suspend fun uploadBitmap(bitmap: Bitmap, folder: String = "lubana_gym"): Result<String> {
        return suspendCancellableCoroutine { continuation ->
            val stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, stream)
            val byteArray = stream.toByteArray()

            MediaManager.get().upload(byteArray)
                .option("folder", folder)
                .option("resource_type", "image")
                .option("quality", "auto")
                .option("fetch_format", "auto")
                .callback(object : UploadCallback {
                    override fun onStart(requestId: String) {}

                    override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {}

                    override fun onSuccess(requestId: String, resultData: Map<*, *>) {
                        val imageUrl = resultData["secure_url"] as? String
                        if (imageUrl != null) {
                            continuation.resume(Result.success(imageUrl))
                        } else {
                            continuation.resume(Result.failure(Exception("Failed to get image URL")))
                        }
                    }

                    override fun onError(requestId: String, error: ErrorInfo) {
                        continuation.resume(Result.failure(Exception(error.description)))
                    }

                    override fun onReschedule(requestId: String, error: ErrorInfo) {
                        continuation.resume(Result.failure(Exception("Upload rescheduled: ${error.description}")))
                    }
                })
                .dispatch()
        }
    }
}