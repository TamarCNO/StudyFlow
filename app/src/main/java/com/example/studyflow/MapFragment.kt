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
import androidx.lifecycle.lifecycleScope
import com.example.studyflow.databinding.FragmentMapBinding
import com.example.studyflow.model.PostEntity
import com.example.studyflow.model.repository.PostRepository
import com.example.studyflow.model.dao.AppLocalDb
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.*
import com.squareup.picasso.Picasso
import kotlinx.coroutines.launch
import java.util.*

class MapFragment : Fragment(), OnMapReadyCallback, LocationListener {

    private var _binding: FragmentMapBinding? = null
    private val binding get() = _binding!!
    private lateinit var mMap: GoogleMap
    private val LOCATION_PERMISSION_REQUEST_CODE = 1

    private lateinit var postRepository: PostRepository

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMapBinding.inflate(inflater, container, false)

        val mapFragment = childFragmentManager
            .findFragmentById(R.id.mapFragment) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // אתחול ה-Repository עם ה-Dao של Room
        val postDao = AppLocalDb.db.postDao()
        postRepository = PostRepository(postDao)

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

        // טעינת פוסטים מה-DB באופן אסינכרוני
        lifecycleScope.launch {
            val posts = postRepository.getAllPosts()
            updateMapWithPosts(posts)
        }

        mMap.setOnMarkerClickListener { marker ->
            val post = marker.tag
            if (post != null) {
                showPostDialog(post)
                true
            } else false
        }
    }

    private fun updateMapWithPosts(posts: List<PostEntity>) {
        mMap.clear()
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

    private fun showPostDialog(post: Any) {
        val view = layoutInflater.inflate(R.layout.dialog_post_info, null)

        val profileImageView = view.findViewById<ImageView>(R.id.imageProfile)
        val subjectTextView = view.findViewById<TextView>(R.id.textSubject)
        val timeTextView = view.findViewById<TextView>(R.id.textTime)

        if (post is PostEntity) {
            subjectTextView.text = post.subject
            timeTextView.text = post.dateTime

            val optimizedUrl = post.profileImageUrl.replace("/upload/", "/upload/w_200,h_200,c_fill/")

            Picasso.get()
                .load(optimizedUrl)
                .placeholder(R.drawable.profile_placeholder)
                .into(profileImageView)
        }

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

    private fun getLatLngFromAddress(address: String, callback: (LatLng?) -> Unit) {
        val geocoder = Geocoder(requireContext(), Locale.getDefault())
        Thread {
            try {
                val addresses = geocoder.getFromLocationName(address, 1)
                val latLng = if (!addresses.isNullOrEmpty())
                    LatLng(addresses[0].latitude, addresses[0].longitude)
                else null
                requireActivity().runOnUiThread { callback(latLng) }
            } catch (e: Exception) {
                requireActivity().runOnUiThread { callback(null) }
            }
        }.start()
    }

    override fun onLocationChanged(location: Location) {
        // לא בשימוש כרגע
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
