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
 * 게임 결과를 표시하는 Activity입니다.
 * 최종 우승 음식을 보여주고, 우승 기록을 저장하며, 다시하기/마이페이지 이동 기능을 제공합니다.
 * 
 * 레이아웃 파일: res/layout/activity_result.xml
 */
class ResultActivity : AppCompatActivity() {

    private lateinit var binding: ActivityResultBinding
    
    // GameActivity로부터 전달받은 우승 음식
    private var winnerFood: Food? = null
    
    // SharedPreferences 관리 객체
    private lateinit var preferenceManager: PreferenceManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityResultBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // PreferenceManager 초기화
        preferenceManager = PreferenceManager(this)

        // TODO: GameActivity로부터 전달받은 우승 음식 데이터를 가져옵니다.
        // 예: winnerFood = intent.getParcelableExtra<Food>("winner_food")
        // 또는: val foodId = intent.getIntExtra("winner_food_id", -1)
        //      winnerFood = FoodRepository에서 id로 찾기
        
        // TODO: 우승 기록을 저장합니다.
        saveWinRecord()
        
        // TODO: 화면에 우승 음식 정보를 표시합니다.
        displayWinner()
        
        // TODO: 버튼 클릭 이벤트를 설정합니다.
        setupButtons()
    }

    /**
     * 우승 기록을 SharedPreferences에 저장하는 함수입니다.
     */
    private fun saveWinRecord() {
        // TODO: winnerFood가 null이 아니면 WinRecord를 생성하여 PreferenceManager에 저장합니다.
        // 예: val record = WinRecord(
        //     id = System.currentTimeMillis(),
        //     foodName = winnerFood?.name ?: "",
        //     winDate = Date()
        // )
        // preferenceManager.addWinRecord(record)
    }

    /**
     * 화면에 우승 음식 정보를 표시하는 함수입니다.
     */
    private fun displayWinner() {
        // TODO: winnerFood의 이름과 이미지를 화면에 표시합니다.
        // 예: binding.winnerNameTextView.text = winnerFood?.name
        //     Glide.with(this).load(winnerFood?.imageResId).into(binding.winnerImageView)
    }

    /**
     * 버튼 클릭 이벤트를 설정하는 함수입니다.
     */
    private fun setupButtons() {
        // TODO: '다시하기' 버튼 클릭 시 GameActivity로 이동합니다.
        // 예: binding.restartButton.setOnClickListener {
        //     val intent = Intent(this, GameActivity::class.java)
        //     startActivity(intent)
        //     finish()
        // }
        
        // TODO: '마이페이지' 버튼 클릭 시 MyPageActivity로 이동합니다.
        // 예: binding.myPageButton.setOnClickListener {
        //     val intent = Intent(this, MyPageActivity::class.java)
        //     startActivity(intent)
        // }
        
        // TODO: '지도 보기' 버튼 클릭 시 MapActivity로 이동합니다.
        // 예: binding.mapButton.setOnClickListener {
        //     val intent = Intent(this, MapActivity::class.java)
        //     intent.putExtra("winner_food", winnerFood)
        //     // 또는: intent.putExtra("food_name", winnerFood?.name)
        //     startActivity(intent)
        // }
        
        // TODO: '홈으로' 버튼이 있다면 IntroActivity로 이동하는 로직을 구현합니다.
    }

    /**
     * (추후 구현) 지도 API를 연동하여 주변 음식점을 찾아주는 함수입니다.
     */
    private fun showNearbyRestaurants() {
        // TODO: Google Maps API 또는 Kakao Map API를 사용하여
        // winnerFood의 이름으로 주변 음식점을 검색하고 지도에 표시합니다.
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
