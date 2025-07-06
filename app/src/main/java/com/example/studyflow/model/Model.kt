package com.example.studyflow.model

import android.graphics.Bitmap

class Model private constructor() {

    private val cloudinaryModel = CloudinaryModel()

    companion object {
        val shared = Model()
    }

    fun addSessionWithImage(session: Session, image: Bitmap?, callback: (Session?) -> Unit) {
        if (image == null) {
            callback(session)
            return
        }

        uploadImageToCloudinary(image, session.id,
            onSuccess = { imageUrl ->
                val updatedSession = session.copy(imageUrl = imageUrl)
                callback(updatedSession)
            },
            onError = {
                callback(null)
            }
        )
    }

    private fun uploadImageToCloudinary(
        image: Bitmap,
        name: String,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        cloudinaryModel.uploadBitmap(
            bitmap = image,
            onSuccess = onSuccess,
            onError = onError
        )
    }
}
