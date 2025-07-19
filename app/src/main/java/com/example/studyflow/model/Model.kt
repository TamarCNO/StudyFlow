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
            firebaseModel.add(session) { success ->
                callback(success)
            }
        } else {
            cloudinaryModel.uploadBitmap(image,
                onSuccess = { imageUrl ->
                    session.materialImageUrl = imageUrl
                    firebaseModel.add(session) { success ->
                        callback(success)
                    }
                },
                onError = { error ->
                    Log.e("Model", "Cloudinary upload failed: $error")
                    callback(false)
                }
            )
        }
    }

    fun deleteSession(session: Session, callback: (Boolean) -> Unit) {
        firebaseModel.delete(session) { success ->
            callback(success)
        }
    }
}
