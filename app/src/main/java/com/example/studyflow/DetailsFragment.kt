package com.example.studyflow

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.studyflow.model.Session
import com.example.studyflow.model.dao.AppLocalDb
import com.google.firebase.firestore.FirebaseFirestore
import com.squareup.picasso.Picasso
import java.util.concurrent.Executors

class DetailsFragment : Fragment() {

    private lateinit var topicTextView: TextView
    private lateinit var dateTextView: TextView
    private lateinit var timeTextView: TextView
    private lateinit var statusTextView: TextView
    private lateinit var studentEmailTextView: TextView
    private lateinit var materialImageView: ImageView
    private lateinit var editButton: Button
    private lateinit var progressBar: ProgressBar

    private lateinit var sessionDao: com.example.studyflow.model.dao.SessionDao
    private lateinit var firestoreDb: FirebaseFirestore

    private val args: DetailsFragmentArgs by navArgs()
    private val dbExecutor = Executors.newSingleThreadExecutor()

    private var currentSession: Session? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sessionDao = AppLocalDb.db.sessionDao()
        firestoreDb = FirebaseFirestore.getInstance()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_details, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        topicTextView = view.findViewById(R.id.sessionTopicTextView)
        dateTextView = view.findViewById(R.id.sessionDateTextView)
        timeTextView = view.findViewById(R.id.sessionTimeTextView)
        statusTextView = view.findViewById(R.id.sessionStatusValue)
        studentEmailTextView = view.findViewById(R.id.sessionStudentEmailValue)
        materialImageView = view.findViewById(R.id.sessionImageView)
        editButton = view.findViewById(R.id.editSessionButton)
        progressBar = view.findViewById(R.id.progressBar)

        loadSession(args.sessionId)

        editButton.setOnClickListener {
            currentSession?.let { session ->
                val action = DetailsFragmentDirections
                    .actionDetailsFragmentToEditSessionFragment(session.id)
                findNavController().navigate(action)
            }
        }
    }

    private fun loadSession(sessionId: String) {
        progressBar.visibility = View.VISIBLE

        sessionDao.getById(sessionId).observe(viewLifecycleOwner) { session ->
            if (session != null) {
                currentSession = session
                displaySession(session)
                progressBar.visibility = View.GONE
            } else {
                fetchSessionFromFirestore(sessionId)
            }
        }
    }

    private fun fetchSessionFromFirestore(sessionId: String) {
        firestoreDb.collection("sessions").document(sessionId).get()
            .addOnSuccessListener { document ->
                val session = document.toObject(Session::class.java)
                if (session != null) {
                    currentSession = session
                    displaySession(session)
                    dbExecutor.execute {
                        sessionDao.insert(session)
                    }
                } else {
                    showErrorDialog("Session not found")
                    findNavController().popBackStack()
                }
                progressBar.visibility = View.GONE
            }
            .addOnFailureListener { e ->
                showErrorDialog("Error loading session: ${e.message}")
                Log.e("DetailsFragment", "Failed to load session from Firestore", e)
                progressBar.visibility = View.GONE
                findNavController().popBackStack()
            }
    }

    private fun displaySession(session: Session) {
        topicTextView.text = session.topic ?: ""
        dateTextView.text = session.date ?: ""
        timeTextView.text = session.time ?: ""
        statusTextView.text = session.status ?: ""
        studentEmailTextView.text = session.studentEmail ?: ""

        if (!session.materialImageUrl.isNullOrEmpty()) {
            Picasso.get()
                .load(session.materialImageUrl)
                .placeholder(R.drawable.profile_placeholder)
                .error(R.drawable.profile_placeholder)
                .into(materialImageView)
        } else {
            setDefaultImage()
        }
    }

    private fun setDefaultImage() {
        materialImageView.setImageResource(R.drawable.profile_placeholder)
    }

    private fun showErrorDialog(message: String) {
        AlertDialog.Builder(requireContext())
            .setTitle("Error")
            .setMessage(message)
            .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    override fun onDestroy() {
        super.onDestroy()
        dbExecutor.shutdown()
    }
}
