package com.example.studyflow

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.*
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.studyflow.auth.AuthViewModel
import com.example.studyflow.databinding.FragmentEditProfileBinding
import com.squareup.picasso.Picasso
import java.io.IOException

class EditProfileFragment : Fragment() {

    private var _binding: FragmentEditProfileBinding? = null
    private val binding get() = _binding!!
    private val authViewModel: AuthViewModel by viewModels()
    private var selectedImageBitmap: Bitmap? = null

    private val pickImageLauncher: ActivityResultLauncher<String> =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let { handleImageUri(it) }
        }

    private val takePictureLauncher: ActivityResultLauncher<Void?> =
        registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
            bitmap?.let {
                selectedImageBitmap = it
                binding.profileImageView.setImageBitmap(it)
            }
        }

    private val requestCameraPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                takePictureLauncher.launch(null)
            } else {
                Toast.makeText(requireContext(), "Camera permission denied", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentEditProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        authViewModel.fetchUserProfile()

        authViewModel.userProfile.observe(viewLifecycleOwner) { profile ->
            profile?.let {
                binding.inputFirstName.setText(it.firstName)
                binding.inputLastName.setText(it.lastName)
                binding.inputEmail.setText(it.email)
                if (!it.photoUrl.isNullOrBlank()) {
                    Picasso.get().load(it.photoUrl)
                        .placeholder(R.drawable.profile_placeholder)
                        .error(R.drawable.profile_placeholder)
                        .into(binding.profileImageView)
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
            listOf(
                binding.buttonSave,
                binding.inputFirstName,
                binding.inputLastName,
                binding.inputEmail,
                binding.profileImageView,
                binding.cameraIcon
            ).forEach { it.isEnabled = !isLoading }
        }

        binding.buttonSave.setOnClickListener { saveProfile() }

        binding.buttonCancel.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.profileImageView.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        binding.cameraIcon.setOnClickListener {
            when {
                ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED-> {
                    takePictureLauncher.launch(null)
                }
                else -> {
                    requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                }
            }
        }
    }

    private fun saveProfile() {
        val firstName = binding.inputFirstName.text.toString().trim()
        val lastName = binding.inputLastName.text.toString().trim()
        val email = binding.inputEmail.text.toString().trim()

        when {
            firstName.isEmpty() -> {
                binding.inputFirstName.error = "First name is required"
                return
            }
            lastName.isEmpty() -> {
                binding.inputLastName.error = "Last name is required"
                return
            }
            email.isEmpty() -> {
                binding.inputEmail.error = "Email is required"
                return
            }
            !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                binding.inputEmail.error = "Please enter a valid email"
                return
            }
        }

        authViewModel.updateProfile(
            firstName,
            lastName,
            email,
            selectedImageBitmap,
            onSuccess = {
                Toast.makeText(requireContext(), "Profile updated successfully", Toast.LENGTH_SHORT).show()
                findNavController().navigate(R.id.action_editProfileFragment_to_profileFragment)
            }
        )
    }

    private fun handleImageUri(uri: Uri) {
        try {
            selectedImageBitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = ImageDecoder.createSource(requireContext().contentResolver, uri)
                ImageDecoder.decodeBitmap(source)
            } else {
                @Suppress("DEPRECATION")
                MediaStore.Images.Media.getBitmap(requireContext().contentResolver, uri)
            }
            binding.profileImageView.setImageBitmap(selectedImageBitmap)
        } catch (e: IOException) {
            Toast.makeText(requireContext(), "Failed to load image", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
