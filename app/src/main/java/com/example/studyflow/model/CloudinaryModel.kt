package com.example.studyflow.model

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.example.studyflow.base.MyApplication
import java.io.File
import java.io.FileOutputStream

class CloudinaryModel {
    companion object {
        @Volatile
        private var INSTANCE: CloudinaryModel? = null
        fun getInstance(): CloudinaryModel {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: CloudinaryModel().also { INSTANCE = it }
            }
        }
    }

    private fun isMediaManagerInitialized(): Boolean {
        return try {
            MediaManager.get()
            true
        } catch (e: Exception) {
            Log.w("CloudinaryModel", "MediaManager not initialized: ${e.message}")
            false
        }
    }

    fun uploadBitmap(bitmap: Bitmap, onSuccess: (String) -> Unit, onError: (String) -> Unit) {
        val context = MyApplication.Globals.appContext ?: run {
            onError("Application context is null, cannot upload.")
            return
        }

        if (!isMediaManagerInitialized()) {
            onError("MediaManager is not initialized. Please restart the app.")
            return
        }

        var file: File? = null
        try {
            // Create temporary file
            file = bitmapToFile(bitmap, context)

            val mediaManager = try {
                MediaManager.get()
            } catch (e: Exception) {
                file.delete()
                onError("MediaManager became unavailable: ${e.message}")
                return
            }

            mediaManager
                .upload(file.path)
                .unsigned("studyflow_unsigned_uploads")
                .option("folder", "studyflow_images")
                .callback(object : UploadCallback {
                    override fun onStart(requestId: String) {
                        Log.d("CloudinaryModel", "Upload started: $requestId")
                    }

                    override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {
                        val progress = if (totalBytes > 0) (bytes * 100 / totalBytes) else 0
                        Log.d("CloudinaryModel", "Upload progress: $progress%")
                    }

                    override fun onSuccess(requestId: String, resultData: Map<*, *>) {
                        val publicUrl = resultData["secure_url"] as? String
                        file?.delete() // Clean up temp file

                        if (!publicUrl.isNullOrEmpty()) {
                            Log.d("CloudinaryModel", "Upload successful: $publicUrl")
                            onSuccess(publicUrl)
                        } else {
                            Log.e("CloudinaryModel", "Upload succeeded but no URL returned")
                            onError("Upload succeeded but no image URL received")
                        }
                    }

                    override fun onError(requestId: String?, error: ErrorInfo?) {
                        file?.delete() // Clean up temp file
                        val errorMsg = error?.description ?: "Unknown upload error"
                        Log.e("CloudinaryModel", "Upload failed: $errorMsg")
                        onError("Upload failed: $errorMsg")
                    }

                    override fun onReschedule(requestId: String?, error: ErrorInfo?) {
                        Log.w("CloudinaryModel", "Upload rescheduled: ${error?.description}")
                    }
                })
                .dispatch()

        } catch (e: Exception) {
            file?.delete() // Clean up temp file if it was created
            val errorMsg = "Failed to start upload: ${e.message}"
            Log.e("CloudinaryModel", errorMsg, e)
            onError(errorMsg)
        }
    }

    private fun bitmapToFile(bitmap: Bitmap, context: Context): File {
        val file = File(context.cacheDir, "temp_image_${System.currentTimeMillis()}.jpg")

        try {
            FileOutputStream(file).use { outputStream ->
                val compressed = bitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
                if (!compressed) {
                    throw Exception("Failed to compress bitmap")
                }
            }

            if (!file.exists() || file.length() == 0L) {
                throw Exception("Failed to create image file")
            }

            Log.d("CloudinaryModel", "Created temp file: ${file.path}, size: ${file.length()} bytes")
            return file

        } catch (e: Exception) {
            file.delete() // Clean up if something went wrong
            throw Exception("Failed to create temporary image file: ${e.message}")
        }
    }
}