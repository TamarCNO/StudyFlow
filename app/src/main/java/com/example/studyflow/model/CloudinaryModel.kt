package com.example.studyflow.model

import android.content.Context
import android.graphics.Bitmap
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
            false
        }
    }

    fun uploadBitmap(bitmap: Bitmap, onSuccess: (String) -> Unit, onError: (String) -> Unit) {
        val context = MyApplication.Globals.appContext ?: run {
            onError("Application context is null, cannot upload.")
            return
        }

        val file = bitmapToFile(bitmap, context)

        try {
            MediaManager.get()
                .upload(file.path)
                .unsigned("studyflow_unsigned_uploads")
                .option("folder", "studyflow_images")
                .callback(object : UploadCallback {
                    override fun onStart(requestId: String) {}

                    override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {}

                    override fun onSuccess(requestId: String, resultData: Map<*, *>) {
                        val publicUrl = resultData["secure_url"] as? String ?: ""
                        file.delete()
                        onSuccess(publicUrl)
                    }

                    override fun onError(requestId: String?, error: ErrorInfo?) {
                        onError(error?.description ?: "Unknown error")
                        file.delete()
                    }

                    override fun onReschedule(requestId: String?, error: ErrorInfo?) {}
                })
                .dispatch()
        } catch (e: Exception) {
            onError("MediaManager error: ${e.message}")
        }
    }

    fun bitmapToFile(bitmap: Bitmap, context: Context): File {
        val file = File(context.cacheDir, "temp_image_${System.currentTimeMillis()}.jpg")
        FileOutputStream(file).use { outputStream ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
        }
        return file
    }
}