package com.example.studyflow

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.view.*
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.example.studyflow.data.model.Post
import com.example.studyflow.databinding.FragmentMapBinding
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.*
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*

class MapFragment : Fragment(), OnMapReadyCallback, LocationListener {

    private var _binding: FragmentMapBinding? = null
    private val binding get() = _binding!!
    private lateinit var mMap: GoogleMap
    private val LOCATION_PERMISSION_REQUEST_CODE = 1
    private val db = FirebaseFirestore.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMapBinding.inflate(inflater, container, false)

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
        mMap.isBuildingsEnabled = true
        mMap.isIndoorEnabled = true

        checkLocationPermission()
        moveToCurrentLocation()

        fetchPosts { posts ->
            posts.forEach { post ->
                getLatLngFromAddress(post.locationAddress) { latLng ->
                    latLng?.let {
                        val marker = mMap.addMarker(
                            MarkerOptions()
                                .position(it)
                                .title(post.subject)
                        )
                        marker?.tag = post
                    }
                }
            }
        }

        mMap.setOnMarkerClickListener { marker ->
            val post = marker.tag as? Post
            post?.let { showPostDialog(it) }
            true
        }
    }

    private fun showPostDialog(post: Post) {
        val view = layoutInflater.inflate(R.layout.dialog_post_info, null)

        val profileImageView = view.findViewById<ImageView>(R.id.imageProfile)
        val subjectTextView = view.findViewById<TextView>(R.id.textSubject)
        val timeTextView = view.findViewById<TextView>(R.id.textTime)

        subjectTextView.text = post.subject
        timeTextView.text = post.dateTime

        // ✨ שימוש ב-Cloudinary URL בלבד
        val optimizedUrl = post.profileImageUrl
            .replace("/upload/", "/upload/w_200,h_200,c_fill/")

        Glide.with(this)
            .load(optimizedUrl)
            .placeholder(R.drawable.profile_placeholder)
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
        } else {
            requestPermissions(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }
    }

    private fun enableMyLocation() {
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            mMap.isMyLocationEnabled = true
        }
    }

    private fun moveToCurrentLocation() {
        val locationManager =
            requireActivity().getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val locationProvider = LocationManager.GPS_PROVIDER
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            val lastKnownLocation = locationManager.getLastKnownLocation(locationProvider)
            lastKnownLocation?.let {
                val currentLatLng = LatLng(it.latitude, it.longitude)
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f))
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            enableMyLocation()
        } else {
            Toast.makeText(requireContext(), "Location permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // הבאת הפוסטים מ-Firebase
    private fun fetchPosts(callback: (List<Post>) -> Unit) {
        db.collection("posts")
            .get()
            .addOnSuccessListener { documents ->
                val posts = documents.map { doc ->
                    Post(
                        id = doc.id,
                        subject = doc.getString("subject") ?: "",
                        dateTime = doc.getString("dateTime") ?: "",
                        profileImageUrl = doc.getString("profileImageUrl") ?: "",
                        locationAddress = doc.getString("location") ?: ""
                    )
                }
                callback(posts)
            }
            .addOnFailureListener { it.printStackTrace() }
    }

    // המרת כתובת ל-LatLng
    private fun getLatLngFromAddress(address: String, callback: (LatLng?) -> Unit) {
        val geocoder = Geocoder(requireContext(), Locale.getDefault())
        val addresses = geocoder.getFromLocationName(address, 1)
        if (!addresses.isNullOrEmpty()) {
            callback(LatLng(addresses[0].latitude, addresses[0].longitude))
        } else callback(null)
    }

    override fun onLocationChanged(location: Location) {
        // לא בשימוש כרגע
    }
}
