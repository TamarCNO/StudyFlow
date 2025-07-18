package com.example.studyflow

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.preference.PreferenceManager
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.studyflow.databinding.FragmentMapBinding
import com.example.studyflow.model.Session
import com.example.studyflow.model.dao.AppLocalDb
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.*
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import java.io.IOException


class MapFragment : Fragment(), LocationListener, MapEventsReceiver {

    // ViewBinding
    private var _binding: FragmentMapBinding? = null
    private val binding get() = _binding!!

    // Location & Map
    private lateinit var locationManager: LocationManager
    private lateinit var myLocationOverlay: MyLocationNewOverlay

    // Data Collections
    private val cityMarkers = mutableListOf<Marker>()
    private val userMarkers = mutableListOf<Marker>()
    private val sessionMarkers = mutableListOf<Marker>()
    private val sessionLocationData = mutableListOf<SessionLocationData>()

    // State
    private var isLoadingCities = false
    private var currentLocation: Location? = null
    private var showingSessions = true

    // Database & Network
    private val sessionDao by lazy { AppLocalDb.db.sessionDao() }
    private val firestoreDb by lazy { FirebaseFirestore.getInstance() }
    private val client = OkHttpClient()
    private val gson = Gson()
    private lateinit var sharedPreferences: SharedPreferences

    // Permission Launcher
    private val requestLocationPermissionLauncher: ActivityResultLauncher<Array<String>> =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            when {
                permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true -> {
                    startLocationUpdates()
                }
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true -> {
                    startLocationUpdates()
                }
                else -> {
                    Toast.makeText(requireContext(), "Location permission denied", Toast.LENGTH_SHORT).show()
                }
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Initialize ViewBinding
        _binding = FragmentMapBinding.inflate(inflater, container, false)

        Configuration.getInstance().load(
            requireContext(),
            PreferenceManager.getDefaultSharedPreferences(requireContext())
        )

        initializeComponents()
        setupMap()
        setupLocation()
        setupFabButtons()
        loadSessionData()
        loadSavedLocationData()
        fetchCitiesFromApi()

        return binding.root
    }

    private fun initializeComponents() {
        // No need for findViewById - use binding instead
        sharedPreferences = requireContext().getSharedPreferences("MapData", Context.MODE_PRIVATE)
    }

    private fun setupMap() {
        // Access mapView through binding
        binding.mapView.setTileSource(TileSourceFactory.MAPNIK)
        binding.mapView.setMultiTouchControls(true)
        binding.mapView.setBuiltInZoomControls(true)

        // Center map on Tel Aviv
        val telAviv = GeoPoint(32.0853, 34.7818)
        binding.mapView.controller.setZoom(7.0)
        binding.mapView.controller.setCenter(telAviv)

        setupMyLocation()
        setupMapEvents()
    }

    private fun setupMyLocation() {
        val locationProvider = GpsMyLocationProvider(requireContext())
        myLocationOverlay = MyLocationNewOverlay(locationProvider, binding.mapView)
        myLocationOverlay.enableMyLocation()
        binding.mapView.overlays.add(myLocationOverlay)
    }

    private fun setupMapEvents() {
        val mapEventsOverlay = MapEventsOverlay(this)
        binding.mapView.overlays.add(0, mapEventsOverlay)
    }

    private fun setupFabButtons() {
        // Access FABs through binding
        binding.fabMyLocation.setOnClickListener {
            goToMyLocation()
        }

        binding.fabClearMarkers.setOnClickListener {
            if (userMarkers.isNotEmpty()) {
                confirmClearMarkers()
            } else {
                Toast.makeText(requireContext(), "No markers to clear", Toast.LENGTH_SHORT).show()
            }
        }

        binding.fabShowSessions.setOnClickListener {
            toggleSessionMarkersVisibility()
        }

        updateFabVisibility()
    }

    // ================ SESSION INTEGRATION ================

