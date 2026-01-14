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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.material3.LocalRippleConfiguration
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
import androidx.compose.foundation.interaction.MutableInteractionSource
import com.example.foodworldcup.data.Food
import com.example.foodworldcup.data.FoodRepository
import com.example.foodworldcup.ui.FoodListActivity
import com.example.foodworldcup.ui.MyPageActivity
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
fun AppNavigation(initialRoute: String? = null) {
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
                CompositionLocalProvider(LocalRippleConfiguration provides null) {
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
                            // MapScreen에서 PreferenceManager에서 직접 불러오므로 네비게이션만 수행
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
                    }
                )
            }
            
            composable(Screen.List.route) {
                FoodListScreen(
                    onStartGameClick = { selectedFoods ->
                        // FoodListScreen에서 이미 PreferenceManager에 저장됨
                        // 네비게이션만 수행
                        if (selectedFoods.isNotEmpty()) {
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
                SwipeScreen(navController = navController)
            }
            
            composable(Screen.MyPage.route) {
                MyPageScreen()
            }
            
            composable(Screen.Map.route) {
                MapScreen(
                    onNavigateToMyPage = {
                        navController.navigate(Screen.MyPage.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
            
            composable(Screen.Result.route) {
                ResultScreen(
                    navController = navController,
                    onBackClick = {
                        // Swipe 화면으로 돌아가기 (popBackStack이 안 되면 navigate)
                        if (!navController.popBackStack()) {
                            navController.navigate(Screen.Swipe.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    },
                    onViewOnMapClick = {
                        navController.navigate(Screen.Map.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = false
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
                    }
                )
            }
        }
    }
}