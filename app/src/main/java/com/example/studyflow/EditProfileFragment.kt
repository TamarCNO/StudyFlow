package com.example.studyflow

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest

class EditProfileFragment : Fragment() {

    private lateinit var profileImageView: ImageView
    private lateinit var cameraIcon: ImageView
    private lateinit var inputFirstName: EditText
    private lateinit var inputLastName: EditText
    private lateinit var inputEmail: EditText
    private lateinit var buttonSave: Button
    private lateinit var buttonCancel: Button

    private val auth = FirebaseAuth.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_edit_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        profileImageView = view.findViewById(R.id.profileImageView)
        cameraIcon = view.findViewById(R.id.cameraIcon)
        inputFirstName = view.findViewById(R.id.inputFirstName)
        inputLastName = view.findViewById(R.id.inputLastName)
        inputEmail = view.findViewById(R.id.inputEmail)
        buttonSave = view.findViewById(R.id.buttonSave)
        buttonCancel = view.findViewById(R.id.buttonCancel)

        loadUserData()

        cameraIcon.setOnClickListener {
            Toast.makeText(requireContext(), "Change profile photo - to implement", Toast.LENGTH_SHORT).show()
            // כאן אפשר להוסיף פתיחת גלריה או מצלמה
        }

        buttonSave.setOnClickListener {
            val firstName = inputFirstName.text.toString().trim()
            val lastName = inputLastName.text.toString().trim()
            val email = inputEmail.text.toString().trim()

            if (firstName.isEmpty() || lastName.isEmpty() || email.isEmpty()) {
                Toast.makeText(requireContext(), "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            updateUserProfile(firstName, lastName, email)
        }

        buttonCancel.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun loadUserData() {
        val user = auth.currentUser
        if (user == null) {
            Toast.makeText(requireContext(), "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        inputEmail.setText(user.email ?: "")

        // מפצלים את ה-displayName לשם פרטי ושם משפחה
        val displayName = user.displayName ?: ""
        if (displayName.contains(" ")) {
            val parts = displayName.split(" ")
            inputFirstName.setText(parts.getOrNull(0) ?: "")
            inputLastName.setText(parts.getOrNull(1) ?: "")
        } else {
            inputFirstName.setText(displayName)
            inputLastName.setText("")
        }

        // אפשר להוסיף כאן טעינת תמונת פרופיל אם רוצים
    }

    private fun updateUserProfile(firstName: String, lastName: String, email: String) {
        val user = auth.currentUser ?: run {
            Toast.makeText(requireContext(), "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        // ראשית, מעדכנים את האימייל אם הוא שונה
        if (email != user.email) {
            user.updateEmail(email)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        // לאחר עדכון האימייל, נעדכן את שם המשתמש
                        updateDisplayName(user, firstName, lastName)
                    } else {
                        Toast.makeText(requireContext(), "Failed to update email: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                    }
                }
        } else {
            // אם האימייל לא שונה, רק נעדכן את השם
            updateDisplayName(user, firstName, lastName)
        }
    }

    private fun updateDisplayName(user: com.google.firebase.auth.FirebaseUser, firstName: String, lastName: String) {
        val fullName = "$firstName $lastName".trim()

        val profileUpdates = UserProfileChangeRequest.Builder()
            .setDisplayName(fullName)
            // כאן אפשר להוסיף גם setPhotoUri אם יש URL של תמונה
            .build()

        user.updateProfile(profileUpdates)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(requireContext(), "Profile updated successfully", Toast.LENGTH_SHORT).show()
                    findNavController().popBackStack()
                } else {
                    Toast.makeText(requireContext(), "Failed to update profile: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }
}
