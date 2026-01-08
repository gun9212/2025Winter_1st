package com.example.foodworldcup.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.foodworldcup.data.WinRecord
import com.example.foodworldcup.databinding.ActivityMypageBinding
import com.example.foodworldcup.utils.PreferenceManager

/**
 * 마이페이지를 담당하는 Activity입니다.
 * SharedPreferences에 저장된 우승 기록을 리스트 형태로 보여줍니다.
 * 
 * 레이아웃 파일: res/layout/activity_mypage.xml
 */
class MyPageActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMypageBinding
    
    // SharedPreferences 관리 객체
    private lateinit var preferenceManager: PreferenceManager
    
    // 우승 기록 리스트
    private var winRecords: List<WinRecord> = emptyList()
    
    // RecyclerView 어댑터 (추후 구현)
    // private lateinit var adapter: WinRecordAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMypageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // PreferenceManager 초기화
        preferenceManager = PreferenceManager(this)

        // TODO: 저장된 우승 기록을 불러옵니다.
        loadWinRecords()
        
        // TODO: RecyclerView를 설정하고 기록 리스트를 표시합니다.
        setupRecyclerView()
        
        // TODO: 버튼 클릭 이벤트를 설정합니다 (기록 삭제, 홈으로 등)
        setupButtons()
    }

    /**
     * SharedPreferences에서 우승 기록을 불러오는 함수입니다.
     */
    private fun loadWinRecords() {
        // TODO: PreferenceManager.getWinRecords()를 사용하여 저장된 기록을 불러옵니다.
        // winRecords = preferenceManager.getWinRecords()
    }

    /**
     * RecyclerView를 설정하는 함수입니다.
     */
    private fun setupRecyclerView() {
        // TODO: RecyclerView에 LinearLayoutManager를 설정합니다.
        // 예: binding.recyclerView.layoutManager = LinearLayoutManager(this)
        
        // TODO: WinRecordAdapter를 생성하고 RecyclerView에 연결합니다.
        // 예: adapter = WinRecordAdapter(winRecords)
        //     binding.recyclerView.adapter = adapter
        
        // TODO: 기록이 없을 때 보여줄 빈 화면 메시지를 설정합니다.
        // 예: if (winRecords.isEmpty()) {
        //     binding.emptyTextView.visibility = View.VISIBLE
        //     binding.recyclerView.visibility = View.GONE
        // }
    }

    /**
     * 버튼 클릭 이벤트를 설정하는 함수입니다.
     */
    private fun setupButtons() {
        // TODO: '기록 삭제' 버튼이 있다면 클릭 시 모든 기록을 삭제하는 로직을 구현합니다.
        // 예: binding.clearButton.setOnClickListener {
        //     preferenceManager.clearWinRecords()
        //     loadWinRecords()
        //     setupRecyclerView()
        // }
        
        // TODO: '홈으로' 버튼 클릭 시 IntroActivity로 이동하는 로직을 구현합니다.
        // 예: binding.homeButton.setOnClickListener {
        //     val intent = Intent(this, IntroActivity::class.java)
        //     startActivity(intent)
        //     finish()
        // }
    }

    override fun onResume() {
        super.onResume()
        // TODO: 마이페이지가 다시 보일 때 최신 기록을 다시 불러옵니다.
        // (다른 화면에서 새로운 기록이 추가되었을 수 있으므로)
        loadWinRecords()
        setupRecyclerView()
    }

    override fun onPause() {
        super.onPause()
        // TODO: 필요시 마이페이지가 가려질 때 실행할 로직을 여기에 추가합니다.
    }
}
