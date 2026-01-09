package com.example.foodworldcup.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import com.bumptech.glide.Glide
import com.example.foodworldcup.data.FoodRepository
import com.example.foodworldcup.databinding.ActivityIntroBinding
import com.example.foodworldcup.utils.PreferenceManager

/**
 * 앱의 첫 화면(인트로 화면)을 담당하는 Activity입니다.
 * 앱 소개 및 게임 시작 버튼이 있는 화면입니다.
 * 
 * 레이아웃 파일: res/layout/activity_intro.xml
 */
class IntroActivity : BaseActivity() {

    // ViewBinding 변수 선언. lateinit으로 나중에 초기화할 것을 약속합니다.
    private lateinit var binding: ActivityIntroBinding
    
    // SharedPreferences 관리 객체
    private lateinit var preferenceManager: PreferenceManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. 뷰 바인딩 객체를 생성합니다.
        // XML 레이아웃 파일을 메모리에 올리고(inflate) 실제 뷰 객체로 만듭니다.
        binding = ActivityIntroBinding.inflate(layoutInflater)

        // 2. 생성된 뷰의 최상위(root) 뷰를 화면에 표시합니다.
        // 기존의 setContentView(R.layout.activity_intro)를 대체합니다.
        setContentView(binding.root)

        // PreferenceManager 초기화
        preferenceManager = PreferenceManager(this)

        // 하단 네비게이션 바 설정
        setupBottomNavigation(BaseActivity.Screen.HOME)

        // '게임 시작' 버튼 클릭 시 FoodListActivity로 이동
        binding.startButton.setOnClickListener { 
            val intent = Intent(this, FoodListActivity::class.java)
            startActivity(intent)
        }
        
        // '마이페이지' 버튼 클릭 시 MyPageActivity로 이동
        binding.myPageButton.setOnClickListener {
            val intent = Intent(this, MyPageActivity::class.java)
            startActivity(intent)
        }
        
        // 하단 네비게이션 바는 BaseActivity에서 처리됨
    }

    override fun onResume() {
        super.onResume()
        // 인트로 화면이 다시 보일 때 최근 우승 기록을 불러와서 표시합니다.
        loadRecentWinner()
    }

    override fun onPause() {
        super.onPause()
    }

    /**
     * PreferenceManager에서 최근 우승 기록을 불러와서 Recent Winner 섹션에 표시하는 함수입니다.
     * 기록이 없으면 Recent Winner 섹션을 숨깁니다.
     */
    private fun loadRecentWinner() {
        val winRecords = preferenceManager.getWinRecords()
        
        if (winRecords.isEmpty()) {
            // 기록이 없으면 Recent Winner 섹션 숨김
            binding.recentWinnerCard.visibility = View.GONE
            return
        }
        
        // 가장 최근 기록 가져오기 (날짜 기준 내림차순 정렬)
        val recentRecord = winRecords.sortedByDescending { it.winDate }.first()
        
        // 선택된 음식 ID 리스트에서 첫 번째 음식 가져오기
        if (recentRecord.selectedFoods.isNotEmpty()) {
            val firstFoodId = recentRecord.selectedFoods.first()
            val food = FoodRepository.getFoodById(firstFoodId)
            
            if (food != null) {
                // Recent Winner 섹션 표시
                binding.recentWinnerCard.visibility = View.VISIBLE
                binding.recentWinnerNameTextView.text = food.name
                
                // 이미지가 있으면 표시 (현재는 imageResId가 0이므로 표시하지 않음)
                // TODO: 이미지 리소스가 추가되면 아래 코드 활성화
                // if (food.imageResId != 0) {
                //     Glide.with(this)
                //         .load(food.imageResId)
                //         .placeholder(R.drawable.ic_launcher_background)
                //         .into(binding.recentWinnerImageView)
                // }
            } else {
                // 음식을 찾을 수 없으면 섹션 숨김
                binding.recentWinnerCard.visibility = View.GONE
            }
        } else {
            // 선택된 음식이 없으면 섹션 숨김
            binding.recentWinnerCard.visibility = View.GONE
        }
    }
}
