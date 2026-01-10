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
import com.example.foodworldcup.R

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
class MapActivity : BaseActivity() {

    private lateinit var binding: ActivityMapBinding
    
    // ResultActivity로부터 전달받은 합격된 음식 리스트
    private var passedFoods: List<Food> = emptyList()
    
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
        
        try {
            binding = ActivityMapBinding.inflate(layoutInflater)
            setContentView(binding.root)

            // 하단 네비게이션 바 설정
            setupBottomNavigation(BaseActivity.Screen.MAP)

            // MapApiHelper 초기화 (예외 처리)
            try {
                mapApiHelper = MapApiHelper(this)
            } catch (e: Exception) {
                android.util.Log.e("MapActivity", "MapApiHelper 초기화 실패", e)
                // MapApiHelper 초기화 실패해도 지도는 표시 가능
            }

            // TODO: ResultActivity로부터 전달받은 합격된 음식 ID 리스트를 가져옵니다.
            // val passedFoodIds = intent.getIntegerArrayListExtra("passed_food_ids") ?: emptyList()
            // TODO: 음식 ID로 FoodRepository에서 음식 리스트를 가져옵니다.
            // passedFoods = passedFoodIds.mapNotNull { id -> 
            //     FoodRepository.getFoodList().find { it.id == id } 
            // }
            
            // 위치 권한을 확인하고 요청합니다.
            checkLocationPermission()
            
            // 지도를 초기화합니다.
            initializeMap()
            
            // 버튼 클릭 이벤트를 설정합니다.
            setupButtons()
        } catch (e: Exception) {
            android.util.Log.e("MapActivity", "onCreate 오류", e)
            e.printStackTrace()
            // 오류 발생 시 이전 화면으로 돌아가기
            finish()
        }
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
        try {
            // Google Maps를 초기화합니다.
            // SupportMapFragment를 가져와서 지도를 초기화합니다.
            val mapFragment = supportFragmentManager.findFragmentById(R.id.mapView) as? com.google.android.gms.maps.SupportMapFragment
            mapFragment?.getMapAsync { googleMap ->
                try {
                    // 지도 설정 (마커 클릭 리스너 등)
                    // 기본 지도 설정
                    googleMap.uiSettings.isZoomControlsEnabled = true
                    googleMap.uiSettings.isMyLocationButtonEnabled = true
                    
                    // 위치 권한이 있으면 현재 위치 표시
                    if (ContextCompat.checkSelfPermission(
                            this,
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        googleMap.isMyLocationEnabled = true
                    }
                } catch (e: Exception) {
                    android.util.Log.e("MapActivity", "지도 설정 오류", e)
                    e.printStackTrace()
                }
            } ?: run {
                android.util.Log.e("MapActivity", "SupportMapFragment를 찾을 수 없습니다")
            }
        } catch (e: Exception) {
            android.util.Log.e("MapActivity", "지도 초기화 오류", e)
            e.printStackTrace()
        }
    }

    /**
     * 주변 음식점을 검색하는 함수입니다.
     * 합격된 모든 음식에 대해 음식점을 검색합니다.
     */
    private fun searchNearbyRestaurants() {
        // TODO: MapApiHelper를 사용하여 합격된 각 음식 이름으로 주변 음식점을 검색합니다.
        // 모든 음식에 대한 검색 결과를 합쳐서 표시합니다.
        // 예: val allRestaurants = mutableListOf<Restaurant>()
        //     passedFoods.forEach { food ->
        //         mapApiHelper.searchRestaurants(
        //             foodName = food.name,
        //             latitude = currentLocation?.latitude ?: 0.0,
        //             longitude = currentLocation?.longitude ?: 0.0,
        //             onSuccess = { restaurants ->
        //                 allRestaurants.addAll(restaurants)
        //                 if (allRestaurants.size >= passedFoods.size) {
        //                     restaurantList = allRestaurants
        //                     displayRestaurantsOnMap()
        //                 }
        //             },
        //             onError = { error ->
        //                 // 에러 처리
        //             }
        //         )
        //     }
    }

    /**
     * 검색된 음식점을 지도에 마커로 표시하는 함수입니다.
     * 음식별 이미지로 커스터마이징한 마커를 표시합니다.
     */
    private fun displayRestaurantsOnMap() {
        // TODO: restaurantList의 각 음식점에 대해 지도에 마커를 추가합니다.
        // 마커는 해당 음식의 이미지로 커스터마이징합니다.
        // 마커 클릭 시 음식점 정보와 어떤 음식을 판매하는지 표시합니다.
        // 예: restaurantList.forEach { restaurant ->
        //     val food = passedFoods.find { it.name == restaurant.sellingFoodName }
        //     val markerIcon = BitmapDescriptorFactory.fromResource(food?.imageResId ?: R.drawable.default_food)
        //     val marker = googleMap.addMarker(
        //         MarkerOptions()
        //             .position(LatLng(restaurant.latitude, restaurant.longitude))
        //             .title(restaurant.name)
        //             .snippet("${restaurant.address} - ${food?.name}")
        //             .icon(markerIcon)
        //     )
        // }
    }

    /**
     * 버튼 클릭 이벤트를 설정하는 함수입니다.
     */
    private fun setupButtons() {
        try {
            // '뒤로가기' 버튼 클릭 시 이전 화면으로 돌아갑니다.
            binding.backButton?.setOnClickListener {
                finish()
            }
            
            // TODO: '다시 검색' 버튼 클릭 시 searchNearbyRestaurants()를 호출합니다.
            // binding.refreshButton?.setOnClickListener {
            //     searchNearbyRestaurants()
            // }
            
            // TODO: '내 위치로' 버튼 클릭 시 지도 중심을 현재 위치로 이동시킵니다.
            // binding.myLocationButton?.setOnClickListener {
            //     currentLocation?.let { location ->
            //         // 지도 중심을 현재 위치로 이동
            //     }
            // }
        } catch (e: Exception) {
            android.util.Log.e("MapActivity", "버튼 설정 오류", e)
            e.printStackTrace()
        }
    }

    override fun onResume() {
        super.onResume()
        // SupportMapFragment는 자동으로 생명주기를 관리합니다.
    }

    override fun onPause() {
        super.onPause()
        // SupportMapFragment는 자동으로 생명주기를 관리합니다.
    }

    override fun onDestroy() {
        super.onDestroy()
        // SupportMapFragment는 자동으로 생명주기를 관리합니다.
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        // SupportMapFragment는 자동으로 생명주기를 관리합니다.
    }

    override fun onLowMemory() {
        super.onLowMemory()
        // SupportMapFragment는 자동으로 생명주기를 관리합니다.
    }
}