    private fun loadSessionData() {
        lifecycleScope.launch {
            try {
                val sessions = withContext(Dispatchers.IO) {
                    sessionDao.getAll().value ?: emptyList()
                }

                loadSessionsFromFirestore()
                displaySessionMarkers(sessions)

            } catch (e: Exception) {
                Log.e("MapFragment", "Error loading sessions", e)
            }
        }
    }

    private fun loadSessionsFromFirestore() {
        firestoreDb.collection("sessions").get()
            .addOnSuccessListener { snapshot ->
                val sessions = snapshot.toObjects(Session::class.java)
                lifecycleScope.launch {
                    displaySessionMarkers(sessions)
                }
            }
            .addOnFailureListener { e ->
                Log.e("MapFragment", "Firestore error", e)
            }
    }

    private fun displaySessionMarkers(sessions: List<Session>) {
        clearSessionMarkers()

        sessions.forEach { session ->
            val savedLocation = getSessionLocation(session.id)

            if (savedLocation != null) {
                addSessionMarker(session, savedLocation)
            } else {
                val defaultLocation = currentLocation?.let {
                    GeoPoint(it.latitude, it.longitude)
                } ?: GeoPoint(32.0853, 34.7818)

                val newLocationData = SessionLocationData(
                    sessionId = session.id,
                    latitude = defaultLocation.latitude,
                    longitude = defaultLocation.longitude,
                    address = "Unknown location",
                    timestamp = System.currentTimeMillis()
                )

                saveSessionLocation(session.id, newLocationData)
                addSessionMarker(session, newLocationData)
            }
        }

        updateMapInfo()
    }

    private fun addSessionMarker(session: Session, locationData: SessionLocationData) {
        val geoPoint = GeoPoint(locationData.latitude, locationData.longitude)

        val marker = Marker(binding.mapView)
        marker.position = geoPoint
        marker.title = session.topic ?: "Session"
        marker.snippet = "Click for details"
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)

        marker.icon = ContextCompat.getDrawable(requireContext(), android.R.drawable.ic_dialog_info)

        marker.setOnMarkerClickListener { _, _ ->
            showSessionDetails(session, locationData)
            true
        }

