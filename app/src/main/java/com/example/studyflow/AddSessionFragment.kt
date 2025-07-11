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
import com.example.studyflow.model.Session

class AddSessionFragment : Fragment() {

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
                binding.sessionImageView.setImageBitmap(bitmap)
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

        binding.saveSessionButton.setOnClickListener { onSaveClicked(it) }

        // Take photo button
        binding.editMaterialImageButton.setOnClickListener { cameraLauncher.launch(null) }

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
TODO("cheak email")
        val session = Session(topic = topic, date = date, time = time, imageUrl = "")

        binding.progressBar.visibility = View.VISIBLE

        if (didSetImage) {
            val bitmap = (binding.sessionImageView.drawable as? BitmapDrawable)?.bitmap
            if (bitmap != null) {
                Model.shared.addSessionWithImage(session, bitmap) { updatedSession ->
                    binding.progressBar.visibility = View.GONE
                    if (updatedSession != null) {
                        Navigation.findNavController(view).popBackStack()
                    } else {
                        Toast.makeText(requireContext(), "Failed to upload image", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Toast.makeText(requireContext(), "Error retrieving image", Toast.LENGTH_SHORT).show()
                binding.progressBar.visibility = View.GONE
            }
        } else {
            Model.shared.addSessionWithImage(session, null) { updatedSession ->
                binding.progressBar.visibility = View.GONE
                if (updatedSession != null) {
                    Navigation.findNavController(view).popBackStack()
                } else {
                    Toast.makeText(requireContext(), "Failed to save session", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}