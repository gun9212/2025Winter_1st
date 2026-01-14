package com.example.foodworldcup.ui.compose

import android.util.Log
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.foodworldcup.data.FoodRepository
import com.example.foodworldcup.utils.PreferenceManager
import com.kakao.sdk.common.util.Utility
import kotlinx.coroutines.delay

/**
 * 앱의 시작 화면 Composable
 * SplashScreen을 표시하고 AppNavigation으로 전환합니다.
 */
@Composable
fun StartScreen(navigateTo: String? = null) {
    val context = LocalContext.current
    val activity = context as? ComponentActivity
    
    // Intent에서 passedFoodIds 추출
    val passedFoodIds = remember {
        activity?.intent?.getIntegerArrayListExtra("passed_food_ids")?.toList()
    }
    
    // FoodRepository 초기화
    LaunchedEffect(Unit) {
        FoodRepository.initialize(context)
        
        // Kakao 키 해시 로그
        val keyHash = Utility.getKeyHash(context)
        Log.d("KakaoKeyHash", "현재 키 해시값: $keyHash")
        
        // passedFoodIds 처리
        if (passedFoodIds != null && passedFoodIds.isNotEmpty()) {
            val preferenceManager = PreferenceManager(context)
            preferenceManager.saveFinalFoodIds(passedFoodIds)
        }
    }
    
    // SplashScreen 표시 후 AppNavigation으로 전환
    var showSplash by remember { mutableStateOf(true) }
    
    LaunchedEffect(Unit) {
        delay(1500) // 1.5초 대기
        showSplash = false
    }
    
    if (showSplash) {
        SplashScreen()
    } else {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            AppNavigation(initialRoute = navigateTo)
        }
    }
}
