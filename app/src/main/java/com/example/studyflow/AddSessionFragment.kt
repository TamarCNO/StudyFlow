package com.example.studyflow

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.studyflow.databinding.FragmentAddSessionBinding
import com.example.studyflow.model.CloudinaryModel
import com.example.studyflow.model.Session
import com.example.studyflow.model.dao.AppLocalDb
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestoreSettings
import com.google.firebase.firestore.memoryCacheSettings
import com.squareup.picasso.Picasso
import java.util.concurrent.Executors
import android.widget.ImageButton

class AddSessionFragment : Fragment() {

    private var _binding: FragmentAddSessionBinding? = null
    private val binding get() = _binding!!

    private lateinit var sessionTopicEditText: EditText
    private lateinit var sessionDateEditText: EditText
    private lateinit var sessionTimeEditText: EditText
    private lateinit var sessionStatusEditText: EditText
    private lateinit var sessionStudentEmailEditText: EditText
    private lateinit var sessionImageView: ImageView
    private lateinit var saveSessionButton: Button
    private lateinit var editMaterialImageButton: ImageButton
    private lateinit var progressBar: ProgressBar

    private lateinit var pickImageLauncher: ActivityResultLauncher<String>
    private lateinit var takePictureLauncher: ActivityResultLauncher<Void?>

    private var selectedImageBitmap: Bitmap? = null
    private var selectedImageUri: Uri? = null

    private lateinit var sessionDao: com.example.studyflow.model.dao.SessionDao
    private lateinit var firestoreDb: FirebaseFirestore
    private val cloudinaryModel = CloudinaryModel()

    private val dbExecutor = Executors.newSingleThreadExecutor()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // אתחול DAO ל-DB לוקאלי
        sessionDao = AppLocalDb.db.sessionDao()

        // אתחול Firestore עם הגדרות לזיכרון
        firestoreDb = FirebaseFirestore.getInstance().apply {
            firestoreSettings = firestoreSettings {
                setLocalCacheSettings(memoryCacheSettings { })
            }
        }
        pickImageLauncher =
            registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
                uri?.let {
                    selectedImageUri = it
                    selectedImageBitmap = null
                    Picasso.get().load(it).into(sessionImageView)
                }
            }

        // Launcher לצילום תמונה חדשה
        takePictureLauncher =
            registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap: Bitmap? ->
                bitmap?.let {
                    selectedImageBitmap = it
                    selectedImageUri = null
                    sessionImageView.setImageBitmap(it)
                }
            }
    }
    private fun onSaveClicked() {
        val topic = sessionTopicEditText.text.toString().trim()
        val date = sessionDateEditText.text.toString().trim()
        val time = sessionTimeEditText.text.toString().trim()
        val status = sessionStatusEditText.text.toString().trim()
        val studentEmail = sessionStudentEmailEditText.text.toString().trim()

        if (topic.isEmpty() || date.isEmpty() || time.isEmpty() || status.isEmpty() || studentEmail.isEmpty()) {
            Toast.makeText(requireContext(), "Please fill all fields", Toast.LENGTH_SHORT).show()
            return
        }

        setLoadingState(true)

        when {
            selectedImageBitmap != null -> {
                uploadImageToCloudinary(selectedImageBitmap!!) { imageUrl ->
                    if (imageUrl != null) {
                        saveSessionToFirestore(topic, date, time, status, studentEmail, imageUrl)
                    } else {
                        Toast.makeText(requireContext(), "Failed to upload image", Toast.LENGTH_SHORT).show()
                        setLoadingState(false)
                    }
                }
            }
            selectedImageUri != null -> {
                val bitmap = uriToBitmap(selectedImageUri!!)
                if (bitmap != null) {
                    uploadImageToCloudinary(bitmap) { imageUrl ->
                        if (imageUrl != null) {
                            saveSessionToFirestore(topic, date, time, status, studentEmail, imageUrl)
                        } else {
                            Toast.makeText(requireContext(), "Failed to upload image", Toast.LENGTH_SHORT).show()
                            setLoadingState(false)
                        }
                    }
                } else {
                    Toast.makeText(requireContext(), "Failed to process selected image", Toast.LENGTH_SHORT).show()
                    setLoadingState(false)
                }
            }
            else -> {
                saveSessionToFirestore(topic, date, time, status, studentEmail, "")
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddSessionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sessionTopicEditText = binding.sessionTopicEditText
        sessionDateEditText = binding.sessionDateEditText
        sessionTimeEditText = binding.sessionTimeEditText
        sessionStatusEditText = binding.sessionStatusEditText
        sessionStudentEmailEditText = binding.sessionStudentEmailEditText
        sessionImageView = binding.sessionImageView
        saveSessionButton = binding.saveSessionButton
        editMaterialImageButton = binding.editMaterialImageButton
        progressBar = binding.progressBar

        progressBar.visibility = View.GONE

        saveSessionButton.setOnClickListener { onSaveClicked() }
        editMaterialImageButton.setOnClickListener { showImageSourceOptions() }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onDestroy() {
        super.onDestroy()
        dbExecutor.shutdown()
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

    private fun uriToBitmap(uri: Uri): Bitmap? {
        return try {
            val source = ImageDecoder.createSource(requireContext().contentResolver, uri)
            ImageDecoder.decodeBitmap(source)
        } catch (e: Exception) {
            Log.e("AddSessionFragment", "Error converting URI to Bitmap: ${e.message}", e)
            null
        }
    }

    private fun saveSessionToFirestore(
        topic: String,
        date: String,
        time: String,
        status: String,
        studentEmail: String,
        imageUrl: String
    ) {
        val newSession = Session(
            id = "",
            topic = topic,
            date = date,
            time = time,
            status = status,
            studentEmail = studentEmail,
            materialImageUrl = imageUrl
        )

        firestoreDb.collection("sessions").add(newSession)
            .addOnSuccessListener { documentReference ->
                val firestoreId = documentReference.id
                val sessionWithId = newSession.copy(id = firestoreId)

                dbExecutor.execute {
                    sessionDao.insert(sessionWithId)

                    requireActivity().runOnUiThread {
                        Toast.makeText(
                            requireContext(),
                            "Session saved successfully!",
                            Toast.LENGTH_SHORT
                        ).show()
                        setLoadingState(false)

                        val action = AddSessionFragmentDirections
                            .actionAddSessionFragmentToDetailsFragment(firestoreId)
                        action.materialImageUrl = sessionWithId.materialImageUrl ?: ""
                        findNavController().navigate(action)
                    }
                }
            }
    }
        private fun uploadImageToCloudinary(
        bitmap: Bitmap,
        onComplete: (String?) -> Unit
    ) {
        cloudinaryModel.uploadBitmap(
            bitmap,
            onSuccess = { url ->
                onComplete(url)
            },
            onError = { error ->
                Log.e("AddSessionFragment", "Cloudinary upload failed: $error")
                onComplete(null)
            }
        )
    }

    private fun setLoadingState(isLoading: Boolean) {
        progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        saveSessionButton.isEnabled = !isLoading
        sessionTopicEditText.isEnabled = !isLoading
        sessionDateEditText.isEnabled = !isLoading
        sessionTimeEditText.isEnabled = !isLoading
        sessionStatusEditText.isEnabled = !isLoading
        sessionStudentEmailEditText.isEnabled = !isLoading
        sessionImageView.isEnabled = !isLoading
        editMaterialImageButton.isEnabled = !isLoading
    }
}