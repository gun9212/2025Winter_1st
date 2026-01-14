package com.example.foodworldcup.ui.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.draw.clip
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import android.graphics.BitmapFactory
import com.example.foodworldcup.data.FoodRepository
import com.example.foodworldcup.utils.BitmapUtils
import com.example.foodworldcup.utils.ImageLoader
import com.example.foodworldcup.utils.PreferenceManager
import com.example.foodworldcup.R
import com.example.foodworldcup.ui.compose.AppColors

/**
 * Intro 화면의 메인 Composable
 */
@Composable
fun IntroScreen(
    onStartTournamentClick: () -> Unit,
    onRecentWinnerClick: () -> Unit
) {
    val context = LocalContext.current
    val preferenceManager = remember { PreferenceManager(context) }
    val colorScheme = MaterialTheme.colorScheme
    
    // Recent Winner 데이터 로드
    val recentWinner = remember {
        loadRecentWinner(context, preferenceManager)
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colorScheme.background)
            .padding(horizontal = 24.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // 메인 제목과 서브타이틀
        MainTitleSection()
        
        Spacer(modifier = Modifier.height(20.dp))
        
        // How it works 섹션
        HowItWorksSection()
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Start Tournament 버튼
        StartTournamentButton(
            onClick = onStartTournamentClick
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Recent Winner 섹션
        if (recentWinner != null) {
            YesterdaysWinnerSection(
                foodName = recentWinner.name,
                foodImage = recentWinner.image,
                onClick = onRecentWinnerClick
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
    }
}

/**
 * 메인 제목 섹션: 큰 제목과 서브타이틀
 */
@Composable
private fun MainTitleSection() {
    val colorScheme = MaterialTheme.colorScheme
    val context = LocalContext.current
    
    // 이미지 크기 설정 (작게 조정)
    val imageSizeDp = 120.dp
    
    // 메추리_누끼.png 이미지 로드 (메추리_누끼2.png가 없으므로 기존 파일 사용)
    val characterBitmap = remember {
        try {
            val assetStream = context.assets.open("메추리_누끼.png")
            val bitmap = BitmapFactory.decodeStream(assetStream)
            assetStream.close()
            android.util.Log.d("IntroScreen", "메추리_누끼.png 로드 성공")
            bitmap
        } catch (e: Exception) {
            android.util.Log.e("IntroScreen", "이미지 로드 실패: 메추리_누끼.png - ${e.message}")
            null
        }
    }
    
    // 색상 정의
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 40.dp, start = 0.dp, end = 24.dp),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 왼쪽: 메추리 캐릭터 이미지
        if (characterBitmap != null) {
            Image(
                bitmap = characterBitmap.asImageBitmap(),
                contentDescription = "메추리 캐릭터",
                modifier = Modifier
                    .size(imageSizeDp)
                    .offset(x = (12).dp)
                    .padding(end = 18.dp)
            )
        } else {
            // 이미지 로드 실패 시 플레이스홀더
            Box(
                modifier = Modifier
                    .size(imageSizeDp)
                    .background(
                        color = colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "🐣",
                    fontSize = 40.sp
                )
            }
        }
        
        // 오른쪽: 텍스트
        Column(
            modifier = Modifier
                .padding(start = 16.dp)
                .weight(1f)
        ) {
            // 첫 번째 줄: "메추리알!"
            Text(
                text = "메추리알!",
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = AppColors.DarkBrown,
                lineHeight = 52.sp
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // 두 번째 줄: "메뉴는 추려서 우리가 알려줄게!" (하이라이트 적용)
            Text(
                text = buildAnnotatedString {
                    val fullText = "메뉴는 추려서\n우리가 알려줄게!"
                    // 하이라이트할 글자의 인덱스 (0부터 시작)
                    val highlightIndices = setOf(0, 4, 9, 12) // "메", "추", "리", "알"
                    
                    fullText.forEachIndexed { index, char ->
                        if (highlightIndices.contains(index)) {
                            withStyle(
                                style = SpanStyle(
                                    color = colorScheme.onPrimary,
                                    fontSize = 26.sp
                                )
                            ) {
                                append(char)
                            }
                        } else {
                            append(char)
                        }
                    }
                },
                fontSize = 18.sp,
                color = Color.Gray,
                lineHeight = 28.sp,
                softWrap = false
            )
        }
    }
}

/**
 * How it works 섹션: 3개의 카드 (세로 배치)
 */
@Composable
private fun HowItWorksSection() {
    val colorScheme = MaterialTheme.colorScheme
    Column {
        Text(
            text = "어떻게 하나요?",
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        // Browse List 카드
        HowItWorksCard(
            icon = Icons.AutoMirrored.Filled.List,
            title = "추릴 메뉴를 골라주세요!",
            description = "아래 추릴 메뉴 고르기 버튼을 눌러\n후보군을 추려주세요",
            modifier = Modifier.padding(bottom = 10.dp)
        )
        
        // Swipe to Choose 카드
        HowItWorksCard(
            icon = Icons.Default.Gesture,
            imagePath = "marker/swipe_누끼.png",
            title = "합격과 불합격을 골라주세요!",
            description = "합격은 오른쪽, 불합격은 왼쪽으로\n밀어주세요",
            modifier = Modifier.padding(bottom = 10.dp)
        )
        
        // Find Restaurant 카드
        HowItWorksCard(
            icon = Icons.Default.Map,
            title = "추린 음식을 하는 식당을\n찾아보세요!",
            description = "지도에서 만나보실 수 있어요.",
            modifier = Modifier.padding(bottom = 10.dp)
        )
    }
}

/**
 * How it works 개별 카드 (가로 배치)
 */
@Composable
private fun HowItWorksCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    imagePath: String? = null,
    title: String,
    description: String,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val context = LocalContext.current
    
    // 이미지 로드 (imagePath가 있는 경우)
    val imageBitmap = remember(imagePath) {
        if (imagePath != null) {
            try {
                val assetStream = context.assets.open(imagePath)
                val bitmap = BitmapFactory.decodeStream(assetStream)
                assetStream.close()
                bitmap
            } catch (e: Exception) {
                android.util.Log.e("IntroScreen", "이미지 로드 실패: $imagePath - ${e.message}")
                null
            }
        } else {
            null
        }
    }
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 2.dp,
                shape = RoundedCornerShape(12.dp)
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 아이콘 또는 이미지 (연한 아이보리 원형 배경)
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        color = colorScheme.surfaceVariant,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (imageBitmap != null) {
                    // 이미지가 있으면 이미지 표시
                    Image(
                        bitmap = imageBitmap.asImageBitmap(),
                        contentDescription = title,
                        modifier = Modifier.size(24.dp),
                        colorFilter = ColorFilter.tint(colorScheme.onPrimary)
                    )
                } else {
                    // 이미지가 없으면 아이콘 표시
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = colorScheme.onPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            // 텍스트
            Column {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colorScheme.onBackground,
                    lineHeight = 16.sp
                )
                Text(
                    text = description,
                    fontSize = 12.sp,
                    color = colorScheme.onSurfaceVariant,
                    lineHeight = 12.sp
                )
            }
        }
    }
}

