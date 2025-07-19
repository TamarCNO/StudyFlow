package com.example.studyflow

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import com.example.studyflow.databinding.FragmentMapBinding
import com.example.studyflow.model.Session
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.osmdroid.api.IMapController
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Marker

class MapFragment : Fragment() {

    private var _binding: FragmentMapBinding? = null
    private val binding get() = _binding!!

    private lateinit var mapController: IMapController
    private val client = OkHttpClient()

    private val sessionViewModel: SessionListViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Configuration.getInstance().load(
            requireContext(),
            requireContext().getSharedPreferences("osm_prefs", 0)
        )
        _binding = FragmentMapBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupMap()

        // Observe session list
        sessionViewModel.sessions.observe(viewLifecycleOwner) { sessionList ->
            viewLifecycleOwner.lifecycleScope.launch {
                _binding?.mapView?.overlays?.clear()

                sessionList.forEach { session ->
                    session.locationAddress?.let { address ->
                        val geoPoint = geocodeAddress(address)
                        if (geoPoint != null) {
                            addMarkerSafely(geoPoint, session)
                        }
                    }
                }

                _binding?.mapView?.invalidate()
            }
        }
    }

    private fun setupMap() {
        _binding?.mapView?.setMultiTouchControls(true)
        mapController = _binding?.mapView?.controller ?: return
        mapController.setZoom(10.0)
        mapController.setCenter(GeoPoint(32.0853, 34.7818)) // מרכז תל אביב
    }

    private suspend fun geocodeAddress(address: String): GeoPoint? = withContext(Dispatchers.IO) {
        try {
            val url =
                "https://nominatim.openstreetmap.org/search?format=json&q=${address.replace(" ", "+")}&limit=1"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "StudyFlowApp")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null

                val responseBody = response.body?.string() ?: return@withContext null
                val jsonArray = JSONArray(responseBody)
                if (jsonArray.length() == 0) return@withContext null

                val firstResult = jsonArray.getJSONObject(0)
                val lat = firstResult.getDouble("lat")
                val lon = firstResult.getDouble("lon")

                GeoPoint(lat, lon)
            }
        } catch (e: Exception) {
            Log.e("MapFragment", "Geocoding failed for address: $address", e)
            null
        }
    }

    private fun addMarkerSafely(geoPoint: GeoPoint, session: Session) {
        _binding?.let { binding ->
            val marker = Marker(binding.mapView)
            marker.position = geoPoint
            marker.title = session.topic ?: "Session"
            marker.subDescription = "${session.date ?: "-"} ${session.time ?: "-"}"
            marker.setOnMarkerClickListener { _, _ ->
                AlertDialog.Builder(requireContext())
                    .setTitle(session.topic ?: "Session Details")
                    .setMessage(
                        """
                        Date: ${session.date ?: "-"}
                        Time: ${session.time ?: "-"}
                        Status: ${session.status ?: "-"}
                        Student: ${session.studentEmail ?: "-"}
                        Address: ${session.locationAddress ?: "-"}
                        """.trimIndent()
                    )
                    .setPositiveButton("Close", null)
                    .show()
                true
            }
            binding.mapView.overlays.add(marker)
        }
    }

    override fun onResume() {
        super.onResume()
        _binding?.mapView?.onResume()
    }

    override fun onPause() {
        super.onPause()
        _binding?.mapView?.onPause()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
