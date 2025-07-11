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
    {
        companion object {
            private const val ID_KEY = "id"
            private const val SUBJECT_KEY = "subject"
            private const val DATE_TIME_KEY = "dateTime"
            private const val PROFILE_IMAGE_URL_KEY = "profileImageUrl"
            private const val LOCATION_ADDRESS_KEY = "locationAddress"

            fun fromJSON(json: Map<String, Any>): PostEntity = with(json) {
                val id = this[ID_KEY] as? String ?: ""
                val subject = this[SUBJECT_KEY] as? String ?: ""
                val dateTime = this[DATE_TIME_KEY] as? String ?: ""
                val profileImageUrl = this[PROFILE_IMAGE_URL_KEY] as? String ?: ""
                val locationAddress = this[LOCATION_ADDRESS_KEY] as? String ?: ""

                return PostEntity(
                    id = id,
                    subject = subject,
                    dateTime = dateTime,
                    profileImageUrl = profileImageUrl,
                    locationAddress = locationAddress
                )
            }
        }

        val json: Map<String, Any>
            get() = mapOf(
                ID_KEY to id,
                SUBJECT_KEY to subject,
                DATE_TIME_KEY to dateTime,
                PROFILE_IMAGE_URL_KEY to profileImageUrl,
                LOCATION_ADDRESS_KEY to locationAddress
            )
    }