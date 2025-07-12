package com.example.studyflow.auth

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

class AuthViewModel : ViewModel() {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    private val _user = MutableLiveData<FirebaseUser?>(auth.currentUser)
    val user: LiveData<FirebaseUser?> = _user

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    private val _loading = MutableLiveData(false)
    val loading: LiveData<Boolean> = _loading

    fun signUp(email: String, password: String, onSuccess: () -> Unit) {
        _loading.value = true
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                _loading.value = false
                if (task.isSuccessful) {
                    _user.value = auth.currentUser
                    onSuccess()
                } else {
                    _errorMessage.value = task.exception?.message ?: "Sign up failed"
                }
            }
    }

    fun login(email: String, password: String, onSuccess: () -> Unit) {
        _loading.value = true
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                _loading.value = false
                if (task.isSuccessful) {
                    _user.value = auth.currentUser
                    onSuccess()
                } else {
                    _errorMessage.value = task.exception?.message ?: "Login failed"
                }
            }
    }
    fun isUserLoggedIn(): Boolean = auth.currentUser != null
    fun logout() {
        auth.signOut()
        _user.value = null
    }

    fun clearError() {
        _errorMessage.value = null
    }
}
