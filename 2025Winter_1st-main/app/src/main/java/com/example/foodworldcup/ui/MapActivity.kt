package com.example.foodworldcup.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.widget.Button
import android.widget.LinearLayout
import com.example.foodworldcup.BuildConfig
import com.example.foodworldcup.R
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.kakao.vectormap.*
import com.kakao.vectormap.camera.CameraUpdateFactory
import com.kakao.vectormap.label.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query


// --- [1] 데이터 모델 (Kakao Local API JSON 응답용) ---
data class KakaoSearchResponse(
    val documents: List<Place>,
    val meta: Meta
)

data class Meta(
    val total_count: Int,
    val pageable_count: Int,
    val is_end: Boolean
)

data class Place(
    val id: String? = null, // place_id (상세 정보 조회용)
    val place_name: String,
    val category_name: String? = null,
    val category_group_code: String? = null,
    val phone: String? = null,
    val address_name: String? = null,
    val road_address_name: String? = null,
    val x: String, // 경도 (Longitude)
    val y: String, // 위도 (Latitude)
    val place_url: String? = null,
    val distance: String? = null,
    val foodType: String? = null // 음식 종류 (검색 시 설정)
)

// 상세 정보 응답 모델
data class PlaceDetailResponse(
    val documents: List<PlaceDetail>
)

data class PlaceDetail(
    val id: String? = null,
    val place_name: String? = null,
    val category_name: String? = null,
    val phone: String? = null,
    val address_name: String? = null,
    val road_address_name: String? = null,
    val x: String? = null,
    val y: String? = null,
    val place_url: String? = null,
    val home_page: String? = null,
    val bcode: String? = null,
    val hcode: String? = null
)

// --- [2] API 인터페이스 (Retrofit) ---
interface KakaoApiService {
    @GET("v2/local/search/keyword.json")
    fun searchPlace(
        @Header("Authorization") apiKey: String,
        @Query("query") query: String,
        @Query("x") longitude: String? = null,
        @Query("y") latitude: String? = null,
        @Query("radius") radius: Int? = null, // 미터 단위
        @Query("page") page: Int = 1,
        @Query("size") size: Int = 15
    ): Call<KakaoSearchResponse>

    // 상세 정보 조회 (place_id 사용)
    @GET("v2/local/search/detail.json")
    fun getPlaceDetail(
        @Header("Authorization") apiKey: String,
        @Query("id") placeId: String
    ): Call<PlaceDetailResponse>
}

// --- [3] 메인 액티비티 ---
class MapActivity : BaseActivity() {

    companion object {
        private const val PERMISSION_REQUEST_CODE = 1001
        private const val SEARCH_RADIUS = 4000 // 10km (미터 단위)
    }

    private lateinit var kakaoMap: KakaoMap
    private lateinit var mapView: MapView
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var progressBar: ProgressBar
    private lateinit var searchStatusText: TextView
    private lateinit var placeRecyclerView: RecyclerView
    private lateinit var confirmButton: Button
    
    // 선택 상태 관리
    private var selectedPlaceIndex: Int = -1 // 선택된 Place의 인덱스 (-1이면 선택 안됨)

    // Intent로 받은 음식 이름들 (합격된 음식 목록)
    private var foodNames: List<String> = emptyList()

    // Retrofit 설정
    private val retrofit = Retrofit.Builder()
        .baseUrl("https://dapi.kakao.com/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    private val apiService = retrofit.create(KakaoApiService::class.java)

    // API 키는 BuildConfig에서 가져옴 (local.properties에서 읽어옴)
    private val restApiKey: String = "KakaoAK ${BuildConfig.KAKAO_REST_API_KEY}"

    // 현재 위치
    private var currentLatitude: Double? = null
    private var currentLongitude: Double? = null

    // 마커 저장용 (마커 클릭 시 정보 표시를 위해)
    private val placeMarkers = mutableMapOf<Label, Place>()
    private val placeToLabelMap = mutableMapOf<String, Label>() // Place ID로 Label 찾기용
    private var selectedLabel: Label? = null // 현재 선택된 마커
    
    // 검색 결과 리스트
    private val searchResults = mutableListOf<Place>()
    private lateinit var placeAdapter: PlaceAdapter
    
    // 마커 스타일 (일반, 선택된 마커, 현재 위치 마커)
    private var normalMarkerStyle: LabelStyles? = null
    private var selectedMarkerStyle: LabelStyles? = null
    private var myLocationMarkerStyle: LabelStyles? = null
    private var myLocationLabel: Label? = null // 현재 위치 마커

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)

        // 하단 네비게이션 바 설정
        setupBottomNavigation(BaseActivity.Screen.MAP)


        // Intent에서 음식 이름 리스트 받기
        foodNames = intent.getStringArrayListExtra("food_names") ?: emptyList()
        
        // 합격된 음식 ID 리스트가 전달된 경우 (ResultActivity에서)
        val passedFoodIds = intent.getIntegerArrayListExtra("passed_food_ids")
        if (passedFoodIds != null && passedFoodIds.isNotEmpty()) {
            // FoodRepository에서 음식 이름 가져오기
            foodNames = passedFoodIds.mapNotNull { id ->
                com.example.foodworldcup.data.FoodRepository.getFoodById(id)?.name
            }
        }

        // 음식 이름이 없으면 기본값 사용
        if (foodNames.isEmpty()) {
            foodNames = listOf("치킨", "피자", "삼겹살")
        }

        // UI 초기화
        progressBar = findViewById(R.id.progressBar)
        searchStatusText = findViewById(R.id.searchStatusText)
        mapView = findViewById(R.id.map_view)
        placeRecyclerView = findViewById(R.id.placeRecyclerView)

        // 위치 클라이언트 초기화
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // 현재 위치 버튼 설정
        findViewById<android.widget.ImageButton>(R.id.myLocationButton)?.setOnClickListener {
            moveToMyLocation()
        }

        // 확인 버튼 초기화
        confirmButton = findViewById(R.id.confirmButton)
        confirmButton.setOnClickListener {
            // 확인 버튼 클릭 시 업적 페이지로 이동
            navigateToAchievement()
        }

        // RecyclerView 초기화
        placeAdapter = PlaceAdapter(searchResults, -1) { place, position ->
            // 리스트 아이템 클릭 시 선택 상태 변경
            handlePlaceItemClick(place, position)
        }
        val layoutManager = LinearLayoutManager(this)
        placeRecyclerView.layoutManager = layoutManager
        placeRecyclerView.adapter = placeAdapter

        // Sticky Header 추가 (선택된 음식 종류가 상단에 고정)
        val stickyHeaderDecoration = StickyHeaderItemDecoration(placeAdapter)
        placeRecyclerView.addItemDecoration(stickyHeaderDecoration)

        // 스와이프 기능 설정
        setupSwipeGesture()
        
