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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.example.foodworldcup.data.FoodRepository
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
}

/**
 * 앱의 메인 네비게이션 구조
 * Bottom Navigation Bar와 NavHost를 포함합니다.
 */
@Composable
fun AppNavigation() {
    val context = LocalContext.current
    val navController = rememberNavController()
    val colorScheme = MaterialTheme.colorScheme
    
    // 최근 우승자 정보 로드
    val recentWinner = remember {
        loadRecentWinner(context)
    }
    
    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = colorScheme.surface,
                modifier = Modifier.shadow(elevation = 8.dp)
            ) {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                
                NavigationBarItem(
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Home,
                            contentDescription = "Home",
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    label = { Text("Home") },
                    selected = currentDestination?.hierarchy?.any { it.route == Screen.Home.route } == true,
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
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.List,
                            contentDescription = "List",
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    label = { Text("List") },
                    selected = currentDestination?.hierarchy?.any { it.route == Screen.List.route } == true,
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
                        Icon(
                            imageVector = Icons.Default.Gesture,
                            contentDescription = "Swipe",
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    label = { Text("Swipe") },
                    selected = currentDestination?.hierarchy?.any { it.route == Screen.Swipe.route } == true,
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
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "MyPage",
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    label = { Text("MyPage") },
                    selected = currentDestination?.hierarchy?.any { it.route == Screen.MyPage.route } == true,
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
                        Icon(
                            imageVector = Icons.Default.Map,
                            contentDescription = "Map",
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    label = { Text("Map") },
                    selected = currentDestination?.hierarchy?.any { it.route == Screen.Map.route } == true,
                    onClick = {
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
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(paddingValues)
        ) {
            composable(Screen.Home.route) {
                IntroScreen(
                    onStartTournamentClick = {
                        // List Tab으로 이동
                        navController.navigate(Screen.List.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onRecentWinnerClick = {
                        val intent = Intent(context, MyPageActivity::class.java)
                        context.startActivity(intent)
                    },
                    recentWinnerName = recentWinner?.name,
                    recentWinnerImage = recentWinner?.image
                )
            }
            
            composable(Screen.List.route) {
                FoodListScreen(
                    onStartGameClick = { selectedFoods ->
                        // TODO: 게임 화면으로 이동하는 로직 구현
                        // val intent = Intent(context, GameActivity::class.java)
                        // context.startActivity(intent)
                    }
                )
            }
            
            composable(Screen.Swipe.route) {
                PlaceholderScreen("Swipe")
            }
            
            composable(Screen.MyPage.route) {
                MyPageScreen()
            }
            
            composable(Screen.Map.route) {
                PlaceholderScreen("Map")
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
