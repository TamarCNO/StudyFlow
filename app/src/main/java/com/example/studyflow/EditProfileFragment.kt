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

class EditProfileFragment : Fragment(){

        private lateinit var profileImageView: ImageView
        private lateinit var cameraIcon: ImageView
        private lateinit var inputFirstName: EditText
        private lateinit var inputLastName: EditText
        private lateinit var inputEmail: EditText
        private lateinit var buttonSave: Button
        private lateinit var buttonCancel: Button

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

            cameraIcon.setOnClickListener {
                Toast.makeText(requireContext(), "Change profile photo", Toast.LENGTH_SHORT).show()
            }

            buttonSave.setOnClickListener {
                val firstName = inputFirstName.text.toString().trim()
                val lastName = inputLastName.text.toString().trim()
                val email = inputEmail.text.toString().trim()

                if (firstName.isEmpty() || lastName.isEmpty() || email.isEmpty()) {
                    Toast.makeText(requireContext(), "Please fill all fields", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                Toast.makeText(requireContext(), "Profile saved", Toast.LENGTH_SHORT).show()
            }

            buttonCancel.setOnClickListener {
                requireActivity().onBackPressedDispatcher.onBackPressed()
            }
        }
    }
