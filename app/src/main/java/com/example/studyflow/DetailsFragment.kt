package com.example.studyflow

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.studyflow.databinding.FragmentDetailsBinding
import com.example.studyflow.model.Session
import com.example.studyflow.model.dao.AppLocalDb
import com.google.firebase.firestore.FirebaseFirestore
import com.squareup.picasso.Picasso
import java.util.concurrent.Executors

class DetailsFragment : Fragment() {

    private var _binding: FragmentDetailsBinding? = null
    private val binding get() = _binding!!

    private val args: DetailsFragmentArgs by navArgs()
    private lateinit var viewModel: SessionListViewModel

    private val sessionDao by lazy { AppLocalDb.db.sessionDao() }
    private val firestoreDb by lazy { FirebaseFirestore.getInstance() }
    private val dbExecutor = Executors.newSingleThreadExecutor()

    private var currentSession: Session? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(requireActivity())[SessionListViewModel::class.java]
        binding.progressBar.visibility = View.VISIBLE

        viewModel.sessions.observe(viewLifecycleOwner) { sessions ->
            val session = sessions.find { it.id == args.sessionId }
            if (session != null) {
                currentSession = session
                fillUI(session)
                binding.progressBar.visibility = View.GONE
            } else {
                loadFromLocalOrRemote(args.sessionId)
            }
        }

        binding.editSessionButton.setOnClickListener {
            currentSession?.let { session ->
                val action = DetailsFragmentDirections.actionDetailsFragmentToEditSessionFragment(session.id)
                findNavController().navigate(action)
            }
        }
    }

    private fun loadFromLocalOrRemote(sessionId: String) {
        sessionDao.getById(sessionId).observe(viewLifecycleOwner) { session ->
            if (session != null) {
                currentSession = session
                fillUI(session)
                binding.progressBar.visibility = View.GONE
            } else {
                loadFromFirestore(sessionId)
            }
        }
    }

    private fun loadFromFirestore(sessionId: String) {
        firestoreDb.collection("sessions").document(sessionId).get()
            .addOnSuccessListener { document ->
                val session = document.toObject(Session::class.java)
                if (session != null) {
                    currentSession = session
                    fillUI(session)

                    dbExecutor.execute {
                        sessionDao.insert(session)
                    }
                } else {
                    showErrorDialog("Session not found")
                    findNavController().popBackStack()
                }
                binding.progressBar.visibility = View.GONE
            }
            .addOnFailureListener { e ->
                showErrorDialog("Failed to load session: ${e.message}")
                Log.e("DetailsFragment", "Firestore load error", e)
                binding.progressBar.visibility = View.GONE
                findNavController().popBackStack()
            }
    }

    private fun fillUI(session: Session) {
        binding.sessionTopicTextView.text = session.topic ?: ""
        binding.sessionDateTextView.text = session.date ?: ""
        binding.sessionTimeTextView.text = session.time ?: ""
        binding.sessionStatusValue.text = session.status ?: ""
        binding.sessionStudentEmailValue.text = session.studentEmail ?: ""
        binding.sessionLocationValue.text = session.locationAddress ?: ""

        if (!session.materialImageUrl.isNullOrEmpty()) {
            Picasso.get()
                .load(session.materialImageUrl)
                .into(binding.sessionImageView)
        } else {
            binding.sessionImageView.setImageResource(R.drawable.materials)
        }
    }

    private fun showErrorDialog(message: String) {
        AlertDialog.Builder(requireContext())
            .setTitle("Error")
            .setMessage(message)
            .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        dbExecutor.shutdown()
    }
}
