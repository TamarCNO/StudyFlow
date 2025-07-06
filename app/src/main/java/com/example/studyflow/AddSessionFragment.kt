package com.example.studyflow

import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.navigation.Navigation
import com.example.studyflow.databinding.FragmentAddSessionBinding
import com.example.studyflow.model.Model
import com.example.studyflow.model.StudySession

class AddStudySessionFragment : Fragment() {

    private var cameraLauncher: ActivityResultLauncher<Void?>? = null
    private var binding: FragmentAddSessionBinding? = null
    private var didSetImage = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentAddSessionBinding.inflate(inflater, container, false)

        binding?.saveSessionButton?.setOnClickListener(::onSaveClicked)
        binding?.takePhotoButton?.setOnClickListener { cameraLauncher?.launch(null) }

        cameraLauncher = registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
            binding?.photoPreview?.setImageBitmap(bitmap)
            didSetImage = true
        }

        return binding?.root
    }

    override fun onDestroy() {
        super.onDestroy()
        binding = null
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        menu.clear()
        super.onCreateOptionsMenu(menu, inflater)
    }

    private fun onSaveClicked(view: View) {
        val topic = binding?.sessionTopicEditText?.text.toString().trim()
        val date = binding?.sessionDateEditText?.text.toString().trim()
        val time = binding?.sessionTimeEditText?.text.toString().trim()

        val session = StudySession(topic = topic, date = date, time = time, imageUrl = "")

        binding?.progressBar?.visibility = View.VISIBLE

        if (didSetImage) {
            val bitmap = (binding?.photoPreview?.drawable as BitmapDrawable).bitmap
            Model.shared.addStudySession(session, bitmap, Model.Storage.CLOUDINARY) {
                binding?.progressBar?.visibility = View.GONE
                Navigation.findNavController(view).popBackStack()
            }
        } else {
            Model.shared.addStudySession(session, null, Model.Storage.CLOUDINARY) {
                binding?.progressBar?.visibility = View.GONE
                Navigation.findNavController(view).popBackStack()
            }
        }
    }
}