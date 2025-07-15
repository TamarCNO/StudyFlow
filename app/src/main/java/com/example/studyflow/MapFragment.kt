package com.example.studyflow

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.studyflow.databinding.FragmentMapBinding
import com.example.studyflow.model.Session
import com.example.studyflow.model.dao.AppLocalDb
import com.example.studyflow.model.dao.SessionDao
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.*
import com.squareup.picasso.Picasso
import java.util.*
import java.util.concurrent.Executors

class MapFragment : Fragment(), OnMapReadyCallback, LocationListener {

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
        private const val DEFAULT_ZOOM = 15f // רמת זום ברירת מחדל
    }

    private var _binding: FragmentMapBinding? = null
    private val binding get() = _binding!! // גישה בטוחה לבינדינג

    private lateinit var mMap: GoogleMap
    private val sessionDao by lazy { AppLocalDb.db.sessionDao() }
    private lateinit var locationManager: LocationManager

    // אקזקיוטור ייעודי לפעולות רקע כמו Geocoder
    private val backgroundExecutor = Executors.newSingleThreadExecutor()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMapBinding.inflate(inflater, container, false)

        // אתחול LocationManager
        locationManager = requireActivity().getSystemService(Context.LOCATION_SERVICE) as LocationManager

        val mapFragment = childFragmentManager
            .findFragmentById(R.id.mapFragment) as SupportMapFragment
        mapFragment.getMapAsync(this)

        binding.plusButton.setOnClickListener {
            mMap.animateCamera(CameraUpdateFactory.zoomIn())
        }
        binding.minusButton.setOnClickListener {
            mMap.animateCamera(CameraUpdateFactory.zoomOut())
        }

        return binding.root
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.isBuildingsEnabled = true // הפעלת תצוגת בניינים בתלת מימד
        mMap.isIndoorEnabled = true // הפעלת תצוגת פנים מבנים

        // בדוק הרשאת מיקום וטפל בה
        checkLocationPermission()

        sessionDao.getAll().observe(viewLifecycleOwner) { sessions ->
            updateMapWithSessions(sessions)
        }

        // הגדרת מאזין ללחיצה על מארקר
        mMap.setOnMarkerClickListener { marker ->
            val session = marker.tag as? Session
            if (session != null) {
                showSessionDialog(session)
                true // סמן שהאירוע טופל
            } else {
                Log.w("MapFragment", "Marker tag is not a Session or is null.")
                false // תן להתנהגות ברירת המחדל של המפה להתרחש (אם יש)
            }
        }
    }

    private fun updateMapWithSessions(sessions: List<Session>) {
        mMap.clear()
        sessions.forEach { session ->
            getLatLngFromAddress(session.locationAddress) { latLng ->
                latLng?.let {
                    val marker = mMap.addMarker(
                        MarkerOptions()
                            .position(it)
                            .title(session.topic)
                    )
                    marker?.tag = session
                } ?: run {
                    Log.e("MapFragment", "Could not get LatLng for address: ${session.locationAddress}")
                }
            }
        }
    }

    private fun showSessionDialog(session: Session) {
        val view = layoutInflater.inflate(R.layout.dialog_post_info, null)

        val profileImageView = view.findViewById<ImageView>(R.id.imageProfile)
        val subjectTextView = view.findViewById<TextView>(R.id.textSubject)
        val timeTextView = view.findViewById<TextView>(R.id.textTime)

        subjectTextView.text = session.topic
        timeTextView.text = "${session.date} at ${session.time}"

        val optimizedUrl = session.materialImageUrl?.replace("/upload/", "/upload/w_200,h_200,c_fill/") ?: ""

        Picasso.get()
            .load(optimizedUrl)
            .placeholder(R.drawable.profile_placeholder)
            .error(R.drawable.profile_placeholder)
            .into(profileImageView)

        AlertDialog.Builder(requireContext())
            .setView(view)
            .setPositiveButton("Close", null)
            .show()
    }

    private fun checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            enableMyLocation()
            startLocationUpdates()
            moveToCurrentLocation()
        } else {
            requestPermissions(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }
    }

    private fun enableMyLocation() {
        try {
            mMap.isMyLocationEnabled = true
        } catch (e: SecurityException) {
            Log.e("MapFragment", "Failed to enable My Location layer: ${e.message}", e)
            Toast.makeText(requireContext(), "Location permission denied, cannot show current location on map.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun startLocationUpdates() {
        try {
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                5000L,
                10f,
                this
            )
        } catch (e: SecurityException) {
            Log.e("MapFragment", "Location permission missing for updates: ${e.message}", e)
            Toast.makeText(requireContext(), "Location updates permission denied.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun moveToCurrentLocation() {
        try {
            val lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            lastKnownLocation?.let {
                val currentLatLng = LatLng(it.latitude, it.longitude)
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, DEFAULT_ZOOM))
            } ?: run {
                Toast.makeText(requireContext(), "Could not retrieve last known location.", Toast.LENGTH_SHORT).show()
            }
        } catch (e: SecurityException) {
            Log.e("MapFragment", "Location permission missing for last known location: ${e.message}", e)
            Toast.makeText(requireContext(), "Location access denied.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                enableMyLocation()
                startLocationUpdates()
                moveToCurrentLocation()
            } else {
                Toast.makeText(requireContext(), "Location permission denied. Map features may be limited.", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun getLatLngFromAddress(address: String?, callback: (LatLng?) -> Unit) { // Changed address to String? here too
        if (address.isNullOrBlank()) {
            Log.w("MapFragment", "Address is null or blank, skipping geocoding.")
            callback(null)
            return
        }

        val geocoder = Geocoder(requireContext(), Locale.getDefault())
        backgroundExecutor.execute {
            try {
                val addresses = geocoder.getFromLocationName(address, 1)
                val latLng = if (!addresses.isNullOrEmpty())
                    LatLng(addresses[0].latitude, addresses[0].longitude)
                else null

                requireActivity().runOnUiThread { callback(latLng) }
            } catch (e: Exception) {
                Log.e("MapFragment", "Geocoder failed for address '$address': ${e.message}", e)
                requireActivity().runOnUiThread { callback(null) }
            }
        }
    }

    override fun onLocationChanged(location: Location) {
        val latLng = LatLng(location.latitude, location.longitude)
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, DEFAULT_ZOOM))
    }

    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
        Log.d("MapFragment", "Location provider status changed: $provider, status: $status")
    }

    override fun onProviderEnabled(provider: String) {
        Log.d("MapFragment", "Location provider enabled: $provider")
    }

    override fun onProviderDisabled(provider: String) {
        Log.d("MapFragment", "Location provider disabled: $provider")
        Toast.makeText(requireContext(), "Please enable location services.", Toast.LENGTH_LONG).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        try {
            locationManager.removeUpdates(this)
        } catch (e: SecurityException) {
            Log.e("MapFragment", "Failed to remove location updates: ${e.message}", e)
        }
        _binding = null
    }

    override fun onDestroy() {
        super.onDestroy()
        backgroundExecutor.shutdown()
    }
}