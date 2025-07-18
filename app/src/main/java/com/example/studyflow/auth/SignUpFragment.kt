package com.example.studyflow.auth

import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.studyflow.databinding.FragmentSignUpBinding
import com.example.studyflow.model.CloudinaryModel

import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.core.app.ActivityCompat

class SignUpFragment : Fragment() {

    private var _binding: FragmentSignUpBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AuthViewModel by viewModels()
    private val cloudinaryModel = CloudinaryModel.getInstance()

    private var capturedImageBitmap: Bitmap? = null

    private val CAMERA_PERMISSION_REQUEST_CODE = 1001

    private val takePictureLauncher = registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        bitmap?.let {
            capturedImageBitmap = it
            binding.profileImageView.setImageBitmap(it)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSignUpBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.cameraButton.setOnClickListener {
            if (ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                takePictureLauncher.launch(null)
            } else {
                ActivityCompat.requestPermissions(
                    requireActivity(),
                    arrayOf(android.Manifest.permission.CAMERA),
                    CAMERA_PERMISSION_REQUEST_CODE
                )
            }
        }

        binding.registerButton.setOnClickListener {
            val email = binding.emailEditText.text.toString().trim()
            val password = binding.passwordEditText.text.toString()
            val confirmPassword = binding.confirmPasswordEditText.text.toString()

            if (email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(requireContext(), "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password != confirmPassword) {
                Toast.makeText(requireContext(), "Passwords do not match", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            binding.progressBar.visibility = View.VISIBLE

            capturedImageBitmap?.let { bitmap ->
                cloudinaryModel.uploadBitmap(
                    bitmap,
                    onSuccess = { imageUrl ->
                        registerUser(email, password, imageUrl)
                    },
                    onError = { errorMsg ->
                        binding.progressBar.visibility = View.GONE
                        Toast.makeText(requireContext(), "Image upload failed: $errorMsg", Toast.LENGTH_LONG).show()
                    }
                )
            } ?: run {
                registerUser(email, password, null)
            }
        }

        binding.alreadyHaveAccountLink.setOnClickListener {
            findNavController().navigate(SignUpFragmentDirections.actionSignUpFragmentToLoginFragment())
        }

        observeViewModel()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                takePictureLauncher.launch(null)
            } else {
                Toast.makeText(requireContext(), "Camera permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun registerUser(email: String, password: String, imageUrl: String?) {
        viewModel.signUp(email, password) {
            binding.progressBar.visibility = View.GONE
            Toast.makeText(requireContext(), "Registration successful!", Toast.LENGTH_SHORT).show()
            findNavController().navigate(SignUpFragmentDirections.actionSignUpFragmentToLoginFragment())
        }
    }

    private fun observeViewModel() {
        viewModel.errorMessage.observe(viewLifecycleOwner) { error ->
            error?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                viewModel.clearError()
                binding.progressBar.visibility = View.GONE
            }
        }

        viewModel.loading.observe(viewLifecycleOwner) { isLoading ->
            binding.registerButton.isEnabled = !isLoading
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.emailEditText.isEnabled = !isLoading
            binding.passwordEditText.isEnabled = !isLoading
            binding.confirmPasswordEditText.isEnabled = !isLoading
            binding.alreadyHaveAccountLink.isEnabled = !isLoading
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        viewModel.clearError()
    }
}
