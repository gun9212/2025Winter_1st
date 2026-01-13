package com.example.foodworldcup.ui.compose

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.example.foodworldcup.data.Food
import com.example.foodworldcup.data.FoodRepository
import com.example.foodworldcup.game.GameStateManager
import com.example.foodworldcup.ui.ResultActivity
import com.example.foodworldcup.utils.PreferenceManager
import android.content.Intent
import kotlinx.coroutines.launch

/**
 * 스와이프 방향
 */
private enum class SwipeDirection {
    Like,
    Pass
}

/**
 * 스와이프 액션 (버튼 클릭 시 사용)
 */
private enum class SwipeAction {
    Like,
    Pass
}

/**
 * SwipeScreen - 토너먼트 스와이프 화면
 */
@Composable
fun SwipeScreen() {
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme
    val preferenceManager = remember { PreferenceManager(context) }
    
    // FoodRepository 초기화 확인
    LaunchedEffect(Unit) {
        if (FoodRepository.getFoodList().isEmpty()) {
            FoodRepository.initialize(context)
        }
    }
    
    // PreferenceManager에서 선택된 음식 ID 불러오기
    val initialFoods = remember {
        val selectedFoodIds = preferenceManager.getSelectedFoodIds()
        if (selectedFoodIds.isNotEmpty()) {
            selectedFoodIds.mapNotNull { id ->
                FoodRepository.getFoodById(id)
            }.shuffled()
        } else {
            FoodRepository.getFoodList().shuffled()
        }
    }
    
    // GameStateManager 초기화
    val gameStateManager = remember {
        GameStateManager(initialFoods)
    }
    
    // 남은 음식 리스트 상태 (gameStateManager가 변경되면 업데이트)
    var remainingFoods by remember(gameStateManager) {
        mutableStateOf(gameStateManager.getRemainingFoods())
    }
    
    // gameStateManager가 변경될 때 remainingFoods 업데이트
    LaunchedEffect(gameStateManager) {
        remainingFoods = gameStateManager.getRemainingFoods()
    }
    
    // 초기 총 개수
    val initialTotalCount = remember(initialFoods) { initialFoods.size }
    
    // 진행 상황 계산
    val completed = initialTotalCount - remainingFoods.size
    val progress = if (initialTotalCount > 0) completed.toFloat() / initialTotalCount else 0f
    
    // 스와이프 트리거 상태 (버튼 클릭 시 사용)
    var swipeTrigger by remember { mutableStateOf<SwipeAction?>(null) }
    
    // 스킵 다이얼로그 상태
    var showSkipDialog by remember { mutableStateOf(false) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colorScheme.background)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 상단 헤더 (TOURNAMENT, 진행 상황 등)
        TournamentHeader(
            progress = progress,
            currentProgress = "$completed/$initialTotalCount",
            onSkipClick = {
                val passedFoods = gameStateManager.getPassedFoods()
                if (passedFoods.isEmpty()) {
                    android.widget.Toast.makeText(
                        context,
                        "합격된 음식이 없습니다. 최소 하나 이상 선택해주세요.",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                } else {
                    showSkipDialog = true
                }
            }
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // 카드 스택 (높이 제한하여 버튼과 겹치지 않도록)
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            if (remainingFoods.isNotEmpty()) {
                // 뒤에 있는 카드들 (최대 2개) - 역순으로 배치하여 zIndex 효과 구현
                val visibleCards = minOf(3, remainingFoods.size)
                for (i in (visibleCards - 1) downTo 0) {
                    if (i < remainingFoods.size) {
                        val food = remainingFoods[i]
                        val isTopCard = i == 0
                        
                        // key를 사용하여 각 카드를 독립적으로 관리
                        key(food.id) {
                            SwipeableCard(
                                food = food,
                                isTopCard = isTopCard,
                                swipeTrigger = if (isTopCard) swipeTrigger else null,
                                onSwipeLeft = {
                                    // Pass (왼쪽으로 스와이프)
                                    if (isTopCard && remainingFoods.isNotEmpty()) {
                                        val swipedFood = remainingFoods[0]
                                        gameStateManager.swipeCard(
                                            com.yuyakaido.android.cardstackview.Direction.Left,
                                            swipedFood
                                        )
                                        remainingFoods = gameStateManager.getRemainingFoods()
                                        swipeTrigger = null
                                        
                                        // 게임 종료 확인
                                        if (gameStateManager.isGameFinished()) {
                                            finishGame(context, gameStateManager)
                                        }
                                    }
                                },
                                onSwipeRight = {
                                    // Like (오른쪽으로 스와이프)
                                    if (isTopCard && remainingFoods.isNotEmpty()) {
                                        val swipedFood = remainingFoods[0]
                                        gameStateManager.swipeCard(
                                            com.yuyakaido.android.cardstackview.Direction.Right,
                                            swipedFood
                                        )
                                        remainingFoods = gameStateManager.getRemainingFoods()
                                        swipeTrigger = null
                                        
                                        // 게임 종료 확인
                                        if (gameStateManager.isGameFinished()) {
                                            finishGame(context, gameStateManager)
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            } else {
                // 카드가 없을 때
                Text(
                    text = "모든 카드를 확인했습니다!",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = colorScheme.onSurfaceVariant
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 액션 버튼들
        ActionButtons(
            onPassClick = {
                // X 버튼 클릭 - 왼쪽으로 스와이프
                if (remainingFoods.isNotEmpty()) {
                    swipeTrigger = SwipeAction.Pass
                }
            },
            onUndoClick = {
                // 되감기 버튼 클릭
                if (gameStateManager.canRewind()) {
                    gameStateManager.rewind()
                    remainingFoods = gameStateManager.getRemainingFoods()
                }
            },
            onLikeClick = {
                // 하트 버튼 클릭 - 오른쪽으로 스와이프
                if (remainingFoods.isNotEmpty()) {
                    swipeTrigger = SwipeAction.Like
                }
            },
            canUndo = gameStateManager.canRewind()
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 스킵 다이얼로그
        SkipGameDialog(
            showDialog = showSkipDialog,
            passedFoodCount = gameStateManager.getPassedFoods().size,
            onConfirm = {
                showSkipDialog = false
                finishGameWithPassedFoods(context, gameStateManager.getPassedFoods())
            },
            onDismiss = {
                showSkipDialog = false
            }
        )
    }
}

/**
 * 토너먼트 헤더
 */
@Composable
private fun TournamentHeader(
    progress: Float,
    currentProgress: String,
    onSkipClick: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "TOURNAMENT",
                fontSize = 12.sp,
                color = colorScheme.primary,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = currentProgress,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(8.dp))
            // 진행 바
            LinearProgressIndicator(
                progress = progress,
                modifier = Modifier
                    .width(200.dp)
                    .height(4.dp),
                color = colorScheme.primary,
                trackColor = colorScheme.surfaceVariant
            )
        }
        
        TextButton(onClick = onSkipClick) {
            Text("Skip", color = colorScheme.primary)
        }
    }
}

/**
 * 스와이프 가능한 카드
 */
@Composable
private fun SwipeableCard(
    food: Food,
    isTopCard: Boolean,
    swipeTrigger: SwipeAction?,
    onSwipeLeft: () -> Unit,
    onSwipeRight: () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme
    val scope = rememberCoroutineScope()
    
    // 카드 위치 및 회전 상태 (food.id를 key로 사용하여 각 카드가 독립적으로 관리되도록)
    val offsetX = remember(food.id) { Animatable(0f) }
    val offsetY = remember(food.id) { Animatable(0f) }
    val rotation = remember(food.id) { Animatable(0f) }
    
    // 스와이프 방향 표시 (Like/Nope)
    var swipeDirection by remember(food.id) { mutableStateOf<SwipeDirection?>(null) }
    
    // 스와이프 완료 여부 (카드가 화면 밖으로 나갔는지)
    var isSwipedOut by remember(food.id) { mutableStateOf(false) }
    
    // food가 변경될 때 상태 리셋
    LaunchedEffect(food.id) {
        offsetX.snapTo(0f)
        offsetY.snapTo(0f)
        rotation.snapTo(0f)
        swipeDirection = null
        isSwipedOut = false
    }
    
    // 버튼 클릭으로 트리거된 스와이프 처리
    LaunchedEffect(swipeTrigger) {
        if (isTopCard && swipeTrigger != null && !isSwipedOut) {
            val screenWidth = context.resources.displayMetrics.widthPixels.toFloat()
            when (swipeTrigger) {
                SwipeAction.Pass -> {
                    // 왼쪽으로 스와이프
                    swipeDirection = SwipeDirection.Pass
                    isSwipedOut = true
                    // 즉시 콜백 호출하여 다음 카드 활성화
                    onSwipeLeft()
                    // 애니메이션은 백그라운드에서 계속 진행
                    offsetX.animateTo(-screenWidth * 1.5f, spring())
                    rotation.animateTo(-30f, spring())
                }
                SwipeAction.Like -> {
                    // 오른쪽으로 스와이프
                    swipeDirection = SwipeDirection.Like
                    isSwipedOut = true
                    // 즉시 콜백 호출하여 다음 카드 활성화
                    onSwipeRight()
                    // 애니메이션은 백그라운드에서 계속 진행
                    offsetX.animateTo(screenWidth * 1.5f, spring())
                    rotation.animateTo(30f, spring())
                }
            }
        }
    }
    
    // 드래그 제스처 처리
    val dragModifier = if (isTopCard && !isSwipedOut) {
        Modifier.pointerInput(food.id) {
            var isDragging = false
            detectDragGestures(
                onDragStart = { offset ->
                    isDragging = true
                },
                onDragEnd = {
                    // 드래그 종료 시 스와이프 판정
                    if (isDragging && !isSwipedOut) {
                        val threshold = size.width * 0.3f
                        scope.launch {
                            if (offsetX.value > threshold) {
                                // 오른쪽으로 충분히 스와이프 - Like
                                swipeDirection = SwipeDirection.Like
                                isSwipedOut = true
                                // 즉시 콜백 호출하여 다음 카드 활성화
                                onSwipeRight()
                                // 애니메이션은 백그라운드에서 계속 진행
                                offsetX.animateTo(size.width * 1.5f, spring())
                                rotation.animateTo(30f, spring())
                            } else if (offsetX.value < -threshold) {
                                // 왼쪽으로 충분히 스와이프 - Pass
                                swipeDirection = SwipeDirection.Pass
                                isSwipedOut = true
                                // 즉시 콜백 호출하여 다음 카드 활성화
                                onSwipeLeft()
                                // 애니메이션은 백그라운드에서 계속 진행
                                offsetX.animateTo(-size.width * 1.5f, spring())
                                rotation.animateTo(-30f, spring())
                            } else {
                                // 원래 위치로 복귀
                                offsetX.animateTo(0f, spring())
                                offsetY.animateTo(0f, spring())
                                rotation.animateTo(0f, spring())
                                swipeDirection = null
                            }
                        }
                        isDragging = false
                    }
                },
                onDragCancel = {
                    // 드래그 취소 시 원래 위치로 복귀
                    if (isDragging && !isSwipedOut) {
                        scope.launch {
                            offsetX.animateTo(0f, spring())
                            offsetY.animateTo(0f, spring())
                            rotation.animateTo(0f, spring())
                            swipeDirection = null
                        }
                        isDragging = false
                    }
                }
            ) { change, dragAmount ->
                if (!isSwipedOut) {
                    change.consume()
                    scope.launch {
                        offsetX.snapTo(offsetX.value + dragAmount.x)
                        offsetY.snapTo(offsetY.value + dragAmount.y)
                        
                        // 회전 계산 (드래그 거리에 비례, GameActivity의 setMaxDegree(20.0f) 참고)
                        val rotationAmount = offsetX.value / size.width * 20f
                        rotation.snapTo(rotationAmount)
                        
                        // 스와이프 방향 표시 (GameActivity의 setSwipeThreshold(0.3f) 참고)
                        val threshold = size.width * 0.2f
                        swipeDirection = when {
                            offsetX.value > threshold -> SwipeDirection.Like
                            offsetX.value < -threshold -> SwipeDirection.Pass
                            else -> null
                        }
                    }
                }
            }
        }
    } else {
        Modifier
    }
    
    // 스와이프된 카드는 화면에 표시하지 않음
    if (!isSwipedOut) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.92f) // 화면 너비의 92% (더 넓게)
                .aspectRatio(0.85f) // 가로:세로 비율 (가로가 더 넓게, 높이 줄임)
                .heightIn(max = 240.dp) // 최대 높이 제한
                .offset(
                    x = offsetX.value.dp,
                    y = offsetY.value.dp
                )
                .rotate(rotation.value)
                .shadow(
                    elevation = if (isTopCard) 8.dp else 4.dp,
                    shape = RoundedCornerShape(16.dp)
                )
                .clip(RoundedCornerShape(16.dp))
                .then(dragModifier),
            contentAlignment = Alignment.BottomCenter
        ) {
            // 배경 이미지
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(context)
                    .data("file:///android_asset/${food.imagePath}")
                    .crossfade(true)
                    .build(),
                contentDescription = food.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                loading = {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = colorScheme.primary)
                    }
                },
                error = {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Restaurant,
                            contentDescription = null,
                            tint = colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }
            )
            
            // 하단 그라데이션 오버레이
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.7f)
                            )
                        )
                    )
            )
            
            // 음식 이름 텍스트
            Text(
                text = food.name,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(16.dp)
            )
            
            // 스와이프 방향 표시 (Like/Nope)
            swipeDirection?.let { direction ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = when (direction) {
                        SwipeDirection.Like -> Alignment.TopEnd
                        SwipeDirection.Pass -> Alignment.TopStart
                    }
                ) {
                    Text(
                        text = when (direction) {
                            SwipeDirection.Like -> "LIKE"
                            SwipeDirection.Pass -> "NOPE"
                        },
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = when (direction) {
                            SwipeDirection.Like -> Color(0xFF4CAF50)
                            SwipeDirection.Pass -> Color(0xFFF44336)
                        },
                        modifier = Modifier
                            .rotate(if (direction == SwipeDirection.Pass) -20f else 20f)
                            .padding(16.dp)
                    )
                }
            }
        }
    }
}

