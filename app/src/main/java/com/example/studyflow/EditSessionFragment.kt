package com.example.studyflow

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.studyflow.model.Session
import com.example.studyflow.model.dao.AppLocalDb
import com.google.android.material.textfield.TextInputEditText
import com.squareup.picasso.Picasso
import java.util.concurrent.Executors

class EditSessionFragment : Fragment() {

    private val args: EditSessionFragmentArgs by navArgs()

    private lateinit var topicEditText: TextInputEditText
    private lateinit var dateEditText: TextInputEditText
    private lateinit var timeEditText: TextInputEditText
    private lateinit var statusEditText: TextInputEditText
    private lateinit var studentEmailEditText: TextInputEditText
    private lateinit var materialImageView: ImageView
    private lateinit var editMaterialImageButton: ImageButton
    private lateinit var saveButton: Button
    private lateinit var cancelButton: Button
    private lateinit var deleteButton: Button
    private lateinit var progressBar: ProgressBar

    private val dbExecutor = Executors.newSingleThreadExecutor()
    private val sessionDao by lazy { AppLocalDb.db.sessionDao() }

    private var currentSession: Session? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_edit_session, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // אתחול שדות UI
        topicEditText = view.findViewById(R.id.sessionTopicEditText)
        dateEditText = view.findViewById(R.id.sessionDateEditText)
        timeEditText = view.findViewById(R.id.sessionTimeEditText)
        statusEditText = view.findViewById(R.id.sessionStatusEditText)
        studentEmailEditText = view.findViewById(R.id.sessionStudentEmailEditText)
        materialImageView = view.findViewById(R.id.sessionImageView)
        editMaterialImageButton = view.findViewById(R.id.editMaterialImageButton)
        saveButton = view.findViewById(R.id.saveSessionButton)
        cancelButton = view.findViewById(R.id.cancelSessionButton)
        deleteButton = view.findViewById(R.id.deleteSessionButton)
        progressBar = view.findViewById(R.id.progressBar)

        loadSession()

        editMaterialImageButton.setOnClickListener {
            Toast.makeText(requireContext(), "Edit image feature not implemented yet", Toast.LENGTH_SHORT).show()
        }

        saveButton.setOnClickListener {
            saveSession()
        }

        cancelButton.setOnClickListener {
            findNavController().popBackStack()
        }

        deleteButton.setOnClickListener {
            deleteSession()
        }
    }

    private fun loadSession() {
        progressBar.visibility = View.VISIBLE
        sessionDao.getById(args.sessionId).observe(viewLifecycleOwner) { session ->
            progressBar.visibility = View.GONE
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
        topicEditText.setText(session.topic)
        dateEditText.setText(session.date)
        timeEditText.setText(session.time)
        statusEditText.setText(session.status)
        studentEmailEditText.setText(session.studentEmail)

        if (!session.materialImageUrl.isNullOrEmpty()) {
            Picasso.get()
                .load(session.materialImageUrl)
                .placeholder(R.drawable.profile_placeholder)
                .error(R.drawable.profile_placeholder)
                .into(materialImageView)
        } else {
            materialImageView.setImageResource(R.drawable.profile_placeholder)
        }
        }
        private fun saveSession() {
        val topic = topicEditText.text.toString().trim()
        val date = dateEditText.text.toString().trim()
        val time = timeEditText.text.toString().trim()
        val status = statusEditText.text.toString().trim()
        val studentEmail = studentEmailEditText.text.toString().trim()

        if (topic.isEmpty()) {
            topicEditText.error = "Topic cannot be empty"
            return
        }

        progressBar.visibility = View.VISIBLE

        val updatedSession = currentSession?.copy(
            topic = topic,
            date = date,
            time = time,
            status = status,
            studentEmail = studentEmail
            // שים לב: אם רוצים לשנות תמונה - צריך לטפל בנפרד
        ) ?: Session(
            id = args.sessionId,
            topic = topic,
            date = date,
            time = time,
            status = status,
            studentEmail = studentEmail,
            materialImageUrl = "" // או args.materialImageUrl לפי מה שרלוונטי
        )

        dbExecutor.execute {
            try {
                sessionDao.update(updatedSession)
                requireActivity().runOnUiThread {
                    progressBar.visibility = View.GONE
                    Toast.makeText(requireContext(), "Session updated", Toast.LENGTH_SHORT).show()
                    findNavController().popBackStack()
                }
            } catch (e: Exception) {
                requireActivity().runOnUiThread {
                    progressBar.visibility = View.GONE
                    Toast.makeText(requireContext(), "Error updating session: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun deleteSession() {
        progressBar.visibility = View.VISIBLE
        currentSession?.let { session ->
            dbExecutor.execute {
                try {
                    sessionDao.delete(session)
                    requireActivity().runOnUiThread {
                        progressBar.visibility = View.GONE
                        Toast.makeText(requireContext(), "Session deleted", Toast.LENGTH_SHORT).show()
                        findNavController().popBackStack()
                    }
                } catch (e: Exception) {
                    requireActivity().runOnUiThread {
                        progressBar.visibility = View.GONE
                        Toast.makeText(requireContext(), "Error deleting session: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        } ?: run {
            progressBar.visibility = View.GONE
            Toast.makeText(requireContext(), "No session to delete", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        dbExecutor.shutdown()
    }
}
