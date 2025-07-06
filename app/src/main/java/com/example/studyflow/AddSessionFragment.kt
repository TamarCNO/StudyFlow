package com.example.studyflow

import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.navigation.Navigation
import com.example.studyflow.databinding.FragmentAddSessionBinding
import com.example.studyflow.model.Model
import com.example.studyflow.model.StudySession

class AddStudySessionFragment : Fragment() {

    private lateinit var cameraLauncher: ActivityResultLauncher<Void?>
    private var _binding: FragmentAddSessionBinding? = null
    private val binding get() = _binding!!
    private var didSetImage = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)

        // Register camera launcher
        cameraLauncher = registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
            if (bitmap != null) {
                binding.photoPreview.setImageBitmap(bitmap)
                didSetImage = true
            } else {
                Toast.makeText(requireContext(), "No image captured", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddSessionBinding.inflate(inflater, container, false)

        // Save button
        binding.saveSessionButton.setOnClickListener { onSaveClicked(it) }

        // Take photo button
        binding.takePhotoButton.setOnClickListener { cameraLauncher.launch(null) }

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        menu.clear()
        super.onCreateOptionsMenu(menu, inflater)
    }

    private fun onSaveClicked(view: View) {
        val topic = binding.sessionTopicEditText.text.toString().trim()
        val date = binding.sessionDateEditText.text.toString().trim()
        val time = binding.sessionTimeEditText.text.toString().trim()

        if (topic.isEmpty() || date.isEmpty() || time.isEmpty()) {
            Toast.makeText(requireContext(), "Please fill in all fields", Toast.LENGTH_SHORT).show()
            return
        }

        val session = StudySession(topic = topic, date = date, time = time, imageUrl = "")

        binding.progressBar.visibility = View.VISIBLE

        val onComplete = {
            if (isAdded) {
                binding.progressBar.visibility = View.GONE
                Navigation.findNavController(view).popBackStack()
            }
        }

        if (didSetImage) {
            val bitmap = (binding.photoPreview.drawable as? BitmapDrawable)?.bitmap
            if (bitmap != null) {
                Model.shared.addStudySession(session, bitmap, Model.Storage.CLOUDINARY, onComplete)
            } else {
                Toast.makeText(requireContext(), "Error retrieving image", Toast.LENGTH_SHORT).show()
                binding.progressBar.visibility = View.GONE
            }
        } else {
            Model.shared.addStudySession(session, null, Model.Storage.CLOUDINARY, onComplete)
        }
    }
}
