package com.example.foodworldcup.ui.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.example.foodworldcup.data.Food
import com.example.foodworldcup.data.FoodRepository
import com.example.foodworldcup.data.MapSelectedFood
import com.example.foodworldcup.utils.DateFormatter
import com.example.foodworldcup.utils.ImageLoader
import com.example.foodworldcup.utils.KakaoMapHelper
import com.example.foodworldcup.utils.PreferenceManager
import java.util.*

/**
 * MyPageScreen - 마이페이지 화면
 */
@Composable
fun MyPageScreen() {
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme
    val preferenceManager = remember { PreferenceManager(context) }
    
    // 선택된 음식 리스트 가져오기
    val mapSelectedFoods = remember { mutableStateListOf<MapSelectedFood>() }
    val selectedFoodDetails = remember { mutableStateMapOf<Long, FoodDetailItem>() }
    
    // BottomSheet 상태
    var selectedFoodDetail by remember { mutableStateOf<FoodDetailItem?>(null) }
    var showBottomSheet by remember { mutableStateOf(false) }
    
    // 초기 데이터 로드
    LaunchedEffect(Unit) {
        val foods = preferenceManager.getMapSelectedFoods()
        mapSelectedFoods.clear()
        mapSelectedFoods.addAll(foods.sortedByDescending { it.selectedDate })
        
        // 각 음식의 상세 정보 로드
        foods.forEach { mapFood ->
            val food = FoodRepository.getFoodById(mapFood.foodId)
            if (food != null) {
                val characterImagePath = if (!food.characterImagePath.isNullOrEmpty()) {
                    ImageLoader.getCharacterImagePaths(
                        food.characterImagePath,
                        food.name,
                        food.category
                    ).firstOrNull { path ->
                        try {
                            val stream = context.assets.open(path)
                            stream.close()
                            true
                        } catch (e: Exception) {
                            false
                        }
                    } ?: food.characterImagePath
                } else {
                    null
                }
                
                selectedFoodDetails[mapFood.id] = FoodDetailItem(
                    mapSelectedFood = mapFood,
                    food = food,
                    characterImagePath = characterImagePath
                )
            }
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colorScheme.background)
    ) {
        // 상단 프로필 영역 (1/3 정도)
        ProfileHeaderSection(
            totalCount = mapSelectedFoods.size,
            lastPlayedTime = if (mapSelectedFoods.isNotEmpty()) {
                mapSelectedFoods.first().selectedDate
            } else null
        )
        
        // 음식 리스트 그리드
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(mapSelectedFoods) { mapFood ->
                val detail = selectedFoodDetails[mapFood.id]
                if (detail != null) {
                    FoodPlateItem(
                        foodDetail = detail,
                        onClick = {
                            selectedFoodDetail = detail
                            showBottomSheet = true
                        }
                    )
                } else {
                    // 빈 접시 (데이터 로딩 중)
                    EmptyPlateItem()
                }
            }
        }
    }
    
    // BottomSheet
    if (showBottomSheet && selectedFoodDetail != null) {
        FoodDetailBottomSheet(
            foodDetail = selectedFoodDetail!!,
            preferenceManager = preferenceManager,
            onDismiss = { showBottomSheet = false },
            onDelete = { deletedId ->
                preferenceManager.removeMapSelectedFood(deletedId)
                mapSelectedFoods.removeAll { it.id == deletedId }
                selectedFoodDetails.remove(deletedId)
                showBottomSheet = false
            },
            onMemoUpdate = { updatedId ->
                // 메모 업데이트 후 리스트 새로고침
                val updatedFoods = preferenceManager.getMapSelectedFoods()
                mapSelectedFoods.clear()
                mapSelectedFoods.addAll(updatedFoods.sortedByDescending { it.selectedDate })
                
                // 상세 정보도 업데이트
                updatedFoods.forEach { mapFood ->
                    if (mapFood.id == updatedId) {
                        val food = FoodRepository.getFoodById(mapFood.foodId)
                        if (food != null) {
                            val characterImagePath = if (!food.characterImagePath.isNullOrEmpty()) {
                                ImageLoader.getCharacterImagePaths(
                                    food.characterImagePath,
                                    food.name,
                                    food.category
                                ).firstOrNull { path ->
                                    try {
                                        val stream = context.assets.open(path)
                                        stream.close()
                                        true
                                    } catch (e: Exception) {
                                        false
                                    }
                                } ?: food.characterImagePath
                            } else {
                                null
                            }
                            
                            selectedFoodDetails[updatedId] = FoodDetailItem(
                                mapSelectedFood = mapFood,
                                food = food,
                                characterImagePath = characterImagePath
                            )
                        }
                    }
                }
            }
        )
    }
}

/**
 * 음식 상세 정보를 담는 데이터 클래스
 */
private data class FoodDetailItem(
    val mapSelectedFood: MapSelectedFood,
    val food: Food,
    val characterImagePath: String?
)

/**
 * 상단 프로필 헤더 섹션
 */
