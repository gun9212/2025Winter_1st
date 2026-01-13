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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.foodworldcup.data.Food
import com.example.foodworldcup.data.FoodRepository
import com.example.foodworldcup.ui.compose.FoodWorldCupTheme
import com.example.foodworldcup.ui.compose.ResultNavTab
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
                    ResultScreenContent(
                        initialPassedFoods = passedFoods,
                        preferenceManager = preferenceManager,
                        onBackClick = { finish() },
                        onViewOnMapClick = { currentPassedFoods ->
                            if (currentPassedFoods.isEmpty()) {
                                Toast.makeText(this, "통과한 음식이 없습니다.", Toast.LENGTH_SHORT).show()
                            } else {
                                val intent = Intent(this, MapActivity::class.java)
                                val foodIds = currentPassedFoods.map { it.id }
                                intent.putIntegerArrayListExtra("passed_food_ids", ArrayList(foodIds))
                                startActivity(intent)
                            }
                        },
                        onRetryClick = {
                            // MainActivity로 이동하고 List 탭으로 이동
                            val intent = Intent(this, MainActivity::class.java)
                            intent.putExtra("navigate_to", "list")
                            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                            startActivity(intent)
                            finish()
                        },
                        onMyPageClick = {
                            val intent = Intent(this, MainActivity::class.java)
                            intent.putExtra("navigate_to", "mypage")
                            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                            startActivity(intent)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ResultScreenContent(
    initialPassedFoods: List<Food>,
    preferenceManager: PreferenceManager,
    onBackClick: () -> Unit,
    onViewOnMapClick: (List<Food>) -> Unit,
    onRetryClick: () -> Unit,
    onMyPageClick: () -> Unit
) {
    val context = LocalContext.current
    var selectedNavTab by remember { mutableStateOf<ResultNavTab?>(null) }
    
    // 음식 리스트를 mutableStateListOf로 관리하여 제거 가능하게 함
    val passedFoods = remember { mutableStateListOf<Food>().apply { addAll(initialPassedFoods) } }
    
    // 음식 제거 핸들러
    val onRemoveFood: (Food) -> Unit = { food ->
        passedFoods.remove(food)
        // PreferenceManager의 final_food_ids도 업데이트
        val updatedFoodIds = passedFoods.map { it.id }
        if (updatedFoodIds.isNotEmpty()) {
            preferenceManager.saveFinalFoodIds(updatedFoodIds)
        } else {
            // 모든 음식이 제거되면 빈 리스트 저장
            preferenceManager.saveFinalFoodIds(emptyList())
        }
    }
    
    ResultScreen(
        passedFoods = passedFoods,
        onBackClick = onBackClick,
        onViewOnMapClick = { onViewOnMapClick(passedFoods.toList()) },
        onRetryClick = onRetryClick,
        onMyPageClick = onMyPageClick,
        onRemoveFood = onRemoveFood,
        onNavTabSelected = { tab ->
            selectedNavTab = tab
            // 네비게이션 탭 클릭 시 해당 화면으로 이동
            when (tab) {
                ResultNavTab.HOME -> {
                    val intent = Intent(context, MainActivity::class.java)
                    intent.putExtra("navigate_to", "home")
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                    context.startActivity(intent)
                    // finish()는 하지 않음 - 사용자가 뒤로가기로 돌아올 수 있도록
                }
                ResultNavTab.LIST -> {
                    val intent = Intent(context, MainActivity::class.java)
                    intent.putExtra("navigate_to", "list")
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                    context.startActivity(intent)
                }
                ResultNavTab.SWIPE -> {
                    // Swipe 탭은 게임이 진행 중일 때만 의미가 있으므로 List로 이동
                    val intent = Intent(context, MainActivity::class.java)
                    intent.putExtra("navigate_to", "list")
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                    context.startActivity(intent)
                }
                ResultNavTab.MYPAGE -> {
                    val intent = Intent(context, MainActivity::class.java)
                    intent.putExtra("navigate_to", "mypage")
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                    context.startActivity(intent)
                }
                ResultNavTab.MAP -> {
                    if (passedFoods.isNotEmpty()) {
                        val intent = Intent(context, MapActivity::class.java)
                        val foodIds = passedFoods.map { it.id }
                        intent.putIntegerArrayListExtra("passed_food_ids", ArrayList(foodIds))
                        context.startActivity(intent)
                    } else {
                        Toast.makeText(context, "통과한 음식이 없습니다.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        },
        selectedNavTab = selectedNavTab
    )
}
