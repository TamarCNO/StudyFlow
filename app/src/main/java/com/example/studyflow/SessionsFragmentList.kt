package com.example.studyflow

import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
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
import androidx.core.view.MenuProvider
import androidx.lifecycle.Lifecycle

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

    private val sharedPref by lazy {
        requireContext().getSharedPreferences("sync_prefs", android.content.Context.MODE_PRIVATE)
    }

    private var isSyncing = false
    private var isFirstLoad = true

    companion object {
        private const val LAST_SYNC_KEY = "last_sync_timestamp"
        private const val SYNC_INTERVAL_MS = 10 * 60 * 1000L
        private const val FORCE_SYNC_INTERVAL_MS = 60 * 60 * 1000L
        private const val CACHE_VALIDITY_MS = 5 * 60 * 1000L
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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

        setupRecyclerView()
        observeData()
        setupMenu()
    }

    private fun setupMenu() {
        val menuProvider = object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.bar_menu, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.profileFragment -> {
                        try {
                            findNavController().navigate(R.id.action_sessionsFragmentList_to_profileFragment)
                            true
                        } catch (e: Exception) {
                            Log.e("SessionsFragmentList", "Navigation error to profile", e)
                            false
                        }
                    }
                    R.id.mapFragment -> {
                        try {
                            findNavController().navigate(R.id.action_sessionsFragmentList_to_mapFragment)
                            true
                        } catch (e: Exception) {
                            Log.e("SessionsFragmentList", "Navigation error to map", e)
                            false
                        }
                    }
                    else -> false
                }
            }
        }

        requireActivity().addMenuProvider(menuProvider, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    private fun setupRecyclerView() {
        binding.sessionsRecyclerView.layoutManager = LinearLayoutManager(requireContext())

        sessionsAdapter = SessionsAdapter(emptyList())
        sessionsAdapter.listener = object : SessionsViewHolder.OnItemClickListener {
            override fun onItemClick(session: Session) {
                val action = SessionsFragmentListDirections
                    .actionSessionsFragmentListToDetailsFragment(session.id)
                findNavController().navigate(action)
            }
        }

        binding.sessionsRecyclerView.adapter = sessionsAdapter
    }

    private fun observeData() {
        sessionDao.getAll().observe(viewLifecycleOwner) { sessions ->
            updateAdapterSafely(sessions)
            viewModel.setSessions(sessions)
            handleFirstLoadSync(sessions)
        }
    }

    private fun updateAdapterSafely(sessions: List<Session>) {
        if (::sessionsAdapter.isInitialized) {
            sessionsAdapter.set(sessions)
            sessionsAdapter.notifyDataSetChanged()
        }
    }

    private fun handleFirstLoadSync(localSessions: List<Session>) {
        if (!isFirstLoad) return
        isFirstLoad = false

        when {
            localSessions.isEmpty() -> performSync(showLoading = true, forceSync = true)
            shouldSyncBasedOnTime() -> performSync(showLoading = false, forceSync = false)
        }
    }

    private fun shouldSyncBasedOnTime(): Boolean {
        if (isSyncing) return false
        val lastSync = sharedPref.getLong(LAST_SYNC_KEY, 0)
        val currentTime = System.currentTimeMillis()
        val timeSinceLastSync = currentTime - lastSync

        return when {
            lastSync == 0L -> true
            timeSinceLastSync > FORCE_SYNC_INTERVAL_MS -> true
            timeSinceLastSync > SYNC_INTERVAL_MS -> true
            else -> false
        }
    }

    private fun performSync(showLoading: Boolean, forceSync: Boolean = false) {
        if (isSyncing) return

        if (!forceSync) {
            val lastSync = sharedPref.getLong(LAST_SYNC_KEY, 0)
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastSync < CACHE_VALIDITY_MS) {
                if (showLoading) setLoadingState(false)
                return
            }
        }

        isSyncing = true
        if (showLoading) setLoadingState(true)

        firestoreDb.collection("sessions")
            .get()
            .addOnSuccessListener { snapshot ->
                handleSyncSuccess(snapshot.toObjects(Session::class.java), showLoading)
            }
            .addOnFailureListener { e ->
                handleSyncFailure(e, showLoading)
            }
    }

    private fun handleSyncSuccess(sessions: List<Session>, showLoading: Boolean) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                sessionDao.deleteAll()
                sessionDao.insertAll(sessions)
                saveLastSyncTime()
                withContext(Dispatchers.Main) {
                    finalizeSyncOperation(showLoading, true)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    finalizeSyncOperation(showLoading, false)
                    showErrorToast("Failed to save data locally: ${e.message}")
                }
            }
        }
    }

    private fun handleSyncFailure(error: Exception, showLoading: Boolean) {
        finalizeSyncOperation(showLoading, false)
        if (showLoading) {
            showErrorToast("Sync failed: ${error.localizedMessage ?: "Unknown error"}")
        }
    }

    private fun finalizeSyncOperation(showLoading: Boolean, success: Boolean) {
        isSyncing = false
        if (showLoading) setLoadingState(false)
    }

    private fun saveLastSyncTime() {
        try {
            sharedPref.edit().putLong(LAST_SYNC_KEY, System.currentTimeMillis()).apply()
        } catch (_: Exception) {}
    }

    private fun showErrorToast(message: String) {
        if (isAdded && context != null) {
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun setLoadingState(isLoading: Boolean) {
        _binding?.apply {
            progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            sessionsRecyclerView.visibility = if (isLoading) View.GONE else View.VISIBLE
        }
    }

    fun refreshData() {
        if (!isSyncing) {
            performSync(showLoading = true, forceSync = true)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        isSyncing = false
        _binding = null
    }

    private fun clearSyncCache() {
        try {
            sharedPref.edit().remove(LAST_SYNC_KEY).apply()
        } catch (_: Exception) {}
    }

    private fun forceSyncNow() {
        clearSyncCache()
        isFirstLoad = true
        performSync(showLoading = true, forceSync = true)
    }
}
