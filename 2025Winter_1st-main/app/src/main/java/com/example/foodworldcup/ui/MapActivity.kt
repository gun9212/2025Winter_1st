package com.example.foodworldcup.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.createBitmap
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.foodworldcup.R
import com.example.foodworldcup.api.*
import com.example.foodworldcup.ui.adapter.*
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.kakao.vectormap.*
import com.kakao.vectormap.camera.CameraUpdateFactory
import com.kakao.vectormap.label.*

// Data models and API interface moved to com.example.foodworldcup.api package

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

    // API Helper
    private val mapApiHelper = MapApiHelper()

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
        setupBottomNavigation(Screen.MAP)

        // Intent에서 음식 이름 리스트 받기
        foodNames = intent.getStringArrayListExtra("food_names") ?: emptyList()

        // 합격된 음식 ID 리스트가 전달된 경우 (ResultActivity에서)
        val passedFoodIds = intent.getIntegerArrayListExtra("passed_food_ids")
        if (passedFoodIds != null && passedFoodIds.isNotEmpty()) {
            // FoodRepository에서 음식 이름 가져오기
            foodNames =
                    passedFoodIds.mapNotNull { id ->
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
        placeAdapter =
                PlaceAdapter(searchResults, -1) { place, position ->
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
        placeRecyclerView.addOnScrollListener(
                object : RecyclerView.OnScrollListener() {
                    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                        super.onScrolled(recyclerView, dx, dy)
                        // 스크롤 시 ItemDecoration 재그리기
                        recyclerView.invalidateItemDecorations()
                    }
                }
        )

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

    /** 카카오맵 초기화 참고: https://apis.map.kakao.com/android_v2/docs/getting-started/quickstart/ */
    private fun initMap() {
        mapView.start(
                object : MapLifeCycleCallback() {
                    override fun onMapDestroy() {
                        // 지도가 파괴될 때 처리
                    }

                    override fun onMapError(error: Exception) {
                        Toast.makeText(
                                        this@MapActivity,
                                        "지도를 불러오는데 실패했습니다: ${error.message}",
                                        Toast.LENGTH_SHORT
                                )
                                .show()
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
                                val index =
                                        searchResults.indexOfFirst {
                                            (it.id != null && it.id == place.id) ||
                                                    (it.id == null &&
                                                            it.place_name == place.place_name &&
                                                            it.x == place.x &&
                                                            it.y == place.y)
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

    // 마커 스타일 캐시 (Key: "foodType_selected" 또는 "foodType_normal")
    private val markerStylesCache = mutableMapOf<String, LabelStyles>()

    /** 마커 스타일 가져오기 (캐시 사용) */
    private fun getMarkerStyles(
            kakaoMap: KakaoMap,
            foodType: String?,
            isSelected: Boolean
    ): LabelStyles? {
        val key = "${foodType ?: "default"}_${if (isSelected) "selected" else "normal"}"

        // 캐시에 있으면 반환
        if (markerStylesCache.containsKey(key)) {
            return markerStylesCache[key]
        }

        // 캐시에 없으면 생성
        val labelManager = kakaoMap.labelManager ?: return null

        val context = this
        val iconResId =
                if (isSelected) R.drawable.ic_marker_selected else R.drawable.ic_marker_restaurant

        // 비트맵 생성 (아이콘 + 음식 캐릭터)
        val bitmap =
                createCombinedMarkerBitmap(context, iconResId, foodType, isSelected) ?: return null

        // LabelStyle 생성
        val style = LabelStyle.from(bitmap)

        // LabelStyles 생성 및 등록
        val styles = LabelStyles.from(style)
        val addedStyles = labelManager.addLabelStyles(styles)

        if (addedStyles != null) {
            markerStylesCache[key] = addedStyles
            return addedStyles
        }

        return null
    }

    /** 마커 비트맵 생성 (기본 아이콘 + 음식 캐릭터 합성) */
    private fun createCombinedMarkerBitmap(
            context: android.content.Context,
            iconResId: Int,
            foodType: String?,
            isSelected: Boolean
    ): android.graphics.Bitmap? {
        try {
            // 1. 기본 마커 Drawable 로드
            val drawable = ContextCompat.getDrawable(context, iconResId) ?: return null

            // 크기 설정 (기존 로직 유지: 50% 축소)
            val density = context.resources.displayMetrics.density
            val baseSizeDp = if (isSelected) 32 else 24 // selected: 64->32, normal: 48->24
            val width = (baseSizeDp * density).toInt()
            val height = (baseSizeDp * density).toInt()

            // 2. 비트맵 생성
            val markerBitmap = createBitmap(width, height)
            val canvas = android.graphics.Canvas(markerBitmap)

            // 3. 음식 캐릭터 이미지 로드 및 그리기 시도
            var characterDrawn = false
            if (foodType != null) {
                val assetPath = findAssetPath(foodType)
                if (assetPath != null) {
                    val assetStream = context.assets.open(assetPath)
                    val characterBitmap = android.graphics.BitmapFactory.decodeStream(assetStream)
                    assetStream.close()

                    if (characterBitmap != null) {
                        characterDrawn = true

                        // ⭐ [핵심 수정] 이미지의 실질적 내용(누끼 부분)의 크기를 구함
                        val contentBounds = getContentBounds(characterBitmap)
                        val contentWidth = contentBounds.width()
                        val contentHeight = contentBounds.height()

                        if (isSelected) {
                            // 선택된 경우: 파란색 핀 배경 + 그 위에 캐릭터
                            drawable.setBounds(0, 0, width, height)
                            drawable.draw(canvas)

                            // 타겟 영역: 핀 머리 부분 (상단 70% 영역)
                            val targetAreaWidth = (width * 0.7f)
                            val targetAreaHeight = (width * 0.7f) // 정사각형 가정

                            // 스케일 계산 (실질적 크기 기준)
                            val scale =
                                    Math.min(
                                            targetAreaWidth / contentWidth,
                                            targetAreaHeight / contentHeight
                                    )

                            // 매트릭스 설정: 이동 -> 스케일 -> 중앙 배치
                            val matrix = android.graphics.Matrix()

                            // 1. 내용의 좌상단을 (0,0)으로 이동
                            matrix.postTranslate(
                                    -contentBounds.left.toFloat(),
                                    -contentBounds.top.toFloat()
                            )

                            // 2. 스케일 적용
                            matrix.postScale(scale, scale)

                            // 3. 타겟 영역의 중앙으로 이동
                            // 타겟 영역의 중심 좌표
                            val targetCenterX = width / 2f
                            // 핀의 머리 부분 중앙 (대략 상단 10% + 핀머리/2)
                            val headerTopOffset = height * 0.1f
                            val targetCenterY = headerTopOffset + targetAreaHeight / 2f

                            // 현재 비트맵(내용)의 중심이 (0,0)이 아니므로, 스케일된 내용의 절반 크기만큼 빼줌
                            val tx = targetCenterX - (contentWidth * scale / 2f)
                            val ty = targetCenterY - (contentHeight * scale / 2f)

                            matrix.postTranslate(tx, ty)

                            // 보간법 적용을 위해 Paint 사용 (ANTI_ALIAS)
                            val paint = android.graphics.Paint()
                            paint.isAntiAlias = true
                            paint.isFilterBitmap = true

                            canvas.drawBitmap(characterBitmap, matrix, paint)
                        } else {
                            // 선택 안된 경우: 핀 배경 없이 음식 캐릭터만
                            // 타겟 영역: 마커 전체의 90%
                            val targetAreaWidth = (width * 0.9f)
                            val targetAreaHeight = (height * 0.9f)

                            // 스케일 계산
                            val scale =
                                    Math.min(
                                            targetAreaWidth / contentWidth,
                                            targetAreaHeight / contentHeight
                                    )

                            val matrix = android.graphics.Matrix()
                            matrix.postTranslate(
                                    -contentBounds.left.toFloat(),
                                    -contentBounds.top.toFloat()
                            )
                            matrix.postScale(scale, scale)

                            // 중앙 정렬
                            val targetCenterX = width / 2f
                            val targetCenterY = height / 2f

                            val tx = targetCenterX - (contentWidth * scale / 2f)
                            val ty = targetCenterY - (contentHeight * scale / 2f)

                            matrix.postTranslate(tx, ty)

                            val paint = android.graphics.Paint()
                            paint.isAntiAlias = true
                            paint.isFilterBitmap = true

                            canvas.drawBitmap(characterBitmap, matrix, paint)
                        }
                    }
                }
            }

            // 4. 캐릭터를 그리지 못했으면 핀(기본 마커) 그리기 - fallback
            if (!characterDrawn) {
                drawable.setBounds(0, 0, width, height)
                drawable.draw(canvas)
            }

            return markerBitmap
        } catch (e: Exception) {
            android.util.Log.e("MapActivity", "비트맵 생성 실패: ${e.message}")
            return null
        }
    }

    /** 비트맵에서 투명하지 않은 실질적 영역 구하기 */
    private fun getContentBounds(bitmap: android.graphics.Bitmap): android.graphics.Rect {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        var minX = width
        var maxX = -1
        var minY = height
        var maxY = -1

        for (y in 0 until height) {
            for (x in 0 until width) {
                // Alpha 값 확인 (MSB 8bit)
                val alpha = (pixels[y * width + x] shr 24) and 0xFF
                if (alpha > 50) { // 약간의 투명도는 무시 (노이즈 방지)
                    if (x < minX) minX = x
                    if (x > maxX) maxX = x
                    if (y < minY) minY = y
                    if (y > maxY) maxY = y
                }
            }
        }

        if (maxX < minX || maxY < minY) {
            // 내용이 없으면 전체 반환
            return android.graphics.Rect(0, 0, width, height)
        }

        return android.graphics.Rect(minX, minY, maxX + 1, maxY + 1)
    }

    /** Assets에서 음식 종류에 맞는 이미지 경로 찾기 */
    private fun findAssetPath(foodType: String): String? {
        val categories = listOf("아시안", "양식", "일식", "중식", "한식")
        val fileName = "${foodType}_캐릭터누끼.png"

        for (category in categories) {
            val path = "food_character_images/$category/$fileName"
            try {
                // 파일 존재 여부 확인을 위해 open 시도
                val stream = assets.open(path)
                stream.close()
                return path
            } catch (e: java.io.IOException) {
                // 파일이 없으면 다음 카테고리 확인
                continue
            }
        }
        return null
    }

    /** 마커 스타일 초기화 (일반 및 선택된 마커) */
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
                    val bitmap = createBitmap(width, height)
                    val canvas = android.graphics.Canvas(bitmap)
                    drawable.setBounds(0, 0, width, height)
                    drawable.draw(canvas)

                    android.util.Log.d("MapActivity", "일반 마커 비트맵 생성 완료: ${width}x${height}")

                    val normalStyle = LabelStyle.from(bitmap)
                    if (normalStyle != null) {
                        val styles = LabelStyles.from(normalStyle)
                        normalMarkerStyle = labelManager.addLabelStyles(styles)
                        android.util.Log.d(
                                "MapActivity",
                                "일반 마커 스타일 추가 완료: ${normalMarkerStyle != null}"
                        )
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
                    val bitmap = createBitmap(width, height)
                    val canvas = android.graphics.Canvas(bitmap)
                    drawable.setBounds(0, 0, width, height)
                    drawable.draw(canvas)

                    android.util.Log.d("MapActivity", "선택된 마커 비트맵 생성 완료: ${width}x${height}")

                    val selectedStyle = LabelStyle.from(bitmap)
                    if (selectedStyle != null) {
                        val styles = LabelStyles.from(selectedStyle)
                        selectedMarkerStyle = labelManager.addLabelStyles(styles)
                        android.util.Log.d(
                                "MapActivity",
                                "선택된 마커 스타일 추가 완료: ${selectedMarkerStyle != null}"
                        )
                    } else {
                        android.util.Log.e(
                                "MapActivity",
                                "LabelStyle.from()이 null을 반환했습니다 (선택된 마커)"
                        )
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

                    val bitmap = createBitmap(width, height)
                    val canvas = android.graphics.Canvas(bitmap)
                    drawable.setBounds(0, 0, width, height)
                    drawable.draw(canvas)

                    android.util.Log.d("MapActivity", "현재 위치 마커 비트맵 생성 완료: ${width}x${height}")

                    val locationStyle = LabelStyle.from(bitmap)
                    if (locationStyle != null) {
                        val styles = LabelStyles.from(locationStyle)
                        myLocationMarkerStyle = labelManager.addLabelStyles(styles)
                        android.util.Log.d(
                                "MapActivity",
                                "현재 위치 마커 스타일 추가 완료: ${myLocationMarkerStyle != null}"
                        )
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

    /** 음식점 선택 (지도에서 클릭 시 - 마커 클릭) */
    private fun selectPlace(place: Place) {
        // 1. 선택된 마커로 변경
        updateSelectedMarker(place)

        // 2. 지도 중심 이동
        moveToPlace(place)

        // 3. 리스트에서 해당 항목으로 스크롤
        movePlaceToTop(place)

        // 4. 리스트에서 선택 상태 업데이트
        val index =
                searchResults.indexOfFirst {
                    (it.id != null && it.id == place.id) ||
                            (it.id == null &&
                                    it.place_name == place.place_name &&
                                    it.x == place.x &&
                                    it.y == place.y)
                }
        if (index >= 0) {
            handlePlaceItemClick(place, index)
        }
    }

    /** 리스트 아이템 클릭 처리 (선택 상태 관리) */
    private fun handlePlaceItemClick(place: Place, position: Int) {
        // 같은 아이템을 두 번 클릭하면 선택 해제
        if (selectedPlaceIndex == position) {
            selectedPlaceIndex = -1
            confirmButton.visibility = View.GONE
            // 선택 해제 시 모든 항목 원래 색으로 복원
        } else {
            // 다른 아이템 선택
            selectedPlaceIndex = position
            confirmButton.visibility = View.VISIBLE

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

    /** 현재 화면에 보이는 모든 아이템 뷰를 직접 업데이트 (실시간 색상 변경) */
    private fun updateVisibleItemViews() {
        val layoutManager = placeRecyclerView.layoutManager as? LinearLayoutManager ?: return

        val firstVisible = layoutManager.findFirstVisibleItemPosition()
        val lastVisible = layoutManager.findLastVisibleItemPosition()

        if (firstVisible == RecyclerView.NO_POSITION || lastVisible == RecyclerView.NO_POSITION)
                return

        // 모든 visible 아이템 업데이트
        for (i in firstVisible..lastVisible) {
            val viewHolder = placeRecyclerView.findViewHolderForAdapterPosition(i)
            if (viewHolder is PlaceAdapter.PlaceViewHolder) {
                val item = placeAdapter.items.getOrNull(i)
                if (item is PlaceAdapter.AdapterItem.PlaceItem &&
                                item.index < placeAdapter.places.size
                ) {
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

    /** 스와이프 제스처 설정 */
    private fun setupSwipeGesture() {
        val itemTouchHelper =
                ItemTouchHelper(
                        object :
                                ItemTouchHelper.SimpleCallback(
                                        0,
                                        ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
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

                            override fun onSwiped(
                                    viewHolder: RecyclerView.ViewHolder,
                                    direction: Int
                            ) {
                                val position = viewHolder.bindingAdapterPosition
                                if (position == RecyclerView.NO_POSITION ||
                                                position >= placeAdapter.items.size
                                )
                                        return

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
                                if (position == RecyclerView.NO_POSITION ||
                                                position >= placeAdapter.items.size
                                ) {
                                    super.onChildDraw(
                                            c,
                                            recyclerView,
                                            viewHolder,
                                            dX,
                                            dY,
                                            actionState,
                                            isCurrentlyActive
                                    )
                                    return
                                }

                                // 헤더 아이템은 스와이프하지 않음
                                if (placeAdapter.items[position] is PlaceAdapter.AdapterItem.Header
                                ) {
                                    super.onChildDraw(
                                            c,
                                            recyclerView,
                                            viewHolder,
                                            dX,
                                            dY,
                                            actionState,
                                            isCurrentlyActive
                                    )
                                    return
                                }

                                if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
                                    val itemView = viewHolder.itemView
                                    val leftSwipeBackground =
                                            itemView.findViewById<
                                                    androidx.cardview.widget.CardView>(
                                                    R.id.leftSwipeBackground
                                            )
                                    val rightSwipeBackground =
                                            itemView.findViewById<
                                                    androidx.cardview.widget.CardView>(
                                                    R.id.rightSwipeBackground
                                            )
                                    val itemCard =
                                            itemView.findViewById<
                                                    androidx.cardview.widget.CardView>(
                                                    R.id.itemCard
                                            )

                                    if (leftSwipeBackground != null &&
                                                    rightSwipeBackground != null &&
                                                    itemCard != null
                                    ) {
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
                                            rightSwipeBackground.visibility = View.VISIBLE
                                            leftSwipeBackground.visibility = View.GONE
                                        } else if (dX < 0) {
                                            // 왼쪽 스와이프 (상세정보) - 아이템이 왼쪽으로 이동하면 오른쪽에 배경 표시, 오른쪽에 글씨
                                            leftSwipeBackground.visibility = View.VISIBLE
                                            rightSwipeBackground.visibility = View.GONE
                                        } else {
                                            leftSwipeBackground.visibility = View.GONE
                                            rightSwipeBackground.visibility = View.GONE
                                        }

                                        // ItemTouchHelper의 기본 그리기를 dX=0으로 호출하여 itemView가 이동하지 않도록 함
                                        // itemCard는 이미 translationX로 직접 이동시켰으므로 배경만 고정됨
                                        super.onChildDraw(
                                                c,
                                                recyclerView,
                                                viewHolder,
                                                0f,
                                                dY,
                                                actionState,
                                                isCurrentlyActive
                                        )
                                    } else {
                                        // 배경이나 itemCard를 찾을 수 없으면 기본 동작 사용
                                        super.onChildDraw(
                                                c,
                                                recyclerView,
                                                viewHolder,
                                                dX,
                                                dY,
                                                actionState,
                                                isCurrentlyActive
                                        )
                                    }
                                } else {
                                    // 스와이프가 아닐 때는 배경 숨김 및 위치 초기화
                                    val leftSwipeBackground =
                                            viewHolder.itemView.findViewById<
                                                    androidx.cardview.widget.CardView>(
                                                    R.id.leftSwipeBackground
                                            )
                                    val rightSwipeBackground =
                                            viewHolder.itemView.findViewById<
                                                    androidx.cardview.widget.CardView>(
                                                    R.id.rightSwipeBackground
                                            )
                                    val itemCard =
                                            viewHolder.itemView.findViewById<
                                                    androidx.cardview.widget.CardView>(
                                                    R.id.itemCard
                                            )
                                    leftSwipeBackground?.visibility = View.GONE
                                    rightSwipeBackground?.visibility = View.GONE
                                    leftSwipeBackground?.translationX = 0f
                                    rightSwipeBackground?.translationX = 0f
                                    itemCard?.translationX = 0f
                                    viewHolder.itemView.translationX = 0f
                                    super.onChildDraw(
                                            c,
                                            recyclerView,
                                            viewHolder,
                                            dX,
                                            dY,
                                            actionState,
                                            isCurrentlyActive
                                    )
                                }
                            }

                            override fun clearView(
                                    recyclerView: RecyclerView,
                                    viewHolder: RecyclerView.ViewHolder
                            ) {
                                super.clearView(recyclerView, viewHolder)
                                // 스와이프 완료 후 아이템 위치 초기화
                                val itemView = viewHolder.itemView
                                val leftSwipeBackground =
                                        itemView.findViewById<androidx.cardview.widget.CardView>(
                                                R.id.leftSwipeBackground
                                        )
                                val rightSwipeBackground =
                                        itemView.findViewById<androidx.cardview.widget.CardView>(
                                                R.id.rightSwipeBackground
                                        )
                                val itemCard =
                                        itemView.findViewById<androidx.cardview.widget.CardView>(
                                                R.id.itemCard
                                        )

                                leftSwipeBackground?.visibility = View.GONE
                                rightSwipeBackground?.visibility = View.GONE
                                leftSwipeBackground?.translationX = 0f
                                rightSwipeBackground?.translationX = 0f
                                itemCard?.translationX = 0f
                                itemView.translationX = 0f
                            }
                        }
                )

        itemTouchHelper.attachToRecyclerView(placeRecyclerView)
    }

    /** 상세정보 페이지 열기 */
    private fun openPlaceDetail(place: Place) {
        // 카카오맵 상세정보 페이지로 이동
        openKakaoMapDetail(place)
    }

    /** 길찾기 페이지 열기 */
    private fun openNavigation(place: Place) {
        val lat = place.y.toDoubleOrNull()
        val lng = place.x.toDoubleOrNull()
        if (lat != null && lng != null) {
            openNavigation(lat, lng, place.place_name)
        }
    }

    /** 업적 페이지로 이동 */
    private fun navigateToAchievement() {
        if (selectedPlaceIndex >= 0 && selectedPlaceIndex < searchResults.size) {
            val intent = Intent(this, MyPageActivity::class.java)
            startActivity(intent)
        }
    }

    /**
     * 선택된 마커 업데이트 (크기 변경) 선택된 마커의 작은 마커를 제거하고 큰 마커를 추가합니다. 이전에 선택된 마커가 있으면 큰 마커를 제거하고 작은 마커를 다시
     * 추가합니다.
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
                    val oldPlaceKey =
                            oldPlace.id ?: "${oldPlace.place_name}_${oldPlace.x}_${oldPlace.y}"
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

    /** 마커 크기 업데이트 (선택/비선택) 선택된 경우: 큰 마커 추가 비선택 시: 작은 마커 다시 추가 */
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
                // ⭐ [핵심 수정] getMarkerStyles 사용 및 setRank(1000)
                // 숫자가 클수록 다른 마커들보다 위에 그려집니다.
                val customStyles = getMarkerStyles(kakaoMap, place.foodType, true) ?: styles
                val options =
                        LabelOptions.from(LatLng.from(lat, lng))
                                .setStyles(customStyles)
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
                // ⭐ [핵심 수정] getMarkerStyles 사용 및 setRank(0)
                // 선택되지 않은 마커는 낮은 순위를 줍니다.
                val customStyles = getMarkerStyles(kakaoMap, place.foodType, false) ?: styles
                val options =
                        LabelOptions.from(LatLng.from(lat, lng)).setStyles(customStyles).setRank(0)

                val normalLabel = layer.addLabel(options)
                if (normalLabel != null) {
                    placeMarkers[normalLabel] = place
                    placeToLabelMap[placeKey] = normalLabel
                    android.util.Log.d(
                            "MapActivity",
                            "작은 마커 다시 추가 성공 (Rank 0): ${place.place_name}"
                    )
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("MapActivity", "마커 크기 업데이트 실패: ${e.message}", e)
            e.printStackTrace()
        }
    }

    /** 리스트에서 선택한 음식점으로 스크롤 (화면 상단에 보이게, sticky header 높이 고려) */
    private fun movePlaceToTop(place: Place) {
        val index =
                searchResults.indexOfFirst {
                    (it.id != null && it.id == place.id) ||
                            (it.id == null &&
                                    it.place_name == place.place_name &&
                                    it.x == place.x &&
                                    it.y == place.y)
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
                    if (i < placeAdapter.items.size &&
                                    placeAdapter.items[i] is PlaceAdapter.AdapterItem.Header
                    ) {
                        val headerView = layoutManager?.findViewByPosition(i)
                        headerHeight = headerView?.height ?: 0

                        // 헤더 뷰가 아직 측정되지 않은 경우, 헤더 레이아웃에서 높이 계산
                        if (headerHeight == 0) {
                            val headerLayout =
                                    LayoutInflater.from(this)
                                            .inflate(R.layout.item_food_type_header, null)
                            headerLayout.measure(
                                    View.MeasureSpec.makeMeasureSpec(
                                            placeRecyclerView.width,
                                            View.MeasureSpec.EXACTLY
                                    ),
                                    View.MeasureSpec.makeMeasureSpec(
                                            0,
                                            View.MeasureSpec.UNSPECIFIED
                                    )
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

    /** 특정 장소로 지도 이동 (현재 줌 레벨 유지) */
    private fun moveToPlace(place: Place) {
        if (!::kakaoMap.isInitialized) {
            Toast.makeText(this, "지도가 아직 준비되지 않았습니다.", Toast.LENGTH_SHORT).show()
            return
        }

        val lat = place.y.toDoubleOrNull()
        val lng = place.x.toDoubleOrNull()

        if (lat != null && lng != null) {
            // 현재 줌 레벨을 유지하면서 중심만 이동
            val cameraUpdate = CameraUpdateFactory.newCenterPosition(LatLng.from(lat, lng))
            kakaoMap.moveCamera(cameraUpdate)
        }
    }

    /** 위치 권한 확인 및 검색 시작 */
    private fun checkLocationPermissionAndSearch() {
        when {
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
                    PackageManager.PERMISSION_GRANTED -> {
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

    /** 권한 요청 결과 처리 */
    override fun onRequestPermissionsResult(
            requestCode: Int,
            permissions: Array<out String>,
            grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() &&
                                grantResults[0] == PackageManager.PERMISSION_GRANTED
                ) {
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

    /** 내 위치 찾기 및 검색 시작 */
    private fun findMyLocationAndSearch() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) !=
                        PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        fusedLocationClient.lastLocation
                .addOnSuccessListener { location ->
                    if (location != null) {
                        currentLatitude = location.latitude
                        currentLongitude = location.longitude

                        // kakaoMap이 초기화되었는지 확인
                        if (::kakaoMap.isInitialized) {
                            // 현재 위치 마커 추가
                            addMyLocationMarker(location.latitude, location.longitude)

                            // 지도 중심을 내 위치로 이동
                            val cameraUpdate =
                                    CameraUpdateFactory.newCenterPosition(
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
                        Toast.makeText(
                                        this,
                                        "현재 위치를 가져올 수 없습니다. 전체 지역에서 검색합니다.",
                                        Toast.LENGTH_SHORT
                                )
                                .show()
                        searchRestaurantsWithoutLocation()
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "위치를 가져오는데 실패했습니다: ${e.message}", Toast.LENGTH_SHORT)
                            .show()
                    searchRestaurantsWithoutLocation()
                }
    }

    /** 내 위치로 지도 이동 */
    private fun moveToMyLocation() {
        if (!::kakaoMap.isInitialized) {
            Toast.makeText(this, "지도가 아직 준비되지 않았습니다.", Toast.LENGTH_SHORT).show()
            return
        }

        if (currentLatitude != null && currentLongitude != null) {
            // 현재 위치 마커 추가/업데이트
            addMyLocationMarker(currentLatitude!!, currentLongitude!!)

            // 현재 줌 레벨 유지하면서 중심만 이동
            val cameraUpdate =
                    CameraUpdateFactory.newCenterPosition(
                            LatLng.from(currentLatitude!!, currentLongitude!!)
                    )
            kakaoMap.moveCamera(cameraUpdate)
            // Toast.makeText(this, "현재 위치로 이동했습니다.", Toast.LENGTH_SHORT).show()
        } else {
            // 위치 정보가 없으면 다시 권한 확인 및 위치 가져오기
            checkLocationPermissionAndSearch()
        }
    }

    /** 현재 위치 마커 추가 */
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

            val styles =
                    myLocationMarkerStyle
                            ?: run {
                                android.util.Log.w("MapActivity", "현재 위치 마커 스타일이 초기화되지 않았습니다.")
                                return
                            }

            val labelManager = kakaoMap.labelManager
            val layer = labelManager?.layer ?: return

            val latLng = LatLng.from(latitude, longitude)
            val options = LabelOptions.from(latLng).setStyles(styles)

            val label = layer.addLabel(options)
            if (label != null) {
                myLocationLabel = label
                android.util.Log.d("MapActivity", "현재 위치 마커 추가 성공: ($latitude, $longitude)")
            }
        } catch (e: Exception) {
            android.util.Log.e("MapActivity", "현재 위치 마커 추가 실패: ${e.message}", e)
        }
    }

    /** 레이어에서 마커 제거 Kakao Map SDK v2에서는 Label 객체나 Layer의 remove 메서드를 사용합니다. */
    private fun removeLabelFromLayer(label: Label) {
        if (!::kakaoMap.isInitialized) return

        try {
            // Kakao Map SDK v2에서 마커 제거 방법 시도
            val removeMethod = label.javaClass.getMethod("remove")
            removeMethod.invoke(label)
            android.util.Log.d("MapActivity", "마커 제거 성공 (Label.remove 메서드)")
            return
        } catch (e: Exception) {
            android.util.Log.e("MapActivity", "마커 제거 실패: ${e.message}", e)
            e.printStackTrace()
        }
    }

    /** 음식종류별로 그룹화하고 거리순으로 정렬 */
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
            val sorted =
                    places.sortedWith(
                            compareBy(
                                    { it.distance.isNullOrBlank() }, // 거리 없는 것 먼저
                                    {
                                        it.distance?.toDoubleOrNull() ?: Double.MAX_VALUE
                                    } // 거리 가까운 순
                            )
                    )
            sortedResults.addAll(sorted)
        }

        searchResults.clear()
        searchResults.addAll(sortedResults)
    }

    /** 음식점 검색 (위치 기반, 10km 반경) */
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
                            searchStatusText.postDelayed(
                                    { searchStatusText.visibility = android.view.View.GONE },
                                    3000
                            )
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

    /** 음식점 검색 (위치 없이 전체 지역 검색) */
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
                            searchStatusText.postDelayed(
                                    { searchStatusText.visibility = android.view.View.GONE },
                                    3000
                            )
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

    /** Kakao Local API로 주변 음식점 검색 (10km 반경) */
    private fun searchNearbyRestaurants(
            query: String,
            foodType: String,
            latitude: Double,
            longitude: Double,
            onComplete: () -> Unit,
            onError: (String) -> Unit
    ) {
        mapApiHelper.searchPlaces(
                query = query,
                foodType = foodType,
                latitude = latitude,
                longitude = longitude,
                onSuccess = { places ->
                    places.forEach { place ->
                        addMarker(place)
                        // 중복 방지: 이미 같은 id가 있는지 확인
                        if (place.id != null && !searchResults.any { it.id == place.id }) {
                            searchResults.add(place)
                        } else if (place.id == null &&
                                        !searchResults.any {
                                            it.place_name == place.place_name &&
                                                    it.x == place.x &&
                                                    it.y == place.y
                                        }
                        ) {
                            searchResults.add(place)
                        }
                    }
                    onComplete()
                },
                onError = onError
        )
    }

    /** Kakao Local API로 음식점 검색 (위치 정보 없이) */
    private fun searchRestaurantsWithoutLocation(
            query: String,
            foodType: String,
            onComplete: () -> Unit,
            onError: (String) -> Unit
    ) {
        mapApiHelper.searchPlaces(
                query = query,
                foodType = foodType,
                latitude = null,
                longitude = null,
                onSuccess = { places ->
                    places.forEach { place ->
                        addMarker(place)
                        // 중복 방지: 이미 같은 id가 있는지 확인
                        if (place.id != null && !searchResults.any { it.id == place.id }) {
                            searchResults.add(place)
                        } else if (place.id == null &&
                                        !searchResults.any {
                                            it.place_name == place.place_name &&
                                                    it.x == place.x &&
                                                    it.y == place.y
                                        }
                        ) {
                            searchResults.add(place)
                        }
                    }

                    // 첫 번째 검색 결과로 지도 이동 (위치 정보가 없을 때)
                    if (places.isNotEmpty() && currentLatitude == null && ::kakaoMap.isInitialized
                    ) {
                        val firstPlace = places[0]
                        val lat = firstPlace.y.toDoubleOrNull()
                        val lng = firstPlace.x.toDoubleOrNull()
                        if (lat != null && lng != null) {
                            val cameraUpdate =
                                    CameraUpdateFactory.newCenterPosition(LatLng.from(lat, lng))
                            kakaoMap.moveCamera(cameraUpdate)
                            val zoomUpdate = CameraUpdateFactory.zoomTo(13)
                            kakaoMap.moveCamera(zoomUpdate)
                        }
                    }
                    onComplete()
                },
                onError = onError
        )
    }

    /** 지도에 마커 추가 */
    private fun addMarker(place: Place) {
        // kakaoMap이 초기화되지 않았으면 마커 추가 불가
        if (!::kakaoMap.isInitialized) {
            android.util.Log.w("MapActivity", "kakaoMap이 초기화되지 않아 마커를 추가할 수 없습니다.")
            return
        }

        // Kakao API는 x=경도(longitude), y=위도(latitude)를 반환
        val lat =
                place.y.toDoubleOrNull()
                        ?: run {
                            android.util.Log.w("MapActivity", "위도 변환 실패: ${place.y}")
                            return
                        }
        val lng =
                place.x.toDoubleOrNull()
                        ?: run {
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

            // 일반 마커 스타일 사용 (동적 생성)
            val styles =
                    getMarkerStyles(kakaoMap, place.foodType, false)
                            ?: normalMarkerStyle // 실패 시 기본 스타일 사용
                                    ?: run {
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
            val options = LabelOptions.from(latLng).setStyles(styles)

            // 마커 추가
            android.util.Log.d("MapActivity", "마커 옵션 생성 완료: lat=$lat, lng=$lng")
            val label = layer.addLabel(options)
            if (label != null) {
                placeMarkers[label] = place
                val placeKey = place.id ?: "${place.place_name}_${place.x}_${place.y}"
                placeToLabelMap[placeKey] = label
                android.util.Log.d(
                        "MapActivity",
                        "마커 추가 성공: ${place.place_name}, label=$label, 총 마커 수: ${placeMarkers.size}"
                )
            } else {
                android.util.Log.e(
                        "MapActivity",
                        "마커 추가 실패: label이 null입니다. place: ${place.place_name}, 좌표: ($lat, $lng)"
                )
            }
        } catch (e: Exception) {
            // 마커 추가 실패 시 로그만 출력하고 계속 진행
            android.util.Log.e("MapActivity", "마커 추가 실패: ${e.message}", e)
            e.printStackTrace()
        }
    }

    /** 모든 마커 제거 (음식점 마커만, 현재 위치 마커는 유지) */
    private fun clearMarkers() {
        if (!::kakaoMap.isInitialized) return

        try {
            // 음식점 마커만 제거 (현재 위치 마커는 제외)
            val labelsToRemove = placeMarkers.keys.filter { it != myLocationLabel }.toList()
            labelsToRemove.forEach { label -> removeLabelFromLayer(label) }

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

    /** 카카오맵에서 상세 정보 보기 (리뷰, 사진 등 확인 가능) */
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

    /** 길찾기 앱 열기 */
    private fun openNavigation(latitude: Double, longitude: Double, placeName: String) {
        // 카카오맵 앱으로 길찾기
        val kakaoMapUri = "kakaomap://route?ep=$latitude,$longitude&by=CAR"
        val kakaoIntent = Intent(Intent.ACTION_VIEW, Uri.parse(kakaoMapUri))

        if (kakaoIntent.resolveActivity(packageManager) != null) {
            startActivity(kakaoIntent)
            return
        }

        // 카카오맵이 없으면 네이버 지도 앱 시도
        val naverMapUri =
                "nmap://route/car?dlat=$latitude&dlng=$longitude&dname=${Uri.encode(placeName)}"
        val naverIntent = Intent(Intent.ACTION_VIEW, Uri.parse(naverMapUri))

        if (naverIntent.resolveActivity(packageManager) != null) {
            startActivity(naverIntent)
            return
        }

        // 길찾기 앱이 없으면 웹 브라우저로 카카오맵 길찾기 페이지 열기
        try {
            val webUrl =
                    "https://map.kakao.com/link/to/${Uri.encode(placeName)},$latitude,$longitude"
            val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse(webUrl))
            startActivity(webIntent)
        } catch (e: Exception) {
            android.util.Log.e("MapActivity", "웹 브라우저로 길찾기 열기 실패: ${e.message}")
            Toast.makeText(this, "길찾기를 열 수 없습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    /** 로딩 상태 표시/숨기기 */
    private fun showLoading(show: Boolean) {
        if (show) {
            progressBar.visibility = View.VISIBLE
            searchStatusText.visibility = View.VISIBLE
        } else {
            progressBar.visibility = View.GONE
        }
    }
}
