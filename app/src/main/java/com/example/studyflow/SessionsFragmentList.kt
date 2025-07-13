package com.example.studyflow

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.studyflow.adapter.SessionsAdapter
import com.example.studyflow.databinding.FragmentSessionsListBinding
import com.example.studyflow.model.Session
import com.example.studyflow.model.dao.AppLocalDb
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestoreSettings
import com.google.firebase.firestore.memoryCacheSettings
import java.util.concurrent.Executors // Import ExecutorService

class SessionsFragmentList : Fragment() {

    private var _binding: FragmentSessionsListBinding? = null
    private val binding get() = _binding!!

    private lateinit var recyclerView: RecyclerView
    private lateinit var sessionsAdapter: SessionsAdapter
    private lateinit var addSessionFab: FloatingActionButton
    private lateinit var progressBar: ProgressBar

    private lateinit var sessionDao: com.example.studyflow.model.dao.SessionDao
    private lateinit var firestoreDb: FirebaseFirestore

    private val dbExecutor = Executors.newSingleThreadExecutor() // For Room operations on background thread

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sessionDao = AppLocalDb.getDatabase(requireContext()).sessionDao()
        firestoreDb = FirebaseFirestore.getInstance().apply {
            firestoreSettings = firestoreSettings {
                setLocalCacheSettings(memoryCacheSettings { })
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSessionsListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = binding.sessionsRecyclerView
        addSessionFab = binding.addSessionFab
        progressBar = binding.progressBar

        recyclerView.layoutManager = LinearLayoutManager(context)
        sessionsAdapter = SessionsAdapter(emptyList()) { session ->
            val action = SessionsFragmentListDirections.actionSessionsFragmentListToDetailsFragment(sessionId = session.id)
            findNavController().navigate(action)
        }
        recyclerView.adapter = sessionsAdapter

        addSessionFab.setOnClickListener {
            findNavController().navigate(R.id.action_sessionsFragmentList_to_addSessionFragment)
        }

        // Observe LiveData from Room
        sessionDao.getAll().observe(viewLifecycleOwner) { sessions ->
            sessionsAdapter.updateSessions(sessions)
            if (sessions.isEmpty()) {
                setLoadingState(true)
                refreshSessionsFromFirestore(true) // Show loading if local is empty
            } else {
                setLoadingState(false) // Hide loading if local data is present
                refreshSessionsFromFirestore(false) // Refresh from network silently
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onDestroy() {
        super.onDestroy()
        dbExecutor.shutdown() // Shut down the executor when fragment is destroyed
    }

    private fun refreshSessionsFromFirestore(showLoading: Boolean) {
        if (showLoading) setLoadingState(true)

        firestoreDb.collection("sessions").get()
            .addOnSuccessListener { querySnapshot ->
                val sessionsFromFirestore = querySnapshot.toObjects(Session::class.java)

                dbExecutor.execute { // Run Room operations on background thread
                    sessionDao.deleteAll()
                    sessionDao.insertAll(sessionsFromFirestore)
                    Log.d("SessionsFragmentList", "Successfully refreshed ${sessionsFromFirestore.size} sessions from Firestore.")
                    if (showLoading) requireActivity().runOnUiThread { setLoadingState(false) } // Update UI on main thread
                }
            }
            .addOnFailureListener { e ->
                Log.e("SessionsFragmentList", "Error refreshing sessions from Firestore", e)
                if (showLoading) {
                    requireActivity().runOnUiThread {
                        Toast.makeText(requireContext(), "Failed to load sessions: ${e.message}", Toast.LENGTH_LONG).show()
                        setLoadingState(false)
                    }
                }
            }
    }

    private fun setLoadingState(isLoading: Boolean) {
        progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        recyclerView.visibility = if (isLoading) View.GONE else View.VISIBLE
        addSessionFab.isEnabled = !isLoading
    }
}