package com.example.studyflow.model

import android.util.Log
import com.example.studyflow.base.Constants
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.firestoreSettings
import com.google.firebase.firestore.memoryCacheSettings

class FirebaseModel {
    private val db = Firebase.firestore

    init {
        val settings = firestoreSettings {
            setLocalCacheSettings(memoryCacheSettings { })
        }
        db.firestoreSettings = settings
    }

    fun getAllSessions(callback: (List<Session>) -> Unit) {
        db.collection(Constants.COLLECTIONS.SESSIONS).get()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    try {
                        val sessions = task.result.toObjects(Session::class.java)
                        callback(sessions)
                    } catch (e: Exception) {
                        Log.e("FirebaseModel", "Failed to deserialize sessions: ${e.message}")

                        try {
                            val sessions = task.result.map { Session.fromJSON(it.data) }
                            callback(sessions)
                        } catch (fallbackException: Exception) {
                            Log.e("FirebaseModel", "Fallback fromJSON also failed: ${fallbackException.message}")
                            callback(emptyList())
                        }
                    }
                } else {
                    Log.e("FirebaseModel", "getAllSessions failed: ${task.exception?.message}")
                    callback(emptyList())
                }
            }
    }

    fun add(session: Session, callback: () -> Unit) {
        db.collection(Constants.COLLECTIONS.SESSIONS)
            .document(session.id)
            .set(session)
            .addOnSuccessListener {
                Log.d("FirebaseModel", "Session added successfully: ${session.id}")
                callback()
            }
            .addOnFailureListener { exception ->
                Log.e("FirebaseModel", "add session failed: ${exception.message}")
                callback()
            }
    }
}