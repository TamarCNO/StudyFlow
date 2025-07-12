package com.example.studyflow.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "students")
data class Student(
    @PrimaryKey val id: String = "",
    val first_name: String = "",
    val last_name: String = "",
    val password: String = "",
    val email: String = "",
    val profileImageUrl: String? = null
) {
    companion object {
        private const val ID_KEY = "id"
        private const val FIRST_NAME_KEY = "first_name"
        private const val LAST_NAME_KEY = "last_name"
        private const val PASSWORD_KEY = "password"
        private const val EMAIL_KEY = "email"
        private const val PROFILE_IMAGE_URL_KEY = "profileImageUrl"

        fun fromJSON(json: Map<String, Any>): Student = with(json) {
            val id = this[ID_KEY] as? String ?: ""
            val firstName = this[FIRST_NAME_KEY] as? String ?: ""
            val lastName = this[LAST_NAME_KEY] as? String ?: ""
            val password = this[PASSWORD_KEY] as? String ?: ""
            val email = this[EMAIL_KEY] as? String ?: ""
            val profileImageUrl = this[PROFILE_IMAGE_URL_KEY] as? String

            return Student(
                id = id,
                first_name = firstName,
                last_name = lastName,
                password = password,
                email = email,
                profileImageUrl = profileImageUrl
            )
        }
    }

    val json: Map<String, Any?>
        get() = mapOf(
            ID_KEY to id,
            FIRST_NAME_KEY to first_name,
            LAST_NAME_KEY to last_name,
            PASSWORD_KEY to password,
            EMAIL_KEY to email,
            PROFILE_IMAGE_URL_KEY to profileImageUrl
        )
}
