package com.example.studyflow.model

import android.graphics.Bitmap
import android.util.Log

class Model private constructor() {
    companion object {
        val shared = Model()
    }

    private val firebaseModel = FirebaseModel()
    private val cloudinaryModel = CloudinaryModel.getInstance()

    fun getAllSessions(callback: (List<Session>) -> Unit) {
        firebaseModel.getAllSessions(callback)
    }

    fun addSession(session: Session, image: Bitmap?, callback: (Boolean) -> Unit) {
        if (image == null) {
            firebaseModel.add(session) {
                callback(true)
            }
        } else {
            cloudinaryModel.uploadBitmap(image,
                onSuccess = { imageUrl ->
                    session.materialImageUrl = imageUrl
                    firebaseModel.add(session) {
                        callback(true)
                    }
                },
                onError = { error ->
                    Log.e("Model", "Cloudinary upload failed: $error")
                    callback(false)
                }
            )
        }
    }
}