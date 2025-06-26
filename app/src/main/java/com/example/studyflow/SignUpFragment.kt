package com.example.studyflow
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment

class SignUpFragment : Fragment() {
    private lateinit var nameEditText: EditText
    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var registerButton: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var loginLink: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_sign_up, container, false)

        nameEditText = view.findViewById(R.id.nameEditText)
        emailEditText = view.findViewById(R.id.emailEditText)
        passwordEditText = view.findViewById(R.id.passwordEditText)
        registerButton = view.findViewById(R.id.registerButton)
        progressBar = view.findViewById(R.id.register_progress)
        loginLink = view.findViewById(R.id.loginLink)

        registerButton.setOnClickListener {
        }

        loginLink.setOnClickListener {
        }

        return view
    }
}