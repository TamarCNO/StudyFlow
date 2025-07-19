package com.example.studyflow

import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.studyflow.databinding.FragmentEditSessionBinding
import com.example.studyflow.model.Model
import com.example.studyflow.model.Session
import com.example.studyflow.model.dao.AppLocalDb
import com.squareup.picasso.Picasso
import java.util.*
import java.util.concurrent.Executors

class EditSessionFragment : Fragment() {

    private val args: EditSessionFragmentArgs by navArgs()

    private var _binding: FragmentEditSessionBinding? = null
    private val binding get() = _binding!!

    private val dbExecutor = Executors.newSingleThreadExecutor()
    private val sessionDao by lazy { AppLocalDb.db.sessionDao() }

    private var currentSession: Session? = null
    private var selectedImageBitmap: Bitmap? = null

    private lateinit var takePictureLauncher: ActivityResultLauncher<Void?>

    private val requestCameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
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
                if (isAdded && _binding != null) {
                    binding.sessionImageView.setImageBitmap(it)
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEditSessionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.editMaterialImageButton.setOnClickListener {
            if (ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED) {
                takePictureLauncher.launch(null)
            } else {
                requestCameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
            }
        }

        binding.sessionDateEditText.setOnClickListener { showDatePicker() }
        binding.sessionTimeEditText.setOnClickListener { showTimePicker() }

        binding.saveSessionButton.setOnClickListener { saveSession() }
        binding.cancelSessionButton.setOnClickListener { findNavController().popBackStack() }
        binding.deleteSessionButton.setOnClickListener { deleteSession() }

        loadSession()
    }

    private fun loadSession() {
        binding.progressBar.visibility = View.VISIBLE
        sessionDao.getById(args.sessionId).observe(viewLifecycleOwner) { session ->
            binding.progressBar.visibility = View.GONE
            if (session != null) {
                currentSession = session
                populateFields(session)
            } else {
                Toast.makeText(requireContext(), "Session not found", Toast.LENGTH_LONG).show()
                findNavController().popBackStack()
            }
        }
    }

    private fun populateFields(session: Session) {
        binding.sessionTopicEditText.setText(session.topic)
        binding.sessionDateEditText.setText(session.date)
        binding.sessionTimeEditText.setText(session.time)
        binding.sessionStatusEditText.setText(session.status)
        binding.sessionStudentEmailEditText.setText(session.studentEmail)
        binding.sessionLocationEditText.setText(session.locationAddress ?: "")

        if (!session.materialImageUrl.isNullOrEmpty()) {
            Picasso.get()
                .load(session.materialImageUrl)
                .placeholder(R.drawable.materials)
                .error(R.drawable.materials)
                .into(binding.sessionImageView)
        } else {
            binding.sessionImageView.setImageResource(R.drawable.materials)
        }
    }

    private fun showDatePicker() {
        val c = Calendar.getInstance()
        val year = c.get(Calendar.YEAR)
        val month = c.get(Calendar.MONTH)
        val day = c.get(Calendar.DAY_OF_MONTH)

        android.app.DatePickerDialog(requireContext(), { _, y, m, d ->
            binding.sessionDateEditText.setText("$d/${m + 1}/$y")
        }, year, month, day).show()
    }

    private fun showTimePicker() {
        val c = Calendar.getInstance()
        val hour = c.get(Calendar.HOUR_OF_DAY)
        val minute = c.get(Calendar.MINUTE)

        android.app.TimePickerDialog(requireContext(), { _, h, m ->
            binding.sessionTimeEditText.setText(String.format("%02d:%02d", h, m))
        }, hour, minute, true).show()
    }

    private fun saveSession() {
        val topic = binding.sessionTopicEditText.text.toString().trim()
        val date = binding.sessionDateEditText.text.toString().trim()
        val time = binding.sessionTimeEditText.text.toString().trim()
        val status = binding.sessionStatusEditText.text.toString().trim()
        val studentEmail = binding.sessionStudentEmailEditText.text.toString().trim()
        val locationAddress = binding.sessionLocationEditText.text.toString().trim()

        if (topic.isEmpty()) {
            binding.sessionTopicEditText.error = "Topic cannot be empty"
            return
        }

        binding.progressBar.visibility = View.VISIBLE

        val updatedSession = currentSession?.apply {
            this.topic = topic
            this.date = date
            this.time = time
            this.status = status
            this.studentEmail = studentEmail
            this.locationAddress = locationAddress
        } ?: Session(
            id = args.sessionId,
            topic = topic,
            date = date,
            time = time,
            status = status,
            studentEmail = studentEmail,
            locationAddress = locationAddress
        )

        Model.shared.addSession(updatedSession, selectedImageBitmap) { success ->
            if (isAdded && _binding != null) {

                binding.progressBar.visibility = View.GONE
                if (success) {
                    Toast.makeText(requireContext(), "Session saved successfully", Toast.LENGTH_SHORT).show()
                    findNavController().navigate(R.id.action_editSessionFragment_to_sessionsFragmentList)
                } else {
                    Toast.makeText(requireContext(), "Failed to save session", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun deleteSession() {
        currentSession?.let { session ->
            binding.progressBar.visibility = View.VISIBLE
            Model.shared.deleteSession(session) { success ->
                if (isAdded && _binding != null) {

                    binding.progressBar.visibility = View.GONE
                    if (success) {
                        Toast.makeText(requireContext(), "Session deleted", Toast.LENGTH_SHORT).show()
                        findNavController().popBackStack()
                    } else {
                        Toast.makeText(requireContext(), "Failed to delete session", Toast.LENGTH_LONG).show()
                    }
                }
            }
        } ?: run {
            Toast.makeText(requireContext(), "No session to delete", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onDestroy() {
        super.onDestroy()
        dbExecutor.shutdown()
    }
}
