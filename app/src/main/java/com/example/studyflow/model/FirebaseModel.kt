package com.example.studyflow.model

import android.util.Log
import com.example.studyflow.base.Constants
import com.google.firebase.firestore.FirebaseFirestore

class FirebaseModel {
    private val db = FirebaseFirestore.getInstance()

    fun getAllSessions(callback: (List<Session>) -> Unit) {
        db.collection(Constants.COLLECTIONS.SESSIONS).get()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val sessions = task.result.toObjects(Session::class.java)
                    callback(sessions)
                } else {
                    Log.e("FirebaseModel", "getAllSessions failed: ${task.exception?.message}")
                    callback(emptyList())
                }
            }
    }

    fun add(session: Session, callback: (Boolean) -> Unit) {
        db.collection(Constants.COLLECTIONS.SESSIONS)
            .document(session.id)
            .set(session)
            .addOnSuccessListener {
                Log.d("FirebaseModel", "Session added successfully: ${session.id}")
                callback(true)
            }
            .addOnFailureListener { exception ->
                Log.e("FirebaseModel", "add session failed: ${exception.message}")
                callback(false)
            }
    }

    fun delete(session: Session, callback: (Boolean) -> Unit) {
        db.collection(Constants.COLLECTIONS.SESSIONS)
            .document(session.id)
            .delete()
            .addOnSuccessListener {
                Log.d("FirebaseModel", "Session deleted successfully: ${session.id}")
                callback(true)
            }
            .addOnFailureListener { exception ->
                Log.e("FirebaseModel", "Failed to delete session: ${exception.message}")
                callback(false)
            }
    }
}
