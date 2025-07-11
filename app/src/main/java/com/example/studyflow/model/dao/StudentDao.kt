package com.example.studyflow.model.dao

import androidx.room.*
import com.example.studyflow.model.Student

@Dao
interface StudentDao {
    @Query("SELECT * FROM students")
    fun getAll(): List<Student>

    @Query("SELECT * FROM students WHERE id = :id")
    fun getStudentById(id: String): Student?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(vararg students: Student)

    @Delete
    suspend fun delete(student: Student)
}
