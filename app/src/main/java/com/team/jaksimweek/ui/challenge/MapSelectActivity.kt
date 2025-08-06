package com.team.jaksimweek.ui.challenge

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.team.jaksimweek.databinding.ActivityMapSelectBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import java.io.IOException
import java.util.Locale
import com.team.jaksimweek.R

class MapSelectActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var binding: ActivityMapSelectBinding
    private lateinit var mMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private var selectedLatLng: LatLng? = null
    private var selectedAddress: String? = null
    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) -> {
                enableMyLocation()
            }
            permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false) -> {
                enableMyLocation()
            }
            else -> {
                Toast.makeText(this, "위치 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapSelectBinding.inflate(layoutInflater)
        setContentView(binding.root)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        if (intent.getBooleanExtra("displayOnly", false)) {
            binding.selectLocationButton.visibility = View.GONE
            binding.searchLayout.visibility = View.GONE
        } else {
            setupSearch()
            binding.selectLocationButton.setOnClickListener {
                if (selectedLatLng != null && selectedAddress != null) {
                    val intent = Intent()
                    intent.putExtra("latitude", selectedLatLng!!.latitude)
                    intent.putExtra("longitude", selectedLatLng!!.longitude)
                    intent.putExtra("address", selectedAddress)
                    setResult(RESULT_OK, intent)
                    finish()
                } else {
                    Toast.makeText(this, "먼저 지도를 탭하거나 검색하여 위치를 선택하세요.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    private fun setupSearch() {
        binding.btnSearch.setOnClickListener {
            searchLocation()
        }
        binding.etSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                searchLocation()
                true
            } else {
                false
            }
        }
    }
    private fun searchLocation() {
        val searchQuery = binding.etSearch.text.toString()
        if (searchQuery.isBlank()) {
            Toast.makeText(this, "검색어를 입력해주세요.", Toast.LENGTH_SHORT).show()
            return
        }

        hideKeyboard()
        val geocoder = Geocoder(this, Locale.KOREAN)
        try {
            val addressList: List<Address>? = geocoder.getFromLocationName(searchQuery, 1)
            if (addressList != null && addressList.isNotEmpty()) {
                val address = addressList[0]
                val latLng = LatLng(address.latitude, address.longitude)
                mMap.clear()
                mMap.addMarker(MarkerOptions().position(latLng).title(searchQuery))
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
                selectedLatLng = latLng
                getAddressFromLatLng(latLng)
            } else {
                Toast.makeText(this, "검색 결과가 없습니다.", Toast.LENGTH_SHORT).show()
            }
        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(this, "지오코딩 서비스에 접근할 수 없습니다.", Toast.LENGTH_SHORT).show()
        }
    }
    private fun hideKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.etSearch.windowToken, 0)
    }


    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        val lat = intent.getDoubleExtra("latitude", 0.0)
        val lng = intent.getDoubleExtra("longitude", 0.0)
        val address = intent.getStringExtra("address")
        val displayOnly = intent.getBooleanExtra("displayOnly", false)

        if (lat != 0.0 && lng != 0.0) {
            val location = LatLng(lat, lng)
            mMap.addMarker(MarkerOptions().position(location).title(address ?: ""))
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 15f))
        } else {
            if (!displayOnly) {
                enableMyLocation()
            }
        }

        if (!displayOnly) {
            mMap.setOnMapClickListener { latLng ->
                mMap.clear()
                mMap.addMarker(MarkerOptions().position(latLng))
                selectedLatLng = latLng
                getAddressFromLatLng(latLng)
            }
        }
    }

    private fun enableMyLocation() {
        if (ActivityCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            mMap.isMyLocationEnabled = true
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    val currentLatLng = LatLng(location.latitude, location.longitude)
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f))
                } else {
                    val defaultLocation = LatLng(37.5665, 126.9780)
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 15f))
                }
            }
        } else {
            locationPermissionRequest.launch(arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ))
        }
    }
    private fun getAddressFromLatLng(latLng: LatLng) {
        try {
            val geocoder = Geocoder(this, Locale.KOREAN)
            val addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
            if (addresses != null && addresses.isNotEmpty()) {
                selectedAddress = addresses[0].getAddressLine(0)
                binding.selectLocationButton.text = "'$selectedAddress' 선택 완료"
            } else {
                selectedAddress = "주소를 찾을 수 없습니다."
                binding.selectLocationButton.text = "이 위치로 선택하기"
            }
        } catch (e: Exception) {
            e.printStackTrace()
            selectedAddress = "주소 변환 오류"
            binding.selectLocationButton.text = "이 위치로 선택하기"
        }
    }
}