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
                // Sign in success, update UI with the signed-in user's information
                Toast.makeText(requireContext(), "Login successful!", Toast.LENGTH_SHORT).show()
                findNavController().navigate(LoginFragmentDirections.actionLoginFragmentToSessionsFragmentList())
            }
        }

        // --- NEW --- You have a signUpLink and actionLoginFragmentToSignUpFragment
        // This implies you will have a separate SignUpFragment.
        // We need to create that next, and also update nav_graph.xml
        binding.signUpLink.setOnClickListener {
            findNavController().navigate(LoginFragmentDirections.actionLoginFragmentToSignUpFragment())
        }

        observeViewModel()
    }

    private fun observeViewModel() {
        viewModel.errorMessage.observe(viewLifecycleOwner) { error ->
            error?.let {
                // If sign in fails, display a message to the user.
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                viewModel.clearError()
            }
        }

        viewModel.loading.observe(viewLifecycleOwner) { isLoading ->
            // Use binding.progressBar for visibility
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE // Ensure ID matches your XML
            binding.loginButton.isEnabled = !isLoading
            binding.emailEditText.isEnabled = !isLoading // Disable inputs while loading
            binding.passwordEditText.isEnabled = !isLoading
            binding.signUpLink.isEnabled = !isLoading // Disable sign-up link too
        }

        // Observe the user LiveData from AuthViewModel to handle automatic navigation if already logged in
        viewModel.user.observe(viewLifecycleOwner) { user ->
            if (user != null && findNavController().currentDestination?.id == R.id.loginFragment) {
                // User is logged in and we are currently on the login fragment, navigate to sessions
                findNavController().navigate(LoginFragmentDirections.actionLoginFragmentToSessionsFragmentList())
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        // It's good practice to clear messages here too, in case navigation doesn't happen immediately
        viewModel.clearError()
    }
}