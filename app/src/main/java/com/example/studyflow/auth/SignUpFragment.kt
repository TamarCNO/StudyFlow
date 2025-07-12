package com.example.studyflow.auth

import AuthViewModel
import android.os.Bundle
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.Navigation
import com.example.studyflow.R


class SignUpFragment : Fragment() {

    private val viewModel: AuthViewModel by viewModels()

    private lateinit var profileImageView: ImageView
    private lateinit var cameraButton: ImageButton
    private lateinit var firstNameEditText: EditText
    private lateinit var lastNameEditText: EditText
    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var confirmPasswordEditText: EditText
    private lateinit var registerButton: Button
    private lateinit var alreadyHaveAccountLink: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_sign_up, container, false)

        profileImageView = view.findViewById(R.id.profileImageView)
        cameraButton = view.findViewById(R.id.cameraButton)
        firstNameEditText = view.findViewById(R.id.firstNameEditText)
        lastNameEditText = view.findViewById(R.id.lastNameEditText)
        emailEditText = view.findViewById(R.id.emailEditText)
        passwordEditText = view.findViewById(R.id.passwordEditText)
        confirmPasswordEditText = view.findViewById(R.id.confirmPasswordEditText)
        registerButton = view.findViewById(R.id.registerButton)
        alreadyHaveAccountLink = view.findViewById(R.id.alreadyHaveAccountLink)

        cameraButton.setOnClickListener {
            Toast.makeText(requireContext(), "Implement camera or gallery selection", Toast.LENGTH_SHORT).show()
            // TODO: הוסף קוד לפתיחת מצלמה או גלריה
        }

        registerButton.setOnClickListener {
            val firstName = firstNameEditText.text.toString().trim()
            val lastName = lastNameEditText.text.toString().trim()
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString()
            val confirmPassword = confirmPasswordEditText.text.toString()

            if (firstName.isEmpty()) {
                firstNameEditText.error = "Please enter your first name"
                return@setOnClickListener
            }
            if (lastName.isEmpty()) {
                lastNameEditText.error = "Please enter your last name"
                return@setOnClickListener
            }
            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                emailEditText.error = "Please enter a valid email"
                return@setOnClickListener
            }
            if (password.length < 6) {
                passwordEditText.error = "Password must be at least 6 characters"
                return@setOnClickListener
            }
            if (password != confirmPassword) {
                confirmPasswordEditText.error = "Passwords do not match"
                return@setOnClickListener
            }

            registerButton.isEnabled = false

            val form = RegisterForm(firstName, lastName, email, password)
            viewModel.register(form) {
                registerButton.isEnabled = true
                // ניווט חזרה למסך ההתחברות אחרי הרשמה מוצלחת
                Navigation.findNavController(view).navigate(R.id.action_signUpFragment_to_loginFragment)
            }
        }

        alreadyHaveAccountLink.setOnClickListener {
            Navigation.findNavController(it).navigate(R.id.action_signUpFragment_to_loginFragment)
        }

        // התבוננות בטעינה ושגיאות
        viewModel.loadingState.observe(viewLifecycleOwner, Observer { loadingState ->
            registerButton.isEnabled = (loadingState != LoadingState.Loading)
        })

        viewModel.exceptionsState.observe(viewLifecycleOwner, Observer { exception ->
            exception?.let {
                Toast.makeText(requireContext(), "Error: ${it.message}", Toast.LENGTH_LONG).show()
            }
        })

        return view
    }
}
