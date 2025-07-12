package com.example.studyflow.model.repository

import androidx.lifecycle.LiveData
import com.example.studyflow.model.Student
import com.example.studyflow.model.dao.StudentDao

class StudentRepository(private val studentDao: StudentDao) {
    fun getAllStudents(): LiveData<List<Student>> = studentDao.getAll()

    fun getStudentById(id: String): LiveData<Student?> = studentDao.getStudentById(id)

    suspend fun insertStudent(student: Student) {
        studentDao.insertAll(student)
    }

    suspend fun deleteStudent(student: Student) {
        studentDao.delete(student)
    }
}
