package com.example.studyflow.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.studyflow.databinding.FragmentLoginBinding
import com.example.studyflow.R

class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AuthViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.loginButton.setOnClickListener {
            val email = binding.emailEditText.text.toString().trim()
            val password = binding.passwordEditText.text.toString()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(requireContext(), "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            viewModel.login(email, password) {
                Toast.makeText(requireContext(), "Login successful!", Toast.LENGTH_SHORT).show()
                // Navigation is handled by the user observer below
            }
        }

        binding.signUpLink.setOnClickListener {
            findNavController().navigate(LoginFragmentDirections.actionLoginFragmentToSignUpFragment())
        }

        observeViewModel()
    }

    private fun observeViewModel() {
        viewModel.errorMessage.observe(viewLifecycleOwner) { error ->
            error?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                viewModel.clearError()
            }
        }

        viewModel.loading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.loginButton.isEnabled = !isLoading
            binding.emailEditText.isEnabled = !isLoading
            binding.passwordEditText.isEnabled = !isLoading
            binding.signUpLink.isEnabled = !isLoading
        }

        viewModel.user.observe(viewLifecycleOwner) { user ->
            if (user != null) {
                val currentDestination = findNavController().currentDestination
                if (currentDestination?.id == R.id.loginFragment) {
                    try {
                        findNavController().navigate(LoginFragmentDirections.actionLoginFragmentToSessionsFragmentList())
                    } catch (e: Exception) {
                        // Navigation might fail if already navigating or fragment is destroyed
                        android.util.Log.w("LoginFragment", "Navigation failed: ${e.message}")
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        viewModel.clearError()
    }
}