/**
 * 액션 버튼들
 */
@Composable
private fun ActionButtons(
    onPassClick: () -> Unit,
    onUndoClick: () -> Unit,
    onLikeClick: () -> Unit,
    canUndo: Boolean
) {
    val colorScheme = MaterialTheme.colorScheme
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Pass 버튼 (X)
        IconButton(
            onClick = onPassClick,
            modifier = Modifier
                .size(64.dp)
                .background(
                    color = Color(0xFFF44336).copy(alpha = 0.1f),
                    shape = CircleShape
                )
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Pass",
                tint = Color(0xFFF44336),
                modifier = Modifier.size(32.dp)
            )
        }
        
        // Undo 버튼
        IconButton(
            onClick = onUndoClick,
            enabled = canUndo,
            modifier = Modifier
                .size(64.dp)
                .background(
                    color = colorScheme.surfaceVariant.copy(alpha = if (canUndo) 1f else 0.3f),
                    shape = CircleShape
                )
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = "Undo",
                tint = if (canUndo) colorScheme.primary else colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                modifier = Modifier.size(32.dp)
            )
        }
        
        // Like 버튼 (하트)
        IconButton(
            onClick = onLikeClick,
            modifier = Modifier
                .size(64.dp)
                .background(
                    color = Color(0xFF4CAF50).copy(alpha = 0.1f),
                    shape = CircleShape
                )
        ) {
            Icon(
                imageVector = Icons.Default.Favorite,
                contentDescription = "Like",
                tint = Color(0xFF4CAF50),
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

/**
 * 게임 종료 처리 함수
 */
private fun finishGame(
    context: android.content.Context,
    gameStateManager: GameStateManager
) {
    val passedFoods = gameStateManager.getPassedFoods()
    
    if (passedFoods.isEmpty()) {
        android.widget.Toast.makeText(
            context,
            "합격된 음식이 없습니다. 다시 시작하세요.",
            android.widget.Toast.LENGTH_SHORT
        ).show()
        return
    }
    
    // ResultActivity로 이동
    val intent = Intent(context, ResultActivity::class.java)
    val passedFoodIds = passedFoods.map { it.id }
    intent.putIntegerArrayListExtra("passed_food_ids", ArrayList(passedFoodIds))
    // MainActivity를 백스택에서 제거하고 ResultActivity 시작
    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
    context.startActivity(intent)
    // MainActivity 종료
    (context as? android.app.Activity)?.finish()
}

/**
 * 스킵 기능 다이얼로그
 */
@Composable
private fun SkipGameDialog(
    showDialog: Boolean,
    passedFoodCount: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    if (showDialog) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Text("게임 스킵")
            },
            text = {
                Text("현재까지 합격된 ${passedFoodCount}개의 음식만 가지고 진행하시겠습니까?")
            },
            confirmButton = {
                TextButton(onClick = onConfirm) {
                    Text("스킵")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("취소")
                }
            }
        )
    }
}

/**
 * 합격된 음식 리스트를 가지고 게임을 종료합니다.
 */
private fun finishGameWithPassedFoods(
    context: android.content.Context,
    passedFoods: List<Food>
) {
    if (passedFoods.isEmpty()) {
        android.widget.Toast.makeText(
            context,
            "합격된 음식이 없습니다. 다시 시작하세요.",
            android.widget.Toast.LENGTH_SHORT
        ).show()
        return
    }
    
    // ResultActivity로 이동
    val intent = Intent(context, ResultActivity::class.java)
    val passedFoodIds = passedFoods.map { it.id }
    intent.putIntegerArrayListExtra("passed_food_ids", ArrayList(passedFoodIds))
    // MainActivity를 백스택에서 제거하고 ResultActivity 시작
    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
    context.startActivity(intent)
    // MainActivity 종료
    (context as? android.app.Activity)?.finish()
}
