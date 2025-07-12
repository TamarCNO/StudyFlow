package com.example.studyflow.model

import android.content.Context
import android.graphics.Bitmap
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.cloudinary.android.policy.GlobalUploadPolicy
import com.cloudinary.android.policy.UploadPolicy
import com.example.studyflow.base.MyApplication
import java.io.File
import java.io.FileOutputStream

class CloudinaryModel {
    private val cloudinaryConfig = mapOf(
        "cloud_name" to "dcicwlwov"
    )

    init {
        MyApplication.Globals.appContext?.let { appContext  ->
            MediaManager.init(appContext , cloudinaryConfig)
            MediaManager.get().globalUploadPolicy = GlobalUploadPolicy.Builder()
                .maxConcurrentRequests(3)
                .networkPolicy(UploadPolicy.NetworkType.UNMETERED)
                .build()
        }
    }

    fun uploadBitmap(bitmap: Bitmap, onSuccess: (String) -> Unit, onError: (String) -> Unit) {
        val context = MyApplication.Globals.appContext ?: run {
            onError("Application context is null, cannot upload.")
            return
        }
        val file = bitmapToFile(bitmap, context)
        MediaManager.get().upload(file.path)
            .option("upload_preset", "studyflow_unsigned_uploads")
            .callback(object : UploadCallback {
                override fun onStart(requestId: String) { }
                override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) { }
                override fun onSuccess(requestId: String, resultData: Map<*, *>) {
                    val publicUrl = resultData["secure_url"] as? String ?: ""
                    if (publicUrl.isNotEmpty()) {
                        onSuccess(publicUrl)
                    } else {
                        onError("Empty URL received from Cloudinary")
                        System.err.println("Cloudinary Upload Warning: Public URL empty for $requestId.")
                    }
                    file.delete()
                }
                override fun onError(requestId: String?, error: ErrorInfo?) {
                    onError(error?.description ?: "Unknown error")
                    file.delete()
                }
                override fun onReschedule(requestId: String?, error: ErrorInfo?) {
                    println("Cloudinary Upload Rescheduled for $requestId. Error: ${error?.description}")
                    file.delete()
                }
            })
            .dispatch()
    }

    fun bitmapToFile(bitmap: Bitmap, context: Context): File {
        val file = File(context.cacheDir, "temp_image_${System.currentTimeMillis()}.jpg")
        FileOutputStream(file).use { outputStream ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
        }
        return file
    }
}
