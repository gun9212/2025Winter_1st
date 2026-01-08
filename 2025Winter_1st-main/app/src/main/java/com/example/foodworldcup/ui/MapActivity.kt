package com.example.foodworldcup.ui

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.foodworldcup.data.Food
import com.example.foodworldcup.data.Restaurant
import com.example.foodworldcup.databinding.ActivityMapBinding
import com.example.foodworldcup.api.MapApiHelper

/**
 * 지도 화면을 담당하는 Activity입니다.
 * 우승한 음식을 판매하는 주변 음식점을 지도에 표시합니다.
 * 
 * 레이아웃 파일: res/layout/activity_map.xml
 * 
 * 주요 기능:
 * - Google Maps를 사용하여 지도 표시
 * - 현재 위치 권한 요청 및 위치 표시
 * - 우승 음식 이름으로 주변 음식점 검색
 * - 검색된 음식점을 지도에 마커로 표시
 * - 마커 클릭 시 음식점 정보 표시
 */
class MapActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMapBinding
    
    // ResultActivity로부터 전달받은 우승 음식
    private var winnerFood: Food? = null
    
    // 지도 API 헬퍼 객체
    private lateinit var mapApiHelper: MapApiHelper
    
    // 검색된 음식점 리스트
    private var restaurantList: List<Restaurant> = emptyList()
    
    // 현재 위치
    private var currentLocation: Location? = null
    
    // 위치 권한 요청 코드
    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // MapApiHelper 초기화
        mapApiHelper = MapApiHelper(this)

        // TODO: ResultActivity로부터 전달받은 우승 음식 데이터를 가져옵니다.
        // 예: winnerFood = intent.getParcelableExtra<Food>("winner_food")
        // 또는: val foodName = intent.getStringExtra("food_name")
        
        // TODO: 위치 권한을 확인하고 요청합니다.
        checkLocationPermission()
        
        // TODO: 지도를 초기화합니다.
        initializeMap()
        
        // TODO: 버튼 클릭 이벤트를 설정합니다.
        setupButtons()
    }

    /**
     * 위치 권한을 확인하고 필요시 요청하는 함수입니다.
     */
    private fun checkLocationPermission() {
        // TODO: ACCESS_FINE_LOCATION 권한이 있는지 확인합니다.
        // 권한이 없으면 ActivityCompat.requestPermissions()로 요청합니다.
        // 권한이 있으면 getCurrentLocation()을 호출합니다.
        
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        } else {
            getCurrentLocation()
        }
    }

    /**
     * 위치 권한 요청 결과를 처리하는 함수입니다.
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        // TODO: 권한이 승인되었으면 getCurrentLocation()을 호출합니다.
        // 권한이 거부되었으면 사용자에게 안내 메시지를 표시합니다.
        
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocation()
            } else {
                // 권한이 거부된 경우 처리
                // 예: Toast.makeText(this, "위치 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * 현재 위치를 가져오는 함수입니다.
     */
    private fun getCurrentLocation() {
        // TODO: FusedLocationProviderClient를 사용하여 현재 위치를 가져옵니다.
        // 위치를 가져온 후 currentLocation에 저장하고, 지도 중심을 현재 위치로 이동시킵니다.
        // 그리고 searchNearbyRestaurants()를 호출합니다.
    }

    /**
     * 지도를 초기화하는 함수입니다.
     */
    private fun initializeMap() {
        // TODO: Google Maps를 초기화합니다.
        // 예: binding.mapView.onCreate(savedInstanceState)
        //     binding.mapView.getMapAsync { googleMap ->
        //         this.googleMap = googleMap
        //         // 지도 설정 (마커 클릭 리스너 등)
        //     }
    }

    /**
     * 주변 음식점을 검색하는 함수입니다.
     */
    private fun searchNearbyRestaurants() {
        // TODO: MapApiHelper를 사용하여 우승 음식 이름으로 주변 음식점을 검색합니다.
        // 예: mapApiHelper.searchRestaurants(
        //     foodName = winnerFood?.name ?: "",
        //     latitude = currentLocation?.latitude ?: 0.0,
        //     longitude = currentLocation?.longitude ?: 0.0,
        //     onSuccess = { restaurants ->
        //         restaurantList = restaurants
        //         displayRestaurantsOnMap()
        //     },
        //     onError = { error ->
        //         // 에러 처리
        //     }
        // )
    }

    /**
     * 검색된 음식점을 지도에 마커로 표시하는 함수입니다.
     */
    private fun displayRestaurantsOnMap() {
        // TODO: restaurantList의 각 음식점에 대해 지도에 마커를 추가합니다.
        // 마커 클릭 시 음식점 정보를 보여주는 다이얼로그를 표시합니다.
        // 예: restaurantList.forEach { restaurant ->
        //     val marker = googleMap.addMarker(
        //         MarkerOptions()
        //             .position(LatLng(restaurant.latitude, restaurant.longitude))
        //             .title(restaurant.name)
        //             .snippet(restaurant.address)
        //     )
        // }
    }

    /**
     * 버튼 클릭 이벤트를 설정하는 함수입니다.
     */
    private fun setupButtons() {
        // TODO: '다시 검색' 버튼 클릭 시 searchNearbyRestaurants()를 호출합니다.
        
        // TODO: '내 위치로' 버튼 클릭 시 지도 중심을 현재 위치로 이동시킵니다.
        
        // TODO: '뒤로가기' 버튼 클릭 시 이전 화면으로 돌아갑니다.
        // 예: binding.backButton.setOnClickListener { finish() }
    }

    override fun onResume() {
        super.onResume()
        // TODO: 지도 뷰의 onResume()을 호출합니다.
        // 예: binding.mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        // TODO: 지도 뷰의 onPause()를 호출합니다.
        // 예: binding.mapView.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        // TODO: 지도 뷰의 onDestroy()를 호출합니다.
        // 예: binding.mapView.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        // TODO: 지도 뷰의 onSaveInstanceState()를 호출합니다.
        // 예: binding.mapView.onSaveInstanceState(outState)
    }

    override fun onLowMemory() {
        super.onLowMemory()
        // TODO: 지도 뷰의 onLowMemory()를 호출합니다.
        // 예: binding.mapView.onLowMemory()
    }
}
