package com.example.studyflow.model

import androidx.room.Entity
import androidx.room.PrimaryKey

    @Entity(tableName = "posts")
    data class PostEntity(
        @PrimaryKey val id: String,
        val subject: String,
        val dateTime: String,
        val profileImageUrl: String,
        val locationAddress: String
    )