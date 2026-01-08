package com.example.foodworldcup.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.foodworldcup.data.Food
import com.example.foodworldcup.data.FoodRepository
import com.example.foodworldcup.databinding.ActivityGameBinding

/**
 * 게임의 메인 화면을 담당하는 Activity입니다.
 * 상단 토글로 카테고리 필터링이 가능하고, 중앙 카드 스와이프로 음식을 선택합니다.
 * 
 * 레이아웃 파일: res/layout/activity_game.xml
 */
class GameActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGameBinding
    
    // 현재 게임에 사용할 음식 리스트
    private var currentFoodList: List<Food> = emptyList()
    
    // 현재 선택된 카테고리 (기본값: "전체" 또는 null)
    private var selectedCategory: String? = null
    
    // 현재 라운드의 두 음식 인덱스
    private var currentRound: Int = 0
    
    // 최종 우승 음식
    private var winnerFood: Food? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGameBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // TODO: 게임 초기화 로직을 여기에 구현합니다.
        // 1. FoodRepository에서 전체 음식 리스트를 가져옵니다.
        // 2. 상단 토글 버튼들을 동적으로 생성합니다 (전체, 한식, 중식, 양식 등)
        // 3. 초기 음식 리스트를 설정하고 첫 라운드를 시작합니다.
        initializeGame()
    }

    /**
     * 게임을 초기화하는 함수입니다.
     */
    private fun initializeGame() {
        // TODO: FoodRepository.getFoodList()로 전체 음식 리스트를 가져옵니다.
        // TODO: 상단 토글 버튼을 설정합니다 (전체, 한식, 중식 등)
        // TODO: 카드 스택 뷰를 초기화하고 첫 두 음식을 표시합니다.
    }

    /**
     * 카테고리 필터링을 처리하는 함수입니다.
     * 상단 토글 버튼 클릭 시 호출됩니다.
     *
     * @param category 선택된 카테고리 (null이면 전체)
     */
    private fun filterByCategory(category: String?) {
        // TODO: FoodRepository.getFoodListByCategory()를 사용하여 필터링된 리스트를 가져옵니다.
        // TODO: currentFoodList를 업데이트하고 게임을 다시 시작합니다.
        selectedCategory = category
    }

    /**
     * 사용자가 음식을 선택했을 때 호출되는 함수입니다.
     * 카드 스와이프 이벤트에서 호출됩니다.
     *
     * @param selectedFood 선택된 음식
     */
    private fun onFoodSelected(selectedFood: Food) {
        // TODO: 선택된 음식을 다음 라운드로 진출시킵니다.
        // TODO: 다음 라운드가 있는지 확인하고, 없으면 최종 우승 음식을 결정합니다.
        // TODO: 모든 라운드가 끝나면 ResultActivity로 이동합니다.
    }

    /**
     * 다음 라운드를 시작하는 함수입니다.
     */
    private fun startNextRound() {
        // TODO: 현재 라운드의 두 음식을 카드 스택에 표시합니다.
        // TODO: currentRound를 증가시킵니다.
    }

    /**
     * 게임이 끝났을 때 호출되는 함수입니다.
     * 최종 우승 음식을 ResultActivity로 전달합니다.
     */
    private fun finishGame() {
        // TODO: winnerFood를 Intent에 담아 ResultActivity로 이동합니다.
        // 예: val intent = Intent(this, ResultActivity::class.java)
        //     intent.putExtra("winner_food", winnerFood)
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
