package com.example.foodworldcup.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.recyclerview.widget.GridLayoutManager
import com.example.foodworldcup.data.Food
import com.example.foodworldcup.data.FoodRepository
import com.example.foodworldcup.data.WinRecord
import com.example.foodworldcup.databinding.ActivityResultBinding
import com.example.foodworldcup.ui.adapter.PassedFoodAdapter
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
        
        // 우승 기록 저장 (통과한 음식이 있을 때만)
        if (passedFoods.isNotEmpty()) {
            saveWinRecord(passedFoodIds)
        }
        
        // 화면에 합격된 음식 리스트를 표시합니다.
        displayPassedFoods()
        
        // 버튼 클릭 이벤트 설정
        setupButtons()
    }

    /**
     * 우승 기록을 저장하는 함수입니다.
     * 통과한 음식 리스트를 WinRecord로 변환하여 PreferenceManager에 저장합니다.
     * 
     * @param passedFoodIds 통과한 음식 ID 리스트
     */
    private fun saveWinRecord(passedFoodIds: List<Int>) {
        try {
            // WinRecord 생성
            val winRecord = WinRecord(
                id = System.currentTimeMillis(), // 타임스탬프를 ID로 사용 (고유성 보장)
                selectedFoods = passedFoodIds, // 통과한 음식 ID 리스트
                winDate = Date(), // 현재 날짜 및 시간
                memo = "" // 기본 메모는 빈 문자열 (나중에 마이페이지에서 편집 가능)
            )
            
            // PreferenceManager를 통해 저장
            preferenceManager.addWinRecord(winRecord)
            
            // 저장 완료 로그
            Log.d("ResultActivity", "우승 기록 저장 완료: ${passedFoodIds.size}개 음식")
        } catch (e: Exception) {
            // 저장 실패 시 로그 출력
            Log.e("ResultActivity", "우승 기록 저장 실패", e)
            e.printStackTrace()
            // 사용자에게는 에러를 보여주지 않음 (선택사항: Toast로 알림 가능)
        }
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

        // RecyclerView 어댑터 생성 및 설정 (2열 그리드 레이아웃)
        passedFoodAdapter = PassedFoodAdapter(passedFoods)
        binding.passedFoodRecyclerView.layoutManager = GridLayoutManager(this, 2)
        binding.passedFoodRecyclerView.adapter = passedFoodAdapter
        
        // 통과한 음식 개수 표시
        binding.resultTitleTextView.text = "통과한 음식 (${passedFoods.size}개)"
    }

    /**
     * 버튼 클릭 이벤트를 설정하는 함수입니다.
     */
    private fun setupButtons() {
        // 다시하기 버튼: FoodListActivity로 이동 (게임 재시작)
        binding.restartButton.setOnClickListener {
            val intent = Intent(this, FoodListActivity::class.java)
            startActivity(intent)
            finish() // ResultActivity 종료
        }

        // 마이페이지 버튼: MyPageActivity로 이동
        binding.myPageButton.setOnClickListener {
            val intent = Intent(this, MyPageActivity::class.java)
            startActivity(intent)
        }

        // 지도 보기 버튼: MapActivity로 이동 (통과한 음식 전달)
        binding.nextButton.setOnClickListener {
            if (passedFoods.isEmpty()) {
                Toast.makeText(this, "통과한 음식이 없습니다.", Toast.LENGTH_SHORT).show()
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
        // 결과 화면이 다시 보일 때 추가 처리 (필요시)
    }

    override fun onPause() {
        super.onPause()
        // 결과 화면이 가려질 때 추가 처리 (필요시)
    }
}