        // 어댑터 변경 시 ItemDecoration 업데이트
        placeRecyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                // 스크롤 시 ItemDecoration 재그리기
                recyclerView.invalidateItemDecorations()
            }
        })

        // 카카오맵 초기화
        initMap()
    }

    override fun onResume() {
        super.onResume()
        // Kakao Map SDK 권장사항: MapView lifecycle 관리
        if (::mapView.isInitialized) {
            mapView.resume()
        }
    }

    override fun onPause() {
        super.onPause()
        // Kakao Map SDK 권장사항: MapView lifecycle 관리
        if (::mapView.isInitialized) {
            mapView.pause()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // MapView 리소스 정리는 onMapDestroy()에서 자동으로 처리됩니다
        // 별도의 stop() 메서드 호출은 필요 없습니다
    }

    /**
     * 카카오맵 초기화
     * 참고: https://apis.map.kakao.com/android_v2/docs/getting-started/quickstart/
     */
    private fun initMap() {
        mapView.start(
            object : MapLifeCycleCallback() {
                override fun onMapDestroy() {
                    // 지도가 파괴될 때 처리
                }

                override fun onMapError(error: Exception) {
                    Toast.makeText(this@MapActivity, "지도를 불러오는데 실패했습니다: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            },
            object : KakaoMapReadyCallback() {
            override fun onMapReady(kakaoMap: KakaoMap) {
                    this@MapActivity.kakaoMap = kakaoMap

                    // 마커 스타일 초기화
                    initMarkerStyles(kakaoMap)

                    // 마커 클릭 이벤트 설정 (다이얼로그 없이 선택만 처리)
                kakaoMap.setOnLabelClickListener { _, _, label ->
                        val place = placeMarkers[label]
                    if (place != null) {
                            // 마커 클릭 시 지도에서만 선택 처리 (다이얼로그 없음)
                            updateSelectedMarker(place)
                            moveToPlace(place)
                            
                            // 리스트에서도 선택 상태 업데이트
                            val index = searchResults.indexOfFirst { 
                                (it.id != null && it.id == place.id) || 
                                (it.id == null && it.place_name == place.place_name && 
                                 it.x == place.x && it.y == place.y)
                            }
                            if (index >= 0) {
                                handlePlaceItemClick(place, index)
                            }
                    }
                    true
                }

                    // 위치 권한 확인 후 위치 가져오기 및 검색 시작
                    checkLocationPermissionAndSearch()
                }
            }
        )
    }

    /**
     * 마커 스타일 초기화 (일반 및 선택된 마커)
     */
    private fun initMarkerStyles(kakaoMap: KakaoMap) {
        try {
            android.util.Log.d("MapActivity", "마커 스타일 초기화 시작")
            
            val labelManager = kakaoMap.labelManager
            if (labelManager == null) {
                android.util.Log.e("MapActivity", "labelManager가 null입니다")
                return
            }

            // Drawable을 Bitmap으로 변환하여 사용
            // 일반 마커 스타일 (작은 빨간 핀) - 크기 50% 축소
            try {
                val drawable = ContextCompat.getDrawable(this, R.drawable.ic_marker_restaurant)
                if (drawable != null) {
                    // Vector drawable을 비트맵으로 변환 (dp를 px로 변환) - 50% 축소 (24dp)
                    val density = resources.displayMetrics.density
                    val width = (24 * density).toInt() // 24dp (원래 48dp의 50%)
                    val height = (24 * density).toInt() // 24dp
                    
                    // 비트맵 생성 및 그리기
                    val bitmap = android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.ARGB_8888)
                    val canvas = android.graphics.Canvas(bitmap)
                    drawable.setBounds(0, 0, width, height)
                    drawable.draw(canvas)
                    
                    android.util.Log.d("MapActivity", "일반 마커 비트맵 생성 완료: ${width}x${height}")
                    
                    val normalStyle = LabelStyle.from(bitmap)
                    if (normalStyle != null) {
                        val styles = LabelStyles.from(normalStyle)
                        normalMarkerStyle = labelManager.addLabelStyles(styles)
                        android.util.Log.d("MapActivity", "일반 마커 스타일 추가 완료: ${normalMarkerStyle != null}")
                    } else {
                        android.util.Log.e("MapActivity", "LabelStyle.from()이 null을 반환했습니다")
                    }
                } else {
                    android.util.Log.e("MapActivity", "ic_marker_restaurant drawable을 찾을 수 없습니다")
                }
            } catch (e: Exception) {
                android.util.Log.e("MapActivity", "일반 마커 스타일 생성 실패: ${e.message}", e)
                e.printStackTrace()
            }

            // 선택된 마커 스타일 (큰 파란 핀) - 크기 50% 축소
            try {
                val drawable = ContextCompat.getDrawable(this, R.drawable.ic_marker_selected)
                if (drawable != null) {
                    // Vector drawable을 비트맵으로 변환 (더 큰 크기) - 50% 축소 (32dp)
                    val density = resources.displayMetrics.density
                    val width = (32 * density).toInt() // 32dp (원래 64dp의 50%)
                    val height = (32 * density).toInt() // 32dp
                    
                    // 비트맵 생성 및 그리기
                    val bitmap = android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.ARGB_8888)
                    val canvas = android.graphics.Canvas(bitmap)
                    drawable.setBounds(0, 0, width, height)
                    drawable.draw(canvas)
                    
                    android.util.Log.d("MapActivity", "선택된 마커 비트맵 생성 완료: ${width}x${height}")
                    
                    val selectedStyle = LabelStyle.from(bitmap)
                    if (selectedStyle != null) {
                        val styles = LabelStyles.from(selectedStyle)
                        selectedMarkerStyle = labelManager.addLabelStyles(styles)
                        android.util.Log.d("MapActivity", "선택된 마커 스타일 추가 완료: ${selectedMarkerStyle != null}")
                    } else {
                        android.util.Log.e("MapActivity", "LabelStyle.from()이 null을 반환했습니다 (선택된 마커)")
                    }
                } else {
                    android.util.Log.e("MapActivity", "ic_marker_selected drawable을 찾을 수 없습니다")
                }
            } catch (e: Exception) {
                android.util.Log.e("MapActivity", "선택된 마커 스타일 생성 실패: ${e.message}", e)
                e.printStackTrace()
            }

            // 현재 위치 마커 스타일 (파란색 원형)
            try {
                val drawable = ContextCompat.getDrawable(this, R.drawable.ic_my_location_marker)
                if (drawable != null) {
                    val density = resources.displayMetrics.density
                    val width = (16 * density).toInt() // 16dp (원래 32dp의 50%)
                    val height = (16 * density).toInt() // 16dp
                    
                    val bitmap = android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.ARGB_8888)
                    val canvas = android.graphics.Canvas(bitmap)
                    drawable.setBounds(0, 0, width, height)
                    drawable.draw(canvas)
                    
                    android.util.Log.d("MapActivity", "현재 위치 마커 비트맵 생성 완료: ${width}x${height}")
                    
                    if (bitmap != null) {
                        val locationStyle = LabelStyle.from(bitmap)
                        if (locationStyle != null) {
                            val styles = LabelStyles.from(locationStyle)
                            myLocationMarkerStyle = labelManager.addLabelStyles(styles)
                            android.util.Log.d("MapActivity", "현재 위치 마커 스타일 추가 완료: ${myLocationMarkerStyle != null}")
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("MapActivity", "현재 위치 마커 스타일 생성 실패: ${e.message}", e)
                e.printStackTrace()
            }
        } catch (e: Exception) {
            android.util.Log.e("MapActivity", "마커 스타일 초기화 실패: ${e.message}", e)
            e.printStackTrace()
        }
    }

    /**
     * 음식점 선택 (지도에서 클릭 시 - 마커 클릭)
     */
    private fun selectPlace(place: Place) {
        // 1. 선택된 마커로 변경
        updateSelectedMarker(place)
        
        // 2. 지도 중심 이동
        moveToPlace(place)
        
        // 3. 리스트에서 해당 항목으로 스크롤
        movePlaceToTop(place)
        
        // 4. 리스트에서 선택 상태 업데이트
        val index = searchResults.indexOfFirst { 
            (it.id != null && it.id == place.id) || 
            (it.id == null && it.place_name == place.place_name && 
             it.x == place.x && it.y == place.y)
        }
        if (index >= 0) {
            handlePlaceItemClick(place, index)
        }
    }

    /**
     * 리스트 아이템 클릭 처리 (선택 상태 관리)
     */
    private fun handlePlaceItemClick(place: Place, position: Int) {
        // 같은 아이템을 두 번 클릭하면 선택 해제
        if (selectedPlaceIndex == position) {
            selectedPlaceIndex = -1
            confirmButton.visibility = android.view.View.GONE
            // 선택 해제 시 모든 항목 원래 색으로 복원
        } else {
            // 다른 아이템 선택
            selectedPlaceIndex = position
            confirmButton.visibility = android.view.View.VISIBLE
            
            // 지도에서도 선택
            updateSelectedMarker(place)
            moveToPlace(place)
            movePlaceToTop(place)
        }
        
        // 어댑터에 선택 상태 업데이트 (실시간 반영)
        placeAdapter.updateSelectedIndex(selectedPlaceIndex)
        
        // 모든 visible ViewHolder를 직접 업데이트하여 실시간 반영 보장
        updateVisibleItemViews()
        
        // Sticky Header 업데이트를 위해 ItemDecoration 재그리기
        placeRecyclerView.invalidateItemDecorations()
    }
    
    /**
     * 현재 화면에 보이는 모든 아이템 뷰를 직접 업데이트 (실시간 색상 변경)
     */
    private fun updateVisibleItemViews() {
        val layoutManager = placeRecyclerView.layoutManager as? LinearLayoutManager ?: return
        
        val firstVisible = layoutManager.findFirstVisibleItemPosition()
        val lastVisible = layoutManager.findLastVisibleItemPosition()
        
        if (firstVisible == RecyclerView.NO_POSITION || lastVisible == RecyclerView.NO_POSITION) return
        
        // 모든 visible 아이템 업데이트
        for (i in firstVisible..lastVisible) {
            val viewHolder = placeRecyclerView.findViewHolderForAdapterPosition(i)
            if (viewHolder is PlaceAdapter.PlaceViewHolder) {
                val item = placeAdapter.items.getOrNull(i)
                if (item is PlaceAdapter.AdapterItem.PlaceItem && item.index < placeAdapter.places.size) {
                    val place = placeAdapter.places[item.index]
                    val isSelected = item.index == selectedPlaceIndex
                    
                    // 선택 상태에 따라 배경색 즉시 변경
                    if (selectedPlaceIndex >= 0) {
                        if (isSelected) {
                            // 선택된 항목: 흰색 배경
                            viewHolder.itemCard.setCardBackgroundColor(
                                ContextCompat.getColor(this, android.R.color.white)
                            )
                            viewHolder.itemContent.setBackgroundColor(
                                ContextCompat.getColor(this, android.R.color.white)
                            )
                        } else {
                            // 비선택 항목: 회색 배경
                            viewHolder.itemCard.setCardBackgroundColor(
                                ContextCompat.getColor(this, android.R.color.darker_gray)
                            )
                            viewHolder.itemContent.setBackgroundColor(
                                ContextCompat.getColor(this, android.R.color.darker_gray)
                            )
                        }
                    } else {
                        // 선택 상태가 아니면 모두 흰색 (원래 색상)
                        viewHolder.itemCard.setCardBackgroundColor(
                            ContextCompat.getColor(this, android.R.color.white)
                        )
                        viewHolder.itemContent.setBackgroundColor(
                            ContextCompat.getColor(this, android.R.color.white)
                        )
                    }
                }
            }
        }
    }

    /**
     * 스와이프 제스처 설정
     */
    private fun setupSwipeGesture() {
        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
            0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
        ) {
            override fun getSwipeDirs(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder
            ): Int {
                // Header 아이템은 스와이프 비활성화
                if (viewHolder.itemViewType == PlaceAdapter.VIEW_TYPE_HEADER) {
                    return 0
                }
                return super.getSwipeDirs(recyclerView, viewHolder)
            }

            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                return false
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.bindingAdapterPosition
                if (position == RecyclerView.NO_POSITION || position >= placeAdapter.items.size) return

                when (val item = placeAdapter.items[position]) {
                    is PlaceAdapter.AdapterItem.PlaceItem -> {
                        if (item.index < searchResults.size) {
                            val place = searchResults[item.index]
                            when (direction) {
                                ItemTouchHelper.LEFT -> {
                                    // 왼쪽 스와이프: 상세정보
                                    openPlaceDetail(place)
                                }
                                ItemTouchHelper.RIGHT -> {
                                    // 오른쪽 스와이프: 길찾기
                                    openNavigation(place)
                                }
                            }
                        }
                    }
                    else -> {
                        // 헤더는 스와이프하지 않음
                        return
                    }
                }
                // 스와이프 후 아이템 복원 (스와이프는 실제로 삭제하지 않음)
                placeAdapter.notifyItemChanged(position)
            }

            override fun onChildDraw(
                c: android.graphics.Canvas,
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                dX: Float,
                dY: Float,
                actionState: Int,
                isCurrentlyActive: Boolean
            ) {
                val position = viewHolder.bindingAdapterPosition
                if (position == RecyclerView.NO_POSITION || position >= placeAdapter.items.size) {
                    super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
                    return
                }

                // 헤더 아이템은 스와이프하지 않음
                if (placeAdapter.items[position] is PlaceAdapter.AdapterItem.Header) {
                    super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
                    return
                }

                if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
                    val itemView = viewHolder.itemView
                    val leftSwipeBackground = itemView.findViewById<androidx.cardview.widget.CardView>(R.id.leftSwipeBackground)
                    val rightSwipeBackground = itemView.findViewById<androidx.cardview.widget.CardView>(R.id.rightSwipeBackground)
                    val itemCard = itemView.findViewById<androidx.cardview.widget.CardView>(R.id.itemCard)

                    if (leftSwipeBackground != null && rightSwipeBackground != null && itemCard != null) {
                        // 배경의 높이를 아이템과 맞춤
                        val itemHeight = itemView.height
                        if (itemHeight > 0) {
                            val leftParams = leftSwipeBackground.layoutParams
                            if (leftParams.height != itemHeight) {
                                leftParams.height = itemHeight
                                leftSwipeBackground.layoutParams = leftParams
                            }
                            val rightParams = rightSwipeBackground.layoutParams
                            if (rightParams.height != itemHeight) {
                                rightParams.height = itemHeight
                                rightSwipeBackground.layoutParams = rightParams
                            }
                        }
                        
                        // 배경을 완전히 고정: translationX를 항상 0으로 강제 설정 (움직이지 않음)
                        leftSwipeBackground.translationX = 0f
                        rightSwipeBackground.translationX = 0f
                        itemView.translationX = 0f
                        
                        // itemCard만 스와이프에 따라 이동 (배경은 완전히 고정)
                        itemCard.translationX = dX
                        
                        // 배경 표시 여부만 업데이트 (위치는 변경하지 않음 - 완전히 고정)
                        if (dX > 0) {
                            // 오른쪽 스와이프 (길찾기) - 아이템이 오른쪽으로 이동하면 왼쪽에 배경 표시, 왼쪽에 글씨
                            rightSwipeBackground.visibility = android.view.View.VISIBLE
                            leftSwipeBackground.visibility = android.view.View.GONE
                        } else if (dX < 0) {
                            // 왼쪽 스와이프 (상세정보) - 아이템이 왼쪽으로 이동하면 오른쪽에 배경 표시, 오른쪽에 글씨
                            leftSwipeBackground.visibility = android.view.View.VISIBLE
                            rightSwipeBackground.visibility = android.view.View.GONE
                        } else {
                            leftSwipeBackground.visibility = android.view.View.GONE
                            rightSwipeBackground.visibility = android.view.View.GONE
                        }
                        
                        // ItemTouchHelper의 기본 그리기를 dX=0으로 호출하여 itemView가 이동하지 않도록 함
                        // itemCard는 이미 translationX로 직접 이동시켰으므로 배경만 고정됨
                        super.onChildDraw(c, recyclerView, viewHolder, 0f, dY, actionState, isCurrentlyActive)
                    } else {
                        // 배경이나 itemCard를 찾을 수 없으면 기본 동작 사용
                        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
                    }
                } else {
                    // 스와이프가 아닐 때는 배경 숨김 및 위치 초기화
                    val leftSwipeBackground = viewHolder.itemView.findViewById<androidx.cardview.widget.CardView>(R.id.leftSwipeBackground)
                    val rightSwipeBackground = viewHolder.itemView.findViewById<androidx.cardview.widget.CardView>(R.id.rightSwipeBackground)
                    val itemCard = viewHolder.itemView.findViewById<androidx.cardview.widget.CardView>(R.id.itemCard)
                    leftSwipeBackground?.visibility = android.view.View.GONE
                    rightSwipeBackground?.visibility = android.view.View.GONE
                    leftSwipeBackground?.translationX = 0f
                    rightSwipeBackground?.translationX = 0f
                    itemCard?.translationX = 0f
                    viewHolder.itemView.translationX = 0f
                    super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
                }
            }
            
            override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
                super.clearView(recyclerView, viewHolder)
                // 스와이프 완료 후 아이템 위치 초기화
                val itemView = viewHolder.itemView
                val leftSwipeBackground = itemView.findViewById<androidx.cardview.widget.CardView>(R.id.leftSwipeBackground)
                val rightSwipeBackground = itemView.findViewById<androidx.cardview.widget.CardView>(R.id.rightSwipeBackground)
                val itemCard = itemView.findViewById<androidx.cardview.widget.CardView>(R.id.itemCard)
                
                leftSwipeBackground?.visibility = android.view.View.GONE
                rightSwipeBackground?.visibility = android.view.View.GONE
                leftSwipeBackground?.translationX = 0f
                rightSwipeBackground?.translationX = 0f
                itemCard?.translationX = 0f
                itemView.translationX = 0f
            }
        })

        itemTouchHelper.attachToRecyclerView(placeRecyclerView)
    }

    /**
     * 상세정보 페이지 열기
     */
    private fun openPlaceDetail(place: Place) {
        // 카카오맵 상세정보 페이지로 이동
        openKakaoMapDetail(place)
    }

    /**
     * 길찾기 페이지 열기
     */
    private fun openNavigation(place: Place) {
        val lat = place.y.toDoubleOrNull()
        val lng = place.x.toDoubleOrNull()
        if (lat != null && lng != null) {
            openNavigation(lat, lng, place.place_name)
        }
    }

    /**
     * 업적 페이지로 이동
     */
    private fun navigateToAchievement() {
        if (selectedPlaceIndex >= 0 && selectedPlaceIndex < searchResults.size) {
            val intent = Intent(this, MyPageActivity::class.java)
            startActivity(intent)
        }
    }

    /**
     * 선택된 마커 업데이트 (크기 변경)
     * 선택된 마커의 작은 마커를 제거하고 큰 마커를 추가합니다.
     * 이전에 선택된 마커가 있으면 큰 마커를 제거하고 작은 마커를 다시 추가합니다.
     */
    private fun updateSelectedMarker(place: Place) {
        if (!::kakaoMap.isInitialized) return
        
        try {
            val placeKey = place.id ?: "${place.place_name}_${place.x}_${place.y}"
            
            // 이전에 선택된 마커가 있으면 큰 마커 제거하고 작은 마커 다시 추가
            selectedLabel?.let { oldSelectedLabel ->
                val oldPlace = placeMarkers[oldSelectedLabel]
                if (oldPlace != null) {
                    // 이전 선택된 마커의 큰 마커 제거
                    removeLabelFromLayer(oldSelectedLabel)
                    placeMarkers.remove(oldSelectedLabel)
                    
                    // 이전 선택된 마커의 작은 마커 다시 추가
                    val oldPlaceKey = oldPlace.id ?: "${oldPlace.place_name}_${oldPlace.x}_${oldPlace.y}"
                    placeToLabelMap.remove("${oldPlaceKey}_selected")
                    updateMarkerSize(oldPlace, false)
                }
                selectedLabel = null
            }
            
            // 현재 선택된 마커의 작은 마커 찾기
            val currentNormalLabel = placeToLabelMap[placeKey]
            
            // 작은 마커가 있으면 제거
            if (currentNormalLabel != null) {
                removeLabelFromLayer(currentNormalLabel)
                placeMarkers.remove(currentNormalLabel)
                placeToLabelMap.remove(placeKey)
            }
            
            // 큰 마커 추가
            updateMarkerSize(place, true)
        } catch (e: Exception) {
            android.util.Log.e("MapActivity", "마커 업데이트 실패: ${e.message}", e)
            e.printStackTrace()
        }
    }

    /**
     * 마커 크기 업데이트 (선택/비선택)
     * 선택된 경우: 큰 마커 추가
     * 비선택 시: 작은 마커 다시 추가
     */
    private fun updateMarkerSize(place: Place, isSelected: Boolean) {
        if (!::kakaoMap.isInitialized) return
    
        try {
            val placeKey = place.id ?: "${place.place_name}_${place.x}_${place.y}"
            val lat = place.y.toDoubleOrNull() ?: return
            val lng = place.x.toDoubleOrNull() ?: return
    
            val labelManager = kakaoMap.labelManager ?: return
            val layer = labelManager.layer ?: return
    
            if (isSelected) {
                // 선택된 경우: 큰 파란 마커 추가
                val styles = selectedMarkerStyle ?: return
    
                // ⭐ [핵심 수정] setRank(1000) 추가
                // 숫자가 클수록 다른 마커들보다 위에 그려집니다.
                val options = LabelOptions.from(LatLng.from(lat, lng))
                    .setStyles(styles)
                    .setRank(1000) 
    
                val selectedLabelNew = layer.addLabel(options)
                if (selectedLabelNew != null) {
                    placeMarkers[selectedLabelNew] = place
                    placeToLabelMap["${placeKey}_selected"] = selectedLabelNew
                    selectedLabel = selectedLabelNew
                    android.util.Log.d("MapActivity", "큰 마커 추가 성공 (Rank 1000): ${place.place_name}")
                }
            } else {
                // 비선택 시: 작은 마커 다시 추가
                val styles = normalMarkerStyle ?: return
    
                // ⭐ [핵심 수정] setRank(0) 추가 (기본값)
                // 선택되지 않은 마커는 낮은 순위를 줍니다.
                val options = LabelOptions.from(LatLng.from(lat, lng))
                    .setStyles(styles)
                    .setRank(0) 
    
                val normalLabel = layer.addLabel(options)
                if (normalLabel != null) {
                    placeMarkers[normalLabel] = place
                    placeToLabelMap[placeKey] = normalLabel
                    android.util.Log.d("MapActivity", "작은 마커 다시 추가 성공 (Rank 0): ${place.place_name}")
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("MapActivity", "마커 크기 업데이트 실패: ${e.message}", e)
            e.printStackTrace()
        }
    }

    /**
     * 리스트에서 선택한 음식점으로 스크롤 (화면 상단에 보이게, sticky header 높이 고려)
     */
    private fun movePlaceToTop(place: Place) {
        val index = searchResults.indexOfFirst { 
            (it.id != null && it.id == place.id) || 
            (it.id == null && it.place_name == place.place_name && 
             it.x == place.x && it.y == place.y)
        }
        
        if (index >= 0 && ::placeAdapter.isInitialized) {
            // 어댑터의 getAdapterPositionForPlace 메서드를 사용하여 실제 position 찾기
            val adapterPosition = placeAdapter.getAdapterPositionForPlace(index)
            if (adapterPosition >= 0) {
                // sticky header 높이 계산 (헤더가 있는 경우)
                var headerHeight = 0
                val layoutManager = placeRecyclerView.layoutManager as? LinearLayoutManager
                
                // 현재 위치의 헤더 찾기
                for (i in adapterPosition downTo 0) {
                    if (i < placeAdapter.items.size && placeAdapter.items[i] is PlaceAdapter.AdapterItem.Header) {
                        val headerView = layoutManager?.findViewByPosition(i)
                        headerHeight = headerView?.height ?: 0
                        
                        // 헤더 뷰가 아직 측정되지 않은 경우, 헤더 레이아웃에서 높이 계산
                        if (headerHeight == 0) {
                            val headerLayout = LayoutInflater.from(this).inflate(R.layout.item_food_type_header, null)
                            headerLayout.measure(
                                android.view.View.MeasureSpec.makeMeasureSpec(placeRecyclerView.width, android.view.View.MeasureSpec.EXACTLY),
                                android.view.View.MeasureSpec.makeMeasureSpec(0, android.view.View.MeasureSpec.UNSPECIFIED)
                            )
                            headerHeight = headerLayout.measuredHeight
                        }
                        break
                    }
                }
                
                // sticky header가 표시되면 그 높이만큼 오프셋 추가
                val offset = headerHeight
                
                // 스크롤하여 화면 상단에 보이게 하기 (sticky header 아래에 표시)
                layoutManager?.scrollToPositionWithOffset(adapterPosition, offset)
            }
        }
    }

    /**
     * 특정 장소로 지도 이동 (현재 줌 레벨 유지)
     */
    private fun moveToPlace(place: Place) {
        if (!::kakaoMap.isInitialized) {
            Toast.makeText(this, "지도가 아직 준비되지 않았습니다.", Toast.LENGTH_SHORT).show()
            return
        }
        
        val lat = place.y.toDoubleOrNull()
        val lng = place.x.toDoubleOrNull()
        
        if (lat != null && lng != null) {
            // 현재 줌 레벨을 유지하면서 중심만 이동
            val cameraUpdate = CameraUpdateFactory.newCenterPosition(
                LatLng.from(lat, lng)
            )
            kakaoMap.moveCamera(cameraUpdate)
        }
    }

    /**
     * 위치 권한 확인 및 검색 시작
     */
    private fun checkLocationPermissionAndSearch() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                // 권한이 이미 있음
                findMyLocationAndSearch()
            }
            ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) -> {
                // 권한 설명이 필요한 경우
                AlertDialog.Builder(this)
                    .setTitle("위치 권한 필요")
                    .setMessage("주변 음식점을 검색하기 위해 위치 권한이 필요합니다.")
                    .setPositiveButton("확인") { _, _ ->
                        ActivityCompat.requestPermissions(
                            this,
                            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                            PERMISSION_REQUEST_CODE
                        )
                    }
                    .setNegativeButton("취소", null)
                    .show()
            }
            else -> {
                // 권한 요청
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    PERMISSION_REQUEST_CODE
                )
            }
        }
    }

    /**
     * 권한 요청 결과 처리
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        when (requestCode) {
            PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // 권한 승인됨
                    findMyLocationAndSearch()
                } else {
                    // 권한 거부됨
                    Toast.makeText(this, "위치 권한이 필요합니다. 설정에서 권한을 허용해주세요.", Toast.LENGTH_LONG).show()
                    // 위치 없이도 검색 가능하도록 (전체 지역 검색)
                    searchRestaurantsWithoutLocation()
                }
            }
        }
    }

    /**
     * 내 위치 찾기 및 검색 시작
     */
    private fun findMyLocationAndSearch() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                currentLatitude = location.latitude
                currentLongitude = location.longitude

                // kakaoMap이 초기화되었는지 확인
                if (::kakaoMap.isInitialized) {
                    // 현재 위치 마커 추가
                    addMyLocationMarker(location.latitude, location.longitude)

                // 지도 중심을 내 위치로 이동
                    val cameraUpdate = CameraUpdateFactory.newCenterPosition(
                        LatLng.from(location.latitude, location.longitude)
                    )
                    kakaoMap.moveCamera(cameraUpdate)

                    // 지도 줌 레벨 설정 (Float 타입)
                    val zoomUpdate = CameraUpdateFactory.zoomTo(14)
                    kakaoMap.moveCamera(zoomUpdate)
                }

                // 음식점 검색 시작
                searchRestaurants()
            } else {
                Toast.makeText(this, "현재 위치를 가져올 수 없습니다. 전체 지역에서 검색합니다.", Toast.LENGTH_SHORT).show()
                searchRestaurantsWithoutLocation()
            }
        }.addOnFailureListener { e ->
            Toast.makeText(this, "위치를 가져오는데 실패했습니다: ${e.message}", Toast.LENGTH_SHORT).show()
            searchRestaurantsWithoutLocation()
        }
    }

    /**
     * 내 위치로 지도 이동
     */
    private fun moveToMyLocation() {
        if (!::kakaoMap.isInitialized) {
            Toast.makeText(this, "지도가 아직 준비되지 않았습니다.", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (currentLatitude != null && currentLongitude != null) {
            // 현재 위치 마커 추가/업데이트
            addMyLocationMarker(currentLatitude!!, currentLongitude!!)
            
            // 현재 줌 레벨 유지하면서 중심만 이동
            val cameraUpdate = CameraUpdateFactory.newCenterPosition(
                LatLng.from(currentLatitude!!, currentLongitude!!)
            )
            kakaoMap.moveCamera(cameraUpdate)
            // Toast.makeText(this, "현재 위치로 이동했습니다.", Toast.LENGTH_SHORT).show()
        } else {
            // 위치 정보가 없으면 다시 권한 확인 및 위치 가져오기
            checkLocationPermissionAndSearch()
        }
    }

    /**
     * 현재 위치 마커 추가
     */
    private fun addMyLocationMarker(latitude: Double, longitude: Double) {
        if (!::kakaoMap.isInitialized) return
        
        try {
            // 기존 현재 위치 마커 제거
            myLocationLabel?.let { oldLabel ->
                removeLabelFromLayer(oldLabel)
                placeMarkers.remove(oldLabel)
                myLocationLabel = null
            }
            
            // 현재 위치 마커 스타일이 없으면 초기화
            if (myLocationMarkerStyle == null) {
                initMarkerStyles(kakaoMap)
            }
            
            val styles = myLocationMarkerStyle ?: run {
                android.util.Log.w("MapActivity", "현재 위치 마커 스타일이 초기화되지 않았습니다.")
                return
            }
            
            val labelManager = kakaoMap.labelManager
            val layer = labelManager?.layer ?: return
            
            val latLng = LatLng.from(latitude, longitude)
            val options = LabelOptions.from(latLng)
                .setStyles(styles)
            
            val label = layer.addLabel(options)
            if (label != null) {
                myLocationLabel = label
                android.util.Log.d("MapActivity", "현재 위치 마커 추가 성공: ($latitude, $longitude)")
            }
        } catch (e: Exception) {
            android.util.Log.e("MapActivity", "현재 위치 마커 추가 실패: ${e.message}", e)
        }
    }

    /**
     * 레이어에서 마커 제거
     * Kakao Map SDK v2에서는 Label 객체나 Layer의 remove 메서드를 사용합니다.
     */
    private fun removeLabelFromLayer(label: Label) {
        if (!::kakaoMap.isInitialized) return
        
        try {
            val layer = kakaoMap.labelManager?.layer ?: return
            
            // Kakao Map SDK v2에서 마커 제거 방법 시도
            // 방법 1: Label 객체의 remove() 메서드
            try {
                val removeMethod = label.javaClass.getMethod("remove")
                removeMethod.invoke(label)
                android.util.Log.d("MapActivity", "마커 제거 성공 (Label.remove 메서드)")
                return
            } catch (e: NoSuchMethodException) {
                // 방법 2: Layer의 remove(Label) 메서드
                try {
                    val removeMethod = layer.javaClass.getMethod("remove", Label::class.java)
                    removeMethod.invoke(layer, label)
                    android.util.Log.d("MapActivity", "마커 제거 성공 (Layer.remove 메서드)")
                    return
                } catch (e2: NoSuchMethodException) {
                    // 방법 3: Layer의 removeLabel(Label) 메서드
                    try {
                        val removeLabelMethod = layer.javaClass.getMethod("removeLabel", Label::class.java)
                        removeLabelMethod.invoke(layer, label)
                        android.util.Log.d("MapActivity", "마커 제거 성공 (Layer.removeLabel 메서드)")
                        return
                    } catch (e3: NoSuchMethodException) {
                        // 방법 4: Label 객체의 delete() 메서드
                        try {
                            val deleteMethod = label.javaClass.getMethod("delete")
                            deleteMethod.invoke(label)
                            android.util.Log.d("MapActivity", "마커 제거 성공 (Label.delete 메서드)")
                            return
                        } catch (e4: NoSuchMethodException) {
                            android.util.Log.w("MapActivity", "마커 제거 메서드를 찾을 수 없습니다. 모든 가능한 메서드를 시도했습니다.")
                            android.util.Log.w("MapActivity", "Label 클래스 메서드: ${label.javaClass.methods.map { it.name }.take(10)}")
                            android.util.Log.w("MapActivity", "Layer 클래스 메서드: ${layer.javaClass.methods.map { it.name }.filter { it.contains("remove", ignoreCase = true) || it.contains("delete", ignoreCase = true) || it.contains("clear", ignoreCase = true) }}")
                        }
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("MapActivity", "마커 제거 실패: ${e.message}", e)
            e.printStackTrace()
        }
    }

    /**
     * 카테고리 이름에서 "음식점>" 제거
     * 예: "음식점>양식>이탈리안" -> "양식>이탈리안"
     */
    private fun parseCategoryName(categoryName: String?): String {
        if (categoryName.isNullOrBlank()) return "카테고리 정보 없음"
        
        return if (categoryName.startsWith("음식점 >")) {
            categoryName.substring(8) // "음식점>" 제거 (7자)
        } else {
            categoryName
        }
    }

    /**
     * 음식종류별로 그룹화하고 거리순으로 정렬
     */
    private fun sortAndGroupPlacesByFoodType() {
        // 음식종류별로 그룹화
        val grouped = searchResults.groupBy { it.foodType ?: "기타" }
        
        // 음식종류 순서 유지 (foodNames 순서대로)
        val sortedFoodTypes = foodNames + (grouped.keys - foodNames.toSet())
        
        // 거리순으로 정렬하고 음식종류별로 재구성
        val sortedResults = mutableListOf<Place>()
        sortedFoodTypes.forEach { foodType ->
            val places = grouped[foodType] ?: emptyList()
            // 거리순으로 정렬 (거리가 있는 것 우선, 그 다음 거리 가까운 순)
            val sorted = places.sortedWith(compareBy(
                { it.distance.isNullOrBlank() }, // 거리 없는 것 먼저
                { it.distance?.toDoubleOrNull() ?: Double.MAX_VALUE } // 거리 가까운 순
            ))
            sortedResults.addAll(sorted)
        }
        
        searchResults.clear()
        searchResults.addAll(sortedResults)
    }

    /**
     * 음식점 검색 (위치 기반, 10km 반경)
     */
    private fun searchRestaurants() {
        if (currentLatitude == null || currentLongitude == null) {
            Toast.makeText(this, "위치 정보가 없습니다.", Toast.LENGTH_SHORT).show()
            return
        }

        // 기존 마커 및 검색 결과 제거
        clearMarkers()
        searchResults.clear()
        placeAdapter.updatePlaces(searchResults)

        // 검색 시작
        showLoading(true)
        searchStatusText.text = "검색 중... (${foodNames.size}개 음식, 반경 10km)"

        var completedSearches = 0
        val totalSearches = foodNames.size

        // 각 음식별로 검색
        foodNames.forEach { foodName ->
            val searchQuery = "$foodName 음식점"
            searchNearbyRestaurants(
                searchQuery,
                foodName, // 음식 종류 전달
                currentLatitude!!,
                currentLongitude!!,
                onComplete = {
                    completedSearches++
                    if (completedSearches >= totalSearches) {
                        // 검색 완료 후 음식종류별 그룹화 및 거리순 정렬
                        sortAndGroupPlacesByFoodType()
                        showLoading(false)
                        searchStatusText.text = "검색 완료! (${searchResults.size}개 결과)"
                        placeAdapter.updatePlaces(searchResults)
                        searchStatusText.postDelayed({
                            searchStatusText.visibility = android.view.View.GONE
                        }, 3000)
                    }
                },
                onError = { errorMsg ->
                    completedSearches++
                    if (completedSearches >= totalSearches) {
                        sortAndGroupPlacesByFoodType()
                        showLoading(false)
                        placeAdapter.updatePlaces(searchResults)
                        Toast.makeText(this@MapActivity, errorMsg, Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }
    }

    /**
     * 음식점 검색 (위치 없이 전체 지역 검색)
     */
    private fun searchRestaurantsWithoutLocation() {
        // 기존 마커 및 검색 결과 제거
        clearMarkers()
        searchResults.clear()
        placeAdapter.updatePlaces(searchResults)

        // 검색 시작
        showLoading(true)
        searchStatusText.text = "검색 중... (${foodNames.size}개 음식)"

        var completedSearches = 0
        val totalSearches = foodNames.size

        // 각 음식별로 검색 (위치 정보 없이)
        foodNames.forEach { foodName ->
            val searchQuery = "$foodName 음식점"
            searchRestaurantsWithoutLocation(
                searchQuery,
                foodName, // 음식 종류 전달
                onComplete = {
                    completedSearches++
                    if (completedSearches >= totalSearches) {
                        // 검색 완료 후 음식종류별 그룹화 및 정렬
                        sortAndGroupPlacesByFoodType()
                        showLoading(false)
                        searchStatusText.text = "검색 완료! (${searchResults.size}개 결과)"
                        placeAdapter.updatePlaces(searchResults)
                        searchStatusText.postDelayed({
                            searchStatusText.visibility = android.view.View.GONE
                        }, 3000)
                    }
                },
                onError = { errorMsg ->
                    completedSearches++
                    if (completedSearches >= totalSearches) {
                        sortAndGroupPlacesByFoodType()
                        showLoading(false)
                        placeAdapter.updatePlaces(searchResults)
                        Toast.makeText(this@MapActivity, errorMsg, Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }
    }

    /**
     * Kakao Local API로 주변 음식점 검색 (10km 반경)
     */
    private fun searchNearbyRestaurants(
        query: String,
        foodType: String,
        latitude: Double,
        longitude: Double,
        onComplete: () -> Unit,
        onError: (String) -> Unit
    ) {
        apiService.searchPlace(
            apiKey = restApiKey,
            query = query,
            longitude = longitude.toString(),
            latitude = latitude.toString(),
            radius = SEARCH_RADIUS, // 10km
            page = 1,
            size = 15
        ).enqueue(object : Callback<KakaoSearchResponse> {
            override fun onResponse(
                call: Call<KakaoSearchResponse>,
                response: Response<KakaoSearchResponse>
            ) {
                if (response.isSuccessful) {
                    val places = response.body()?.documents ?: emptyList()
                    places.forEach { place ->
                        // 음식 종류를 Place에 추가
                        val placeWithFoodType = place.copy(foodType = foodType)
                        addMarker(placeWithFoodType)
                        // 중복 방지: 이미 같은 id가 있는지 확인
                        if (placeWithFoodType.id != null && !searchResults.any { it.id == placeWithFoodType.id }) {
                            searchResults.add(placeWithFoodType)
                        } else if (placeWithFoodType.id == null && !searchResults.any { 
                            it.place_name == placeWithFoodType.place_name && 
                            it.x == placeWithFoodType.x && 
                            it.y == placeWithFoodType.y 
                        }) {
                            searchResults.add(placeWithFoodType)
                        }
                    }
                    onComplete()
                } else {
                    val errorMsg = "검색 실패: ${response.code()} ${response.message()}"
                    if (response.code() == 401) {
                        onError("API 키가 유효하지 않습니다. local.properties의 KAKAO_REST_API_KEY를 확인해주세요.")
                    } else {
                        onError(errorMsg)
                    }
                }
            }

            override fun onFailure(call: Call<KakaoSearchResponse>, t: Throwable) {
                onError("네트워크 오류: ${t.message}")
            }
        })
    }

    /**
     * Kakao Local API로 음식점 검색 (위치 정보 없이)
     */
    private fun searchRestaurantsWithoutLocation(
        query: String,
        foodType: String,
        onComplete: () -> Unit,
        onError: (String) -> Unit
    ) {
        apiService.searchPlace(
            apiKey = restApiKey,
            query = query,
            longitude = null,
            latitude = null,
            radius = null,
            page = 1,
            size = 15
        ).enqueue(object : Callback<KakaoSearchResponse> {
            override fun onResponse(
                call: Call<KakaoSearchResponse>,
                response: Response<KakaoSearchResponse>
            ) {
                if (response.isSuccessful) {
                    val places = response.body()?.documents ?: emptyList()
                    places.forEach { place ->
                        // 음식 종류를 Place에 추가
                        val placeWithFoodType = place.copy(foodType = foodType)
                        addMarker(placeWithFoodType)
                        // 중복 방지: 이미 같은 id가 있는지 확인
                        if (placeWithFoodType.id != null && !searchResults.any { it.id == placeWithFoodType.id }) {
                            searchResults.add(placeWithFoodType)
                        } else if (placeWithFoodType.id == null && !searchResults.any { 
                            it.place_name == placeWithFoodType.place_name && 
                            it.x == placeWithFoodType.x && 
                            it.y == placeWithFoodType.y 
                        }) {
                            searchResults.add(placeWithFoodType)
                        }
                    }
                    
                    // 첫 번째 검색 결과로 지도 이동 (위치 정보가 없을 때)
                    if (places.isNotEmpty() && currentLatitude == null && ::kakaoMap.isInitialized) {
                        val firstPlace = places[0]
                        val lat = firstPlace.y.toDoubleOrNull()
                        val lng = firstPlace.x.toDoubleOrNull()
                        if (lat != null && lng != null) {
                            val cameraUpdate = CameraUpdateFactory.newCenterPosition(
                                LatLng.from(lat, lng)
                            )
                            kakaoMap.moveCamera(cameraUpdate)
                            val zoomUpdate = CameraUpdateFactory.zoomTo(13)
                            kakaoMap.moveCamera(zoomUpdate)
                        }
                    }
                    onComplete()
                } else {
                    val errorMsg = "검색 실패: ${response.code()} ${response.message()}"
                    if (response.code() == 401) {
                        onError("API 키가 유효하지 않습니다. local.properties의 KAKAO_REST_API_KEY를 확인해주세요.")
                    } else {
                        onError(errorMsg)
                    }
                }
            }

            override fun onFailure(call: Call<KakaoSearchResponse>, t: Throwable) {
                onError("네트워크 오류: ${t.message}")
            }
        })
    }

    /**
     * 지도에 마커 추가
     */
    private fun addMarker(place: Place) {
        // kakaoMap이 초기화되지 않았으면 마커 추가 불가
        if (!::kakaoMap.isInitialized) {
            android.util.Log.w("MapActivity", "kakaoMap이 초기화되지 않아 마커를 추가할 수 없습니다.")
            return
        }
        
        // Kakao API는 x=경도(longitude), y=위도(latitude)를 반환
        val lat = place.y.toDoubleOrNull() ?: run {
            android.util.Log.w("MapActivity", "위도 변환 실패: ${place.y}")
            return
        }
        val lng = place.x.toDoubleOrNull() ?: run {
            android.util.Log.w("MapActivity", "경도 변환 실패: ${place.x}")
            return
        }

        try {
            android.util.Log.d("MapActivity", "마커 추가 시도: ${place.place_name} at ($lat, $lng)")
            
            // 일반 마커 스타일이 없으면 초기화
            if (normalMarkerStyle == null) {
                android.util.Log.d("MapActivity", "마커 스타일이 null이므로 초기화합니다")
                initMarkerStyles(kakaoMap)
            }

            // 일반 마커 스타일 사용
            val styles = normalMarkerStyle ?: run {
                android.util.Log.e("MapActivity", "마커 스타일이 초기화되지 않았습니다.")
                return
            }

            val labelManager = kakaoMap.labelManager
            val layer = labelManager?.layer
            if (layer == null) {
                android.util.Log.e("MapActivity", "layer가 null입니다")
                return
            }

            val latLng = LatLng.from(lat, lng)
            val options = LabelOptions.from(latLng)
                .setStyles(styles)

            // 마커 추가
            android.util.Log.d("MapActivity", "마커 옵션 생성 완료: lat=$lat, lng=$lng")
            val label = layer.addLabel(options)
            if (label != null) {
                placeMarkers[label] = place
                val placeKey = place.id ?: "${place.place_name}_${place.x}_${place.y}"
                placeToLabelMap[placeKey] = label
                android.util.Log.d("MapActivity", "마커 추가 성공: ${place.place_name}, label=$label, 총 마커 수: ${placeMarkers.size}")
            } else {
                android.util.Log.e("MapActivity", "마커 추가 실패: label이 null입니다. place: ${place.place_name}, 좌표: ($lat, $lng)")
            }
        } catch (e: Exception) {
            // 마커 추가 실패 시 로그만 출력하고 계속 진행
            android.util.Log.e("MapActivity", "마커 추가 실패: ${e.message}", e)
            e.printStackTrace()
        }
    }

    /**
     * 모든 마커 제거 (음식점 마커만, 현재 위치 마커는 유지)
     */
    private fun clearMarkers() {
        if (!::kakaoMap.isInitialized) return
        
        try {
            val layer = kakaoMap.labelManager?.layer ?: return
            
            // 음식점 마커만 제거 (현재 위치 마커는 제외)
            val labelsToRemove = placeMarkers.keys.filter { it != myLocationLabel }.toList()
            labelsToRemove.forEach { label ->
                removeLabelFromLayer(label)
            }
            
            // 선택된 마커도 제거
            selectedLabel?.let { label ->
                if (label != myLocationLabel) {
                    removeLabelFromLayer(label)
                }
            }
            
            // 추적 정보 클리어 (현재 위치 마커 제외)
            val myLocationLabelToKeep = myLocationLabel
            placeMarkers.clear()
            placeToLabelMap.clear()
            selectedLabel = null
            
            // 현재 위치 마커는 유지 (더미 Place 객체는 생성하지 않음, 마커만 유지)
            myLocationLabel = myLocationLabelToKeep
        } catch (e: Exception) {
            android.util.Log.e("MapActivity", "마커 제거 실패: ${e.message}", e)
            // 오류 발생 시 추적 정보만 클리어
            placeMarkers.clear()
            placeToLabelMap.clear()
            selectedLabel = null
        }
    }


    /**
     * 카카오맵에서 상세 정보 보기 (리뷰, 사진 등 확인 가능)
     */
    private fun openKakaoMapDetail(place: Place) {
        // place_url이 있으면 사용
        if (!place.place_url.isNullOrBlank()) {
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(place.place_url))
                startActivity(intent)
                return
            } catch (e: Exception) {
                android.util.Log.e("MapActivity", "카카오맵 URL 열기 실패: ${e.message}")
            }
        }

        // place_url이 없거나 실패하면 좌표 기반으로 카카오맵 열기
        val lat = place.y.toDoubleOrNull()
        val lng = place.x.toDoubleOrNull()
        if (lat != null && lng != null) {
            // 카카오맵 앱으로 장소 검색하여 열기
            val kakaoMapUri = "kakaomap://search?q=${Uri.encode(place.place_name)}"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(kakaoMapUri))
            
            if (intent.resolveActivity(packageManager) != null) {
                startActivity(intent)
            } else {
                // 카카오맵 앱이 없으면 웹 브라우저로 열기
                val webUrl = "https://map.kakao.com/link/search/${Uri.encode(place.place_name)}"
                val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse(webUrl))
                startActivity(webIntent)
            }
        } else {
            Toast.makeText(this, "위치 정보가 없어 카카오맵을 열 수 없습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * 길찾기 앱 열기
     */
    private fun openNavigation(latitude: Double, longitude: Double, placeName: String) {
        // 카카오맵 앱으로 길찾기
        val kakaoMapUri = "kakaomap://route?ep=$latitude,$longitude&by=CAR"
        val kakaoIntent = Intent(Intent.ACTION_VIEW, Uri.parse(kakaoMapUri))
        
        if (kakaoIntent.resolveActivity(packageManager) != null) {
            startActivity(kakaoIntent)
            return
        }
        
        // 카카오맵이 없으면 네이버 지도 앱 시도
        val naverMapUri = "nmap://route/car?dlat=$latitude&dlng=$longitude&dname=${Uri.encode(placeName)}"
        val naverIntent = Intent(Intent.ACTION_VIEW, Uri.parse(naverMapUri))
        
        if (naverIntent.resolveActivity(packageManager) != null) {
            startActivity(naverIntent)
            return
        }
        
        // 길찾기 앱이 없으면 웹 브라우저로 카카오맵 길찾기 페이지 열기
        try {
            val webUrl = "https://map.kakao.com/link/to/${Uri.encode(placeName)},$latitude,$longitude"
            val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse(webUrl))
            startActivity(webIntent)
        } catch (e: Exception) {
            android.util.Log.e("MapActivity", "웹 브라우저로 길찾기 열기 실패: ${e.message}")
            Toast.makeText(this, "길찾기를 열 수 없습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * 로딩 상태 표시/숨기기
     */
    private fun showLoading(show: Boolean) {
        if (show) {
            progressBar.visibility = android.view.View.VISIBLE
            searchStatusText.visibility = android.view.View.VISIBLE
        } else {
            progressBar.visibility = android.view.View.GONE
        }
    }
}

/**
 * Sticky Header ItemDecoration - 선택된 음식 종류가 상단에 고정되도록 함
 */
class StickyHeaderItemDecoration(private val adapter: PlaceAdapter) : RecyclerView.ItemDecoration() {
    
    private var cachedHeaderView: View? = null
    private var cachedHeaderPosition = -1
    private var headerAlpha: Float = 1f
    
    override fun getItemOffsets(
        outRect: android.graphics.Rect,
        view: android.view.View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        val position = parent.getChildAdapterPosition(view)
        if (position != RecyclerView.NO_POSITION && position < adapter.items.size) {
            // 헤더 아이템에 대해서만 RecyclerView의 paddingStart만큼 음수 마진을 주어 x=0부터 시작하도록 함
            if (adapter.items[position] is PlaceAdapter.AdapterItem.Header) {
                outRect.left = -parent.paddingStart
                outRect.right = -parent.paddingEnd
            }
        }
    }
    
    override fun onDrawOver(c: android.graphics.Canvas, parent: RecyclerView, state: RecyclerView.State) {
        val layoutManager = parent.layoutManager as? LinearLayoutManager ?: return
        
        // 현재 화면에 보이는 첫 번째 아이템
        val firstVisiblePosition = layoutManager.findFirstVisibleItemPosition()
        if (firstVisiblePosition == RecyclerView.NO_POSITION) return
        
        // 현재 화면에 보이는 첫 번째 헤더 찾기
        var currentHeaderPosition = -1
        for (i in firstVisiblePosition downTo 0) {
            if (i < adapter.items.size && adapter.items[i] is PlaceAdapter.AdapterItem.Header) {
                currentHeaderPosition = i
                break
            }
        }
        
        // 첫 번째 헤더가 없으면 선택된 항목의 헤더를 사용 (선택된 항목이 있는 경우)
        if (currentHeaderPosition < 0 && adapter.selectedIndex >= 0 && adapter.selectedIndex < adapter.places.size) {
            val selectedPlace = adapter.places[adapter.selectedIndex]
            val selectedFoodType = selectedPlace.foodType
            if (selectedFoodType != null) {
                // 선택된 음식 종류의 헤더 위치 찾기
                for (i in adapter.items.indices) {
                    when (val item = adapter.items[i]) {
                        is PlaceAdapter.AdapterItem.Header -> {
                            if (item.foodType == selectedFoodType) {
                                currentHeaderPosition = i
                                break
                            }
                        }
                        is PlaceAdapter.AdapterItem.PlaceItem -> {
                            if (item.index == adapter.selectedIndex) {
                                // 선택된 Place가 속한 헤더를 찾기 위해 위로 거슬러 올라감
                                for (j in i downTo 0) {
                                    if (adapter.items[j] is PlaceAdapter.AdapterItem.Header) {
                                        currentHeaderPosition = j
                                        break
                                    }
                                }
                                break
                            }
                        }
                    }
                    if (currentHeaderPosition >= 0) break
                }
            }
        }
        
        val headerPosition = currentHeaderPosition
        
        // 실제 헤더 뷰가 화면에 있는지 확인 (스크롤 전)
        val actualHeaderView = layoutManager.findViewByPosition(headerPosition)
        // RecyclerView의 padding을 고려하지 않고 실제 헤더 위치 사용 (RecyclerView 내부 좌표계)
        val headerTop = actualHeaderView?.top ?: Int.MAX_VALUE
        
        // 헤더가 화면 상단에 고정되어야 하는지 확인 (헤더가 스크롤되어 위로 사라질 때)
        // 실제 헤더가 paddingTop(0dp) 영역을 벗어나면 sticky header 표시
        val shouldShowStickyHeader = headerPosition >= 0 && (headerTop < 0 || firstVisiblePosition > headerPosition)
        
        if (shouldShowStickyHeader) {
            // 헤더가 화면 상단에 고정되어야 함
            var headerView = cachedHeaderView
            
            // 캐시된 헤더가 없거나 다른 헤더인 경우 새로 생성
            val isNewHeader = headerView == null || cachedHeaderPosition != headerPosition
            if (isNewHeader) {
                val headerHolder = adapter.createViewHolder(parent, PlaceAdapter.VIEW_TYPE_HEADER)
                adapter.onBindViewHolder(headerHolder, headerPosition)
                headerView = headerHolder.itemView
                
                // 헤더 크기 측정 및 레이아웃 (좌우로 꽉 차도록)
                val widthSpec = android.view.View.MeasureSpec.makeMeasureSpec(
                    parent.width, 
                    android.view.View.MeasureSpec.EXACTLY
                )
                val heightSpec = android.view.View.MeasureSpec.makeMeasureSpec(
                    0, android.view.View.MeasureSpec.UNSPECIFIED
                )
                headerView.measure(widthSpec, heightSpec)
                headerView.layout(0, 0, headerView.measuredWidth, headerView.measuredHeight)
                
                cachedHeaderView = headerView
                cachedHeaderPosition = headerPosition
                headerAlpha = 1f
            }
            
            headerView?.let { view ->
                // 다음 헤더가 올라오면서 스택 효과 적용
                var nextHeaderPosition = -1
                for (i in (headerPosition + 1) until adapter.items.size) {
                    if (adapter.items[i] is PlaceAdapter.AdapterItem.Header) {
                        nextHeaderPosition = i
                        break
                    }
                }
                
                var topOffset = 0
                if (nextHeaderPosition > 0 && firstVisiblePosition >= nextHeaderPosition) {
                    val nextHeaderView = layoutManager.findViewByPosition(nextHeaderPosition)
                    if (nextHeaderView != null) {
                        // paddingTop이 0이므로 nextHeaderView.top을 그대로 사용
                        // 다음 헤더가 올라오고 있으면 스택 효과로 현재 헤더를 위로 밀어냄
                        if (nextHeaderView.top < view.height) {
                            topOffset = nextHeaderView.top - view.height
                        }
                    }
                }
                
                // 헤더를 상단에 그리기 (실제 헤더와 같은 위치, x=0부터 시작)
                c.save()
                val xOffset = 0f // getItemOffsets로 padding을 상쇄했으므로 x=0부터 시작
                val yOffset = topOffset.toFloat()
                c.translate(xOffset, yOffset)
                parent.drawChild(c, view, parent.drawingTime)
                c.restore()
            }
        } else {
            // 실제 헤더가 화면에 보이면 sticky header는 표시하지 않음
            cachedHeaderView = null
            cachedHeaderPosition = -1
            headerAlpha = 0f
        }
    }
}

/**
 * 검색 결과를 표시하는 RecyclerView 어댑터 (음식종류별 헤더 포함)
 */
class PlaceAdapter(
    private var _places: List<Place>,
    selectedIndexParam: Int = -1,
    private val onItemClick: (Place, Int) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        const val VIEW_TYPE_HEADER = 0
        const val VIEW_TYPE_PLACE = 1
    }

    // 아이템 리스트 구성: 헤더와 Place 아이템을 순서대로 배치 (ItemTouchHelper에서 접근 필요)
    val items = mutableListOf<AdapterItem>()
    
    // Place 리스트에 직접 접근 가능하도록 (ItemDecoration에서 사용)
    val places: List<Place>
        get() = _places
    
    // 선택 상태 업데이트 (생성자 파라미터로 초기화)
    var selectedIndex: Int = selectedIndexParam
        private set
    
    init {
        buildItemsList()
    }
    
    private fun buildItemsList() {
        items.clear()
        if (_places.isEmpty()) return
        
        // 첫 번째 음식종류의 헤더 추가
        items.add(AdapterItem.Header(_places[0].foodType ?: "기타"))
        items.add(AdapterItem.PlaceItem(0))
        
        // 나머지 Place 아이템 추가 (음식종류가 바뀔 때마다 헤더 추가)
        for (i in 1 until _places.size) {
            if (_places[i].foodType != _places[i - 1].foodType) {
                items.add(AdapterItem.Header(_places[i].foodType ?: "기타"))
            }
            items.add(AdapterItem.PlaceItem(i))
        }
    }
    
    // 데이터 업데이트
    fun updatePlaces(newPlaces: List<Place>) {
        _places = newPlaces
        buildItemsList()
        notifyDataSetChanged()
    }
    
    // 선택 상태 업데이트
    fun updateSelectedIndex(newSelectedIndex: Int) {
        val oldIndex = selectedIndex
        selectedIndex = newSelectedIndex
        
        // 모든 Place 아이템의 위치를 찾아서 업데이트 (실시간 반영)
        val positionsToUpdate = mutableSetOf<Int>()
        
        // 이전 선택 항목과 새 선택 항목 모두 업데이트
        if (oldIndex >= 0 && oldIndex < _places.size) {
            val oldAdapterPosition = getAdapterPositionForPlace(oldIndex)
            if (oldAdapterPosition >= 0) {
                positionsToUpdate.add(oldAdapterPosition)
            }
        }
        
        if (selectedIndex >= 0 && selectedIndex < _places.size) {
            val newAdapterPosition = getAdapterPositionForPlace(selectedIndex)
            if (newAdapterPosition >= 0) {
                positionsToUpdate.add(newAdapterPosition)
            }
        }
        
        // 선택 상태가 변경되면 모든 Place 아이템의 색상이 변경되어야 함
        // 모든 Place 아이템 위치 추가
        items.forEachIndexed { index, item ->
            if (item is AdapterItem.PlaceItem) {
                positionsToUpdate.add(index)
            }
        }
        
        // 모든 변경된 위치 업데이트
        positionsToUpdate.forEach { position ->
            notifyItemChanged(position)
        }
    }

    sealed class AdapterItem {
        data class Header(val foodType: String) : AdapterItem()
        data class PlaceItem(val index: Int) : AdapterItem()
    }

    inner class HeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val foodTypeText: TextView = itemView.findViewById(R.id.foodTypeText)
    }

    inner class PlaceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val placeNameText: TextView = itemView.findViewById(R.id.placeNameText)
        val categoryText: TextView = itemView.findViewById(R.id.categoryText)
        val addressText: TextView = itemView.findViewById(R.id.addressText)
        val phoneText: TextView = itemView.findViewById(R.id.phoneText)
        val distanceText: TextView = itemView.findViewById(R.id.distanceText)
        val itemCard: androidx.cardview.widget.CardView = itemView.findViewById(R.id.itemCard)
        val itemContent: LinearLayout = itemView.findViewById(R.id.itemContent)
        
        init {
            itemView.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION && position < items.size) {
                    when (val item = items[position]) {
                        is AdapterItem.PlaceItem -> {
                            if (item.index < _places.size) {
                                onItemClick(_places[item.index], item.index)
                            }
                        }
                        else -> {}
                    }
                }
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is AdapterItem.Header -> VIEW_TYPE_HEADER
            is AdapterItem.PlaceItem -> VIEW_TYPE_PLACE
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_HEADER -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_food_type_header, parent, false)
                HeaderViewHolder(view)
            }
            else -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_place, parent, false)
                PlaceViewHolder(view)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        onBindViewHolder(holder, position, mutableListOf<Any>())
    }
    
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int, payloads: MutableList<Any>) {
        when (holder) {
            is HeaderViewHolder -> {
                when (val item = items[position]) {
                    is AdapterItem.Header -> {
                        holder.foodTypeText.text = "🍽️ ${item.foodType}"
                    }
                    else -> {}
                }
            }
            is PlaceViewHolder -> {
                when (val item = items[position]) {
                    is AdapterItem.PlaceItem -> {
                        if (item.index < _places.size) {
                            val place = _places[item.index]
                            val isSelected = item.index == selectedIndex
                            
                            // 선택 상태에 따라 배경색 변경 (실시간 반영)
                            if (selectedIndex >= 0) {
                                if (isSelected) {
                                    // 선택된 항목: 흰색 배경
                                    holder.itemCard.setCardBackgroundColor(
                                        ContextCompat.getColor(holder.itemView.context, android.R.color.white)
                                    )
                                    holder.itemContent.setBackgroundColor(
                                        ContextCompat.getColor(holder.itemView.context, android.R.color.white)
                                    )
                                } else {
                                    // 비선택 항목: 회색 배경
                                    holder.itemCard.setCardBackgroundColor(
                                        ContextCompat.getColor(holder.itemView.context, android.R.color.darker_gray)
                                    )
                                    holder.itemContent.setBackgroundColor(
                                        ContextCompat.getColor(holder.itemView.context, android.R.color.darker_gray)
                                    )
                                }
                            } else {
                                // 선택 상태가 아니면 모두 흰색 (원래 색상)
                                holder.itemCard.setCardBackgroundColor(
                                    ContextCompat.getColor(holder.itemView.context, android.R.color.white)
                                )
                                holder.itemContent.setBackgroundColor(
                                    ContextCompat.getColor(holder.itemView.context, android.R.color.white)
                                )
                            }
                            
                            // 전체 데이터 업데이트
                            holder.placeNameText.text = place.place_name
                            
                            // 카테고리 이름 파싱 ("음식점>" 제거)
                            val categoryName = place.category_name ?: "카테고리 정보 없음"
                            val parsedCategory = if (categoryName.startsWith("음식점>")) {
                                categoryName.substring(7) // "음식점>" 제거
                            } else {
                                categoryName
                            }
                            holder.categoryText.text = parsedCategory
                            
                            // 주소 표시 (도로명 주소 우선, 없으면 지번 주소)
                            val address = place.road_address_name ?: place.address_name ?: "주소 정보 없음"
                            holder.addressText.text = address
                            
                            // 전화번호 표시
                            holder.phoneText.text = place.phone ?: "전화번호 없음"
                            
                            // 거리 표시 (미터 단위를 km로 변환)
                            if (!place.distance.isNullOrBlank()) {
                                val distanceMeter = place.distance.toDoubleOrNull()
                                if (distanceMeter != null) {
                                    val distanceKm = distanceMeter / 1000.0
                                    holder.distanceText.text = if (distanceKm < 1.0) {
                                        "${distanceMeter.toInt()}m"
                                    } else {
                                        String.format("%.1fkm", distanceKm)
                                    }
                                } else {
                                    holder.distanceText.text = ""
                                }
                            } else {
                                holder.distanceText.text = ""
                            }
                        }
                    }
                    else -> {}
                }
            }
        }
    }

    override fun getItemCount(): Int = items.size
    
    // Place 인덱스를 어댑터 position으로 변환 (스크롤용)
    fun getAdapterPositionForPlace(placeIndex: Int): Int {
        return items.indexOfFirst { 
            it is AdapterItem.PlaceItem && (it as AdapterItem.PlaceItem).index == placeIndex
        }
    }
}