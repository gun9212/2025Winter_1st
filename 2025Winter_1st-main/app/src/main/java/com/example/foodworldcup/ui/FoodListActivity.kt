package com.example.foodworldcup.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import com.example.foodworldcup.data.Food
import com.example.foodworldcup.data.FoodRepository
import com.example.foodworldcup.databinding.ActivityFoodListBinding
import com.example.foodworldcup.utils.PreferenceManager

/**
 * 음식 리스트 선택 화면을 담당하는 Activity입니다.
 * 토글 형식으로 음식 장르(한식, 양식, 중식, 일식)를 보여주고,
 * 사용자가 체크한 음식들을 저장/불러오기 합니다.
 * 
 * 레이아웃 파일: res/layout/activity_food_list.xml
 */
class FoodListActivity : BaseActivity() {

    private lateinit var binding: ActivityFoodListBinding
    
    // SharedPreferences 관리 객체
    private lateinit var preferenceManager: PreferenceManager
    
    // 현재 선택된 음식 리스트
    private var selectedFoodIds: MutableSet<Int> = mutableSetOf()
    
    // 현재 선택된 카테고리
    private var selectedCategory: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        try {
            Log.d("FoodListActivity", "=== onCreate 시작 ===")
            binding = ActivityFoodListBinding.inflate(layoutInflater)
            Log.d("FoodListActivity", "바인딩 생성 완료")
            setContentView(binding.root)
            Log.d("FoodListActivity", "레이아웃 설정 완료")

            // PreferenceManager 초기화
            preferenceManager = PreferenceManager(this)
            Log.d("FoodListActivity", "PreferenceManager 초기화 완료")
            
            // 하단 네비게이션 바 설정
            Log.d("FoodListActivity", "하단 네비게이션 바 설정 시작")
            try {
                setupBottomNavigation(BaseActivity.Screen.LIST)
                Log.d("FoodListActivity", "하단 네비게이션 바 설정 완료")
            } catch (e: Exception) {
                Log.e("FoodListActivity", "하단 네비게이션 바 설정 실패", e)
                e.printStackTrace()
                // 네비게이션 바 설정 실패해도 앱은 계속 실행
            }
            
            // TODO: 저장된 체크 리스트 불러오기
            loadSelectedFoods()
            
            // TODO: 카테고리 토글 버튼 설정 (전체, 한식, 양식, 중식, 일식)
            setupCategoryToggles()
            
            // TODO: 음식 리스트 표시 (RecyclerView 또는 리스트뷰)
            setupFoodList()
            
            // TODO: 버튼 클릭 이벤트 설정
            setupButtons()
            
            Log.d("FoodListActivity", "=== onCreate 완료 ===")
        } catch (e: Exception) {
            Log.e("FoodListActivity", "=== onCreate 오류 발생 ===", e)
            Log.e("FoodListActivity", "오류 타입: ${e.javaClass.simpleName}")
            Log.e("FoodListActivity", "오류 메시지: ${e.message}")
            e.printStackTrace()
            throw e
        }
    }

    /**
     * 저장된 체크된 음식 리스트를 불러오는 함수입니다.
     */
    private fun loadSelectedFoods() {
        // TODO: PreferenceManager에서 저장된 음식 ID 리스트를 불러옵니다.
        // 예: selectedFoodIds = preferenceManager.getSelectedFoodIds().toMutableSet()
    }

    /**
     * 카테고리 토글 버튼을 설정하는 함수입니다.
     */
    private fun setupCategoryToggles() {
        // TODO: FoodRepository.getAllCategories()로 카테고리 리스트를 가져옵니다.
        // TODO: 각 카테고리에 대해 토글 버튼을 동적으로 생성합니다.
        // TODO: 토글 버튼 클릭 시 해당 카테고리 음식만 필터링합니다.
        // 예: binding.toggleAll.setOnClickListener { filterByCategory(null) }
        //     binding.toggleKorean.setOnClickListener { filterByCategory("한식") }
    }

    /**
     * 카테고리별로 음식을 필터링하는 함수입니다.
     */
    private fun filterByCategory(category: String?) {
        selectedCategory = category
        setupFoodList()
    }

    /**
     * 음식 리스트를 표시하는 함수입니다.
     */
    private fun setupFoodList() {
        // TODO: 선택된 카테고리에 따라 음식 리스트를 가져옵니다.
        // val foodList = if (selectedCategory == null) {
        //     FoodRepository.getFoodList()
        // } else {
        //     FoodRepository.getFoodListByCategory(selectedCategory!!)
        // }
        // TODO: RecyclerView 어댑터를 생성하고 음식 리스트를 표시합니다.
        // TODO: 각 음식 항목에 체크박스를 추가하고, 체크 상태를 저장/불러오기 합니다.
    }

    /**
     * 음식 체크 상태가 변경될 때 호출되는 함수입니다.
     */
    private fun onFoodCheckedChanged(foodId: Int, isChecked: Boolean) {
        if (isChecked) {
            selectedFoodIds.add(foodId)
        } else {
            selectedFoodIds.remove(foodId)
        }
        // TODO: 선택된 음식 리스트를 즉시 저장합니다.
        saveSelectedFoods()
    }

    /**
     * 선택된 음식 리스트를 저장하는 함수입니다.
     */
    private fun saveSelectedFoods() {
        // TODO: PreferenceManager에 선택된 음식 ID 리스트를 저장합니다.
        // 예: preferenceManager.saveSelectedFoodIds(selectedFoodIds.toList())
    }

    /**
     * 버튼 클릭 이벤트를 설정하는 함수입니다.
     */
    private fun setupButtons() {
        // TODO: '시작' 버튼 클릭 시 선택된 음식 리스트를 GameActivity로 전달합니다.
        // 예: binding.startButton.setOnClickListener {
        //     if (selectedFoodIds.isNotEmpty()) {
        //         val intent = Intent(this, GameActivity::class.java)
        //         intent.putIntegerArrayListExtra("selected_food_ids", ArrayList(selectedFoodIds))
        //         startActivity(intent)
        //     } else {
        //         // 음식을 하나 이상 선택하라는 메시지 표시
        //     }
        // }
        
        // TODO: '마이페이지' 버튼 클릭 시 MyPageActivity로 이동합니다.
        // 예: binding.myPageButton.setOnClickListener {
        //     val intent = Intent(this, MyPageActivity::class.java)
        //     startActivity(intent)
        // }
    }

    override fun onResume() {
        super.onResume()
        // TODO: 화면이 다시 보일 때 저장된 체크 상태를 다시 불러옵니다.
        loadSelectedFoods()
        setupFoodList()
    }

    override fun onPause() {
        super.onPause()
        // TODO: 화면이 가려질 때 선택된 음식 리스트를 저장합니다.
        saveSelectedFoods()
    }
}
