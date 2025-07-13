package com.example.studyflow.auth

import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.UserProfileChangeRequest

class AuthViewModel : ViewModel() {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    private val _user = MutableLiveData<FirebaseUser?>()
    val user: LiveData<FirebaseUser?> = _user

    // FIX APPLIED HERE: Changed to MutableLiveData<String?> to allow null
    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading

    init {
        _user.value = auth.currentUser
        auth.addAuthStateListener { firebaseAuth ->
            _user.value = firebaseAuth.currentUser
            Log.d("AuthViewModel", "Auth state changed. Current user: ${firebaseAuth.currentUser?.email}")
        }
    }

    /**
     * Registers a new user with email and password.
     * This function is also aliased as 'signUp' for clarity in UI.
     * @param onSuccess Callback to be invoked on successful registration.
     */
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
                    Log.d("AuthViewModel", "User registration successful for $email")
                    _user.value = auth.currentUser
                    onSuccess?.invoke()
                } else {
                    val message = when (task.exception) {
                        is FirebaseAuthUserCollisionException -> "Email already in use. Please log in or use a different email."
                        else -> task.exception?.message ?: "Registration failed. Please try again."
                    }
                    Log.e("AuthViewModel", "User registration failed: $message", task.exception)
                    _errorMessage.value = message
                }
            }
    }

    // THIS IS THE MISSING FUNCTION THAT SignUpFragment IS CALLING
    fun signUp(email: String, password: String, onSuccess: (() -> Unit)? = null) {
        register(email, password, onSuccess)
    }

    /**
     * Logs in an existing user with email and password.
     * @param onSuccess Callback to be invoked on successful login.
     */
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
                    Log.d("AuthViewModel", "User login successful for $email")
                    _user.value = auth.currentUser
                    onSuccess?.invoke()
                } else {
                    val message = task.exception?.message ?: "Login failed. Please check your credentials."
                    Log.e("AuthViewModel", "User login failed: $message", task.exception)
                    _errorMessage.value = message
                }
            }
    }

    /**
     * Logs out the current user.
     */
    fun logout() {
        auth.signOut()
        Log.d("AuthViewModel", "User logged out.")
    }

    /**
     * Updates the current user's profile information (display name, email, photo URL).
     * @param newDisplayName The new display name for the user.
     * @param newEmail The new email for the user.
     * @param newPhotoUrl The new photo URL for the user, or null to remove/keep existing.
     * @param onSuccess Callback invoked on successful profile update.
     */
    fun updateProfile(newDisplayName: String, newEmail: String, newPhotoUrl: String?, onSuccess: () -> Unit) {
        val user = auth.currentUser ?: run {
            _errorMessage.value = "User not logged in."
            return
        }

        _loading.value = true

        val updateEmailTask = if (newEmail != user.email) {
            user.updateEmail(newEmail)
        } else null

        val performProfileUpdate = {
            val profileUpdates = UserProfileChangeRequest.Builder().apply {
                displayName = newDisplayName
                photoUri = newPhotoUrl?.let { Uri.parse(it) }
            }.build()

            user.updateProfile(profileUpdates)
                .addOnCompleteListener { task ->
                    _loading.value = false
                    if (task.isSuccessful) {
                        Log.d("AuthViewModel", "Profile updated successfully.")
                        _user.value = auth.currentUser // Refresh user LiveData
                        onSuccess()
                    } else {
                        val message = task.exception?.message ?: "Failed to update profile."
                        Log.e("AuthViewModel", "Profile update failed: $message", task.exception)
                        _errorMessage.value = message
                    }
                }
        }

        if (updateEmailTask != null) {
            updateEmailTask.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d("AuthViewModel", "Email updated successfully.")
                    performProfileUpdate()
                } else {
                    _loading.value = false
                    val message = task.exception?.message ?: "Failed to update email."
                    Log.e("AuthViewModel", "Email update failed: $message", task.exception)
                    _errorMessage.value = message
                }
            }
        } else {
            performProfileUpdate()
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }
}