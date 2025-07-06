package com.example.studyflow

    class SignUpFragment : Fragment() {

        private var nameEditText: TextInputEditText? = null
        private var emailEditText: TextInputEditText? = null
        private var passwordEditText: TextInputEditText? = null
        private var registerButton: Button? = null
        private var registerProgress: ProgressBar? = null
        private var loginLink: TextView? = null

        override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
        ): View? {
            val view = inflater.inflate(R.layout.fragment_sign_up, container, false)

            // קישור לרכיבי ה־UI
            nameEditText = view.findViewById(R.id.nameEditText)
            emailEditText = view.findViewById(R.id.emailEditText)
            passwordEditText = view.findViewById(R.id.passwordEditText)
            registerButton = view.findViewById(R.id.registerButton)
            registerProgress = view.findViewById(R.id.register_progress)
            loginLink = view.findViewById(R.id.loginLink)

            // לחיצה על כפתור הרשמה
            registerButton?.setOnClickListener {
                onRegister()
            }

            // מעבר למסך התחברות
            loginLink?.setOnClickListener {
                (activity as? MainActivity)?.displayFragment(LogInFragment())
            }

            return view
        }

        private fun onRegister() {
            val name = nameEditText?.text.toString().trim()
            val email = emailEditText?.text.toString().trim()
            val password = passwordEditText?.text.toString().trim()

            // בדיקת תקינות
            if (name.isEmpty() || email.isEmpty() || password.length < 6) {
                Toast.makeText(context, "נא למלא את כל השדות כראוי", Toast.LENGTH_SHORT).show()
                return
            }

            // הצגת progress
            registerProgress?.visibility = View.VISIBLE
            registerButton?.isEnabled = false

            // קריאה לפונקציית הרשמה במודל
            Model.instance.doSignUp(name, email, password) { success ->
                activity?.runOnUiThread {
                    registerProgress?.visibility = View.INVISIBLE
                    registerButton?.isEnabled = true

                    if (success) {
                        Toast.makeText(context, "נרשמת בהצלחה!", Toast.LENGTH_SHORT).show()
                        (activity as? MainActivity)?.displayFragment(FeedFragment(), displayHomeButton = true)
                    } else {
                        Toast.makeText(context, "הרשמה נכשלה. נסה שוב.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

}