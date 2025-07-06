package com.example.studyflow.model.dao

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.studyflow.base.MyApplication
import com.example.studyflow.model.entities.Session

@Database(entities = [Session::class], version = 1)
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
            "dbFileName.db"
        )
            .fallbackToDestructiveMigration()
            .build()
    }
}
