    package com.example.studyflow.model.dao

    import androidx.lifecycle.LiveData
    import androidx.room.*
    import com.example.studyflow.model.Session

    @Dao
    interface SessionDao {
        @Query("SELECT * FROM Session")
        fun getAll(): LiveData<List<Session>>

        @Query("SELECT * FROM Session WHERE id = :id")
        fun getSessionById(id: String): LiveData<Session>

        @Insert(onConflict = OnConflictStrategy.REPLACE)
        suspend fun insertAll(vararg sessions: Session)
        @Delete
        suspend fun delete(session: Session)
    }