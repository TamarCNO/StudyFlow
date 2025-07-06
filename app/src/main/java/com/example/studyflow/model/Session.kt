package com.example.studyflow.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Session(
    @PrimaryKey val id: String,
    val title: String,
    val date: String,
    val status: String
)
