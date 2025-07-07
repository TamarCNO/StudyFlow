package com.example.studyflow.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime

@Entity(tableName = "sessions")
data class Session(
    @PrimaryKey val id: String,
    val topic: String,
    val date: String,
    val status: String
)

