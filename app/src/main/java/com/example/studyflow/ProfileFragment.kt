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
import com.squareup.picasso.Picasso
import androidx.fragment.app.viewModels // Import for viewModels delegate
import com.example.studyflow.auth.AuthViewModel // Import your AuthViewModel
import com.google.firebase.auth.FirebaseUser // Keep this import for the user object

class ProfileFragment : Fragment() {

    private lateinit var profileImageView: ImageView
    private lateinit var textDisplayName: TextView // Changed to display name directly
    private lateinit var textEmail: TextView
    private lateinit var buttonSessions: Button
    private lateinit var buttonEditProfile: Button
    private lateinit var buttonLogout: Button

    // Use viewModels delegate to get AuthViewModel instance
    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_profile, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        profileImageView = view.findViewById(R.id.profileImageView)
        textDisplayName = view.findViewById(R.id.textDisplayName) // Link to new TextView in layout
        textEmail = view.findViewById(R.id.textEmail)
        buttonSessions = view.findViewById(R.id.buttonSessions)
        buttonEditProfile = view.findViewById(R.id.buttonEditProfile)
        buttonLogout = view.findViewById(R.id.buttonLogout)

        // Observe the user LiveData from AuthViewModel
        authViewModel.user.observe(viewLifecycleOwner) { user ->
            if (user != null) {
                displayUserInfo(user)
            } else {
                // If user becomes null (e.g., after logout from another part of the app), navigate to login
                Toast.makeText(requireContext(), "User not logged in", Toast.LENGTH_SHORT).show()
                findNavController().navigate(R.id.action_profileFragment_to_loginFragment)
            }
        }

        buttonSessions.setOnClickListener {
            findNavController().navigate(R.id.action_profileFragment_to_sessionsFragmentList)
        }

        buttonEditProfile.setOnClickListener {
            // Navigate to the edit profile screen
            findNavController().navigate(R.id.action_profileFragment_to_editProfileFragment)
        }

        buttonLogout.setOnClickListener {
            authViewModel.logout() // Use ViewModel's logout function
            Toast.makeText(requireContext(), "Logged out successfully", Toast.LENGTH_SHORT).show()
            findNavController().navigate(R.id.action_profileFragment_to_loginFragment)
        }
    }

    /**
     * Displays the current user's information on the profile screen.
     */
    private fun displayUserInfo(user: FirebaseUser) {
        textEmail.text = user.email ?: "No email"
        // Display the full displayName provided by Firebase Authentication
        textDisplayName.text = user.displayName ?: "No Name"

        val photoUrl = user.photoUrl
        if (photoUrl != null) {
            Picasso.get()
                .load(photoUrl)
                .placeholder(R.drawable.profile_placeholder)
                .error(R.drawable.profile_placeholder)
                .into(profileImageView)
        } else {
            profileImageView.setImageResource(R.drawable.profile_placeholder) // Default placeholder if no photo URL
        }
    }
    override fun onDestroyView() {
        super.onDestroyView()
    }
}