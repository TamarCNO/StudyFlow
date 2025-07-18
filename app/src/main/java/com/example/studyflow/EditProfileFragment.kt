package com.example.studyflow.profile

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.studyflow.R
import com.example.studyflow.auth.AuthViewModel
import com.example.studyflow.databinding.FragmentEditProfileBinding
import com.squareup.picasso.Picasso
import java.io.IOException

class EditProfileFragment : Fragment() {

    private var _binding: FragmentEditProfileBinding? = null
    private val binding get() = _binding!!

    private val authViewModel: AuthViewModel by viewModels()

    private var selectedImageBitmap: Bitmap? = null

    // תיקון: ActivityResultLauncher מוגדר כמשתנה של הכיתה
    private val pickImageLauncher: ActivityResultLauncher<String> = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                selectedImageBitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    val source = ImageDecoder.createSource(requireContext().contentResolver, it)
                    ImageDecoder.decodeBitmap(source)
                } else {
                    @Suppress("DEPRECATION")
                    MediaStore.Images.Media.getBitmap(requireContext().contentResolver, it)
                }
                binding.profileImageView.setImageBitmap(selectedImageBitmap)
            } catch (e: IOException) {
                Toast.makeText(requireContext(), "Image load failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEditProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Observer לפרופיל המשתמש
        authViewModel.userProfile.observe(viewLifecycleOwner) { profile ->
            profile?.let {
                binding.inputFirstName.setText(it.firstName)
                binding.inputLastName.setText(it.lastName)
                binding.inputEmail.setText(it.email)
                if (!it.photoUrl.isNullOrBlank()) {
                    Picasso.get()
                        .load(it.photoUrl)
                        .placeholder(R.drawable.profile_placeholder)
                        .error(R.drawable.profile_placeholder)
                        .into(binding.profileImageView)
                }
            }
        }

        // Observer לשגיאות
        authViewModel.errorMessage.observe(viewLifecycleOwner) { error ->
            error?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                authViewModel.clearError()
            }
        }

        // Observer למצב טעינה
        authViewModel.loading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.buttonSave.isEnabled = !isLoading
            binding.inputFirstName.isEnabled = !isLoading
            binding.inputLastName.isEnabled = !isLoading
            binding.inputEmail.isEnabled = !isLoading
            binding.profileImageView.isEnabled = !isLoading
            binding.cameraIcon.isEnabled = !isLoading
        }

        // לחצן שמירה
        binding.buttonSave.setOnClickListener {
            val firstName = binding.inputFirstName.text.toString().trim()
            val lastName = binding.inputLastName.text.toString().trim()
            val email = binding.inputEmail.text.toString().trim()

            // תיקון: הוספת validation טוב יותר
            when {
                firstName.isEmpty() -> {
                    binding.inputFirstName.error = "First name is required"
                    return@setOnClickListener
                }
                lastName.isEmpty() -> {
                    binding.inputLastName.error = "Last name is required"
                    return@setOnClickListener
                }
                email.isEmpty() -> {
                    binding.inputEmail.error = "Email is required"
                    return@setOnClickListener
                }
                !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                    binding.inputEmail.error = "Please enter a valid email"
                    return@setOnClickListener
                }
            }

            authViewModel.updateProfile(
                firstName = firstName,
                lastName = lastName,
                email = email,
                selectedImageBitmap = selectedImageBitmap,
                onSuccess = {
                    Toast.makeText(requireContext(), "Profile updated successfully", Toast.LENGTH_SHORT).show()
                    findNavController().navigate(R.id.action_editProfileFragment_to_profileFragment)
                }
            )
        }

        // לחצן ביטול
        binding.buttonCancel.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        // תיקון: Click listeners לבחירת תמונה
        binding.profileImageView.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }
        binding.cameraIcon.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        // טען את פרטי המשתמש
        authViewModel.fetchUserProfile()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}