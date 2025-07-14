package com.example.studyflow

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.studyflow.auth.AuthViewModel
import com.example.studyflow.databinding.FragmentEditProfileBinding
import com.squareup.picasso.Picasso

class EditProfileFragment : Fragment() {

    private var _binding: FragmentEditProfileBinding? = null
    private val binding get() = _binding!!

    private lateinit var authViewModel: AuthViewModel

    private var selectedImageBitmap: Bitmap? = null
    private var selectedImageUri: Uri? = null

    private lateinit var pickImageLauncher: ActivityResultLauncher<String>
    private lateinit var takePictureLauncher: ActivityResultLauncher<Void?>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEditProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        authViewModel = ViewModelProvider(requireActivity()).get(AuthViewModel::class.java)

        loadUserData()
        setupObservers()

        pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                selectedImageUri = it
                selectedImageBitmap = uriToBitmap(it)
                Picasso.get().load(it).into(binding.profileImageView)
            }
        }

        takePictureLauncher = registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap: Bitmap? ->
            bitmap?.let {
                selectedImageBitmap = it
                selectedImageUri = null
                binding.profileImageView.setImageBitmap(it)
            }
        }

        binding.cameraIcon.setOnClickListener {
            showImageSourceOptions()
        }

        binding.buttonSave.setOnClickListener {
            val firstName = binding.inputFirstName.text.toString().trim()
            val lastName = binding.inputLastName.text.toString().trim()
            val email = binding.inputEmail.text.toString().trim()

            if (firstName.isEmpty() || lastName.isEmpty() || email.isEmpty()) {
                Toast.makeText(requireContext(), "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            authViewModel.updateProfile(firstName, lastName, email, selectedImageBitmap) {
                Toast.makeText(requireContext(), "Profile updated successfully", Toast.LENGTH_SHORT).show()
                clearSelectedImage()
                findNavController().navigate(R.id.action_editProfileFragment_to_profileFragment)
            }
        }

        binding.buttonCancel.setOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun setupObservers() {
        authViewModel.loading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.buttonSave.isEnabled = !isLoading
        }

        authViewModel.errorMessage.observe(viewLifecycleOwner) { errorMessage ->
            errorMessage?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
                authViewModel.clearError()
            }
        }
    }

    private fun showImageSourceOptions() {
        val options = arrayOf("Take Photo", "Choose from Gallery")
        val builder = android.app.AlertDialog.Builder(requireContext())
        builder.setTitle("Select Image Source")
        builder.setItems(options) { _, which ->
            when (which) {
                0 -> takePictureLauncher.launch(null)
                1 -> pickImageLauncher.launch("image/*")
            }
        }
        builder.show()
    }

    private fun loadUserData() {
        authViewModel.user.value?.let { user ->
            binding.inputEmail.setText(user.email ?: "")

            val displayName = user.displayName ?: ""
            if (displayName.contains(" ")) {
                val parts = displayName.split(" ")
                binding.inputFirstName.setText(parts.getOrNull(0) ?: "")
                binding.inputLastName.setText(parts.getOrNull(1) ?: "")
            } else {
                binding.inputFirstName.setText(displayName)
                binding.inputLastName.setText("")
            }

            user.photoUrl?.let {
                Picasso.get().load(it).into(binding.profileImageView)
            }
        } ?: run {
            Toast.makeText(requireContext(), "User not logged in", Toast.LENGTH_SHORT).show()
        }
    }

    private fun uriToBitmap(uri: Uri): Bitmap? {
        return try {
            val source = ImageDecoder.createSource(requireContext().contentResolver, uri)
            ImageDecoder.decodeBitmap(source)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun clearSelectedImage() {
        selectedImageBitmap = null
        selectedImageUri = null
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
