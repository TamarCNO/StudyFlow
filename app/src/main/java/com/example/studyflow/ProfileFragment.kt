package com.example.studyflow

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.squareup.picasso.Picasso

class ProfileFragment : Fragment() {

    private lateinit var profileImageView: ImageView
    private lateinit var textFirstName: TextView
    private lateinit var textLastName: TextView
    private lateinit var textEmail: TextView
    private lateinit var buttonSessions: Button
    private lateinit var buttonEditProfile: Button
    private lateinit var buttonLogout: Button

    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        profileImageView = view.findViewById(R.id.profileImageView)
        textFirstName = view.findViewById(R.id.textFirstName)
        textLastName = view.findViewById(R.id.textLastName)
        textEmail = view.findViewById(R.id.textEmail)
        buttonSessions = view.findViewById(R.id.buttonSessions)
        buttonEditProfile = view.findViewById(R.id.buttonEditProfile)
        buttonLogout = view.findViewById(R.id.buttonLogout)

        loadUserInfo()

        buttonSessions.setOnClickListener {
            findNavController().navigate(R.id.action_profileFragment_to_sessionsFragmentList)
        }

        buttonEditProfile.setOnClickListener {
            findNavController().navigate(R.id.action_profileFragment_to_editProfileFragment)
        }

        buttonLogout.setOnClickListener {
            auth.signOut()
            findNavController().navigate(R.id.action_profileFragment_to_loginFragment)
        }
    }

    private fun loadUserInfo() {
        val user: FirebaseUser? = auth.currentUser
        if (user == null) {
            Toast.makeText(requireContext(), "User not logged in", Toast.LENGTH_SHORT).show()
            findNavController().navigate(R.id.action_profileFragment_to_loginFragment)
            return
        }

        // מציגים את המייל
        textEmail.text = user.email ?: "No email"

        // מציגים את השם מלא אם קיים
        val displayName = user.displayName ?: ""
        if (displayName.contains(" ")) {
            val parts = displayName.split(" ")
            textFirstName.text = parts.getOrNull(0) ?: ""
            textLastName.text = parts.getOrNull(1) ?: ""
        } else {
            // אם אין שם מלא מפוצל, מציגים הכל בשם פרטי
            textFirstName.text = displayName
            textLastName.text = ""
        }

        // טוענים את תמונת הפרופיל אם יש
        val photoUrl = user.photoUrl
        if (photoUrl != null) {
            Picasso.get()
                .load(photoUrl)
                .placeholder(R.drawable.profile_placeholder)
                .error(R.drawable.profile_placeholder)
                .into(profileImageView)
        } else {
            profileImageView.setImageResource(R.drawable.profile_placeholder)
        }
    }
}
