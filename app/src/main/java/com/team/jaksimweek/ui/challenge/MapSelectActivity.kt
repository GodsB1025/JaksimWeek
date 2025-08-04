package com.team.jaksimweek.ui.challenge

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Bundle
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
import java.util.Locale

class MapSelectActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var binding: ActivityMapSelectBinding
    private lateinit var mMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private var selectedLatLng: LatLng? = null
    private var selectedAddress: String? = null

    // 위치 권한 요청을 처리하는 Launcher
    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) -> {
                // 권한이 승인되면 내 위치 활성화
                enableMyLocation()
            }
            permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false) -> {
                // 권한이 승인되면 내 위치 활성화
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

        // '이 위치로 선택하기' 버튼 리스너
        binding.selectLocationButton.setOnClickListener {
            if (selectedLatLng != null && selectedAddress != null) {
                val intent = Intent()
                intent.putExtra("latitude", selectedLatLng!!.latitude)
                intent.putExtra("longitude", selectedLatLng!!.longitude)
                intent.putExtra("address", selectedAddress)
                setResult(RESULT_OK, intent)
                finish() // 액티비티 종료
            } else {
                Toast.makeText(this, "먼저 지도를 탭하여 위치를 선택하세요.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // 지도가 준비되면 호출되는 콜백
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        enableMyLocation() // 내 위치 기능 활성화

        // 지도 탭 리스너 설정
        mMap.setOnMapClickListener { latLng ->
            mMap.clear() // 기존 마커 제거
            mMap.addMarker(MarkerOptions().position(latLng)) // 새로운 마커 추가
            selectedLatLng = latLng
            getAddressFromLatLng(latLng) // 좌표를 주소로 변환
        }
    }

    // 내 위치 기능을 활성화하는 함수
    private fun enableMyLocation() {
        if (ActivityCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            mMap.isMyLocationEnabled = true
            // 마지막으로 알려진 위치로 카메라 이동
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    val currentLatLng = LatLng(location.latitude, location.longitude)
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f))
                } else {
                    // 기본 위치 (서울)
                    val defaultLocation = LatLng(37.5665, 126.9780)
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 15f))
                }
            }
        } else {
            // 권한이 없으면 요청
            locationPermissionRequest.launch(arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ))
        }
    }

    // 좌표를 주소로 변환하는 함수
    private fun getAddressFromLatLng(latLng: LatLng) {
        try {
            val geocoder = Geocoder(this, Locale.KOREAN)
            val addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
            if (addresses != null && addresses.isNotEmpty()) {
                selectedAddress = addresses[0].getAddressLine(0)
                // 버튼 텍스트를 선택된 주소로 변경하여 사용자에게 확인시켜줌
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