package com.example.studyflow

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.studyflow.model.CloudinaryModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.userProfileChangeRequest
import com.squareup.picasso.Picasso

class EditProfileFragment : Fragment() {

    private lateinit var profileImageView: ImageView
    private lateinit var cameraIcon: ImageView
    private lateinit var inputFirstName: EditText
    private lateinit var inputLastName: EditText
    private lateinit var inputEmail: EditText
    private lateinit var buttonSave: Button
    private lateinit var buttonCancel: Button
    private lateinit var progressBar: ProgressBar

    private val auth = FirebaseAuth.getInstance()

    private var selectedImageBitmap: Bitmap? = null
    private var selectedImageUri: Uri? = null

    private lateinit var pickImageLauncher: ActivityResultLauncher<String>
    private lateinit var takePictureLauncher: ActivityResultLauncher<Void?>

    private val cloudinaryModel = CloudinaryModel()

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
        progressBar = view.findViewById(R.id.progressBar)

        loadUserData()

        pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                selectedImageUri = it
                selectedImageBitmap = null
                Picasso.get().load(it).into(profileImageView)
            }
        }

        takePictureLauncher = registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap: Bitmap? ->
            bitmap?.let {
                selectedImageBitmap = it
                selectedImageUri = null
                profileImageView.setImageBitmap(it)
            }
        }

        cameraIcon.setOnClickListener {
            showImageSourceOptions()
        }

        buttonSave.setOnClickListener {
            val firstName = inputFirstName.text.toString().trim()
            val lastName = inputLastName.text.toString().trim()
            val email = inputEmail.text.toString().trim()

            if (firstName.isEmpty() || lastName.isEmpty() || email.isEmpty()) {
                Toast.makeText(requireContext(), "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            uploadProfileImageThenUpdate(firstName, lastName, email)
        }

        buttonCancel.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
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
        val user = auth.currentUser
        if (user == null) {
            Toast.makeText(requireContext(), "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        inputEmail.setText(user.email ?: "")

        val displayName = user.displayName ?: ""
        if (displayName.contains(" ")) {
            val parts = displayName.split(" ")
            inputFirstName.setText(parts.getOrNull(0) ?: "")
            inputLastName.setText(parts.getOrNull(1) ?: "")
        } else {
            inputFirstName.setText(displayName)
            inputLastName.setText("")
        }

        user.photoUrl?.let {
            Picasso.get().load(it).into(profileImageView)
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

    private fun uploadProfileImageThenUpdate(firstName: String, lastName: String, email: String) {
        buttonSave.isEnabled = false
        progressBar.visibility = View.VISIBLE

        when {
            selectedImageBitmap != null -> {
                cloudinaryModel.uploadBitmap(selectedImageBitmap!!, { url ->
                    updateUserProfile(firstName, lastName, email, url)
                }, { errorMsg ->
                    progressBar.visibility = View.GONE
                    buttonSave.isEnabled = true
                    Toast.makeText(requireContext(), "Failed to upload image: $errorMsg", Toast.LENGTH_LONG).show()
                })
            }
            selectedImageUri != null -> {
                val bitmap = uriToBitmap(selectedImageUri!!)
                if (bitmap != null) {
                    cloudinaryModel.uploadBitmap(bitmap, { url ->
                        updateUserProfile(firstName, lastName, email, url)
                    }, { errorMsg ->
                        progressBar.visibility = View.GONE
                        buttonSave.isEnabled = true
                        Toast.makeText(requireContext(), "Failed to upload image: $errorMsg", Toast.LENGTH_LONG).show()
                    })
                } else {
                    progressBar.visibility = View.GONE
                    buttonSave.isEnabled = true
                    Toast.makeText(requireContext(), "Failed to convert image", Toast.LENGTH_LONG).show()
                }
            }
            else -> {
                updateUserProfile(firstName, lastName, email, null)
            }
        }
    }

    private fun updateUserProfile(firstName: String, lastName: String, email: String, photoUrl: String?) {
        val user = auth.currentUser ?: run {
            Toast.makeText(requireContext(), "User not logged in", Toast.LENGTH_SHORT).show()
            progressBar.visibility = View.GONE
            buttonSave.isEnabled = true
            return
        }

        val updateEmailTask = if (email != user.email) {
            user.updateEmail(email)
        } else null

        val updateProfile = {
            val fullName = "$firstName $lastName".trim()
            val profileUpdates = userProfileChangeRequest {
                displayName = fullName
                photoUri = photoUrl?.let { Uri.parse(it) }
            }

            user.updateProfile(profileUpdates)
                .addOnCompleteListener { task ->
                    progressBar.visibility = View.GONE
                    buttonSave.isEnabled = true
                    if (task.isSuccessful) {
                        Toast.makeText(requireContext(), "Profile updated successfully", Toast.LENGTH_SHORT).show()
                        clearSelectedImage()
                        findNavController().navigate(R.id.action_editProfileFragment_to_profileFragment)
                    } else {
                        Toast.makeText(requireContext(), "Failed to update profile: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                    }
                }
        }

        if (updateEmailTask != null) {
            updateEmailTask.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    updateProfile()
                } else {
                    progressBar.visibility = View.GONE
                    buttonSave.isEnabled = true
                    Toast.makeText(requireContext(), "Failed to update email: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
        } else {
            updateProfile()
        }
    }

    private fun clearSelectedImage() {
        selectedImageBitmap = null
        selectedImageUri = null
    }
}
