package com.example.studyflow.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Student(
    @PrimaryKey val id: String = "",
    val first_name: String = "",
    val last_name: String = "",
    val password: String = "",
    val email: String = "",
    val profileImageUrl: String? = null
)