        sessionMarkers.add(marker)
        if (showingSessions) {
            binding.mapView.overlays.add(marker)
        }
        binding.mapView.invalidate()
    }

    private fun showSessionDetails(session: Session, locationData: SessionLocationData) {
        val title = session.topic ?: "Session"

        val message = buildString {
            appendLine("Topic: ${session.topic ?: "No topic"}")
            appendLine("Status: ${session.status ?: "Unknown"}")

            if (!session.date.isNullOrEmpty()) {
                appendLine("Date: ${session.date}")
            }
            if (!session.time.isNullOrEmpty()) {
                appendLine("Time: ${session.time}")
            }
            if (!session.studentEmail.isNullOrEmpty()) {
                appendLine("Student: ${session.studentEmail}")
            }

            appendLine("Location: ${locationData.address}")
            appendLine("Distance: ${calculateDistanceFromCurrentLocation(locationData)}")
        }

        AlertDialog.Builder(requireContext())
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("Open Details") { _, _ ->
                openSessionDetails(session.id)
            }
            .setNeutralButton("Update Location") { _, _ ->
                updateSessionLocation(session, locationData)
            }
            .setNegativeButton("Close", null)
            .show()
    }

    private fun calculateDistanceFromCurrentLocation(locationData: SessionLocationData): String {
        return currentLocation?.let { current ->
            val results = FloatArray(1)
            Location.distanceBetween(
                current.latitude, current.longitude,
                locationData.latitude, locationData.longitude,
                results
            )

            val distance = results[0]
            when {
                distance < 1000 -> "${distance.toInt()} meters"
                else -> "${"%.1f".format(distance / 1000)} km"
            }
        } ?: "Distance not available"
    }

    private fun openSessionDetails(sessionId: String) {
        Toast.makeText(requireContext(), "Session ID: $sessionId\nClick to view details", Toast.LENGTH_LONG).show()
    }

    private fun updateSessionLocation(session: Session, currentLocationData: SessionLocationData) {
        currentLocation?.let { location ->
            val updatedLocationData = currentLocationData.copy(
                latitude = location.latitude,
                longitude = location.longitude,
                address = "Updated location",
                timestamp = System.currentTimeMillis()
            )

            saveSessionLocation(session.id, updatedLocationData)
            loadSessionData()

            Toast.makeText(requireContext(), "Session location updated", Toast.LENGTH_SHORT).show()
        } ?: run {
            Toast.makeText(requireContext(), "Current location not available", Toast.LENGTH_SHORT).show()
        }
    }

    // ================ LOCATION DATA MANAGEMENT ================

    private fun saveSessionLocation(sessionId: String, locationData: SessionLocationData) {
        sessionLocationData.removeAll { it.sessionId == sessionId }
        sessionLocationData.add(locationData)

        val json = gson.toJson(sessionLocationData)
        sharedPreferences.edit().putString("session_locations", json).apply()
    }

    private fun getSessionLocation(sessionId: String): SessionLocationData? {
        return sessionLocationData.find { it.sessionId == sessionId }
    }

    private fun loadSavedLocationData() {
        val json = sharedPreferences.getString("session_locations", null)
        if (!json.isNullOrEmpty()) {
            try {
                val type = object : TypeToken<List<SessionLocationData>>() {}.type
                val savedData: List<SessionLocationData> = gson.fromJson(json, type)
                sessionLocationData.clear()
                sessionLocationData.addAll(savedData)
            } catch (e: Exception) {
                Log.e("MapFragment", "Error loading saved location data", e)
            }
        }
    }

    // ================ USER MARKERS ================

    override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
        p?.let { geoPoint ->
            showAddMarkerDialog(geoPoint)
        }
        return true
    }

    override fun longPressHelper(p: GeoPoint?): Boolean {
        p?.let { geoPoint ->
            removeNearestUserMarker(geoPoint)
        }
        return true
    }

    private fun showAddMarkerDialog(geoPoint: GeoPoint) {
        val options = arrayOf("Add Regular Marker", "Link to Existing Session")

        AlertDialog.Builder(requireContext())
            .setTitle("Choose Action")
            .setItems(options) { _, which: Int ->
                when (which) {
                    0 -> addUserMarker(geoPoint, "User Marker", "Added by user")
                    1 -> linkToExistingSession(geoPoint)
                }
            }
            .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun linkToExistingSession(geoPoint: GeoPoint) {
        lifecycleScope.launch {
            try {
                val sessions = withContext(Dispatchers.IO) {
                    sessionDao.getAll().value ?: emptyList()
                }

                if (sessions.isEmpty()) {
                    Toast.makeText(requireContext(), "No sessions available", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                val sessionTitles = sessions.map { it.topic ?: "Session ${it.id}" }.toTypedArray()

                AlertDialog.Builder(requireContext())
                    .setTitle("Select Session")
                    .setItems(sessionTitles) { _, which: Int ->
                        val selectedSession = sessions[which]
                        linkSessionToLocation(selectedSession, geoPoint)
                    }
                    .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
                    .show()
            } catch (e: Exception) {
                Log.e("MapFragment", "Error loading sessions", e)
                Toast.makeText(requireContext(), "Error loading sessions", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun linkSessionToLocation(session: Session, geoPoint: GeoPoint) {
        val newLocationData = SessionLocationData(
            sessionId = session.id,
            latitude = geoPoint.latitude,
            longitude = geoPoint.longitude,
            address = "New session location",
            timestamp = System.currentTimeMillis()
        )

        saveSessionLocation(session.id, newLocationData)
        loadSessionData()

        val sessionTitle = session.topic ?: "Session"
        Toast.makeText(requireContext(), "Session '$sessionTitle' linked to location", Toast.LENGTH_SHORT).show()
    }

    private fun addUserMarker(geoPoint: GeoPoint, title: String, description: String) {
        val marker = Marker(binding.mapView)
        marker.position = geoPoint
        marker.title = title
        marker.snippet = description
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)

        marker.setOnMarkerClickListener { clickedMarker, _ ->
            Toast.makeText(requireContext(), "${clickedMarker.title}: ${clickedMarker.snippet}", Toast.LENGTH_SHORT).show()
            true
        }

        userMarkers.add(marker)
        binding.mapView.overlays.add(marker)
        binding.mapView.invalidate()
        updateFabVisibility()
        updateMapInfo()
    }

    private fun removeNearestUserMarker(geoPoint: GeoPoint) {
        if (userMarkers.isEmpty()) return

        val nearestMarker = userMarkers.minByOrNull { marker ->
            geoPoint.distanceToAsDouble(marker.position)
        }

        nearestMarker?.let { marker ->
            if (geoPoint.distanceToAsDouble(marker.position) < 1000) {
                binding.mapView.overlays.remove(marker)
                userMarkers.remove(marker)
                binding.mapView.invalidate()
                updateFabVisibility()
                updateMapInfo()
                Toast.makeText(requireContext(), "Marker removed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // ================ UI HELPERS ================

    private fun toggleSessionMarkersVisibility() {
        showingSessions = !showingSessions

        sessionMarkers.forEach { marker ->
            if (showingSessions) {
                if (!binding.mapView.overlays.contains(marker)) {
                    binding.mapView.overlays.add(marker)
                }
            } else {
                binding.mapView.overlays.remove(marker)
            }
        }

        binding.mapView.invalidate()
        updateMapInfo()

        val statusText = if (showingSessions) "Showing sessions" else "Hiding sessions"
        Toast.makeText(requireContext(), statusText, Toast.LENGTH_SHORT).show()
    }

    private fun clearSessionMarkers() {
        sessionMarkers.forEach { marker ->
            binding.mapView.overlays.remove(marker)
        }
        sessionMarkers.clear()
    }

    private fun confirmClearMarkers() {
        AlertDialog.Builder(requireContext())
            .setTitle("Clear Markers")
            .setMessage("Are you sure you want to clear all user markers?")
            .setPositiveButton("Yes") { _, _ ->
                clearUserMarkers()
            }
            .setNegativeButton("No") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun goToMyLocation() {
        if (myLocationOverlay.isMyLocationEnabled) {
            val location = myLocationOverlay.myLocation
            if (location != null) {
                binding.mapView.controller.animateTo(location)
                binding.mapView.controller.setZoom(15.0)
                Toast.makeText(requireContext(), "Centered on your location", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "Location not available", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(requireContext(), "Location services disabled", Toast.LENGTH_SHORT).show()
        }
    }

    private fun clearUserMarkers() {
        userMarkers.forEach { marker ->
            binding.mapView.overlays.remove(marker)
        }
        userMarkers.clear()
        binding.mapView.invalidate()
        updateFabVisibility()
        updateMapInfo()
        Toast.makeText(requireContext(), "User markers cleared", Toast.LENGTH_SHORT).show()
    }

    private fun updateFabVisibility() {
        // Access FAB through binding
        binding.fabClearMarkers.visibility = if (userMarkers.isNotEmpty()) View.VISIBLE else View.GONE
    }

    private fun updateMapInfo() {
        val citiesCount = cityMarkers.size
        val userMarkersCount = userMarkers.size
        val sessionMarkersCount = if (showingSessions) sessionMarkers.size else 0

        val info = when {
            isLoadingCities -> "Loading cities... • Tap to add markers"
            sessionMarkersCount > 0 && userMarkersCount > 0 ->
                "$sessionMarkersCount sessions • $userMarkersCount markers • $citiesCount cities"
            sessionMarkersCount > 0 ->
                "$sessionMarkersCount sessions • $citiesCount cities • Tap to add markers"
            userMarkersCount > 0 ->
                "$userMarkersCount markers • $citiesCount cities • Long press to remove"
            else -> "Tap map to add marker • Long press to remove marker"
        }
        // Access TextView through binding
        binding.textViewMapInfo.text = info
    }

    // ================ LOCATION & PERMISSIONS ================

    private fun setupLocation() {
        locationManager = requireActivity().getSystemService(Context.LOCATION_SERVICE) as LocationManager

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            requestLocationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        } else {
            startLocationUpdates()
        }
    }

    private fun startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED) {

            val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
            val isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

            if (!isGpsEnabled && !isNetworkEnabled) {
                Toast.makeText(requireContext(), "Please enable location services", Toast.LENGTH_LONG).show()
            } else {
                if (isGpsEnabled) {
                    locationManager.requestLocationUpdates(
                        LocationManager.GPS_PROVIDER,
                        5000,
                        10f,
                        this
                    )
                }
                if (isNetworkEnabled) {
                    locationManager.requestLocationUpdates(
                        LocationManager.NETWORK_PROVIDER,
                        5000,
                        10f,
                        this
                    )
                }
            }
        }
    }

    override fun onLocationChanged(location: Location) {
        currentLocation = location
    }

    // ================ CITIES API ================

    private fun fetchCitiesFromApi() {
        isLoadingCities = true
        updateMapInfo()

        val url = "https://data.gov.il/api/3/action/datastore_search?resource_id=d4901968-dad3-4845-a9b0-a57d027f11ab&limit=100"

        val request = Request.Builder()
            .url(url)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                activity?.runOnUiThread {
                    isLoadingCities = false
                    updateMapInfo()
                    if (isAdded) {
                        Toast.makeText(requireContext(), "Failed to load cities data", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val json = response.body?.string()
                if (json != null) {
                    try {
                        val responseObj = gson.fromJson(json, CitiesResponse::class.java)
                        activity?.runOnUiThread {
                            isLoadingCities = false
                            addCitiesMarkers(responseObj.result.records)
                            updateMapInfo()
                        }
                    } catch (e: Exception) {
                        activity?.runOnUiThread {
                            isLoadingCities = false
                            updateMapInfo()
                            if (isAdded) {
                                Toast.makeText(requireContext(), "Error parsing cities data", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            }
        })
    }

    private fun addCitiesMarkers(cities: List<CityRecord>) {
        for (city in cities) {
            val lat = city.latitude?.toDoubleOrNull()
            val lon = city.longitude?.toDoubleOrNull()
            val name = city.englishName?.takeIf { it.isNotBlank() } ?: city.name

            if (lat != null && lon != null && name.isNotBlank()) {
                addCityMarker(GeoPoint(lat, lon), name, "City in Israel")
            }
        }
    }

    private fun addCityMarker(geoPoint: GeoPoint, title: String, description: String) {
        val marker = Marker(binding.mapView)
        marker.position = geoPoint
        marker.title = title
        marker.snippet = description
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)

        marker.setOnMarkerClickListener { clickedMarker, _ ->
            Toast.makeText(requireContext(), clickedMarker.title, Toast.LENGTH_SHORT).show()
            true
        }

        cityMarkers.add(marker)
        binding.mapView.overlays.add(marker)
    }

    // ================ LIFECYCLE ================

    override fun onProviderEnabled(provider: String) {
        Toast.makeText(requireContext(), "$provider enabled", Toast.LENGTH_SHORT).show()
    }

    override fun onProviderDisabled(provider: String) {
        Toast.makeText(requireContext(), "$provider disabled", Toast.LENGTH_SHORT).show()
    }

    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}

    override fun onResume() {
        super.onResume()
        binding.mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        binding.mapView.onPause()
        locationManager.removeUpdates(this)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.mapView.onDetach()
        _binding = null // Important: Clean up binding
    }

    // ================ DATA CLASSES ================

    data class SessionLocationData(
        val sessionId: String,
        val latitude: Double,
        val longitude: Double,
        val address: String,
        val timestamp: Long
    )

    data class CitiesResponse(
        val result: Result
    )

    data class Result(
        val records: List<CityRecord>
    )

    data class CityRecord(
        @SerializedName("שם_ישוב") val name: String,
        @SerializedName("שם_ישוב_אנגלית") val englishName: String?,
        @SerializedName("קו_אורך") val longitude: String?,
        @SerializedName("קו_רוחב") val latitude: String?
    )
}