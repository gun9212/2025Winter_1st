package com.example.foodworldcup.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.foodworldcup.databinding.ActivityIntroBinding

/**
 * 앱의 첫 화면(인트로 화면)을 담당하는 Activity입니다.
 * 앱 소개 및 게임 시작 버튼이 있는 화면입니다.
 * 
 * 레이아웃 파일: res/layout/activity_intro.xml
 */
class IntroActivity : AppCompatActivity() {

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

        // TODO: '게임 시작' 버튼 클릭 이벤트 리스너를 여기에 구현합니다.
        // 예: binding.startButton.setOnClickListener { 
        //     val intent = Intent(this, GameActivity::class.java)
        //     startActivity(intent)
        // }
        
        // TODO: '마이페이지' 버튼이 있다면 클릭 시 MyPageActivity로 이동하는 로직을 구현합니다.
    }

    override fun onResume() {
        super.onResume()
        // TODO: 필요시 인트로 화면이 다시 보일 때 실행할 로직을 여기에 추가합니다.
    }

    override fun onPause() {
        super.onPause()
        // TODO: 필요시 인트로 화면이 가려질 때 실행할 로직을 여기에 추가합니다.
    }
}
