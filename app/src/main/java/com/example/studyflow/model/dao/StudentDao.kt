package com.example.studyflow.model.dao

import androidx.room.*
import com.example.studyflow.model.Student
import androidx.lifecycle.LiveData

@Dao
interface StudentDao {
    @Query("SELECT * FROM students")
    fun getAll(): LiveData<List<Student>>

    @Query("SELECT * FROM students WHERE id = :id")
    fun getStudentById(id: String): LiveData<Student?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(vararg students: Student)

    @Delete
    suspend fun delete(student: Student)
}
