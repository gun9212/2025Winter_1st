package com.example.foodworldcup.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.foodworldcup.data.Food
import com.example.foodworldcup.data.FoodRepository
import com.example.foodworldcup.databinding.ActivityFoodListBinding
import com.example.foodworldcup.ui.adapter.CategoryAdapter
import com.example.foodworldcup.utils.PreferenceManager

/**
 * 음식 리스트 선택 화면을 담당하는 Activity입니다.
 * Food Genres 화면에서 카테고리를 선택하고, 선택된 음식들을 저장합니다.
 * 
 * 레이아웃 파일: res/layout/activity_food_list.xml
 */
class FoodListActivity : BaseActivity() {

    private lateinit var binding: ActivityFoodListBinding
    
    // SharedPreferences 관리 객체
    private lateinit var preferenceManager: PreferenceManager
    
    // 현재 선택된 음식 리스트
    private var selectedFoodIds: MutableSet<Int> = mutableSetOf()
    
    // 카테고리 어댑터
    private lateinit var categoryAdapter: CategoryAdapter
    
    // 펼쳐진 카테고리 인덱스 (null이면 모두 접힘)
    private var expandedCategoryIndex: Int? = null
    
    // 카테고리 이름과 영문 이름 매핑
    private val categoryNameMap = mapOf(
        "한식" to "Korean",
        "양식" to "Western",
        "중식" to "Chinese",
        "일식" to "Japanese",
        "아시안" to "Asian"
    )

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
            
            // 저장된 체크 리스트 불러오기
            loadSelectedFoods()
            
            // 카테고리 리스트 설정
            setupCategoryList()
            
            // 선택 개수 업데이트
            updateSelectedCount()
            
            // 버튼 클릭 이벤트 설정
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
        selectedFoodIds = preferenceManager.getSelectedFoodIds().toMutableSet()
        Log.d("FoodListActivity", "저장된 선택 음식 개수: ${selectedFoodIds.size}")
    }

    /**
     * 카테고리 리스트를 설정하는 함수입니다.
     */
    private fun setupCategoryList() {
        val categories = FoodRepository.getAllCategories()
        
        // 각 카테고리의 선택 상태 확인 (해당 카테고리의 모든 음식이 선택되어 있는지)
        val categoryItems = categories.mapIndexed { index, category ->
            val foodsInCategory = FoodRepository.getFoodListByCategory(category)
            val allSelected = foodsInCategory.isNotEmpty() && 
                foodsInCategory.all { it.id in selectedFoodIds }
            
            CategoryAdapter.CategoryItem(
                categoryName = category,
                categoryNameEn = categoryNameMap[category] ?: category,
                isChecked = allSelected,
                isExpanded = expandedCategoryIndex == index
            )
        }
        
        categoryAdapter = CategoryAdapter(
            categories = categoryItems,
            selectedFoodIds = selectedFoodIds,
            onCategoryCheckedChanged = { categoryName, isChecked ->
                onCategoryCheckedChanged(categoryName, isChecked)
            },
            onFoodCheckedChanged = { foodId, isChecked ->
                onFoodCheckedChanged(foodId, isChecked)
            },
            onCategoryExpanded = { position ->
                onCategoryExpanded(position)
            }
        )
        
        binding.categoryRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.categoryRecyclerView.adapter = categoryAdapter
        
        // RecyclerView가 터치 이벤트를 가로채지 않도록 설정
        binding.categoryRecyclerView.isNestedScrollingEnabled = false
        binding.categoryRecyclerView.overScrollMode = View.OVER_SCROLL_NEVER
        binding.categoryRecyclerView.setHasFixedSize(false)
        
        // RecyclerView의 터치 이벤트를 부모 NestedScrollView로 전달하도록 설정
        binding.categoryRecyclerView.setOnTouchListener { v, event ->
            // 터치 이벤트를 부모 NestedScrollView로 전달
            binding.scrollView.requestDisallowInterceptTouchEvent(false)
            false // 이벤트를 처리하지 않고 부모로 전달
        }
    }

    /**
     * 카테고리 체크 상태가 변경될 때 호출되는 함수입니다.
     */
    private fun onCategoryCheckedChanged(categoryName: String, isChecked: Boolean) {
        val foodsInCategory = FoodRepository.getFoodListByCategory(categoryName)
        
        if (isChecked) {
            // 카테고리 선택: 해당 카테고리의 모든 음식 추가
            foodsInCategory.forEach { food ->
                selectedFoodIds.add(food.id)
            }
        } else {
            // 카테고리 해제: 해당 카테고리의 모든 음식 제거
            foodsInCategory.forEach { food ->
                selectedFoodIds.remove(food.id)
            }
        }
        
        // 선택 개수 업데이트
        updateSelectedCount()
        
        // 저장
        saveSelectedFoods()
        
        // 어댑터 업데이트 (다른 카테고리의 체크 상태도 업데이트)
        setupCategoryList()
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
        
        // 선택 개수 업데이트
        updateSelectedCount()
        
        // 저장
        saveSelectedFoods()
        
        // 어댑터 업데이트 (카테고리 체크 상태도 업데이트)
        setupCategoryList()
    }

    /**
     * 카테고리 펼쳐지기/접히기 토글 함수입니다.
     */
    private fun onCategoryExpanded(position: Int) {
        // 같은 카테고리를 클릭하면 접히기, 다른 카테고리를 클릭하면 펼쳐지기
        expandedCategoryIndex = if (expandedCategoryIndex == position) {
            null
        } else {
            position
        }
        
        // 어댑터 업데이트
        setupCategoryList()
    }

    /**
     * 선택된 음식 개수를 업데이트하는 함수입니다.
     */
    private fun updateSelectedCount() {
        val count = selectedFoodIds.size
        binding.selectedCountTextView.text = "$count selected"
    }

    /**
     * 선택된 음식 리스트를 저장하는 함수입니다.
     */
    private fun saveSelectedFoods() {
        preferenceManager.saveSelectedFoodIds(selectedFoodIds.toList())
        Log.d("FoodListActivity", "선택된 음식 저장 완료: ${selectedFoodIds.size}개")
    }

    /**
     * 버튼 클릭 이벤트를 설정하는 함수입니다.
     */
    private fun setupButtons() {
        // 'Go to Swipe' 버튼 클릭 시 선택된 음식 리스트를 GameActivity로 전달합니다.
        binding.startButton.setOnClickListener {
            if (selectedFoodIds.isNotEmpty()) {
                val intent = Intent(this, GameActivity::class.java)
                intent.putIntegerArrayListExtra("selected_food_ids", ArrayList(selectedFoodIds))
                startActivity(intent)
            } else {
                Toast.makeText(
                    this,
                    "음식을 하나 이상 선택해주세요",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // 화면이 다시 보일 때 저장된 체크 상태를 다시 불러옵니다.
        loadSelectedFoods()
        setupCategoryList()
        updateSelectedCount()
    }

    override fun onPause() {
        super.onPause()
        // 화면이 가려질 때 선택된 음식 리스트를 저장합니다.
        saveSelectedFoods()
    }
}
