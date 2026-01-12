package com.example.foodworldcup.ui

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.view.View
import com.bumptech.glide.Glide
import com.example.foodworldcup.R
import com.example.foodworldcup.data.FoodRepository
import com.example.foodworldcup.data.MapSelectedFood
import com.example.foodworldcup.databinding.ActivityIntroBinding
import com.example.foodworldcup.utils.PreferenceManager
import com.kakao.sdk.common.util.Utility

/**
 * 앱의 첫 화면(인트로 화면)을 담당하는 Activity입니다.
 * 앱 소개 및 게임 시작 버튼이 있는 화면입니다.
 * 
 * 레이아웃 파일: res/layout/activity_intro.xml
 */
class IntroActivity : BaseActivity() {

    // ViewBinding 변수 선언. lateinit으로 나중에 초기화할 것을 약속합니다.
    private lateinit var binding: ActivityIntroBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. 뷰 바인딩 객체를 생성합니다.
        // XML 레이아웃 파일을 메모리에 올리고(inflate) 실제 뷰 객체로 만듭니다.
        binding = ActivityIntroBinding.inflate(layoutInflater)

        // 2. 생성된 뷰의 최상위(root) 뷰를 화면에 표시합니다.
        // 기존의 setContentView(R.layout.activity_intro)를 대체합니다.
        setContentView(binding.root)

        // FoodRepository 초기화 (JSON 파일에서 데이터 로드)
        FoodRepository.initialize(this)

        // 해시 키 추출 및 로그 출력
        val keyHash = Utility.getKeyHash(this)
        Log.d("KakaoKeyHash", "현재 키 해시값: $keyHash")

        // 하단 네비게이션 바 설정
        setupBottomNavigation(BaseActivity.Screen.HOME)

        // '게임 시작' 버튼 클릭 시 FoodListActivity로 이동
        binding.startButton.setOnClickListener { 
            val intent = Intent(this, FoodListActivity::class.java)
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
     * PreferenceManager에서 최근 선택된 음식을 불러와서 Recent Winner 섹션에 표시하는 함수입니다.
     * 기록이 없으면 Recent Winner 섹션을 숨깁니다.
     */
    private fun loadRecentWinner() {
        val mapSelectedFoods = preferenceManager.getMapSelectedFoods()
        
        if (mapSelectedFoods.isEmpty()) {
            // 기록이 없으면 Recent Winner 섹션 숨김
            binding.recentWinnerCard.visibility = View.GONE
            return
        }
        
        // 가장 최근 기록 가져오기 (날짜 기준 내림차순 정렬)
        val recentFood = mapSelectedFoods.sortedByDescending { it.selectedDate }.first()
        
        // 음식 정보 가져오기
        val food = FoodRepository.getFoodById(recentFood.foodId)
        
        if (food != null) {
            // Recent Winner 섹션 표시
            binding.recentWinnerCard.visibility = View.VISIBLE
            binding.recentWinnerNameTextView.text = food.name
            
            // 캐릭터 이미지 로드 (characterImagePath 사용)
            if (!food.characterImagePath.isNullOrEmpty()) {
                // 시도할 경로 리스트
                val pathsToTry = mutableListOf<String>()
                
                // 1. 원본 경로 (캐릭터 이미지 경로)
                pathsToTry.add(food.characterImagePath)
                
                // 2. 확장자 변경 (.png <-> .jpg)
                if (food.characterImagePath.endsWith(".png")) {
                    pathsToTry.add(food.characterImagePath.replace(".png", ".jpg"))
                } else if (food.characterImagePath.endsWith(".jpg")) {
                    pathsToTry.add(food.characterImagePath.replace(".jpg", ".png"))
                }
                
                // 3. 음식 이름으로 직접 찾기 (캐릭터 이미지 경로)
                pathsToTry.add("food_character_images/${food.category}/${food.name}_캐릭터누끼.png")
                pathsToTry.add("food_character_images/${food.category}/${food.name}_캐릭터누끼.jpg")
                
                // 각 경로를 시도
                var loaded = false
                for (path in pathsToTry) {
                    try {
                        val inputStream = assets.open(path)
                        val bitmap = BitmapFactory.decodeStream(inputStream)
                        inputStream.close()
                        
                        if (bitmap != null) {
                            Glide.with(this)
                                .load(bitmap)
                                .placeholder(R.drawable.ic_launcher_background)
                                .error(R.drawable.ic_launcher_background)
                                .centerCrop()
                                .into(binding.recentWinnerImageView)
                            loaded = true
                            break
                        }
                    } catch (e: Exception) {
                        // 다음 경로 시도
                        continue
                    }
                }
                
                if (!loaded) {
                    binding.recentWinnerImageView.setImageResource(R.drawable.ic_launcher_background)
                }
            } else {
                binding.recentWinnerImageView.setImageResource(R.drawable.ic_launcher_background)
            }
        } else {
            // 음식을 찾을 수 없으면 섹션 숨김
            binding.recentWinnerCard.visibility = View.GONE
        }
    }
}
