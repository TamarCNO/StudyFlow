package com.example.studyflow

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment

class SignUpFragment : Fragment() {

    private lateinit var nameInput: EditText
    private lateinit var emailInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var signUpButton: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var loginRedirectButton: Button

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_sign_up, container, false)

        // Initialize views
        nameInput = view.findViewById(R.id.etName)
        emailInput = view.findViewById(R.id.etEmail)
        passwordInput = view.findViewById(R.id.etPassword)
        signUpButton = view.findViewById(R.id.btnSignUp)
        progressBar = view.findViewById(R.id.register_progress)
        loginRedirectButton = view.findViewById(R.id.move_to_log_in)

        // Sign up button click
        signUpButton.setOnClickListener {
            signUpUser()
        }

        // Navigate to login
        loginRedirectButton.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, LoginFragment())
                .commit()
        }

        return view
    }

    private fun signUpUser() {
        val name = nameInput.text.toString().trim()
        val email = emailInput.text.toString().trim()
        val password = passwordInput.text.toString().trim()

        // Validation
        when {
            name.isEmpty() -> {
                nameInput.error = "Please enter your name"
                return
            }
            email.isEmpty() -> {
                emailInput.error = "Please enter your email"
                return
            }
            password.length < 6 -> {
                passwordInput.error = "Password must be at least 6 characters"
                return
            }
        }

        // Simulate signup
        progressBar.visibility = View.VISIBLE
        signUpButton.isEnabled = false

        nameInput.postDelayed({
            progressBar.visibility = View.GONE
            signUpButton.isEnabled = true
            Toast.makeText(requireContext(), "Signed up successfully!", Toast.LENGTH_SHORT).show()
        }, 1000)
    }
}
