package com.example.studyflow.auth

import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.studyflow.model.CloudinaryModel
import com.example.studyflow.model.UserProfile
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class AuthViewModel : ViewModel() {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val cloudinaryModel: CloudinaryModel = CloudinaryModel.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()

    private val _user = MutableLiveData<FirebaseUser?>()
    val user: LiveData<FirebaseUser?> = _user

    private val _userProfile = MutableLiveData<UserProfile?>()
    val userProfile: LiveData<UserProfile?> = _userProfile

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading

    init {
        _user.value = auth.currentUser
        auth.addAuthStateListener { firebaseAuth ->
            _user.value = firebaseAuth.currentUser
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
                    fetchUserProfile()
                    onSuccess?.invoke()
                } else {
                    val message = task.exception?.message ?: "Login failed. Please check your credentials."
                    _errorMessage.value = message
                }
            }
    }

    fun logout() {
        auth.signOut()
        _user.value = null
        _userProfile.value = null
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

                val updatedProfile = UserProfile(
                    uid = user.uid,
                    firstName = firstName,
                    lastName = lastName,
                    email = email,
                    photoUrl = photoUrl
                )

                firestore.collection("user_profiles")
                    .document(user.uid)
                    .set(updatedProfile)
                    .addOnSuccessListener {
                        _userProfile.value = updatedProfile
                        onSuccess()
                    }
                    .addOnFailureListener { e ->
                        _errorMessage.value = e.message ?: "Failed to save user profile."
                    }

            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Failed to update profile."
            } finally {
                _loading.value = false
            }
        }
    }

    fun fetchUserProfile() {
        val uid = auth.currentUser?.uid ?: return
        firestore.collection("user_profiles")
            .document(uid)
            .get()
            .addOnSuccessListener { document ->
                val profile = document.toObject(UserProfile::class.java)
                _userProfile.value = profile
            }
            .addOnFailureListener { e ->
                _errorMessage.value = e.message ?: "Failed to load profile."
            }
    }

    fun clearError() {
        _errorMessage.value = null
    }
}
