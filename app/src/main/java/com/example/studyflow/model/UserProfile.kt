package com.example.studyflow.model

data class UserProfile(
    val uid: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val email: String = "",
    val photoUrl: String? = null
)
