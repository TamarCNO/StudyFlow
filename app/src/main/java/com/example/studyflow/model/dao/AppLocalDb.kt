package com.example.studyflow.model.dao

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.studyflow.base.MyApplication
import com.example.studyflow.model.Session

@Database(entities = [Session::class], version = 10)
abstract class AppLocalDbRepository : RoomDatabase() {
    abstract fun sessionDao(): SessionDao
}

object AppLocalDb {
    val db: AppLocalDbRepository by lazy {
        val context = MyApplication.Globals.appContext
            ?: throw IllegalStateException("Application context not available")

        Room.databaseBuilder(
            context,
            AppLocalDbRepository::class.java,
            "studyflow_database.db"
        )
            .fallbackToDestructiveMigration()
            .build()
    }
}
