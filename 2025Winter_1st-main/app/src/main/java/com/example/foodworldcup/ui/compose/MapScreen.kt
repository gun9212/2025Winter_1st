package com.example.foodworldcup.ui.compose

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import android.net.Uri
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.core.graphics.createBitmap
import com.example.foodworldcup.api.Place
import com.example.foodworldcup.api.MapApiHelper
import com.example.foodworldcup.data.FoodRepository
import com.example.foodworldcup.utils.BitmapUtils
import com.example.foodworldcup.utils.KakaoMapHelper
import com.example.foodworldcup.utils.PreferenceManager
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.kakao.vectormap.*
import com.kakao.vectormap.camera.CameraUpdateFactory
import com.kakao.vectormap.label.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
/**
 * MapScreen - 지도 화면 (Compose 버전)
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MapScreen() {
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp
    val scope = rememberCoroutineScope()
    val preferenceManager = remember { PreferenceManager(context) }
    
    // 위치 권한 상태
    var hasLocationPermission by remember { mutableStateOf(false) }
    
    // 위치 권한 요청 런처
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasLocationPermission = isGranted
        if (isGranted) {
            // 권한이 허용되면 위치 가져오기
        } else {
            Toast.makeText(context, "위치 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
        }
    }
    
    // 초기 권한 확인
    LaunchedEffect(Unit) {
        hasLocationPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    // 음식 이름 리스트
    val foodNames = remember {
        mutableStateListOf<String>()
    }
    
    // PreferenceManager에서 직접 불러오기
    LaunchedEffect(Unit) {
            val lastSearchFoodIds = preferenceManager.getFinalFoodIds()
        val names = if (lastSearchFoodIds.isNotEmpty()) {
                lastSearchFoodIds.mapNotNull { id ->
                    FoodRepository.getFoodById(id)?.name
                }
            } else {
                emptyList()
        }
        foodNames.clear()
        foodNames.addAll(names)
    }
    
    // 현재 위치
    var currentLatitude by remember { mutableStateOf<Double?>(null) }
    var currentLongitude by remember { mutableStateOf<Double?>(null) }
    
    // 검색 결과
    val searchResults = remember { mutableStateListOf<Place>() }
    var selectedPlaceIndex by remember { mutableStateOf(-1) }
    var isLoading by remember { mutableStateOf(false) }
    var searchStatusText by remember { mutableStateOf("") }
    
    // 리스트 상태
    val listState = rememberLazyListState()
    
    // MapView와 KakaoMap 상태
    var mapView by remember { mutableStateOf<MapView?>(null) }
    var kakaoMap by remember { mutableStateOf<KakaoMap?>(null) }
    
    // 마커 관리
    val placeMarkers = remember { mutableMapOf<Label, Place>() }
    val placeToLabelMap = remember { mutableMapOf<String, Label>() }
    var selectedLabel by remember { mutableStateOf<Label?>(null) }
    var myLocationLabel by remember { mutableStateOf<Label?>(null) }
    
    // 마커 스타일 캐시
    val markerStylesCache = remember { mutableMapOf<String, LabelStyles>() }
    var normalMarkerStyle by remember { mutableStateOf<LabelStyles?>(null) }
    var selectedMarkerStyle by remember { mutableStateOf<LabelStyles?>(null) }
    var myLocationMarkerStyle by remember { mutableStateOf<LabelStyles?>(null) }
    
    // API Helper
    val mapApiHelper = remember { MapApiHelper() }
    
    // 위치 클라이언트
    val fusedLocationClient = remember {
        LocationServices.getFusedLocationProviderClient(context)
    }
    
    // 현재 위치 가져오기
    fun findMyLocation() {
        if (!hasLocationPermission) {
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            return
        }
        
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location ->
                if (location != null) {
                    currentLatitude = location.latitude
                    currentLongitude = location.longitude
                    
                    kakaoMap?.let { map ->
                        // 현재 위치 마커 추가
                        addMyLocationMarker(
                            context = context,
                            kakaoMap = map,
                            latitude = location.latitude,
                            longitude = location.longitude,
                            myLocationMarkerStyle = myLocationMarkerStyle,
                            myLocationLabel = myLocationLabel,
                            onLabelCreated = { label ->
                                myLocationLabel = label
                            }
                        )
                        
                        // 지도 중심 이동
                        val cameraUpdate = CameraUpdateFactory.newCenterPosition(
                            LatLng.from(location.latitude, location.longitude)
                        )
                        map.moveCamera(cameraUpdate)
                        
                        val zoomUpdate = CameraUpdateFactory.zoomTo(14)
                        map.moveCamera(zoomUpdate)
                    }
                    
                    // 음식점 검색 시작
                    if (foodNames.isNotEmpty()) {
                        searchRestaurants(
                            context = context,
                            foodNames = foodNames.toList(),
                            latitude = location.latitude,   
                            longitude = location.longitude,
                            mapApiHelper = mapApiHelper,
                            kakaoMap = kakaoMap!!,
                            normalMarkerStyle = normalMarkerStyle,
                            markerStylesCache = markerStylesCache,
                            placeMarkers = placeMarkers,
                            placeToLabelMap = placeToLabelMap,
                            searchResults = searchResults,
                            onStart = {
                                isLoading = true
                                searchStatusText = "검색 중... (${foodNames.size}개 음식, 반경 10km)"
                            },
                            onComplete = {
                                sortAndGroupPlacesByFoodType(searchResults, foodNames.toList())
                                isLoading = false
                                searchStatusText = "검색 완료! (${searchResults.size}개 결과)"
                                // 3초 후 메시지 사라지게
                                scope.launch {
                                    delay(3000)
                                    searchStatusText = ""
                                }
                            },
                            onError = { errorMsg ->
                                isLoading = false
                                Toast.makeText(context, errorMsg, Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                } else {
                    Toast.makeText(context, "현재 위치를 가져올 수 없습니다.", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "위치를 가져오는데 실패했습니다: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
    
    // 지도 초기화
    fun initMap(mapViewInstance: MapView) {
        mapView = mapViewInstance
        mapViewInstance.start(
            object : MapLifeCycleCallback() {
                override fun onMapDestroy() {
                    // 지도가 파괴될 때 처리
                }
                
                override fun onMapError(error: Exception) {
                    Toast.makeText(context, "지도를 불러오는데 실패했습니다: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            },
            object : KakaoMapReadyCallback() {
                override fun onMapReady(map: KakaoMap) {
                    kakaoMap = map
                    
                    // 마커 스타일 초기화
                    initMarkerStyles(
                        context = context,
                        kakaoMap = map,
                        onNormalStyleCreated = { style -> normalMarkerStyle = style },
                        onSelectedStyleCreated = { style -> selectedMarkerStyle = style },
                        onLocationStyleCreated = { style -> myLocationMarkerStyle = style }
                    )
                    
                    // 마커 클릭 이벤트 설정
                    map.setOnLabelClickListener { _, _, label ->
                        val place = placeMarkers[label]
                        if (place != null) {
                            val index = searchResults.indexOfFirst {
                                (it.id != null && it.id == place.id) ||
                                (it.id == null && it.place_name == place.place_name &&
                                 it.x == place.x && it.y == place.y)
                            }
                            if (index >= 0) {
                                selectedPlaceIndex = index
                                updateSelectedMarker(
                                    kakaoMap = map,
                                    place = place,
                                    selectedLabel = selectedLabel,
                                    placeMarkers = placeMarkers,
                                    placeToLabelMap = placeToLabelMap,
                                    markerStylesCache = markerStylesCache,
                                    context = context,
                                    normalMarkerStyle = normalMarkerStyle,
                                    selectedMarkerStyle = selectedMarkerStyle,
                                    onLabelSelected = { label -> selectedLabel = label }
                                )
                                moveToPlace(map, place)
                            }
                        }
                        true
                    }
                    
                    // 위치 권한 확인 후 위치 가져오기 및 검색 시작
                    if (hasLocationPermission) {
                        findMyLocation()
                    } else {
                        permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                    }
                }
            }
        )
    }
    
    // 현재 위치로 이동
    fun moveToMyLocation() {
        if (currentLatitude != null && currentLongitude != null) {
            kakaoMap?.let { map ->
                val cameraUpdate = CameraUpdateFactory.newCenterPosition(
                    LatLng.from(currentLatitude!!, currentLongitude!!)
                )
                map.moveCamera(cameraUpdate)
            }
        } else {
            findMyLocation()
        }
    }
    
    // 화면을 반으로 나누기: 위쪽은 지도, 아래쪽은 리스트
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // 위쪽 절반: 지도
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            AndroidView(
                factory = { ctx ->
                    MapView(ctx).apply {
                        initMap(this)
                    }
                },
                modifier = Modifier.fillMaxSize(),
                update = { view ->
                    // MapView lifecycle 관리
                    view.resume()
                }
            )
            
            // 현재 위치 버튼
            FloatingActionButton(
                onClick = { moveToMyLocation() },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
                containerColor = colorScheme.surface,
                contentColor = colorScheme.primary
            ) {
                Icon(
                    imageVector = Icons.Default.MyLocation,
                    contentDescription = "현재 위치"
                )
            }
            
            // 검색 상태 텍스트
            if (searchStatusText.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.Black.copy(alpha = 0.7f)
                    )
                ) {
                    Text(
                        text = searchStatusText,
                        color = Color.White,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }
        }
        
        // 아래쪽 절반: 리스트
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(colorScheme.surface)
        ) {
            if (searchResults.isNotEmpty()) {
                PlaceListContent(
                    places = searchResults,
                    selectedIndex = selectedPlaceIndex,
                    onPlaceClick = { place, index ->
                        selectedPlaceIndex = if (selectedPlaceIndex == index) -1 else index
                        if (selectedPlaceIndex >= 0 && kakaoMap != null) {
                            updateSelectedMarker(
                                kakaoMap = kakaoMap!!,
                                place = place,
                                selectedLabel = selectedLabel,
                                placeMarkers = placeMarkers,
                                placeToLabelMap = placeToLabelMap,
                                markerStylesCache = markerStylesCache,
                                context = context,
                                normalMarkerStyle = normalMarkerStyle,
                                selectedMarkerStyle = selectedMarkerStyle,
                                onLabelSelected = { label -> selectedLabel = label }
                            )
                            moveToPlace(kakaoMap!!, place)
                        }
                    },
                    listState = listState,
                    onSwipeLeft = { place ->
                        // 왼쪽 스와이프: 길찾기
                        val lat = place.y.toDoubleOrNull()
                        val lng = place.x.toDoubleOrNull()
                        if (lat != null && lng != null) {
                            openNavigation(context, lat, lng, place.place_name)
                        }
                    },
                    onSwipeRight = { place ->
                        // 오른쪽 스와이프: 상세정보
                        val lat = place.y.toDoubleOrNull()
                        val lng = place.x.toDoubleOrNull()
                        KakaoMapHelper.openKakaoMapDetail(
                            context = context,
                            placeId = place.id,
                            latitude = lat,
                            longitude = lng,
                            placeName = place.place_name
                        )
                    }
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    if (isLoading) {
                        CircularProgressIndicator()
                    } else {
                        Text(
                            text = "검색 결과가 없습니다.",
                            color = colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

// Helper functions (MapActivity에서 가져온 로직)
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PlaceListContent(
    places: List<Place>,
    selectedIndex: Int,
    onPlaceClick: (Place, Int) -> Unit,
    onSwipeLeft: (Place) -> Unit,
    onSwipeRight: (Place) -> Unit,
    listState: LazyListState
) {
    val colorScheme = MaterialTheme.colorScheme
    val density = LocalDensity.current
    var lastFoodType by remember { mutableStateOf<String?>(null) }
    
    // 실제 item index를 계산하는 함수
    fun calculateItemIndex(placeIndex: Int): Int {
        if (placeIndex < 0 || placeIndex >= places.size) return -1
        var itemIndex = 0
        var lastType: String? = null
        
        // 선택된 place까지의 모든 item을 카운트
        for (i in 0..placeIndex) {
            val place = places[i]
            // 새로운 음식 종류가 시작되면 sticky header 추가
            if (place.foodType != lastType) {
                lastType = place.foodType
                itemIndex++ // sticky header
            }
            // place item 추가 (선택된 place 포함)
            if (i == placeIndex) {
                // 선택된 place의 item index 반환
                return itemIndex
            }
            itemIndex++ // place item
        }
        return itemIndex
    }
    
    // selectedIndex가 변경될 때 스크롤
    LaunchedEffect(selectedIndex) {
        if (selectedIndex >= 0 && selectedIndex < places.size) {
            val itemIndex = calculateItemIndex(selectedIndex)
            if (itemIndex >= 0) {
                // sticky header 높이: 텍스트(16.sp ≈ 20dp) + 상하 패딩(16.dp * 2 = 32.dp) ≈ 52.dp
                val stickyHeaderHeightDp = 52.dp
                val stickyHeaderHeightPx = with(density) { stickyHeaderHeightDp.toPx().toInt() }
                
                listState.animateScrollToItem(
                    index = itemIndex,
                    scrollOffset = -stickyHeaderHeightPx
                )
            }
        }
    }
    
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        places.forEachIndexed { index, place ->
            // 음식 종류 헤더 (sticky header)
            if (place.foodType != lastFoodType) {
                lastFoodType = place.foodType
                stickyHeader {
                    PlaceHeader(foodType = place.foodType ?: "기타")
                }
            }
            
            // Place 아이템
            item {
                SwipeablePlaceItem(
                    place = place,
                    isSelected = index == selectedIndex,
                    onClick = { onPlaceClick(place, index) },
                    onSwipeLeft = { onSwipeLeft(place) },
                    onSwipeRight = { onSwipeRight(place) }
                )
            }
        }
    }
}

@Composable
private fun PlaceHeader(foodType: String) {
    val colorScheme = MaterialTheme.colorScheme
    
    Surface(
        color = colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = foodType,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = colorScheme.primary,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp, horizontal = 4.dp)
        )
    }
}

@Composable
private fun SwipeablePlaceItem(
    place: Place,
    isSelected: Boolean,
    onClick: () -> Unit,
    onSwipeLeft: () -> Unit,
    onSwipeRight: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    var offsetX by remember { mutableStateOf(0f) }
    val offsetXAnimated by animateFloatAsState(
        targetValue = offsetX,
        animationSpec = spring(dampingRatio = 0.8f),
        label = "swipeOffset"
    )
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
    ) {
        // 스와이프 배경
        Row(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(12.dp))
        ) {
            // 왼쪽 배경 (길찾기)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(colorScheme.secondary)
                    .clip(RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp)),
                contentAlignment = Alignment.CenterStart
            ) {
                Icon(
                    imageVector = Icons.Default.Navigation,
                    contentDescription = "길찾기",
                    tint = Color.White,
                    modifier = Modifier
                        .size(32.dp)
                        .padding(start = 16.dp)
                )
            }
            
            // 오른쪽 배경 (상세정보)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(colorScheme.primary)
                    .clip(RoundedCornerShape(topEnd = 12.dp, bottomEnd = 12.dp)),
                contentAlignment = Alignment.CenterEnd
            ) {
                Text(
                    text = "상세정보",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(end = 16.dp)
                )
            }
        }
        
        // Place 카드
        Card(
            modifier = Modifier
                .fillMaxSize()
                .offset(x = offsetXAnimated.dp)
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            when {
                                offsetX < -300f -> {
                                    onSwipeLeft()
                                    offsetX = 0f
                                }
                                offsetX > 300f -> {
                                    onSwipeRight()
                                    offsetX = 0f
                                }
                                else -> offsetX = 0f
                            }
                        }
                    ) { _, dragAmount ->
                        offsetX = (offsetX + dragAmount * 0.5f).coerceIn(-400f, 400f)
                    }
                }
                .clickable { onClick() },
            colors = CardDefaults.cardColors(
                containerColor = if (isSelected) colorScheme.surface else colorScheme.surfaceVariant
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = place.place_name,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.onSurface
                    )
                    
                    if (!place.category_name.isNullOrEmpty()) {
                        Text(
                            text = place.category_name,
                            fontSize = 14.sp,
                            color = colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                    
                    Row(
                        modifier = Modifier.padding(top = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = place.road_address_name ?: place.address_name ?: "주소 정보 없음",
                            fontSize = 12.sp,
                            color = colorScheme.onSurfaceVariant
                        )
                    }
                    
                    if (!place.phone.isNullOrEmpty()) {
                        Row(
                            modifier = Modifier.padding(top = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Phone,
                                contentDescription = null,
                                tint = colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = place.phone,
                                fontSize = 12.sp,
                                color = colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (place.distance != null) {
                        Text(
                            text = "${place.distance.toDoubleOrNull()?.let { "%.1f".format(it / 1000.0) } ?: "0"} km",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = colorScheme.primary
                        )
                    }
                    
                    if (place.category_group_code == "FD6") {
                        Surface(
                            color = colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "영업중",
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// Helper functions for map operations
private fun initMarkerStyles(
    context: Context,
    kakaoMap: KakaoMap,
    onNormalStyleCreated: (LabelStyles) -> Unit,
    onSelectedStyleCreated: (LabelStyles) -> Unit,
    onLocationStyleCreated: (LabelStyles) -> Unit
) {
    val labelManager = kakaoMap.labelManager ?: return
    
    // 일반 마커 스타일
    try {
        val drawable = ContextCompat.getDrawable(context, com.example.foodworldcup.R.drawable.ic_marker_restaurant)
        if (drawable != null) {
            val density = context.resources.displayMetrics.density
            val width = (24 * density).toInt()
            val height = (24 * density).toInt()
            
            val bitmap = createBitmap(width, height)
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, width, height)
            drawable.draw(canvas)
            
            val normalStyle = LabelStyle.from(bitmap)
            if (normalStyle != null) {
                val styles = LabelStyles.from(normalStyle)
                labelManager.addLabelStyles(styles)?.let { onNormalStyleCreated(it) }
            }
        }
    } catch (e: Exception) {
        android.util.Log.e("MapScreen", "일반 마커 스타일 생성 실패: ${e.message}", e)
    }
    
    // 선택된 마커 스타일
    try {
        val drawable = ContextCompat.getDrawable(context, com.example.foodworldcup.R.drawable.ic_marker_selected)
        if (drawable != null) {
            val density = context.resources.displayMetrics.density
            val width = (32 * density).toInt()
            val height = (32 * density).toInt()
            
            val bitmap = createBitmap(width, height)
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, width, height)
            drawable.draw(canvas)
            
            val selectedStyle = LabelStyle.from(bitmap)
            if (selectedStyle != null) {
                val styles = LabelStyles.from(selectedStyle)
                labelManager.addLabelStyles(styles)?.let { onSelectedStyleCreated(it) }
            }
        }
    } catch (e: Exception) {
        android.util.Log.e("MapScreen", "선택된 마커 스타일 생성 실패: ${e.message}", e)
    }
    
    // 현재 위치 마커 스타일
    try {
        val drawable = ContextCompat.getDrawable(context, com.example.foodworldcup.R.drawable.ic_my_location_marker)
        if (drawable != null) {
            val density = context.resources.displayMetrics.density
            val width = (16 * density).toInt()
            val height = (16 * density).toInt()
            
            val bitmap = createBitmap(width, height)
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, width, height)
            drawable.draw(canvas)
            
            val locationStyle = LabelStyle.from(bitmap)
            if (locationStyle != null) {
                val styles = LabelStyles.from(locationStyle)
                labelManager.addLabelStyles(styles)?.let { onLocationStyleCreated(it) }
            }
        }
    } catch (e: Exception) {
        android.util.Log.e("MapScreen", "현재 위치 마커 스타일 생성 실패: ${e.message}", e)
    }
}

private fun getMarkerStyles(
    context: Context,
    kakaoMap: KakaoMap,
    foodType: String?,
    isSelected: Boolean,
    cache: MutableMap<String, LabelStyles>
): LabelStyles? {
    val key = "${foodType ?: "default"}_${if (isSelected) "selected" else "normal"}"
    
    if (cache.containsKey(key)) {
        return cache[key]
    }
    
    val labelManager = kakaoMap.labelManager ?: return null
    
    val bitmap = createCombinedMarkerBitmap(context, foodType, isSelected) ?: return null
    val style = LabelStyle.from(bitmap) ?: return null
    val styles = LabelStyles.from(style)
    val addedStyles = labelManager.addLabelStyles(styles)
    
    if (addedStyles != null) {
        cache[key] = addedStyles
        return addedStyles
    }
    
    return null
}

private fun createCombinedMarkerBitmap(
    context: Context,
    foodType: String?,
    isSelected: Boolean
): Bitmap? {
    try {
        val density = context.resources.displayMetrics.density
        val baseSizeDp = if (isSelected) 32 else 24
        var width = (baseSizeDp * density).toInt()
        var height = (baseSizeDp * density).toInt()
        
        var customMarkerBitmap: Bitmap? = null
        
        // 선택된 경우 커스텀 마커 이미지 로드
        if (isSelected) {
            try {
                val markerStream = context.assets.open("marker/마커_누끼.png")
                val rawMarkerBitmap = BitmapFactory.decodeStream(markerStream)
                markerStream.close()
                
                if (rawMarkerBitmap != null) {
                    val aspectRatio = rawMarkerBitmap.height.toFloat() / rawMarkerBitmap.width.toFloat()
                    height = (width * aspectRatio).toInt()
                    customMarkerBitmap = Bitmap.createScaledBitmap(rawMarkerBitmap, width, height, true)
                }
            } catch (e: Exception) {
                android.util.Log.e("MapScreen", "커스텀 마커 로드 실패: ${e.message}")
            }
        }
        
        val markerBitmap = createBitmap(width, height)
        val canvas = Canvas(markerBitmap)
        
        if (isSelected && customMarkerBitmap != null) {
            canvas.drawBitmap(customMarkerBitmap, 0f, 0f, null)
        }
        
        // 음식 캐릭터 이미지 로드 및 그리기
        var characterDrawn = false
        if (foodType != null) {
            val assetPath = findAssetPath(context, foodType)
            if (assetPath != null) {
                val assetStream = context.assets.open(assetPath)
                val characterBitmap = BitmapFactory.decodeStream(assetStream)
                assetStream.close()
                
                if (characterBitmap != null) {
                    characterDrawn = true
                    val contentBounds = BitmapUtils.getContentBounds(characterBitmap)
                    val contentWidth = contentBounds.width()
                    val contentHeight = contentBounds.height()
                    
                    if (isSelected) {
                        val targetAreaWidth = (width * 0.7f)
                        val targetAreaHeight = (width * 0.7f)
                        val scale = Math.min(targetAreaWidth / contentWidth, targetAreaHeight / contentHeight)
                        
                        val matrix = android.graphics.Matrix()
                        matrix.postTranslate(-contentBounds.left.toFloat(), -contentBounds.top.toFloat())
                        matrix.postScale(scale, scale)
                        
                        val targetCenterX = width / 2f
                        val headerTopOffset = height * 0.12f
                        val targetCenterY = headerTopOffset + targetAreaHeight / 2f
                        val tx = targetCenterX - (contentWidth * scale / 2f)
                        val ty = targetCenterY - (contentHeight * scale / 2f)
                        matrix.postTranslate(tx, ty)
                        
                        val paint = Paint()
                        paint.isAntiAlias = true
                        paint.isFilterBitmap = true
                        canvas.drawBitmap(characterBitmap, matrix, paint)
                    } else {
                        val targetAreaWidth = (width * 0.9f)
                        val targetAreaHeight = (height * 0.9f)
                        val scale = Math.min(targetAreaWidth / contentWidth, targetAreaHeight / contentHeight)
                        
                        val matrix = android.graphics.Matrix()
                        matrix.postTranslate(-contentBounds.left.toFloat(), -contentBounds.top.toFloat())
                        matrix.postScale(scale, scale)
                        
                        val targetCenterX = width / 2f
                        val targetCenterY = height / 2f
                        val tx = targetCenterX - (contentWidth * scale / 2f)
                        val ty = targetCenterY - (contentHeight * scale / 2f)
                        matrix.postTranslate(tx, ty)
                        
                        val paint = Paint()
                        paint.isAntiAlias = true
                        paint.isFilterBitmap = true
                        canvas.drawBitmap(characterBitmap, matrix, paint)
                    }
                }
            }
        }
        
        return markerBitmap
    } catch (e: Exception) {
        android.util.Log.e("MapScreen", "비트맵 생성 실패: ${e.message}")
        return null
    }
}

private fun findAssetPath(context: Context, foodType: String): String? {
    val categories = listOf("아시안", "양식", "일식", "중식", "한식")
    val fileName = "${foodType}_캐릭터누끼.png"
    
    for (category in categories) {
        val path = "food_character_images/$category/$fileName"
        try {
            val stream = context.assets.open(path)
            stream.close()
            return path
        } catch (e: java.io.IOException) {
            continue
        }
    }
    return null
}

private fun addMarker(
    context: Context,
    kakaoMap: KakaoMap,
    place: Place,
    normalMarkerStyle: LabelStyles?,
    markerStylesCache: MutableMap<String, LabelStyles>,
    placeMarkers: MutableMap<Label, Place>,
    placeToLabelMap: MutableMap<String, Label>
) {
    val lat = place.y.toDoubleOrNull() ?: return
    val lng = place.x.toDoubleOrNull() ?: return
    
    val labelManager = kakaoMap.labelManager ?: return
    val layer = labelManager.layer ?: return
    
    val styles = getMarkerStyles(context, kakaoMap, place.foodType, false, markerStylesCache)
        ?: normalMarkerStyle ?: return
    
    val latLng = LatLng.from(lat, lng)
    val options = LabelOptions.from(latLng).setStyles(styles)
    
    val label = layer.addLabel(options)
    if (label != null) {
        placeMarkers[label] = place
        val placeKey = place.id ?: "${place.place_name}_${place.x}_${place.y}"
        placeToLabelMap[placeKey] = label
    }
}

private fun addMyLocationMarker(
    context: Context,
    kakaoMap: KakaoMap,
    latitude: Double,
    longitude: Double,
    myLocationMarkerStyle: LabelStyles?,
    myLocationLabel: Label?,
    onLabelCreated: (Label) -> Unit
) {
    myLocationLabel?.let { oldLabel ->
        try {
            val removeMethod = oldLabel.javaClass.getMethod("remove")
            removeMethod.invoke(oldLabel)
        } catch (e: Exception) {
            android.util.Log.e("MapScreen", "마커 제거 실패: ${e.message}")
        }
    }
    
    val styles = myLocationMarkerStyle ?: return
    val labelManager = kakaoMap.labelManager ?: return
    val layer = labelManager.layer ?: return
    
    val latLng = LatLng.from(latitude, longitude)
    val options = LabelOptions.from(latLng).setStyles(styles)
    
    val label = layer.addLabel(options)
    if (label != null) {
        onLabelCreated(label)
    }
}

private fun updateSelectedMarker(
    kakaoMap: KakaoMap,
    place: Place,
    selectedLabel: Label?,
    placeMarkers: MutableMap<Label, Place>,
    placeToLabelMap: MutableMap<String, Label>,
    markerStylesCache: MutableMap<String, LabelStyles>,
    context: Context,
    normalMarkerStyle: LabelStyles?,
    selectedMarkerStyle: LabelStyles?,
    onLabelSelected: (Label) -> Unit
) {
    val placeKey = place.id ?: "${place.place_name}_${place.x}_${place.y}"
    
    try {
        // 이전 선택된 마커 제거
        selectedLabel?.let { oldLabel ->
            val oldPlace = placeMarkers[oldLabel]
            if (oldPlace != null) {
                try {
                    val removeMethod = oldLabel.javaClass.getMethod("remove")
                    removeMethod.invoke(oldLabel)
                    placeMarkers.remove(oldLabel)
                    
                    // 이전 선택된 마커의 작은 마커 다시 추가
                    val oldPlaceKey = oldPlace.id ?: "${oldPlace.place_name}_${oldPlace.x}_${oldPlace.y}"
                    placeToLabelMap.remove("${oldPlaceKey}_selected")
                    updateMarkerSize(
                        context = context,
                        kakaoMap = kakaoMap,
                        place = oldPlace,
                        isSelected = false,
                        normalMarkerStyle = normalMarkerStyle,
                        selectedMarkerStyle = selectedMarkerStyle,
                        markerStylesCache = markerStylesCache,
                        placeMarkers = placeMarkers,
                        placeToLabelMap = placeToLabelMap
                    )
                } catch (e: Exception) {
                    android.util.Log.e("MapScreen", "마커 제거 실패: ${e.message}")
                }
            }
        }
        
        // 현재 선택된 마커의 작은 마커 찾기
        val currentNormalLabel = placeToLabelMap[placeKey]
        
        // 작은 마커가 있으면 제거
        if (currentNormalLabel != null) {
            try {
                val removeMethod = currentNormalLabel.javaClass.getMethod("remove")
                removeMethod.invoke(currentNormalLabel)
                placeMarkers.remove(currentNormalLabel)
                placeToLabelMap.remove(placeKey)
            } catch (e: Exception) {
                android.util.Log.e("MapScreen", "마커 제거 실패: ${e.message}")
            }
        }
        
        // 큰 마커 추가
        val newSelectedLabel = updateMarkerSize(
            context = context,
            kakaoMap = kakaoMap,
            place = place,
            isSelected = true,
            normalMarkerStyle = normalMarkerStyle,
            selectedMarkerStyle = selectedMarkerStyle,
            markerStylesCache = markerStylesCache,
            placeMarkers = placeMarkers,
            placeToLabelMap = placeToLabelMap
        )
        
        if (newSelectedLabel != null) {
            onLabelSelected(newSelectedLabel)
        }
    } catch (e: Exception) {
        android.util.Log.e("MapScreen", "마커 업데이트 실패: ${e.message}", e)
    }
}

private fun updateMarkerSize(
    context: Context,
    kakaoMap: KakaoMap,
    place: Place,
    isSelected: Boolean,
    normalMarkerStyle: LabelStyles?,
    selectedMarkerStyle: LabelStyles?,
    markerStylesCache: MutableMap<String, LabelStyles>,
    placeMarkers: MutableMap<Label, Place>,
    placeToLabelMap: MutableMap<String, Label>
): Label? {
    val placeKey = place.id ?: "${place.place_name}_${place.x}_${place.y}"
    val lat = place.y.toDoubleOrNull() ?: return null
    val lng = place.x.toDoubleOrNull() ?: return null
    
    val labelManager = kakaoMap.labelManager ?: return null
    val layer = labelManager.layer ?: return null
    
    if (isSelected) {
        // 선택된 경우: 큰 마커 추가
        val styles = getMarkerStyles(context, kakaoMap, place.foodType, true, markerStylesCache)
            ?: selectedMarkerStyle ?: return null
        
        val options = LabelOptions.from(LatLng.from(lat, lng))
            .setStyles(styles)
            .setRank(1000)
        
        val selectedLabelNew = layer.addLabel(options)
        if (selectedLabelNew != null) {
            placeMarkers[selectedLabelNew] = place
            placeToLabelMap["${placeKey}_selected"] = selectedLabelNew
            return selectedLabelNew
        }
    } else {
        // 비선택 시: 작은 마커 다시 추가
        val styles = getMarkerStyles(context, kakaoMap, place.foodType, false, markerStylesCache)
            ?: normalMarkerStyle ?: return null
        
        val options = LabelOptions.from(LatLng.from(lat, lng))
            .setStyles(styles)
            .setRank(0)
        
        val normalLabel = layer.addLabel(options)
        if (normalLabel != null) {
            placeMarkers[normalLabel] = place
            placeToLabelMap[placeKey] = normalLabel
            return normalLabel
        }
    }
    
    return null
}

private fun moveToPlace(kakaoMap: KakaoMap, place: Place) {
    val lat = place.y.toDoubleOrNull() ?: return
    val lng = place.x.toDoubleOrNull() ?: return
    
    val cameraUpdate = CameraUpdateFactory.newCenterPosition(LatLng.from(lat, lng))
    kakaoMap.moveCamera(cameraUpdate)
}

private fun openNavigation(context: Context, latitude: Double, longitude: Double, placeName: String) {
    // 카카오맵 앱으로 길찾기
    val kakaoMapUri = "kakaomap://route?ep=$latitude,$longitude&by=CAR"
    val kakaoIntent = Intent(Intent.ACTION_VIEW, Uri.parse(kakaoMapUri))
    
    if (kakaoIntent.resolveActivity(context.packageManager) != null) {
        context.startActivity(kakaoIntent)
        return
    }
    
    // 카카오맵이 없으면 네이버 지도 앱 시도
    val naverMapUri = "nmap://route/car?dlat=$latitude&dlng=$longitude&dname=${Uri.encode(placeName)}"
    val naverIntent = Intent(Intent.ACTION_VIEW, Uri.parse(naverMapUri))
    
    if (naverIntent.resolveActivity(context.packageManager) != null) {
        context.startActivity(naverIntent)
        return
    }
    
    // 길찾기 앱이 없으면 웹 브라우저로 카카오맵 길찾기 페이지 열기
    try {
        val webUrl = "https://map.kakao.com/link/to/${Uri.encode(placeName)},$latitude,$longitude"
        val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse(webUrl))
        context.startActivity(webIntent)
    } catch (e: Exception) {
        android.util.Log.e("MapScreen", "웹 브라우저로 길찾기 열기 실패: ${e.message}")
        Toast.makeText(context, "길찾기를 열 수 없습니다.", Toast.LENGTH_SHORT).show()
    }
}

private fun sortAndGroupPlacesByFoodType(
    searchResults: MutableList<Place>,
    foodNames: List<String>
) {
    // 음식종류별로 그룹화
    val grouped = searchResults.groupBy { it.foodType ?: "기타" }
    
    // 음식종류 순서 유지 (foodNames 순서대로)
    val sortedFoodTypes = foodNames + (grouped.keys - foodNames.toSet())
    
    // 거리순으로 정렬하고 음식종류별로 재구성
    val sortedResults = mutableListOf<Place>()
    sortedFoodTypes.forEach { foodType ->
        val places = grouped[foodType] ?: emptyList()
        // 거리순으로 정렬 (거리가 있는 것 우선, 그 다음 거리 가까운 순)
        val sorted = places.sortedWith(
            compareBy(
                { it.distance.isNullOrBlank() }, // 거리 없는 것 먼저
                { it.distance?.toDoubleOrNull() ?: Double.MAX_VALUE } // 거리 가까운 순
            )
        )
        sortedResults.addAll(sorted)
    }
    
    searchResults.clear()
    searchResults.addAll(sortedResults)
}

private fun searchRestaurants(
    context: Context,
    foodNames: List<String>,
    latitude: Double,
    longitude: Double,
    mapApiHelper: MapApiHelper,
    kakaoMap: KakaoMap,
    normalMarkerStyle: LabelStyles?,
    markerStylesCache: MutableMap<String, LabelStyles>,
    placeMarkers: MutableMap<Label, Place>,
    placeToLabelMap: MutableMap<String, Label>,
    searchResults: MutableList<Place>,
    onStart: () -> Unit,
    onComplete: () -> Unit,
    onError: (String) -> Unit
) {
    // 기존 마커 제거
    clearMarkers(placeMarkers, placeToLabelMap)
    searchResults.clear()
    
    onStart()
    
    var completedSearches = 0
    val totalSearches = foodNames.size
    
    // 각 음식별로 검색
    foodNames.forEach { foodName ->
        val searchQuery = "$foodName 음식점"
        mapApiHelper.searchPlaces(
            query = searchQuery,
            foodType = foodName,
            latitude = latitude,
            longitude = longitude,
            onSuccess = { places ->
                places.forEach { place ->
                    // 마커 추가
                    addMarker(
                        context = context,
                        kakaoMap = kakaoMap,
                        place = place,
                        normalMarkerStyle = normalMarkerStyle,
                        markerStylesCache = markerStylesCache,
                        placeMarkers = placeMarkers,
                        placeToLabelMap = placeToLabelMap
                    )
                    
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
                
                completedSearches++
                if (completedSearches >= totalSearches) {
                    onComplete()
                }
            },
            onError = { errorMsg ->
                completedSearches++
                if (completedSearches >= totalSearches) {
                    onComplete()
                }
                if (completedSearches == 1) {
                    onError(errorMsg)
                }
            }
        )
    }
}

private fun clearMarkers(
    placeMarkers: MutableMap<Label, Place>,
    placeToLabelMap: MutableMap<String, Label>
) {
    val labelsToRemove = placeMarkers.keys.toList()
    labelsToRemove.forEach { label ->
        try {
            val removeMethod = label.javaClass.getMethod("remove")
            removeMethod.invoke(label)
        } catch (e: Exception) {
            android.util.Log.e("MapScreen", "마커 제거 실패: ${e.message}")
        }
    }
    
    placeMarkers.clear()
    placeToLabelMap.clear()
} 