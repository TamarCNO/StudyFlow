package com.example.studyflow.model.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.studyflow.model.Session

@Dao
interface SessionDao {

    @Query("SELECT * FROM sessions ORDER BY date DESC, time DESC")
    fun getAll(): LiveData<List<Session>>


    @Query("SELECT * FROM sessions WHERE id = :id")
    fun getById(id: String): LiveData<Session?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(session: Session)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(sessions: List<Session>)

    @Update
    fun update(session: Session)

    @Delete
    fun delete(session: Session)

    @Query("DELETE FROM sessions")
    fun deleteAll()
}