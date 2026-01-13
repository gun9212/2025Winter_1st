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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.text.style.TextAlign
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.draw.clip

/**
 * Intro 화면의 메인 Composable
 */
@Composable
fun IntroScreen(
    onStartTournamentClick: () -> Unit,
    onRecentWinnerClick: () -> Unit,
    recentWinnerName: String? = null,
    recentWinnerImage: String? = null
) {
    Scaffold(
        bottomBar = {
            BottomNavigationBar(
                selectedTab = BottomNavTab.HOME,
                onTabSelected = { /* 네비게이션 처리 */ }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF8F8F8))
                .padding(paddingValues)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // 메인 제목과 서브타이틀
            MainTitleSection()
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // How it works 섹션
            HowItWorksSection()
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Start Tournament 버튼
            StartTournamentButton(
                onClick = onStartTournamentClick
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Recent Winner 섹션
            if (recentWinnerName != null) {
                YesterdaysWinnerSection(
                    foodName = recentWinnerName,
                    foodImage = recentWinnerImage,
                    onClick = onRecentWinnerClick
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

/**
 * 상단 헤더: 왼쪽 아이콘+제목, 오른쪽 프로필+설정 아이콘
 */
@Composable
private fun TopHeaderSection() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp, start = 24.dp, end = 24.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 왼쪽: 포크/나이프 아이콘 + "Food Tournament"
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Restaurant,
                contentDescription = null,
                tint = Color(0xFFFF7F3E),
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Food Tournament",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF333333)
            )
        }
        
        // 오른쪽: 프로필 + 설정 아이콘
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = "Profile",
                tint = Color(0xFF888888),
                modifier = Modifier.size(24.dp)
            )
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Settings",
                tint = Color(0xFF888888),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

/**
 * 메인 제목 섹션: 큰 제목과 서브타이틀
 */
@Composable
private fun MainTitleSection() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Food Tournament",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF333333),
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        Text(
            text = "Pick today's meal in minutes",
            fontSize = 14.sp,
            color = Color(0xFF888888),
            textAlign = TextAlign.Center
        )
    }
}

/**
 * How it works 섹션: 3개의 카드 (세로 배치)
 */
@Composable
private fun HowItWorksSection() {
    Column {
        Text(
            text = "How it works",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF333333),
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        // Browse List 카드
        HowItWorksCard(
            icon = Icons.AutoMirrored.Filled.List,
            title = "Browse List",
            description = "Explore local favorites",
            modifier = Modifier.padding(bottom = 12.dp)
        )
        
        // Swipe to Choose 카드
        HowItWorksCard(
            icon = Icons.Default.Gesture,
            title = "Swipe to Choose",
            description = "Vote on matches",
            modifier = Modifier.padding(bottom = 12.dp)
        )
        
        // Find Restaurant 카드
        HowItWorksCard(
            icon = Icons.Default.Place,
            title = "Find Restaurant",
            description = "Get directions",
            modifier = Modifier.padding(bottom = 0.dp)
        )
    }
}

/**
 * How it works 개별 카드 (가로 배치)
 */
@Composable
private fun HowItWorksCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 2.dp,
                shape = RoundedCornerShape(12.dp)
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFFFFFF)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 아이콘 (연한 아이보리 원형 배경)
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        color = Color(0xFFF5F0E8), // 더 연한 아이보리색
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = Color(0xFFFF7F3E), // 주황색
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // 텍스트
            Column {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF333333)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    fontSize = 14.sp,
                    color = Color(0xFF888888)
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
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 2.dp,
                shape = RoundedCornerShape(12.dp)
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFF8F8F8)
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
                    .size(64.dp)
                    .background(
                        color = Color(0xFFE0E0E0),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (foodImage != null) {
                    SubcomposeAsyncImage(
                        model = ImageRequest.Builder(context)
                            .data("file:///android_asset/$foodImage")
                            .crossfade(true)
                            .build(),
                        contentDescription = foodName,
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape),
                        loading = {
                            // 로딩 중 표시
                            CircularProgressIndicator(
                                modifier = Modifier.size(32.dp),
                                color = Color(0xFFFF7F3E),
                                strokeWidth = 2.dp
                            )
                        },
                        error = {
                            // 이미지 로드 실패 시 이모지 표시
                            Text(
                                text = "🍜",
                                fontSize = 32.sp
                            )
                        }
                    )
                } else {
                    // 이미지 경로가 없을 경우 이모지 표시
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
                    color = Color(0xFFFF7F3E),
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = foodName,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF333333)
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
                        Color(0xFFFF7F3E), // 오렌지 시작
                        Color(0xFFFF9F6E)  // 더 밝은 오렌지 끝
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
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Start Tournament",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