@Composable
private fun ProfileHeaderSection(
    totalCount: Int,
    lastPlayedTime: Long?
) {
    val colorScheme = MaterialTheme.colorScheme
    
    // 마지막 플레이 시간 계산
    val lastPlayedText = remember(lastPlayedTime) {
        if (lastPlayedTime == null) {
            "No records"
        } else {
            val now = System.currentTimeMillis()
            val diff = now - lastPlayedTime
            val hours = diff / (1000 * 60 * 60)
            if (hours < 1) {
                "Last played just now"
            } else if (hours < 24) {
                "Last played ${hours}h ago"
            } else {
                val days = hours / 24
                "Last played ${days}d ago"
            }
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colorScheme.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = "Accepted Food History",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = colorScheme.onBackground
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 통계 칩
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = colorScheme.primaryContainer,
                modifier = Modifier.padding(0.dp)
            ) {
                Text(
                    text = "$totalCount Dishes Collected",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
            
            // 구분점
            Text(
                text = "•",
                fontSize = 14.sp,
                color = colorScheme.onSurfaceVariant
            )
            
            // 마지막 플레이 시간
            Text(
                text = lastPlayedText,
                fontSize = 14.sp,
                color = colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * 음식 접시 아이템
 */
@Composable
private fun FoodPlateItem(
    foodDetail: FoodDetailItem,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 접시 배경과 캐릭터 이미지
        Box(
            modifier = Modifier.size(120.dp),
            contentAlignment = Alignment.Center
        ) {
            // 접시 배경 이미지
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(context)
                    .data("file:///android_asset/plate.png")
                    .build(),
                contentDescription = "접시",
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
            
            // 캐릭터 누끼 이미지
            if (foodDetail.characterImagePath != null) {
                SubcomposeAsyncImage(
                    model = ImageRequest.Builder(context)
                        .data("file:///android_asset/${foodDetail.characterImagePath}")
                        .crossfade(true)
                        .build(),
                    contentDescription = foodDetail.food.name,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(80.dp),
                    loading = {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = colorScheme.primary,
                            strokeWidth = 2.dp
                        )
                    },
                    error = {
                        Icon(
                            imageVector = Icons.Default.Restaurant,
                            contentDescription = null,
                            tint = colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // 음식 이름
        Text(
            text = foodDetail.food.name,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = colorScheme.onBackground,
            maxLines = 1
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        
        // 날짜
        Text(
            text = DateFormatter.dateFormatShort.format(Date(foodDetail.mapSelectedFood.selectedDate)),
            fontSize = 12.sp,
            color = colorScheme.onSurfaceVariant
        )
    }
}

/**
 * 빈 접시 아이템 (로딩 중)
 */
@Composable
private fun EmptyPlateItem() {
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme
    
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(120.dp),
            contentAlignment = Alignment.Center
        ) {
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(context)
                    .data("file:///android_asset/plate.png")
                    .build(),
                contentDescription = "빈 접시",
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

/**
 * 음식 상세 정보 BottomSheet
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FoodDetailBottomSheet(
    foodDetail: FoodDetailItem,
    preferenceManager: PreferenceManager,
    onDismiss: () -> Unit,
    onDelete: (Long) -> Unit,
    onMemoUpdate: (Long) -> Unit
) {
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme
    
    // 메모 상태 (가게별 메모 가져오기)
    val initialMemo = remember(foodDetail.mapSelectedFood.placeName) {
        preferenceManager.getPlaceMemo(foodDetail.mapSelectedFood.placeName) ?: foodDetail.mapSelectedFood.memo
    }
    var memoText by remember { mutableStateOf(initialMemo) }
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = colorScheme.surface,
        dragHandle = {
            Box(
                modifier = Modifier
                    .width(40.dp)
                    .height(4.dp)
                    .padding(vertical = 12.dp)
                    .background(
                        color = colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(2.dp)
                    )
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 음식 이름
            Text(
                text = foodDetail.food.name,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = colorScheme.onSurface
            )
            
            // 선택 날짜
            Text(
                text = DateFormatter.dateFormatLong.format(Date(foodDetail.mapSelectedFood.selectedDate)),
                fontSize = 14.sp,
                color = colorScheme.onSurfaceVariant
            )
            
            Divider()
            
            // 식당 이름 (클릭 가능)
            if (foodDetail.mapSelectedFood.placeName.isNotEmpty() && 
                foodDetail.mapSelectedFood.placeName != "정보 없음") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            KakaoMapHelper.openKakaoMapDetail(context, foodDetail.mapSelectedFood)
                        },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = foodDetail.mapSelectedFood.placeName,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = colorScheme.primary
                        )
                        if (foodDetail.mapSelectedFood.placeAddress.isNotEmpty()) {
                            Text(
                                text = foodDetail.mapSelectedFood.placeAddress,
                                fontSize = 14.sp,
                                color = colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.Default.OpenInNew,
                        contentDescription = "카카오맵 열기",
                        tint = colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            } else {
                Text(
                    text = "식당 정보 없음",
                    fontSize = 16.sp,
                    color = colorScheme.onSurfaceVariant
                )
            }
            
            Divider()
            
            // 메모 섹션
            Text(
                text = "Memo",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = colorScheme.onSurface
            )
            
            OutlinedTextField(
                value = memoText,
                onValueChange = { memoText = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        text = "메모를 입력하세요...",
                        color = colorScheme.onSurfaceVariant
                    )
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = colorScheme.primary,
                    unfocusedBorderColor = colorScheme.outline
                ),
                maxLines = 4,
                minLines = 2
            )
            
            // 버튼들
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 삭제 버튼
                OutlinedButton(
                    onClick = {
                        onDelete(foodDetail.mapSelectedFood.id)
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = colorScheme.error
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("삭제")
                }
                
                // 저장 버튼
                Button(
                    onClick = {
                        preferenceManager.updateMapSelectedFoodMemo(
                            foodDetail.mapSelectedFood.id,
                            memoText
                        )
                        onMemoUpdate(foodDetail.mapSelectedFood.id)
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colorScheme.primary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Save,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("저장")
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
