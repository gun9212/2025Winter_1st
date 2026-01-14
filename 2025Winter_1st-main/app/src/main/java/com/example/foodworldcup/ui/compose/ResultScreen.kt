package com.example.foodworldcup.ui.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.example.foodworldcup.data.Food
import com.example.foodworldcup.data.FoodRepository
import com.example.foodworldcup.ui.compose.Screen
import com.example.foodworldcup.utils.PreferenceManager

/**
 * 결과 화면의 메인 Composable
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultScreen(
    navController: NavController? = null,
    onBackClick: () -> Unit,
    onViewOnMapClick: () -> Unit,
    onRetryClick: () -> Unit,
    onMyPageClick: () -> Unit
) {
    val context = LocalContext.current
    val preferenceManager = remember { PreferenceManager(context) }
    val colorScheme = MaterialTheme.colorScheme
    
    // resultFoods를 mutableStateListOf로 관리
    val resultFoodsList = remember { mutableStateListOf<Food>() }
    
    // PreferenceManager에서 직접 불러오기
    LaunchedEffect(Unit) {
        val savedFoodIds = preferenceManager.getFinalFoodIds()
        if (savedFoodIds.isNotEmpty()) {
            val foods = savedFoodIds.mapNotNull { id ->
                FoodRepository.getFoodById(id)
            }
            resultFoodsList.clear()
            resultFoodsList.addAll(foods)
        }
    }
    
    // 삭제 확인 다이얼로그를 위한 state
    var foodToRemove by remember { mutableStateOf<Food?>(null) }
    
    // 음식 제거 핸들러
    val onRemoveFood: (Food) -> Unit = { food ->
        resultFoodsList.remove(food)
        preferenceManager.removeFoodFromFinalFoodIds(food.id)
    }
    
    // View on Map 클릭 핸들러
    val onViewOnMapClickInternal: () -> Unit = {
        if (resultFoodsList.isNotEmpty()) {
            val foodIds = resultFoodsList.map { it.id }
            preferenceManager.saveFinalFoodIds(foodIds)
            onViewOnMapClick()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "추린 메뉴들 (${resultFoodsList.size})",
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        // navController가 있으면 직접 사용, 없으면 콜백 사용
                        if (navController != null) {
                            if (!navController.popBackStack()) {
                                // popBackStack이 실패하면 Swipe 화면으로 navigate
                                navController.navigate(Screen.Swipe.route) {
                                    launchSingleTop = true
                                }
                            }
                        } else {
                            onBackClick()
                        }
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "뒤로가기",
                            tint = Color.Black
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(colorScheme.background)
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            // 음식 그리드 리스트
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .weight(1f)
                    .padding(top = 16.dp)
            ) {
                items(resultFoodsList) { food ->
                    FoodGridItem(
                        food = food,
                        onRemoveClick = {
                            foodToRemove = food
                        }
                    )
                }
            }
            
            // 하단 버튼 영역
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // View on Map 버튼
                Button(
                    onClick = onViewOnMapClickInternal,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colorScheme.primary
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Map,
                        contentDescription = null,
                        tint = colorScheme.onPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "지도에서 식당 확인하기",
                        color = colorScheme.onPrimary,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                // Retry와 My Page 버튼
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onRetryClick,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color.White,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "Retry",
                            fontWeight = FontWeight.Medium
                        )
                    }
                    
                    OutlinedButton(
                        onClick = onMyPageClick,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color.White,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "My Page",
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
        
        // 삭제 확인 다이얼로그
        foodToRemove?.let { food ->
            AlertDialog(
                onDismissRequest = { foodToRemove = null },
                title = {
                    Text("음식 제거")
                },
                text = {
                    Text("'${food.name}'을(를) 목록에서 제거하시겠습니까?")
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            onRemoveFood(food)
                            foodToRemove = null
                        }
                    ) {
                        Text("제거")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { foodToRemove = null }
                    ) {
                        Text("취소")
                    }
                }
            )
        }
    }
}

/**
 * 음식 그리드 아이템
 */
@Composable
private fun FoodGridItem(
    food: Food,
    onRemoveClick: () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme
    
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        // 이미지 카드
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(16.dp))
        ) {
            if (food.imagePath != null) {
                // 음식 이미지
                SubcomposeAsyncImage(
                    model = ImageRequest.Builder(context)
                        .data("file:///android_asset/${food.imagePath}")
                        .crossfade(true)
                        .build(),
                    loading = {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = colorScheme.primary
                            )
                        }
                    },
                    error = {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "🍽️",
                                fontSize = 32.sp
                            )
                        }
                    },
                    contentDescription = food.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                // 이미지 경로가 없을 경우
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "🍽️",
                        fontSize = 32.sp
                    )
                }
            }
            
            // 우측 상단 닫기 버튼
            IconButton(
                onClick = onRemoveClick,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = (-4).dp, y = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(
                            color = Color.Black.copy(alpha = 0.5f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Remove",
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
        
        // 음식 이름
        Text(
            text = food.name,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = Color.Black,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

