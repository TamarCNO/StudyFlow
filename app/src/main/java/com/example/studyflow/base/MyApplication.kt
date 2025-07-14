package com.example.studyflow.base

import android.app.Application
import com.cloudinary.android.MediaManager
import com.cloudinary.android.policy.GlobalUploadPolicy
import com.cloudinary.android.policy.UploadPolicy

class MyApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        val config = mapOf(
            "cloud_name" to "dcicwlwov"
        )
        MediaManager.init(this, config)

        MediaManager.get().globalUploadPolicy = GlobalUploadPolicy.Builder()
            .maxConcurrentRequests(3)
            .networkPolicy(UploadPolicy.NetworkType.UNMETERED)
            .build()

        Globals.appContext = applicationContext
    }

    object Globals {
        var appContext: android.content.Context? = null
    }
}
