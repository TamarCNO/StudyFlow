package com.example.studyflow

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController

class ProfileFragment : Fragment(){

        private lateinit var profileImageView: ImageView
        private lateinit var textFirstName: TextView
        private lateinit var textLastName: TextView
        private lateinit var textEmail: TextView
        private lateinit var buttonSessions: Button
        private lateinit var buttonEditProfile: Button

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

            buttonSessions.setOnClickListener {
                findNavController().navigate(R.id.action_profileFragment_to_sessionsFragmentList)
            }

            buttonEditProfile.setOnClickListener {
                findNavController().navigate(R.id.action_profileFragment_to_editProfileFragment)
            }
        }
    }