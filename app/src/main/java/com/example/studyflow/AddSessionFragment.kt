package com.example.studyflow

import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.findNavController
import com.example.studyflow.databinding.FragmentAddSessionBinding
import com.example.studyflow.model.Model
import com.example.studyflow.model.Session
import android.content.pm.PackageManager
import java.util.Calendar
import java.util.UUID

class AddSessionFragment : Fragment() {

    private var _binding: FragmentAddSessionBinding? = null
    private val binding get() = _binding!!

    private lateinit var takePictureLauncher: ActivityResultLauncher<Void?>
    private var selectedImageBitmap: Bitmap? = null

    private var selectedDate: String = ""
    private var selectedTime: String = ""

    private val requestCameraPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            takePictureLauncher.launch(null)
        } else {
            Toast.makeText(requireContext(), "Camera permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        takePictureLauncher = registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
            bitmap?.let {
                selectedImageBitmap = it
                binding.sessionImageView.setImageBitmap(it)
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAddSessionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.progressBar.visibility = View.GONE
        setLoadingState(false)

        binding.saveSessionButton.setOnClickListener { onSaveClicked() }
        binding.editMaterialImageButton.setOnClickListener {
            if (ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                takePictureLauncher.launch(null)
            } else {
                requestCameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
            }
        }

        binding.sessionDateEditText.setOnClickListener {
            showDatePicker()
        }
        binding.sessionTimeEditText.setOnClickListener {
            showTimePicker()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun showDatePicker() {
        val c = Calendar.getInstance()
        val year = c.get(Calendar.YEAR)
        val month = c.get(Calendar.MONTH)
        val day = c.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = android.app.DatePickerDialog(requireContext(),
            { _, selectedYear, selectedMonth, selectedDay ->
                selectedDate = "${selectedDay}/${selectedMonth + 1}/$selectedYear"
                binding.sessionDateEditText.setText(selectedDate)
            }, year, month, day)

        datePickerDialog.show()
    }

    private fun showTimePicker() {
        val c = Calendar.getInstance()
        val hour = c.get(Calendar.HOUR_OF_DAY)
        val minute = c.get(Calendar.MINUTE)

        val timePickerDialog = android.app.TimePickerDialog(requireContext(),
            { _, selectedHour, selectedMinute ->
                selectedTime = String.format("%02d:%02d", selectedHour, selectedMinute)
                binding.sessionTimeEditText.setText(selectedTime)
            }, hour, minute, true)

        timePickerDialog.show()
    }

    private fun onSaveClicked() {
        val topic = binding.sessionTopicEditText.text.toString().trim()
        val date = binding.sessionDateEditText.text.toString().trim()
        val time = binding.sessionTimeEditText.text.toString().trim()
        val status = binding.statusEditText.text.toString().trim()
        val studentEmail = binding.sessionStudentEmailEditText.text.toString().trim()
        val locationAddress = binding.sessionLocationEditText.text.toString().trim()

        if (topic.isEmpty() || date.isEmpty() || time.isEmpty() || status.isEmpty() || studentEmail.isEmpty() || locationAddress.isEmpty()) {
            Toast.makeText(requireContext(), "Please fill all fields", Toast.LENGTH_SHORT).show()
            return
        }

        val newSession = Session(
            id = UUID.randomUUID().toString(),
            topic = topic,
            date = date,
            time = time,
            status = status,
            studentEmail = studentEmail,
            materialImageUrl = null,
            locationAddress = locationAddress
        )

        setLoadingState(true)

        Model.shared.addSession(newSession, selectedImageBitmap) { success ->
            setLoadingState(false)
            if (success) {
                Toast.makeText(requireContext(), "Session saved successfully!", Toast.LENGTH_SHORT).show()
                findNavController().popBackStack()
            } else {
                Toast.makeText(requireContext(), "Failed to save session. Please try again.", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun setLoadingState(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.saveSessionButton.isEnabled = !isLoading
        binding.sessionTopicEditText.isEnabled = !isLoading
        binding.sessionDateEditText.isEnabled = !isLoading
        binding.sessionTimeEditText.isEnabled = !isLoading
        binding.statusEditText.isEnabled = !isLoading
        binding.sessionStudentEmailEditText.isEnabled = !isLoading
        binding.sessionImageView.isEnabled = !isLoading
        binding.editMaterialImageButton.isEnabled = !isLoading
        binding.sessionLocationEditText.isEnabled = !isLoading
    }
}