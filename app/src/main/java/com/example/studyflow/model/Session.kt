package com.example.studyflow.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sessions")
data class Session(
    @PrimaryKey var id: String = "",
    var topic: String? = null,
    var date: String? = null,
    var time: String? = null,
    var status: String? = null,
    var studentEmail: String? = null,
    var materialImageUrl: String? = null,
    var locationAddress: String? = null
) {
    companion object {
        private const val ID_KEY = "id"
        private const val TOPIC_KEY = "topic"
        private const val DATE_KEY = "date"
        private const val TIME_KEY = "time"
        private const val STATUS_KEY = "status"
        private const val STUDENT_EMAIL_KEY = "studentEmail"
        private const val MATERIAL_IMAGE_URL_KEY = "materialImageUrl"
        private const val LOCATION_ADDRESS_KEY = "locationAddress"

        fun fromJSON(json: Map<String, Any>): Session = with(json) {
            val id = this[ID_KEY] as? String ?: ""
            val topic = this[TOPIC_KEY] as? String
            val date = this[DATE_KEY] as? String
            val time = this[TIME_KEY] as? String
            val status = this[STATUS_KEY] as? String
            val studentEmail = this[STUDENT_EMAIL_KEY] as? String
            val materialImageUrl = this[MATERIAL_IMAGE_URL_KEY] as? String
            val locationAddress = this[LOCATION_ADDRESS_KEY] as? String

            return Session(
                id = id,
                topic = topic,
                date = date,
                time = time,
                status = status,
                studentEmail = studentEmail,
                materialImageUrl = materialImageUrl,
                locationAddress = locationAddress
            )
        }
    }

    val json: Map<String, Any?>
        get() = mapOf(
            ID_KEY to id,
            TOPIC_KEY to topic,
            DATE_KEY to date,
            TIME_KEY to time,
            STATUS_KEY to status,
            STUDENT_EMAIL_KEY to studentEmail,
            MATERIAL_IMAGE_URL_KEY to materialImageUrl,
            LOCATION_ADDRESS_KEY to locationAddress
        )
}