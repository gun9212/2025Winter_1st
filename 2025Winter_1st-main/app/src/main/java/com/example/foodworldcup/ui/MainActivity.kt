package com.example.foodworldcup.ui

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.foodworldcup.data.FoodRepository
import com.example.foodworldcup.ui.compose.AppNavigation
import com.example.foodworldcup.ui.compose.FoodWorldCupTheme
import com.example.foodworldcup.ui.compose.SplashScreen
import com.example.foodworldcup.utils.PreferenceManager
import com.kakao.sdk.common.util.Utility
import kotlinx.coroutines.delay

/**
 * 앱의 메인 Activity입니다.
 * AppNavigation을 통해 전체 네비게이션 구조를 관리합니다.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Activity가 처음 생성되는 경우에만 스플래시 표시
        val isFirstLaunch = savedInstanceState == null

        // FoodRepository 초기화 (JSON 파일에서 데이터 로드)
        FoodRepository.initialize(this)

        // 해시 키 추출 및 로그 출력
        val keyHash = Utility.getKeyHash(this)
        Log.d("KakaoKeyHash", "현재 키 해시값: $keyHash")

        // Intent에서 특정 탭으로 이동할지 확인
        val navigateTo = intent.getStringExtra("navigate_to")
        
        // Intent에서 전달할 음식 ID 리스트 확인 (PreferenceManager에 저장)
        val passedFoodIds = intent.getIntegerArrayListExtra("passed_food_ids")
        if (passedFoodIds != null && passedFoodIds.isNotEmpty()) {
            val preferenceManager = PreferenceManager(this)
            preferenceManager.saveFinalFoodIds(passedFoodIds.toList())
        }

        setContent {
            FoodWorldCupTheme {
                // Activity가 처음 생성될 때만 스플래시 표시
                var showSplash by remember { mutableStateOf(isFirstLaunch) }
                
                LaunchedEffect(Unit) {
                    if (isFirstLaunch) {
                        delay(1500) // 1.5초 대기
                        showSplash = false
                    } else {
                        // 재생성된 경우 즉시 스플래시 숨김
                        showSplash = false
                    }
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
        }
    }
}
