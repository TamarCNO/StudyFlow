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
        MyApplication.Globals.context?.let { appContext  ->
            MediaManager.init(appContext , cloudinaryConfig)
            MediaManager.get().globalUploadPolicy = GlobalUploadPolicy.Builder()
                .maxConcurrentRequests(3)
                .networkPolicy(UploadPolicy.NetworkType.UNMETERED)
                .build()
        }
    }
        fun uploadBitmap(bitmap: Bitmap, onSuccess: (String) -> Unit, onError: (String) -> Unit) {
            val context = MyApplication.Globals.context ?: run {
                onError("Application context is null, cannot upload.")
                return
            }
val file = bitmapToFile(bitmap, context)
        MediaManager.get().upload(file.path)
            .option("upload_preset", "studyflow_unsigned_uploads") // Optional: Specify a folder in your Cloudinary account
            .callback(object : UploadCallback {
                override fun onStart(requestId: String) {
                    // Called when upload starts
                }

                override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {
                    // Called during upload progress
                }

                override fun onSuccess(requestId: String, resultData: Map<*, *>) {
                    val publicUrl = resultData["secure_url"] as? String ?: ""
                    if (publicUrl.isNotEmpty()) {
                    onSuccess(publicUrl) // Return the URL of the uploaded image
                    println("Cloudinary Upload Successful for $requestId. URL: $publicUrl")
                }
                    else {
                        onError("Empty URL received from Cloudinary")
                        System.err.println("Cloudinary Upload Warning: Public URL empty for $requestId.")
                    }
                    file.delete()
                }

                override fun onError(requestId: String?, error: ErrorInfo?) {
                    onError(error?.description ?: "Unknown error")
                }

                override fun onReschedule(requestId: String?, error: ErrorInfo?) {
                    println("Cloudinary Upload Rescheduled for $requestId. Error: ${error?.description}")                }

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