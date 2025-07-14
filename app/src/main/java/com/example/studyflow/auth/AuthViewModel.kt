package com.example.studyflow.auth

import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.studyflow.model.CloudinaryModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class AuthViewModel : ViewModel() {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val cloudinaryModel: CloudinaryModel

    private val _user = MutableLiveData<FirebaseUser?>()
    val user: LiveData<FirebaseUser?> = _user

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading

    init {
        cloudinaryModel = CloudinaryModel()
        _user.value = auth.currentUser
        auth.addAuthStateListener { firebaseAuth ->
            _user.value = firebaseAuth.currentUser
            Log.d("AuthViewModel", "Auth state changed. Current user: ${firebaseAuth.currentUser?.email}")
        }
    }

    fun register(email: String, password: String, onSuccess: (() -> Unit)? = null) {
        if (email.isBlank() || password.isBlank()) {
            _errorMessage.value = "Email and password cannot be empty."
            return
        }
        _loading.value = true
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                _loading.value = false
                if (task.isSuccessful) {
                    _user.value = auth.currentUser
                    onSuccess?.invoke()
                } else {
                    val message = when (task.exception) {
                        is FirebaseAuthUserCollisionException -> "Email already in use. Please log in or use a different email."
                        else -> task.exception?.message ?: "Registration failed. Please try again."
                    }
                    _errorMessage.value = message
                }
            }
    }

    fun signUp(email: String, password: String, onSuccess: (() -> Unit)? = null) {
        register(email, password, onSuccess)
    }

    fun login(email: String, password: String, onSuccess: (() -> Unit)? = null) {
        if (email.isBlank() || password.isBlank()) {
            _errorMessage.value = "Email and password cannot be empty."
            return
        }
        _loading.value = true
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                _loading.value = false
                if (task.isSuccessful) {
                    _user.value = auth.currentUser
                    onSuccess?.invoke()
                } else {
                    val message = task.exception?.message ?: "Login failed. Please check your credentials."
                    _errorMessage.value = message
                }
            }
    }

    fun logout() {
        auth.signOut()
    }

    fun updateProfile(
        firstName: String,
        lastName: String,
        email: String,
        selectedImageBitmap: Bitmap?,
        onSuccess: () -> Unit
    ) {
        val user = auth.currentUser ?: run {
            _errorMessage.value = "User not logged in."
            return
        }
        _loading.value = true
        _errorMessage.value = null
        viewModelScope.launch {
            try {
                val photoUrl: String? = if (selectedImageBitmap != null) {
                    suspendCancellableCoroutine { continuation ->
                        cloudinaryModel.uploadBitmap(selectedImageBitmap, { url ->
                            continuation.resume(url)
                        }, { errorMsg ->
                            continuation.resumeWithException(Exception(errorMsg))
                        })
                    }
                } else {
                    user.photoUrl?.toString()
                }
                if (email != user.email) {
                    suspendCancellableCoroutine<Unit> { continuation ->
                        user.updateEmail(email)
                            .addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    continuation.resume(Unit)
                                } else {
                                    continuation.resumeWithException(task.exception ?: Exception())
                                }
                            }
                    }
                }
                val fullName = "$firstName $lastName".trim()
                val profileUpdates = UserProfileChangeRequest.Builder()
                    .setDisplayName(fullName)
                    .setPhotoUri(photoUrl?.let { Uri.parse(it) })
                    .build()
                suspendCancellableCoroutine<Unit> { continuation ->
                    user.updateProfile(profileUpdates)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                _user.value = auth.currentUser
                                continuation.resume(Unit)
                            } else {
                                continuation.resumeWithException(task.exception ?: Exception())
                            }
                        }
                }
                onSuccess()
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Failed to update profile."
            } finally {
                _loading.value = false
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }
}
