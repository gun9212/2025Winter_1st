package com.example.foodworldcup.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.foodworldcup.data.Food
import com.example.foodworldcup.data.FoodRepository
import com.example.foodworldcup.ui.compose.AppNavigation
import com.example.foodworldcup.ui.compose.FoodWorldCupTheme
import com.example.foodworldcup.ui.compose.ResultScreen
import com.example.foodworldcup.utils.PreferenceManager

/**
 * 합격된 음식들을 나열하는 Activity입니다.
 * 음식 사진과 이름을 나열하고, 다음 화면(지도)으로 넘어가는 버튼을 제공합니다.
 */
class ResultActivity : ComponentActivity() {

    private lateinit var preferenceManager: PreferenceManager
    
    // GameActivity로부터 전달받은 합격된 음식 리스트
    private var passedFoods: List<Food> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // PreferenceManager 초기화
        preferenceManager = PreferenceManager(this)

        // FoodRepository 초기화 (아직 초기화되지 않은 경우)
        if (FoodRepository.getFoodList().isEmpty()) {
            FoodRepository.initialize(this)
        }

        // GameActivity로부터 전달받은 합격된 음식 ID 리스트를 가져옵니다.
        val passedFoodIds = intent.getIntegerArrayListExtra("passed_food_ids") ?: emptyList()
        
        // 음식 ID로 FoodRepository에서 음식 리스트를 가져옵니다.
        passedFoods = passedFoodIds.mapNotNull { id ->
            FoodRepository.getFoodById(id)
        }
        
        // 지도 검색을 위해 최종 선택된 음식 ID 저장 (통과한 음식이 있을 때만)
        if (passedFoods.isNotEmpty()) {
            preferenceManager.saveFinalFoodIds(passedFoodIds)
        }
        
        setContent {
            FoodWorldCupTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // AppNavigation을 사용하여 Result 화면 표시
                    AppNavigationWithResult(
                        initialPassedFoods = passedFoods,
                        preferenceManager = preferenceManager,
                        onBackClick = { finish() }
                    )
                }
            }
        }
    }
}

@Composable
private fun AppNavigationWithResult(
    initialPassedFoods: List<Food>,
    preferenceManager: PreferenceManager,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current

    // MainActivity로 이동하여 AppNavigation의 NavigationBar 사용
    LaunchedEffect(Unit) {
        val intent = Intent(context, MainActivity::class.java)
        intent.putExtra("navigate_to", "result")
        val foodIds = initialPassedFoods.map { it.id }
        intent.putIntegerArrayListExtra("passed_food_ids", ArrayList(foodIds))
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(intent)
        onBackClick()
    }
}
