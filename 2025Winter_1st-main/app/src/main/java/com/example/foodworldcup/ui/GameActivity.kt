package com.example.foodworldcup.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.foodworldcup.data.Food
import com.example.foodworldcup.data.FoodRepository
import com.example.foodworldcup.databinding.ActivityGameBinding

/**
 * 스와이프 게임 화면을 담당하는 Activity입니다.
 * 음식 이미지와 이름을 보여주고, 왼쪽 스와이프(탈락) / 오른쪽 스와이프(합격)로 선택합니다.
 * 체크해둔 음식들이 전부 선택되면 다음 화면(ResultActivity)으로 이동합니다.
 * 
 * 레이아웃 파일: res/layout/activity_game.xml
 */
class GameActivity : BaseActivity() {

    private lateinit var binding: ActivityGameBinding
    
    // 현재 게임에 사용할 음식 리스트 (FoodListActivity에서 선택된 음식들)
    private var currentFoodList: List<Food> = emptyList()
    
    // 현재 인덱스 (몇 번째 음식인지)
    private var currentIndex: Int = 0
    
    // 합격된 음식 리스트 (오른쪽으로 스와이프한 음식들)
    private var passedFoods: MutableList<Food> = mutableListOf()
    
    // 탈락된 음식 리스트 (왼쪽으로 스와이프한 음식들)
    private var rejectedFoods: MutableList<Food> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGameBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 하단 네비게이션 바 설정
        setupBottomNavigation(BaseActivity.Screen.SWIPE)

        // TODO: FoodListActivity로부터 선택된 음식 ID 리스트를 받아옵니다.
        // val selectedFoodIds = intent.getIntegerArrayListExtra("selected_food_ids")
        // TODO: 선택된 음식 ID로 FoodRepository에서 음식 리스트를 가져옵니다.
        // TODO: 게임 초기화
        initializeGame()
    }

    /**
     * 게임을 초기화하는 함수입니다.
     */
    private fun initializeGame() {
        // TODO: FoodListActivity에서 받은 음식 ID 리스트로 음식 리스트를 구성합니다.
        // 예: val selectedFoodIds = intent.getIntegerArrayListExtra("selected_food_ids") ?: emptyList()
        //     currentFoodList = selectedFoodIds.mapNotNull { id -> 
        //         FoodRepository.getFoodList().find { it.id == id } 
        //     }
        
        // TODO: 첫 번째 음식을 표시합니다.
        displayCurrentFood()
        
        // TODO: 진행 상황을 업데이트합니다 (예: "1/10")
        updateProgress()
    }

    /**
     * 현재 음식을 화면에 표시하는 함수입니다.
     */
    private fun displayCurrentFood() {
        if (currentIndex < currentFoodList.size) {
            val currentFood = currentFoodList[currentIndex]
            // TODO: 음식 이미지와 이름을 화면에 표시합니다.
            // 예: binding.foodNameTextView.text = "${currentFood.name} (${currentFood.category})"
            //     Glide.with(this).load(currentFood.imageResId).into(binding.foodImageView)
        } else {
            // 모든 음식을 선택했으므로 게임 종료
            finishGame()
        }
    }

    /**
     * 진행 상황을 업데이트하는 함수입니다.
     */
    private fun updateProgress() {
        // TODO: "현재/전체" 형식으로 진행 상황을 표시합니다.
        // 예: binding.progressTextView.text = "${currentIndex + 1}/${currentFoodList.size}"
    }

    /**
     * 왼쪽으로 스와이프했을 때 호출되는 함수입니다. (탈락)
     */
    private fun onSwipeLeft(food: Food) {
        rejectedFoods.add(food)
        moveToNextFood()
    }

    /**
     * 오른쪽으로 스와이프했을 때 호출되는 함수입니다. (합격)
     */
    private fun onSwipeRight(food: Food) {
        passedFoods.add(food)
        moveToNextFood()
    }

    /**
     * 다음 음식으로 이동하는 함수입니다.
     */
    private fun moveToNextFood() {
        currentIndex++
        if (currentIndex < currentFoodList.size) {
            displayCurrentFood()
            updateProgress()
        } else {
            // 모든 음식을 선택했으므로 게임 종료
            finishGame()
        }
    }

    /**
     * 게임이 끝났을 때 호출되는 함수입니다.
     * 합격된 음식 리스트를 ResultActivity로 전달합니다.
     */
    private fun finishGame() {
        // TODO: 합격된 음식 리스트를 Intent에 담아 ResultActivity로 이동합니다.
        // 예: val intent = Intent(this, ResultActivity::class.java)
        //     val passedFoodIds = passedFoods.map { it.id }
        //     intent.putIntegerArrayListExtra("passed_food_ids", ArrayList(passedFoodIds))
        //     startActivity(intent)
        //     finish()
    }

    override fun onResume() {
        super.onResume()
        // TODO: 필요시 게임 화면이 다시 보일 때 실행할 로직을 여기에 추가합니다.
    }

    override fun onPause() {
        super.onPause()
        // TODO: 필요시 게임 화면이 가려질 때 실행할 로직을 여기에 추가합니다.
    }

    override fun onDestroy() {
        super.onDestroy()
        // TODO: 필요시 리소스 정리 로직을 여기에 추가합니다.
    }
}