/**
 * 하단 네비게이션 바
 */
@Composable
private fun BottomNavigationBar(
    selectedTab: BottomNavTab,
    onTabSelected: (BottomNavTab) -> Unit
) {
    NavigationBar(
        containerColor = Color(0xFFFFFFFF),
        modifier = Modifier.shadow(elevation = 8.dp)
    ) {
        NavigationBarItem(
            icon = {
                if (selectedTab == BottomNavTab.HOME) {
                    // 선택된 경우: 오렌지 원형 배경에 흰색 아이콘
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                color = Color(0xFFFF7F3E), // 기존 주황색
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Home,
                            contentDescription = "Home",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                } else {
                    Icon(
                        imageVector = Icons.Default.Home,
                        contentDescription = "Home",
                        modifier = Modifier.size(24.dp)
                    )
                }
            },
            label = { Text("Home") },
            selected = selectedTab == BottomNavTab.HOME,
            onClick = { onTabSelected(BottomNavTab.HOME) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color(0xFFFF7F3E), // 기존 주황색
                selectedTextColor = Color(0xFFFF7F3E),
                selectedContainerColor = Color.Transparent, // 선택된 항목 배경 제거
                unselectedIconColor = Color(0xFF888888),
                unselectedTextColor = Color(0xFF888888),
                indicatorColor = Color.Transparent // 인디케이터 배경 제거
            )
        )
        
        NavigationBarItem(
            icon = {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.List,
                    contentDescription = "List",
                    modifier = Modifier.size(24.dp)
                )
            },
            label = { Text("List") },
            selected = selectedTab == BottomNavTab.LIST,
            onClick = { onTabSelected(BottomNavTab.LIST) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color(0xFFFF7F3E), // 기존 주황색
                selectedTextColor = Color(0xFFFF7F3E),
                selectedContainerColor = Color.Transparent, // 선택된 항목 배경 제거
                unselectedIconColor = Color(0xFF888888),
                unselectedTextColor = Color(0xFF888888),
                indicatorColor = Color.Transparent // 인디케이터 배경 제거
            )
        )
        
        NavigationBarItem(
            icon = {
                Icon(
                    imageVector = Icons.Default.Gesture,
                    contentDescription = "Swipe",
                    modifier = Modifier.size(24.dp)
                )
            },
            label = { Text("Swipe") },
            selected = selectedTab == BottomNavTab.SWIPE,
            onClick = { onTabSelected(BottomNavTab.SWIPE) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color(0xFFFF7F3E), // 기존 주황색
                selectedTextColor = Color(0xFFFF7F3E),
                selectedContainerColor = Color.Transparent, // 선택된 항목 배경 제거
                unselectedIconColor = Color(0xFF888888),
                unselectedTextColor = Color(0xFF888888),
                indicatorColor = Color.Transparent // 인디케이터 배경 제거
            )
        )
        
        NavigationBarItem(
            icon = {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "MyPage",
                    modifier = Modifier.size(24.dp)
                )
            },
            label = { Text("MyPage") },
            selected = selectedTab == BottomNavTab.MYPAGE,
            onClick = { onTabSelected(BottomNavTab.MYPAGE) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color(0xFFFF7F3E), // 기존 주황색
                selectedTextColor = Color(0xFFFF7F3E),
                selectedContainerColor = Color.Transparent, // 선택된 항목 배경 제거
                unselectedIconColor = Color(0xFF888888),
                unselectedTextColor = Color(0xFF888888),
                indicatorColor = Color.Transparent // 인디케이터 배경 제거
            )
        )
        
        NavigationBarItem(
            icon = {
                Icon(
                    imageVector = Icons.Default.Map,
                    contentDescription = "Map",
                    modifier = Modifier.size(24.dp)
                )
            },
            label = { Text("Map") },
            selected = selectedTab == BottomNavTab.MAP,
            onClick = { onTabSelected(BottomNavTab.MAP) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color(0xFFFF7F3E), // 기존 주황색
                selectedTextColor = Color(0xFFFF7F3E),
                selectedContainerColor = Color.Transparent, // 선택된 항목 배경 제거
                unselectedIconColor = Color(0xFF888888),
                unselectedTextColor = Color(0xFF888888),
                indicatorColor = Color.Transparent // 인디케이터 배경 제거
            )
        )
    }
}

/**
 * 하단 네비게이션 탭 enum
 */
enum class BottomNavTab {
    HOME, LIST, SWIPE, MYPAGE, MAP
}
