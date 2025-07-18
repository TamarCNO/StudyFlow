package com.example.studyflow

import com.example.studyflow.SessionListViewModel
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

    // ✅ מנגנון בקרת סנכרון מתקדם
    private val sharedPref by lazy {
        requireContext().getSharedPreferences("sync_prefs", android.content.Context.MODE_PRIVATE)
    }

    // ✅ דגל למניעת סנכרון כפול
    private var isSyncing = false

    // ✅ דגל לבדיקה אם זה הטעינה הראשונה
    private var isFirstLoad = true

    companion object {
        private const val LAST_SYNC_KEY = "last_sync_timestamp"
        private const val SYNC_INTERVAL_MS = 10 * 60 * 1000L // 10 דקות
        private const val FORCE_SYNC_INTERVAL_MS = 60 * 60 * 1000L // שעה
        private const val CACHE_VALIDITY_MS = 5 * 60 * 1000L // 5 דקות
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
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
        observeLocalData()
        observeViewModel()
    }

    // ✅ הגדרת RecyclerView
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

    // ✅ צפייה בנתונים מקומיים
    private fun observeLocalData() {
        sessionDao.getAll().observe(viewLifecycleOwner) { sessions ->
            viewModel.setSessions(sessions)

            // רק בטעינה הראשונה נבדוק סנכרון
            if (isFirstLoad) {
                isFirstLoad = false

                if (sessions.isEmpty()) {
                    // אין נתונים מקומיים - סנכרון מיידי
                    Log.d("SessionsFragmentList", "No local data - syncing immediately")
                    performSync(showLoading = true, forceSync = true)
                } else {
                    // יש נתונים מקומיים - בדיקה חכמה
                    Log.d("SessionsFragmentList", "Local data exists - checking if sync needed")
                    checkAndSyncIfNeeded()
                }
            }
        }
    }

    // ✅ צפייה ב-ViewModel
    private fun observeViewModel() {
        viewModel.sessions.observe(viewLifecycleOwner) { sessions ->
            sessionsAdapter.set(sessions)
            sessionsAdapter.notifyDataSetChanged()
        }
    }

    // ✅ בדיקה חכמה אם צריך סנכרון
    private fun checkAndSyncIfNeeded() {
        if (isSyncing) {
            Log.d("SessionsFragmentList", "Already syncing - skipping")
            return
        }

        val lastSync = sharedPref.getLong(LAST_SYNC_KEY, 0)
        val currentTime = System.currentTimeMillis()
        val timeSinceLastSync = currentTime - lastSync

        Log.d("SessionsFragmentList", "Time since last sync: ${timeSinceLastSync / 1000} seconds")

        when {
            lastSync == 0L -> {
                Log.d("SessionsFragmentList", "Never synced - performing initial sync")
                performSync(showLoading = false, forceSync = true)
            }
            timeSinceLastSync > FORCE_SYNC_INTERVAL_MS -> {
                Log.d("SessionsFragmentList", "Force sync needed - too much time passed")
                performSync(showLoading = false, forceSync = true)
            }
            timeSinceLastSync > SYNC_INTERVAL_MS -> {
                Log.d("SessionsFragmentList", "Regular background sync")
                performSync(showLoading = false, forceSync = false)
            }
            else -> {
                Log.d("SessionsFragmentList", "Sync not needed - recent sync available")
            }
        }
    }

    private fun performSync(showLoading: Boolean, forceSync: Boolean = false) {
        if (isSyncing) {
            Log.d("SessionsFragmentList", "Sync already in progress")
            return
        }

        val lastSync = sharedPref.getLong(LAST_SYNC_KEY, 0)
        val currentTime = System.currentTimeMillis()

        if (!forceSync && currentTime - lastSync < CACHE_VALIDITY_MS) {
            Log.d("SessionsFragmentList", "Sync skipped - cache still valid")
            if (showLoading) setLoadingState(false)
            return
        }

        isSyncing = true
        if (showLoading) setLoadingState(true)

        Log.d("SessionsFragmentList", "Starting Firestore sync (force: $forceSync)")

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

                sharedPref.edit()
                    .putLong(LAST_SYNC_KEY, System.currentTimeMillis())
                    .apply()

                Log.d("SessionsFragmentList", "Successfully synced ${sessions.size} sessions")

                withContext(Dispatchers.Main) {
                    isSyncing = false
                    if (showLoading) setLoadingState(false)
                }
            } catch (e: Exception) {
                Log.e("SessionsFragmentList", "Error saving to local DB", e)
                withContext(Dispatchers.Main) {
                    isSyncing = false
                    if (showLoading) setLoadingState(false)
                    showErrorToast("Failed to save data locally")
                }
            }
        }
    }

    private fun handleSyncFailure(error: Exception, showLoading: Boolean) {
        Log.e("SessionsFragmentList", "Firestore sync failed", error)

        lifecycleScope.launch(Dispatchers.Main) {
            isSyncing = false
            if (showLoading) {
                setLoadingState(false)
                showErrorToast("Sync failed: ${error.message}")
            }
        }
    }

    private fun showErrorToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    private fun setLoadingState(isLoading: Boolean) {
        _binding?.apply {
            progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            sessionsRecyclerView.visibility = if (isLoading) View.GONE else View.VISIBLE
        }
    }

    private fun manualRefresh() {
        if (isSyncing) {
            Toast.makeText(requireContext(), "Sync already in progress", Toast.LENGTH_SHORT).show()
            return
        }

        Log.d("SessionsFragmentList", "Manual refresh triggered")
        performSync(showLoading = true, forceSync = true)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.bar_menu, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.profileFragment -> {
                findNavController().navigate(R.id.profileFragment)
                true
            }
            R.id.mapFragment -> {
                findNavController().navigate(R.id.action_sessionsFragmentList_to_mapFragment)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        isSyncing = false
        _binding = null
    }


    private fun clearSyncCache() {
        sharedPref.edit().remove(LAST_SYNC_KEY).apply()
        Log.d("SessionsFragmentList", "Sync cache cleared")
    }
}