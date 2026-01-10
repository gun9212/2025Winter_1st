package com.example.foodworldcup.ui

import android.content.Intent
import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.foodworldcup.data.Food
import com.example.foodworldcup.data.FoodRepository
import com.example.foodworldcup.databinding.ActivityResultBinding
import com.example.foodworldcup.ui.adapter.PassedFoodAdapter

/**
 * 합격된 음식들을 나열하는 Activity입니다.
 * 음식 사진과 이름을 나열하고, 다음 화면(지도)으로 넘어가는 버튼을 제공합니다.
 * 
 * 레이아웃 파일: res/layout/activity_result.xml
 */
class ResultActivity : BaseActivity() {

    private lateinit var binding: ActivityResultBinding
    
    // GameActivity로부터 전달받은 합격된 음식 리스트
    private var passedFoods: List<Food> = emptyList()
    
    // 통과한 음식 어댑터
    private lateinit var passedFoodAdapter: PassedFoodAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityResultBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 하단 네비게이션 바 설정
        setupBottomNavigation(BaseActivity.Screen.ACCEPTED)

        // GameActivity로부터 전달받은 합격된 음식 ID 리스트를 가져옵니다.
        val passedFoodIds = intent.getIntegerArrayListExtra("passed_food_ids") ?: emptyList()
        
        // 음식 ID로 FoodRepository에서 음식 리스트를 가져옵니다.
        passedFoods = passedFoodIds.mapNotNull { id ->
            FoodRepository.getFoodById(id)
        }
        
        // 화면에 합격된 음식 리스트를 표시합니다.
        displayPassedFoods()
        
        // TODO: 버튼 클릭 이벤트를 설정합니다.
        setupButtons()
    }

    /**
     * 화면에 합격된 음식 리스트를 표시하는 함수입니다.
     */
    private fun displayPassedFoods() {
        if (passedFoods.isEmpty()) {
            // 통과한 음식이 없으면 메시지 표시
            binding.resultTitleTextView.text = "통과한 음식이 없습니다"
            return
        }

        // RecyclerView 어댑터 생성 및 설정
        passedFoodAdapter = PassedFoodAdapter(passedFoods)
        binding.passedFoodRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.passedFoodRecyclerView.adapter = passedFoodAdapter
        
        // 통과한 음식 개수 표시
        binding.resultTitleTextView.text = "통과한 음식 (${passedFoods.size}개)"
    }

    /**
     * 버튼 클릭 이벤트를 설정하는 함수입니다.
     */
    private fun setupButtons() {
        // '다음' 버튼 클릭 시 MapActivity로 이동
        binding.nextButton.setOnClickListener {
            if (passedFoods.isEmpty()) {
                android.widget.Toast.makeText(this, "통과한 음식이 없습니다.", android.widget.Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val intent = Intent(this, MapActivity::class.java)
            val passedFoodIds = passedFoods.map { it.id }
            intent.putIntegerArrayListExtra("passed_food_ids", ArrayList(passedFoodIds))
            startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        // TODO: 필요시 결과 화면이 다시 보일 때 실행할 로직을 여기에 추가합니다.
    }

    override fun onPause() {
        super.onPause()
        // TODO: 필요시 결과 화면이 가려질 때 실행할 로직을 여기에 추가합니다.
    }
}
