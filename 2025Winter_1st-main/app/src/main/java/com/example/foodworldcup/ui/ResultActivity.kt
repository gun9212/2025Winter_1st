package com.example.foodworldcup.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.foodworldcup.data.Food
import com.example.foodworldcup.data.WinRecord
import com.example.foodworldcup.databinding.ActivityResultBinding
import com.example.foodworldcup.utils.PreferenceManager
import java.util.Date

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
    
    // SharedPreferences 관리 객체
    private lateinit var preferenceManager: PreferenceManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityResultBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // PreferenceManager 초기화
        preferenceManager = PreferenceManager(this)

        // 하단 네비게이션 바 설정
        setupBottomNavigation(BaseActivity.Screen.ACCEPTED)

        // TODO: GameActivity로부터 전달받은 합격된 음식 ID 리스트를 가져옵니다.
        // val passedFoodIds = intent.getIntegerArrayListExtra("passed_food_ids") ?: emptyList()
        // TODO: 음식 ID로 FoodRepository에서 음식 리스트를 가져옵니다.
        // passedFoods = passedFoodIds.mapNotNull { id -> 
        //     FoodRepository.getFoodList().find { it.id == id } 
        // }
        
        // TODO: 화면에 합격된 음식 리스트를 표시합니다.
        displayPassedFoods()
        
        // TODO: 버튼 클릭 이벤트를 설정합니다.
        setupButtons()
    }

    /**
     * 화면에 합격된 음식 리스트를 표시하는 함수입니다.
     */
    private fun displayPassedFoods() {
        // TODO: RecyclerView 어댑터를 생성하고 합격된 음식 리스트를 표시합니다.
        // 예: val adapter = PassedFoodAdapter(passedFoods)
        //     binding.recyclerView.layoutManager = LinearLayoutManager(this)
        //     binding.recyclerView.adapter = adapter
        // TODO: 각 항목에는 음식 이미지와 이름이 표시됩니다.
    }

    /**
     * 버튼 클릭 이벤트를 설정하는 함수입니다.
     */
    private fun setupButtons() {
        // TODO: '다음' 버튼 클릭 시 MapActivity로 이동합니다.
        // 예: binding.nextButton.setOnClickListener {
        //     val intent = Intent(this, MapActivity::class.java)
        //     val passedFoodIds = passedFoods.map { it.id }
        //     intent.putIntegerArrayListExtra("passed_food_ids", ArrayList(passedFoodIds))
        //     startActivity(intent)
        // }
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
