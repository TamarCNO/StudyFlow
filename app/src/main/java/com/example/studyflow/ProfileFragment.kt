package com.example.studyflow

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.fragment.app.viewModels
import com.example.studyflow.auth.AuthViewModel
import com.example.studyflow.databinding.FragmentProfileBinding
import com.google.firebase.auth.FirebaseUser
import com.squareup.picasso.Picasso

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        authViewModel.user.observe(viewLifecycleOwner) { user ->
            if (user != null) {
                displayUserInfo(user)
            } else {
                Toast.makeText(requireContext(), "User not logged in", Toast.LENGTH_SHORT).show()
                findNavController().navigate(R.id.action_profileFragment_to_loginFragment)
            }
        }

        binding.buttonSessions.setOnClickListener {
            findNavController().navigate(R.id.action_profileFragment_to_sessionsFragmentList)
        }

        binding.buttonEditProfile.setOnClickListener {
            findNavController().navigate(R.id.action_profileFragment_to_editProfileFragment)
        }

        binding.buttonLogout.setOnClickListener {
            authViewModel.logout()
            Toast.makeText(requireContext(), "Logged out successfully", Toast.LENGTH_SHORT).show()
            findNavController().navigate(R.id.loginFragment)
        }
    }

    private fun displayUserInfo(user: FirebaseUser) {
        binding.textEmail.text = user.email ?: "No email"
        binding.textDisplayName.text = user.displayName ?: "No Name"

        val photoUrl = user.photoUrl
        if (photoUrl != null) {
            Picasso.get()
                .load(photoUrl)
                .placeholder(R.drawable.profile_placeholder)
                .error(R.drawable.profile_placeholder)
                .into(binding.profileImageView)
        } else {
            binding.profileImageView.setImageResource(R.drawable.profile_placeholder)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
