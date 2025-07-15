package com.example.studyflow.base

import android.app.Application
import com.cloudinary.android.MediaManager
import com.cloudinary.android.policy.GlobalUploadPolicy
import com.cloudinary.android.policy.UploadPolicy

class MyApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Set the application context first
        Globals.appContext = applicationContext

        // Initialize Cloudinary MediaManager
        try {
            val config = mapOf(
                "cloud_name" to "dcicwlwov"
            )
            MediaManager.init(this, config)

            // Set global upload policy after successful initialization
            MediaManager.get().globalUploadPolicy = GlobalUploadPolicy.Builder()
                .maxConcurrentRequests(3)
                .networkPolicy(UploadPolicy.NetworkType.UNMETERED)
                .build()
        } catch (e: Exception) {
            // Log error but don't crash the app
            android.util.Log.e("MyApplication", "Failed to initialize Cloudinary MediaManager", e)
        }
    }

    object Globals {
        var appContext: android.content.Context? = null
    }
}
