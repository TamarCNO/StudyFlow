package com.example.studyflow

import android.os.Bundle
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.navigation.Navigation

class LoginFragment : Fragment() {

    private lateinit var profileImageView: ImageView
    private lateinit var cameraButton: ImageButton
    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var loginButton: Button
    private lateinit var signUpLink: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_login, container, false)

        profileImageView = view.findViewById(R.id.profileImageView)
        cameraButton = view.findViewById(R.id.cameraButton)
        emailEditText = view.findViewById(R.id.emailEditText)
        passwordEditText = view.findViewById(R.id.passwordEditText)
        loginButton = view.findViewById(R.id.loginButton)
        signUpLink = view.findViewById(R.id.signUpLink)

        cameraButton.setOnClickListener {
            Toast.makeText(requireContext(), "Camera clicked - implement photo capture", Toast.LENGTH_SHORT).show()
            // TODO: הוסף כאן קוד לפתיחת מצלמה או בחירת תמונה
        }

        loginButton.setOnClickListener {
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString()

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                emailEditText.error = "Please enter a valid email"
                return@setOnClickListener
            }

            if (password.length < 6) {
                passwordEditText.error = "Password must be at least 6 characters"
                return@setOnClickListener
            }

            // TODO: הוסף כאן את הלוגיקה של אימות המשתמש (API, DB, Firebase וכו')
            Toast.makeText(requireContext(), "Logging in...", Toast.LENGTH_SHORT).show()
        }

        signUpLink.setOnClickListener {
            Navigation.findNavController(it).navigate(R.id.action_loginFragment_to_signUpFragment)
        }

        return view
    }
}
