package com.example.studyflow.base

import android.app.Application
import android.content.Context
import java.util.concurrent.Executors

class MyApplication : Application() {

    object Globals {
        var appContext: Context? = null
        val executorService = Executors.newFixedThreadPool(4)
    }

    override fun onCreate() {
        super.onCreate()
        Globals.appContext = applicationContext
    }
}
