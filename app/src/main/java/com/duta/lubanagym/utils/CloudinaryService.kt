package com.duta.lubanagym.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.ByteArrayOutputStream
import java.io.InputStream
import kotlin.coroutines.resume
import kotlin.math.min

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

    // UPDATED: Metode upload dengan compress otomatis dan fixed transformations
    suspend fun uploadImage(uri: Uri, folder: String = "lubana_gym", context: Context? = null): Result<String> {
        return suspendCancellableCoroutine { continuation ->
            try {
                // Compress image jika context tersedia
                val compressedByteArray = if (context != null) {
                    compressImage(context, uri, maxSizeKB = 1024) // Maksimal 1MB
                } else {
                    // Fallback ke upload langsung jika context tidak tersedia
                    null
                }

                val uploadRequest = if (compressedByteArray != null) {
                    // Upload compressed image
                    MediaManager.get().upload(compressedByteArray)
                } else {
                    // Upload original image
                    MediaManager.get().upload(uri)
                }

                uploadRequest
                    .option("folder", folder)
                    .option("resource_type", "image")
                    // FIXED: Removed transformation option that was causing errors
                    // Instead, we'll use proper resize during compression
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
            } catch (e: Exception) {
                continuation.resume(Result.failure(e))
            }
        }
    }

    // Improved compress image method to handle resizing properly
    private fun compressImage(context: Context, uri: Uri, maxSizeKB: Int = 1024): ByteArray? {
        return try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            inputStream?.use { stream ->
                val originalBitmap = BitmapFactory.decodeStream(stream)

                // Rotate image based on EXIF data
                val rotatedBitmap = rotateImageIfRequired(context, originalBitmap, uri)

                // Calculate optimal dimensions
                val (width, height) = calculateOptimalDimensions(
                    rotatedBitmap.width,
                    rotatedBitmap.height,
                    maxWidth = 800,  // Reduced from 1080 to 800
                    maxHeight = 800  // Reduced from 1080 to 800
                )

                // Resize bitmap
                val resizedBitmap = Bitmap.createScaledBitmap(rotatedBitmap, width, height, true)

                // Compress to target size
                val compressedBytes = compressBitmapToTargetSize(resizedBitmap, maxSizeKB)

                // Clean up
                if (rotatedBitmap != originalBitmap) {
                    rotatedBitmap.recycle()
                }
                resizedBitmap.recycle()

                compressedBytes
            }
        } catch (e: Exception) {
            android.util.Log.e("CloudinaryService", "Error compressing image: ${e.message}", e)
            null
        }
    }

    // NEW: Hitung dimensi optimal
    private fun calculateOptimalDimensions(
        originalWidth: Int,
        originalHeight: Int,
        maxWidth: Int = 800,
        maxHeight: Int = 800
    ): Pair<Int, Int> {
        if (originalWidth <= maxWidth && originalHeight <= maxHeight) {
            return Pair(originalWidth, originalHeight)
        }

        val aspectRatio = originalWidth.toFloat() / originalHeight.toFloat()

        return if (aspectRatio > 1) {
            // Landscape
            val newWidth = min(maxWidth, originalWidth)
            val newHeight = (newWidth / aspectRatio).toInt()
            Pair(newWidth, newHeight)
        } else {
            // Portrait or square
            val newHeight = min(maxHeight, originalHeight)
            val newWidth = (newHeight * aspectRatio).toInt()
            Pair(newWidth, newHeight)
        }
    }

    // NEW: Rotate image berdasarkan EXIF data
    private fun rotateImageIfRequired(context: Context, bitmap: Bitmap, uri: Uri): Bitmap {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            inputStream?.use { stream ->
                val exif = ExifInterface(stream)
                val orientation = exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )

                when (orientation) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> rotateBitmap(bitmap, 90f)
                    ExifInterface.ORIENTATION_ROTATE_180 -> rotateBitmap(bitmap, 180f)
                    ExifInterface.ORIENTATION_ROTATE_270 -> rotateBitmap(bitmap, 270f)
                    else -> bitmap
                }
            } ?: bitmap
        } catch (e: Exception) {
            android.util.Log.w("CloudinaryService", "Could not read EXIF data: ${e.message}")
            bitmap
        }
    }

    // NEW: Rotate bitmap
    private fun rotateBitmap(bitmap: Bitmap, degrees: Float): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(degrees)
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    // IMPROVED: Better quality settings for JPEG compression
    private fun compressBitmapToTargetSize(bitmap: Bitmap, maxSizeKB: Int): ByteArray {
        val maxSizeBytes = maxSizeKB * 1024
        var quality = 90
        var compressedBytes: ByteArray

        do {
            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
            compressedBytes = outputStream.toByteArray()

            if (compressedBytes.size <= maxSizeBytes || quality <= 10) {
                break
            }

            quality -= 10
        } while (compressedBytes.size > maxSizeBytes)

        android.util.Log.d("CloudinaryService",
            "Compressed image: ${compressedBytes.size / 1024}KB with quality: $quality%")

        return compressedBytes
    }

    // UPDATED: Upload bitmap with compress - simplified approach
    suspend fun uploadBitmap(
        bitmap: Bitmap,
        folder: String = "lubana_gym",
        maxSizeKB: Int = 1024
    ): Result<String> {
        return suspendCancellableCoroutine { continuation ->
            try {
                // Compress bitmap
                val compressedBytes = compressBitmapToTargetSize(bitmap, maxSizeKB)

                MediaManager.get().upload(compressedBytes)
                    .option("folder", folder)
                    .option("resource_type", "image")
                    // FIXED: Removed transformation parameters here too
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
            } catch (e: Exception) {
                continuation.resume(Result.failure(e))
            }
        }
    }

    // NEW: Utility method untuk mendapatkan ukuran file dari URI
    fun getImageSizeKB(context: Context, uri: Uri): Long {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            inputStream?.use { stream ->
                val bytes = stream.readBytes()
                bytes.size / 1024L
            } ?: 0L
        } catch (e: Exception) {
            0L
        }
    }
}