package com.example.studyflow.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sessions")
data class Session(
    @PrimaryKey val id: String,
    val topic: String,
    val date: String,
    val status: String,
    val studentEmail: String
) {
    companion object {
        private const val ID_KEY = "id"
        private const val TOPIC_KEY = "topic"
        private const val DATE_KEY = "date"
        private const val STATUS_KEY = "status"
        private const val STUDENT_EMAIL_KEY = "studentEmail"

        fun fromJSON(json: Map<String, Any>): Session = with(json) {
            val id = this[ID_KEY] as? String ?: ""
            val topic = this[TOPIC_KEY] as? String ?: ""
            val date = this[DATE_KEY] as? String ?: ""
            val status = this[STATUS_KEY] as? String ?: ""
            val studentEmail = this[STUDENT_EMAIL_KEY] as? String ?: ""

            return Session(
                id = id,
                topic = topic,
                date = date,
                status = status,
                studentEmail = studentEmail
            )
        }
    }

    val json: Map<String, Any>
        get() = mapOf(
            ID_KEY to id,
            TOPIC_KEY to topic,
            DATE_KEY to date,
            STATUS_KEY to status,
            STUDENT_EMAIL_KEY to studentEmail
        )
}