/**
 * Recent Winner 섹션
 */
@Composable
private fun YesterdaysWinnerSection(
    foodName: String,
    foodImage: String?,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val colorScheme = MaterialTheme.colorScheme
    
    // 목표 크기 (64.dp)
    val targetSizeDp = 64.dp
    val targetSizePx = with(density) { targetSizeDp.toPx().toInt() }
    
    // 캐릭터 이미지 로드 및 스케일링
    val scaledCharacterBitmap = remember(foodImage, targetSizePx) {
        if (foodImage != null) {
            try {
                val assetStream = context.assets.open(foodImage)
                val originalBitmap = BitmapFactory.decodeStream(assetStream)
                assetStream.close()
                
                if (originalBitmap != null) {
                    // BitmapUtils를 사용하여 크기 통일
                    BitmapUtils.scaleBitmapToFitContent(
                        originalBitmap = originalBitmap,
                        targetWidth = targetSizePx,
                        targetHeight = targetSizePx,
                        targetAreaRatio = 0.75f,
                        centerYRatio = 0.5f,
                        alignBottom = false
                    )
                } else {
                    null
                }
            } catch (e: Exception) {
                null
            }
        } else {
            null
        }
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 2.dp,
                shape = RoundedCornerShape(12.dp)
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = colorScheme.background
        ),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 원형 이미지 (캐릭터누끼 이미지)
            Box(
                modifier = Modifier
                    .size(targetSizeDp)
                    .background(
                        color = colorScheme.outlineVariant,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (scaledCharacterBitmap != null) {
                    Image(
                        bitmap = scaledCharacterBitmap.asImageBitmap(),
                        contentDescription = foodName,
                        modifier = Modifier
                            .size(targetSizeDp)
                            .clip(CircleShape)
                    )
                } else {
                    // 이미지 로드 실패 시 이모지 표시
                    Text(
                        text = "🍜",
                        fontSize = 32.sp
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // 텍스트
            Column {
                Text(
                    text = "Recent Winner",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.primary,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(1.dp))
                Text(
                    text = foodName,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.onBackground
                )
            }
        }
    }
}

/**
 * Start Tournament 버튼 (오렌지 그라데이션, 더 둥근 모서리)
 */
@Composable
private fun StartTournamentButton(
    onClick: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(28.dp)
            )
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        colorScheme.primary, // 오렌지 시작
                        colorScheme.primary  // 더 밝은 오렌지 끝
                    )
                ),
                shape = RoundedCornerShape(28.dp)
            )
            .clip(RoundedCornerShape(28.dp)),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clickable(onClick = onClick),
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
                text = "추릴 메뉴 고르기!",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = colorScheme.onPrimary
            )
        }
    }
}

/**
 * PreferenceManager에서 최근 선택된 음식을 불러오는 함수입니다.
 * 기록이 없으면 null을 반환합니다.
 */
private fun loadRecentWinner(
    context: android.content.Context,
    preferenceManager: PreferenceManager
): RecentWinner? {
    val mapSelectedFoods = preferenceManager.getMapSelectedFoods()
    
    if (mapSelectedFoods.isEmpty()) {
        return null
    }
    
    // 가장 최근 기록 가져오기 (날짜 기준 내림차순 정렬)
    val recentFood = mapSelectedFoods.sortedByDescending { it.selectedDate }.first()
    
    // 음식 정보 가져오기
    val food = FoodRepository.getFoodById(recentFood.foodId)
    
    return if (food != null) {
        // 캐릭터 이미지 경로 찾기
        val characterImagePath = if (!food.characterImagePath.isNullOrEmpty()) {
            val pathsToTry = ImageLoader.getCharacterImagePaths(
                food.characterImagePath,
                food.name,
                food.category
            )
            // 첫 번째로 찾은 유효한 경로 반환
            pathsToTry.firstOrNull { path ->
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
        
        RecentWinner(
            name = food.name,
            image = characterImagePath
        )
    } else {
        null
    }
}

/**
 * 최근 우승자 데이터 클래스
 */
private data class RecentWinner(
    val name: String,
    val image: String?
)