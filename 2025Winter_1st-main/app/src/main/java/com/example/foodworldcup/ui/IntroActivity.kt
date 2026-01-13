package com.example.foodworldcup.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.foodworldcup.data.FoodRepository
import com.example.foodworldcup.ui.compose.IntroScreen
import com.example.foodworldcup.ui.compose.BottomNavTab
import com.example.foodworldcup.utils.ImageLoader
import com.example.foodworldcup.utils.PreferenceManager
import com.kakao.sdk.common.util.Utility

/**
 * 앱의 첫 화면(인트로 화면)을 담당하는 Activity입니다.
 * Jetpack Compose를 사용하여 UI를 구현합니다.
 */
class IntroActivity : ComponentActivity() {

    private val preferenceManager: PreferenceManager by lazy {
        PreferenceManager(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // FoodRepository 초기화 (JSON 파일에서 데이터 로드)
        FoodRepository.initialize(this)

        // 해시 키 추출 및 로그 출력
        val keyHash = Utility.getKeyHash(this)
        Log.d("KakaoKeyHash", "현재 키 해시값: $keyHash")

        // 최근 우승자 정보 로드
        val recentWinner = loadRecentWinner()

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    IntroScreen(
                        onStartTournamentClick = {
                            val intent = Intent(this, FoodListActivity::class.java)
                            startActivity(intent)
                        },
                        onRecentWinnerClick = {
                            val intent = Intent(this, MyPageActivity::class.java)
                            startActivity(intent)
                        },
                        recentWinnerName = recentWinner?.name,
                        recentWinnerImage = recentWinner?.image
                    )
                }
            }
        }
    }

    /**
     * PreferenceManager에서 최근 선택된 음식을 불러오는 함수입니다.
     * 기록이 없으면 null을 반환합니다.
     */
    private fun loadRecentWinner(): RecentWinner? {
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
                        val stream = assets.open(path)
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
}
