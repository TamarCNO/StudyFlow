package com.example.studyflow.profile

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.studyflow.auth.AuthViewModel
import com.example.studyflow.databinding.FragmentEditProfileBinding
import com.squareup.picasso.Picasso
import java.io.IOException

class EditProfileFragment : Fragment() {

    private var _binding: FragmentEditProfileBinding? = null
    private val binding get() = _binding!!

    private val authViewModel: AuthViewModel by viewModels()

    private var selectedImageBitmap: Bitmap? = null
    private val pickImageRequestCode = 101

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEditProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        authViewModel.userProfile.observe(viewLifecycleOwner) { profile ->
            profile?.let {
                binding.inputFirstName.setText(it.firstName)
                binding.inputLastName.setText(it.lastName)
                binding.inputEmail.setText(it.email)
                if (!it.photoUrl.isNullOrBlank()) {
                    Picasso.get().load(it.photoUrl).into(binding.profileImageView)
                }
            }
        }

        authViewModel.errorMessage.observe(viewLifecycleOwner) { error ->
            error?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                authViewModel.clearError()
            }
        }

        authViewModel.loading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.buttonSave.isEnabled = !isLoading
        }

        binding.buttonSave.setOnClickListener {
            val firstName = binding.inputFirstName.text.toString().trim()
            val lastName = binding.inputLastName.text.toString().trim()
            val email = binding.inputEmail.text.toString().trim()
            if (firstName.isEmpty() || lastName.isEmpty() || email.isEmpty()) {
                Toast.makeText(requireContext(), "Please fill in all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            authViewModel.updateProfile(
                firstName = firstName,
                lastName = lastName,
                email = email,
                selectedImageBitmap = selectedImageBitmap,
                onSuccess = {
                    Toast.makeText(requireContext(), "Profile updated successfully", Toast.LENGTH_SHORT).show()
                }
            )
        }

        binding.buttonCancel.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        val openGallery = {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(intent, pickImageRequestCode)
        }

        binding.profileImageView.setOnClickListener { openGallery() }
        binding.cameraIcon.setOnClickListener { openGallery() }

        authViewModel.fetchUserProfile()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == pickImageRequestCode && resultCode == Activity.RESULT_OK && data?.data != null) {
            val imageUri = data.data
            try {
                selectedImageBitmap = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
                    MediaStore.Images.Media.getBitmap(requireContext().contentResolver, imageUri)
                } else {
                    val source = ImageDecoder.createSource(requireContext().contentResolver, imageUri!!)
                    ImageDecoder.decodeBitmap(source)
                }
                binding.profileImageView.setImageBitmap(selectedImageBitmap)
            } catch (e: IOException) {
                Toast.makeText(requireContext(), "Image load failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
