package com.example.studyflow

import SessionListViewModel
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.studyflow.adapter.SessionsAdapter
import com.example.studyflow.adapter.SessionsViewHolder
import com.example.studyflow.databinding.FragmentSessionsRecyclerViewBinding
import com.example.studyflow.model.Session
import com.example.studyflow.model.dao.AppLocalDb
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestoreSettings
import com.google.firebase.firestore.memoryCacheSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SessionsFragmentList : Fragment() {

    private var _binding: FragmentSessionsRecyclerViewBinding? = null
    private val binding get() = _binding!!

    private lateinit var sessionsAdapter: SessionsAdapter
    private val sessionDao by lazy { AppLocalDb.db.sessionDao() }
    private lateinit var viewModel: SessionListViewModel

    private val firestoreDb by lazy {
        FirebaseFirestore.getInstance().apply {
            firestoreSettings = firestoreSettings {
                setLocalCacheSettings(memoryCacheSettings {})
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSessionsRecyclerViewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = androidx.lifecycle.ViewModelProvider(requireActivity())[SessionListViewModel::class.java]

        sessionsAdapter = SessionsAdapter(emptyList())
        sessionsAdapter.listener = object : SessionsViewHolder.OnItemClickListener {
            override fun onItemClick(session: Session) {
                val action = SessionsFragmentListDirections
                    .actionSessionsFragmentListToDetailsFragment(session.id)
                findNavController().navigate(action)
            }
        }

        binding.sessionsRecyclerView.adapter = sessionsAdapter

        // מאזין ל-ROOM, מזין את ה-ViewModel
        sessionDao.getAll().observe(viewLifecycleOwner) { sessions ->
            viewModel.setSessions(sessions)

            if (sessions.isEmpty()) {
                setLoadingState(true)
                refreshSessionsFromFirestore(showLoading = true)
            } else {
                setLoadingState(false)
                refreshSessionsFromFirestore(showLoading = false)
            }
        }

        // מאזין ל-ViewModel, מציג ב-Adapter
        viewModel.sessions.observe(viewLifecycleOwner) { sessions ->
            sessionsAdapter.set(sessions)
            sessionsAdapter.notifyDataSetChanged()
        }
    }

    private fun refreshSessionsFromFirestore(showLoading: Boolean) {
        if (showLoading) setLoadingState(true)

        firestoreDb.collection("sessions").get()
            .addOnSuccessListener { snapshot ->
                val sessions = snapshot.toObjects(Session::class.java)
                lifecycleScope.launch(Dispatchers.IO) {
                    sessionDao.deleteAll()
                    sessionDao.insertAll(sessions)
                    Log.d("SessionsFragmentList", "Synced ${sessions.size} sessions from Firestore")

                    if (showLoading) {
                        withContext(Dispatchers.Main) {
                            setLoadingState(false)
                        }
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e("SessionsFragmentList", "Firestore error", e)
                if (showLoading) {
                    Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_LONG).show()
                    setLoadingState(false)
                }
            }
    }

    private fun setLoadingState(isLoading: Boolean) {
        binding.apply {
            progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            sessionsRecyclerView.visibility = if (isLoading) View.GONE else View.VISIBLE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
