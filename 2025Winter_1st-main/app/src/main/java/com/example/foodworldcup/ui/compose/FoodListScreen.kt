package com.example.foodworldcup.ui.compose

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.example.foodworldcup.data.Food
import com.example.foodworldcup.data.FoodRepository
import com.example.foodworldcup.utils.PreferenceManager

/**
 * 음식 카테고리와 선택된 음식들을 관리하는 데이터 클래스
 */
data class FoodCategory(
    val name: String,
    val emoji: String?,
    val foods: List<Food>
)

/**
 * FoodListScreen - 음식 리스트를 카테고리별로 보여주고 선택할 수 있는 화면
 */
@Composable
fun FoodListScreen(
    onStartGameClick: (List<Food>) -> Unit = {}
) {
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme
    
    // PreferenceManager 초기화
    val preferenceManager = remember { PreferenceManager(context) }
    
    // FoodRepository에서 데이터 가져오기
    val allCategories = remember {
        FoodRepository.getAllCategories().map { categoryName ->
            val foods = FoodRepository.getFoodListByCategory(categoryName)
            // 카테고리별 이모지 (마땅한 이모지가 없으면 null)
            val emoji = when (categoryName) {
                "한식" -> "🍚"
                "중식" -> "🥟"
                "양식" -> "🍝"
                "일식" -> "🍣"
                "아시안" -> "🍜"
                else -> null
            }
            FoodCategory(categoryName, emoji, foods)
        }
    }
    
    // 선택된 음식 ID들을 관리하는 상태 (저장된 선택 불러오기)
    val selectedFoodIds = remember {
        mutableStateSetOf<Int>().apply {
            val savedFoodIds = preferenceManager.getSelectedFoodIds().toMutableSet()
            
            // 저장된 선택이 없으면 모든 음식을 기본 선택으로 설정
            if (savedFoodIds.isEmpty()) {
                val allFoods = FoodRepository.getFoodList()
                addAll(allFoods.map { it.id })
                // 기본 선택을 저장
                preferenceManager.saveSelectedFoodIds(allFoods.map { it.id })
            } else {
                addAll(savedFoodIds)
            }
        }
    }
    
    // 선택 저장 함수
    val saveSelectedFoods: () -> Unit = {
        preferenceManager.saveSelectedFoodIds(selectedFoodIds.toList())
    }
    
    // 화면이 사라질 때 선택 상태 저장 (이전 코드의 onPause와 동일)
    DisposableEffect(Unit) {
        onDispose {
            preferenceManager.saveSelectedFoodIds(selectedFoodIds.toList())
        }
    }
    
    // 카테고리별 펼침 상태 관리 (첫 번째 카테고리는 기본적으로 펼침)
    val expandedCategories = remember {
        mutableStateSetOf<String>().apply {
            if (allCategories.isNotEmpty()) {
                add(allCategories.first().name)
            }
        }
    }
    
    // 선택된 음식 개수
    val selectedCount = selectedFoodIds.size
    
    Scaffold(
        bottomBar = {
            StartGameButton(
                selectedCount = selectedCount,
                onClick = {
                    val selectedFoods = allCategories
                        .flatMap { it.foods }
                        .filter { selectedFoodIds.contains(it.id) }
                    onStartGameClick(selectedFoods)
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(colorScheme.background)
                .padding(paddingValues)
        ) {
            // 헤더
            Text(
                text = "Select Dishes",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = colorScheme.onBackground,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
            )
            
            Text(
                text = "Choose candidates for your bracket",
                fontSize = 14.sp,
                color = colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // 카테고리 리스트
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(allCategories) { category ->
                    CategoryCard(
                        category = category,
                        isExpanded = expandedCategories.contains(category.name),
                        selectedFoodIds = selectedFoodIds,
                        onExpandedChange = { isExpanded ->
                            if (isExpanded) {
                                expandedCategories.add(category.name)
                            } else {
                                expandedCategories.remove(category.name)
                            }
                        },
                        onCategoryToggle = { isSelected ->
                            if (isSelected) {
                                // 카테고리 전체 선택
                                selectedFoodIds.addAll(category.foods.map { it.id })
                            } else {
                                // 카테고리 전체 해제
                                selectedFoodIds.removeAll(category.foods.map { it.id })
                            }
                            // 선택 변경 시 저장
                            saveSelectedFoods()
                        },
                        onFoodToggle = { foodId, isSelected ->
                            if (isSelected) {
                                selectedFoodIds.add(foodId)
                            } else {
                                selectedFoodIds.remove(foodId)
                            }
                            // 선택 변경 시 저장
                            saveSelectedFoods()
                        }
                    )
                }
            }
        }
    }
}

/**
 * 카테고리 카드 (아코디언 UI)
 */
@Composable
private fun CategoryCard(
    category: FoodCategory,
    isExpanded: Boolean,
    selectedFoodIds: Set<Int>,
    onExpandedChange: (Boolean) -> Unit,
    onCategoryToggle: (Boolean) -> Unit,
    onFoodToggle: (Int, Boolean) -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    
    // 카테고리 내 모든 음식이 선택되었는지 확인
    val allSelected = category.foods.all { selectedFoodIds.contains(it.id) }
    val someSelected = category.foods.any { selectedFoodIds.contains(it.id) }
    val indeterminate = someSelected && !allSelected
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = colorScheme.surface
        )
    ) {
        Column {
            // 카테고리 헤더
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onExpandedChange(!isExpanded) }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 카테고리 체크박스
                TriStateCheckbox(
                    state = when {
                        allSelected -> ToggleableState.On
                        someSelected -> ToggleableState.Indeterminate
                        else -> ToggleableState.Off
                    },
                    onClick = { onCategoryToggle(!allSelected) },
                    modifier = Modifier.size(24.dp),
                    colors = CheckboxDefaults.colors(
                        checkedColor = colorScheme.primary
                    )
                )
                
                Spacer(modifier = Modifier.width(12.dp))
                
                // 카테고리 이모지 (있는 경우만 표시)
                if (category.emoji != null) {
                    Box(
                        modifier = Modifier.size(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = category.emoji,
                            fontSize = 20.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                }
                
                // 카테고리 이름
                Text(
                    text = category.name,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                
                // 펼침/접힘 아이콘
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) "접기" else "펼치기",
                    tint = colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            // 음식 리스트 (AnimatedVisibility로 애니메이션)
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column {
                    category.foods.forEach { food ->
                        FoodItem(
                            food = food,
                            isSelected = selectedFoodIds.contains(food.id),
                            onToggle = { isSelected ->
                                onFoodToggle(food.id, isSelected)
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * 음식 아이템
 */
@Composable
private fun FoodItem(
    food: Food,
    isSelected: Boolean,
    onToggle: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle(!isSelected) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 음식 체크박스
        Checkbox(
            checked = isSelected,
            onCheckedChange = onToggle,
            modifier = Modifier.size(24.dp),
            colors = CheckboxDefaults.colors(
                checkedColor = colorScheme.primary
            )
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        // 음식 이미지 (원형, 꽉 차게)
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            if (food.imagePath != null) {
                SubcomposeAsyncImage(
                    model = ImageRequest.Builder(context)
                        .data("file:///android_asset/${food.imagePath}")
                        .crossfade(true)
                        .build(),
                    contentDescription = food.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape),
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
            } else {
                Icon(
                    imageVector = Icons.Default.Restaurant,
                    contentDescription = null,
                    tint = colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        // 음식 이름
        Text(
            text = food.name,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
    }
}

/**
 * Start Game 버튼 (하단 고정)
 */
@Composable
private fun StartGameButton(
    selectedCount: Int,
    onClick: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        shape = RoundedCornerShape(28.dp),
        shadowElevation = 4.dp,
        color = colorScheme.surface
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            colorScheme.primary,
                            colorScheme.orangeGradientEnd
                        )
                    ),
                    shape = RoundedCornerShape(28.dp)
                )
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = colorScheme.onPrimary,
                    modifier = Modifier.size(24.dp)
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Text(
                    text = "Start Game ($selectedCount selected)",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.onPrimary
                )
            }
        }
    }
}
