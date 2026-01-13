package com.example.foodworldcup.ui.compose

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.Alignment
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.animation.core.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.compose.ui.platform.LocalContext
import com.example.foodworldcup.data.Food
import com.example.foodworldcup.data.FoodRepository
import com.example.foodworldcup.ui.FoodListActivity
import com.example.foodworldcup.ui.MyPageActivity
import com.example.foodworldcup.utils.ImageLoader
import com.example.foodworldcup.utils.PreferenceManager

/**
 * 앱의 네비게이션 라우트를 정의하는 sealed class
 */
sealed class Screen(val route: String, val title: String) {
    object Home : Screen("home", "Home")
    object List : Screen("list", "List")
    object Swipe : Screen("swipe", "Swipe")
    object MyPage : Screen("mypage", "MyPage")
    object Map : Screen("map", "Map")
    object Result : Screen("result", "Result")
}

/**
 * 앱의 메인 네비게이션 구조
 * Bottom Navigation Bar와 NavHost를 포함합니다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation(initialRoute: String? = null, initialMapFoodIds: List<Int>? = null, initialResultFoodIds: List<Int>? = null) {
    val context = LocalContext.current
    val navController = rememberNavController()
    val colorScheme = MaterialTheme.colorScheme
    
    // initialRoute에 따라 시작 화면 설정
    val startDestination = when (initialRoute) {
        "list" -> Screen.List.route
        "home" -> Screen.Home.route
        "mypage" -> Screen.MyPage.route
        "result" -> Screen.Result.route
        "map" -> Screen.Map.route
        else -> Screen.Home.route
    }
    
    // 최근 우승자 정보 로드
    val recentWinner = remember {
        loadRecentWinner(context)
    }
    
    // 선택된 음식 리스트 상태 관리 (FoodListScreen에서 SwipeScreen으로 전달)
    var selectedFoodsForGame by remember { mutableStateOf<List<com.example.foodworldcup.data.Food>?>(null) }
    
    // Map 화면으로 전달할 음식 ID 리스트 상태 관리
    var mapFoodIds by remember { mutableStateOf<List<Int>?>(initialMapFoodIds) }
    
    // Result 화면으로 전달할 음식 리스트 상태 관리
    var resultFoods by remember {
        mutableStateOf<List<com.example.foodworldcup.data.Food>?>(
            if (initialResultFoodIds != null) {
                initialResultFoodIds.mapNotNull { id ->
                    FoodRepository.getFoodById(id)
                }
            } else null
        )
    }
    
    // initialRoute가 "result"이고 initialResultFoodIds가 있으면 Result 화면으로 이동
    LaunchedEffect(initialRoute, initialResultFoodIds) {
        if (initialRoute == "result" && initialResultFoodIds != null) {
            val foods = initialResultFoodIds.mapNotNull { id ->
                FoodRepository.getFoodById(id)
            }
            if (foods.isNotEmpty()) {
                resultFoods = foods
                navController.navigate(Screen.Result.route) {
                    popUpTo(navController.graph.findStartDestination().id) {
                        saveState = true
                    }
                    launchSingleTop = true
                    restoreState = false
                }
            }
        }
    }
    
    // initialRoute가 "map"이고 initialMapFoodIds가 있으면 Map 화면으로 이동
    LaunchedEffect(initialRoute, initialMapFoodIds) {
        if (initialRoute == "map" && initialMapFoodIds != null) {
            mapFoodIds = initialMapFoodIds
            navController.navigate(Screen.Map.route) {
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = false
            }
        }
    }
    
    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = colorScheme.surface,
                modifier = Modifier.shadow(elevation = 8.dp)
            ) {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                
                // Bounce 애니메이션을 위한 상태
                val isHomeSelected = currentDestination?.hierarchy?.any { it.route == Screen.Home.route } == true
                val isListSelected = currentDestination?.hierarchy?.any { it.route == Screen.List.route } == true
                val isSwipeSelected = currentDestination?.hierarchy?.any { it.route == Screen.Swipe.route } == true
                val isMyPageSelected = currentDestination?.hierarchy?.any { it.route == Screen.MyPage.route } == true
                val isMapSelected = currentDestination?.hierarchy?.any { it.route == Screen.Map.route } == true
                val isResultSelected = currentDestination?.hierarchy?.any { it.route == Screen.Result.route } == true
                
                // 각 아이콘의 bounce 애니메이션
                val homeScale by animateFloatAsState(
                    targetValue = if (isHomeSelected) 1.2f else 1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    ),
                    label = "homeBounce"
                )
                val listScale by animateFloatAsState(
                    targetValue = if (isListSelected) 1.2f else 1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    ),
                    label = "listBounce"
                )
                val swipeScale by animateFloatAsState(
                    targetValue = if (isSwipeSelected) 1.2f else 1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    ),
                    label = "swipeBounce"
                )
                val myPageScale by animateFloatAsState(
                    targetValue = if (isMyPageSelected) 1.2f else 1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    ),
                    label = "myPageBounce"
                )
                val mapScale by animateFloatAsState(
                    targetValue = if (isMapSelected) 1.2f else 1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    ),
                    label = "mapBounce"
                )
                
                // 선택될 때 bounce 효과를 위해 잠시 scale을 키웠다가 원래대로
                LaunchedEffect(isHomeSelected) {
                    if (isHomeSelected) {
                        // spring 애니메이션이 자동으로 처리
                    }
                }
                
                NavigationBarItem(
                    icon = {
                        if (isHomeSelected) {
                            // 선택된 경우: 오렌지 원형 배경에 흰색 아이콘 + bounce 효과
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .scale(homeScale)
                                    .background(
                                        color = colorScheme.primary,
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Home,
                                    contentDescription = "Home",
                                    tint = colorScheme.onPrimary,
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
                    selected = isHomeSelected,
                    onClick = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = colorScheme.primary,
                        selectedTextColor = colorScheme.primary,
                        indicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                        unselectedIconColor = colorScheme.onSurfaceVariant,
                        unselectedTextColor = colorScheme.onSurfaceVariant
                    )
                )

                NavigationBarItem(
                    icon = {
                        if (isListSelected) {
                            // 선택된 경우: 오렌지 원형 배경에 흰색 아이콘 + bounce 효과
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .scale(listScale)
                                    .background(
                                        color = colorScheme.primary,
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.List,
                                    contentDescription = "List",
                                    tint = colorScheme.onPrimary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        } else {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.List,
                                contentDescription = "List",
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    },
                    label = { Text("List") },
                    selected = isListSelected,
                    onClick = {
                        navController.navigate(Screen.List.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = colorScheme.primary,
                        selectedTextColor = colorScheme.primary,
                        indicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                        unselectedIconColor = colorScheme.onSurfaceVariant,
                        unselectedTextColor = colorScheme.onSurfaceVariant
                    )
                )

                NavigationBarItem(
                    icon = {
                        if (isSwipeSelected) {
                            // 선택된 경우: 오렌지 원형 배경에 흰색 아이콘 + bounce 효과
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .scale(swipeScale)
                                    .background(
                                        color = colorScheme.primary,
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                SubcomposeAsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data("file:///android_asset/marker/swipe_누끼.png")
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = "Swipe",
                                    modifier = Modifier.size(24.dp),
                                    colorFilter = ColorFilter.tint(colorScheme.onPrimary),
                                    loading = {
                                        Icon(
                                            imageVector = Icons.Default.Gesture,
                                            contentDescription = "Swipe",
                                            modifier = Modifier.size(24.dp),
                                            tint = colorScheme.onPrimary
                                        )
                                    },
                                    error = {
                                        Icon(
                                            imageVector = Icons.Default.Gesture,
                                            contentDescription = "Swipe",
                                            modifier = Modifier.size(24.dp),
                                            tint = colorScheme.onPrimary
                                        )
                                    }
                                )
                            }
                        } else {
                            SubcomposeAsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data("file:///android_asset/marker/swipe_누끼.png")
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "Swipe",
                                modifier = Modifier.size(24.dp),
                                colorFilter = ColorFilter.tint(colorScheme.onSurfaceVariant),
                                loading = {
                                    Icon(
                                        imageVector = Icons.Default.Gesture,
                                        contentDescription = "Swipe",
                                        modifier = Modifier.size(24.dp),
                                        tint = colorScheme.onSurfaceVariant
                                    )
                                },
                                error = {
                                    Icon(
                                        imageVector = Icons.Default.Gesture,
                                        contentDescription = "Swipe",
                                        modifier = Modifier.size(24.dp),
                                        tint = colorScheme.onSurfaceVariant
                                    )
                                }
                            )
                        }
                    },
                    label = { Text("Swipe") },
                    selected = isSwipeSelected,
                    onClick = {
                        navController.navigate(Screen.Swipe.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = colorScheme.primary,
                        selectedTextColor = colorScheme.primary,
                        indicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                        unselectedIconColor = colorScheme.onSurfaceVariant,
                        unselectedTextColor = colorScheme.onSurfaceVariant
                    )
                )

                NavigationBarItem(
                    icon = {
                        if (isMyPageSelected) {
                            // 선택된 경우: 오렌지 원형 배경에 흰색 아이콘 + bounce 효과
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .scale(myPageScale)
                                    .background(
                                        color = colorScheme.primary,
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = "MyPage",
                                    tint = colorScheme.onPrimary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        } else {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "MyPage",
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    },
                    label = { Text("MyPage") },
                    selected = isMyPageSelected,
                    onClick = {
                        navController.navigate(Screen.MyPage.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = colorScheme.primary,
                        selectedTextColor = colorScheme.primary,
                        indicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                        unselectedIconColor = colorScheme.onSurfaceVariant,
                        unselectedTextColor = colorScheme.onSurfaceVariant
                    )
                )

                NavigationBarItem(
                    icon = {
                        if (isMapSelected) {
                            // 선택된 경우: 오렌지 원형 배경에 흰색 아이콘 + bounce 효과
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .scale(mapScale)
                                    .background(
                                        color = colorScheme.primary,
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Map,
                                    contentDescription = "Map",
                                    tint = colorScheme.onPrimary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        } else {
                            Icon(
                                imageVector = Icons.Default.Map,
                                contentDescription = "Map",
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    },
                    label = { Text("Map") },
                    selected = isMapSelected,
                    onClick = {
                        // Map 탭 클릭 시에는 저장된 최종 음식 ID 사용
                        val preferenceManager = PreferenceManager(context)
                        val savedFoodIds = preferenceManager.getFinalFoodIds()
                        mapFoodIds = if (savedFoodIds.isNotEmpty()) savedFoodIds else null
                        navController.navigate(Screen.Map.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = colorScheme.primary,
                        selectedTextColor = colorScheme.primary,
                        indicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                        unselectedIconColor = colorScheme.onSurfaceVariant,
                        unselectedTextColor = colorScheme.onSurfaceVariant
                    )
                )
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(paddingValues)
        ) {
            composable(Screen.Home.route) {
                IntroScreen(
                    onStartTournamentClick = {
                        navController.navigate(Screen.List.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onRecentWinnerClick = {
                        navController.navigate(Screen.MyPage.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    recentWinnerName = recentWinner?.name,
                    recentWinnerImage = recentWinner?.image
                )
            }
            
            composable(Screen.List.route) {
                FoodListScreen(
                    onStartGameClick = { selectedFoods ->
                        // 선택된 음식 리스트 저장하고 Swipe 화면으로 이동
                        if (selectedFoods.isNotEmpty()) {
                            selectedFoodsForGame = selectedFoods
                            navController.navigate(Screen.Swipe.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = false // Swipe 화면은 항상 새로운 게임 시작
                            }
                        }
                    }
                )
            }
            
            composable(Screen.Swipe.route) {
                SwipeScreen(
                    selectedFoods = selectedFoodsForGame
                )
            }
            
            composable(Screen.MyPage.route) {
                MyPageScreen()
            }
            
            composable(Screen.Map.route) {
                MapScreen(passedFoodIds = mapFoodIds)
            }
            
            composable(Screen.Result.route) {
                val preferenceManager = remember { PreferenceManager(context) }
                
                // resultFoods를 mutableStateListOf로 관리
                val resultFoodsList = remember { mutableStateListOf<Food>() }
                
                // initialResultFoodIds가 변경되면 resultFoodsList 업데이트
                LaunchedEffect(initialResultFoodIds) {
                    if (initialResultFoodIds != null) {
                        val foods = initialResultFoodIds.mapNotNull { id ->
                            FoodRepository.getFoodById(id)
                        }
                        resultFoodsList.clear()
                        resultFoodsList.addAll(foods)
                    } else {
                        val currentResultFoods = resultFoods
                        if (currentResultFoods != null) {
                            resultFoodsList.clear()
                            resultFoodsList.addAll(currentResultFoods)
                        }
                    }
                }
                
                // resultFoods가 변경되면 resultFoodsList 업데이트
                LaunchedEffect(resultFoods) {
                    val currentResultFoods = resultFoods
                    if (currentResultFoods != null) {
                        resultFoodsList.clear()
                        resultFoodsList.addAll(currentResultFoods)
                    }
                }
                
                ResultScreen(
                    passedFoods = resultFoodsList,
                    onBackClick = {
                        navController.popBackStack()
                    },
                    onViewOnMapClick = {
                        if (resultFoodsList.isNotEmpty()) {
                            val foodIds = resultFoodsList.map { it.id }
                            mapFoodIds = foodIds
                            navController.navigate(Screen.Map.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = false
                            }
                        }
                    },
                    onRetryClick = {
                        navController.navigate(Screen.List.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onMyPageClick = {
                        navController.navigate(Screen.MyPage.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onRemoveFood = { food ->
                        // resultFoodsList에서 제거
                        resultFoodsList.remove(food)
                        
                        // resultFoods도 업데이트
                        resultFoods = if (resultFoodsList.isNotEmpty()) resultFoodsList.toList() else null
                        
                        // PreferenceManager 업데이트
                        val updatedFoodIds = resultFoodsList.map { it.id }
                        if (updatedFoodIds.isNotEmpty()) {
                            preferenceManager.saveFinalFoodIds(updatedFoodIds)
                        } else {
                            preferenceManager.saveFinalFoodIds(emptyList())
                        }
                    }
                )
            }
        }
    }
}

/**
 * PreferenceManager에서 최근 선택된 음식을 불러오는 함수입니다.
 * 기록이 없으면 null을 반환합니다.
 */
private fun loadRecentWinner(context: android.content.Context): RecentWinner? {
    val preferenceManager = PreferenceManager(context)
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
