package com.example.foodworldcup.ui

import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.foodworldcup.data.WinRecord
import com.example.foodworldcup.databinding.ActivityMypageBinding
import com.example.foodworldcup.utils.PreferenceManager

/**
 * 마이페이지를 담당하는 Activity입니다.
 * 우승한 음식들을 갤러리 형식으로 보여주고, 날짜+시간, 메모, 그릇 기능을 제공합니다.
 * 
 * 레이아웃 파일: res/layout/activity_mypage.xml
 */
class MyPageActivity : BaseActivity() {

    private lateinit var binding: ActivityMypageBinding
    
    // SharedPreferences 관리 객체
    private lateinit var preferenceManager: PreferenceManager
    
    // 우승 기록 리스트
    private var winRecords: List<WinRecord> = emptyList()
    
    // RecyclerView 어댑터 (갤러리 형식)
    // private lateinit var adapter: WinRecordGalleryAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMypageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // PreferenceManager 초기화
        preferenceManager = PreferenceManager(this)

        // 하단 네비게이션 바 설정
        setupBottomNavigation(BaseActivity.Screen.ACCEPTED)

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
     * RecyclerView를 설정하는 함수입니다. (갤러리 형식)
     */
    private fun setupRecyclerView() {
        // TODO: RecyclerView에 GridLayoutManager를 설정합니다 (갤러리 형식).
        // 예: binding.recyclerView.layoutManager = GridLayoutManager(this, 2)
        
        // TODO: WinRecordGalleryAdapter를 생성하고 RecyclerView에 연결합니다.
        // 예: adapter = WinRecordGalleryAdapter(winRecords) { record ->
        //     // 갤러리 아이템 클릭 시: 메모 편집 또는 상세 보기
        //     showMemoDialog(record)
        // }
        // binding.recyclerView.adapter = adapter
        
        // TODO: 기록이 없을 때 보여줄 빈 화면 메시지를 설정합니다.
        // 예: if (winRecords.isEmpty()) {
        //     binding.emptyTextView.visibility = View.VISIBLE
        //     binding.recyclerView.visibility = View.GONE
        //     binding.bowlView.visibility = View.GONE
        // } else {
        //     binding.emptyTextView.visibility = View.GONE
        //     binding.recyclerView.visibility = View.VISIBLE
        //     binding.bowlView.visibility = View.VISIBLE
        //     updateBowlView() // 그릇에 음식들 올려두기
        // }
    }
    
    /**
     * 그릇에 음식들을 올려두는 뷰를 업데이트하는 함수입니다.
     */
    private fun updateBowlView() {
        // TODO: 모든 우승 기록의 음식들을 그릇 뷰에 표시합니다.
        // 예: val allFoodImages = winRecords.flatMap { record ->
        //     record.selectedFoods.map { foodId ->
        //         FoodRepository.getFoodList().find { it.id == foodId }?.imageResId
        //     }.filterNotNull()
        // }
        // 그릇 이미지 위에 음식 이미지들을 오버레이로 표시
    }
    
    /**
     * 메모 편집 다이얼로그를 보여주는 함수입니다.
     */
    private fun showMemoDialog(record: WinRecord) {
        // TODO: AlertDialog 또는 BottomSheetDialog를 사용하여 메모 편집 다이얼로그를 표시합니다.
        // 예: val dialog = AlertDialog.Builder(this)
        //     .setTitle("메모 편집")
        //     .setView(EditText(this).apply { setText(record.memo) })
        //     .setPositiveButton("저장") { _, _ ->
        //         // 메모 저장 로직
        //         updateMemo(record.id, memoText)
        //     }
        //     .setNegativeButton("취소", null)
        //     .create()
        // dialog.show()
    }
    
    /**
     * 기록의 메모를 업데이트하는 함수입니다.
     */
    private fun updateMemo(recordId: Long, memo: String) {
        // TODO: PreferenceManager를 통해 해당 기록의 메모를 업데이트합니다.
        // 예: val updatedRecords = winRecords.map { record ->
        //     if (record.id == recordId) record.copy(memo = memo) else record
        // }
        // preferenceManager.saveWinRecords(updatedRecords)
        // loadWinRecords()
        // setupRecyclerView()
    }

    /**
     * 버튼 클릭 이벤트를 설정하는 함수입니다.
     */
    private fun setupButtons() {
        // TODO: 갤러리 아이템의 삭제 버튼 클릭 시 해당 기록을 삭제합니다.
        // (WinRecordGalleryAdapter 내부에서 구현)
        // 예: adapter.onDeleteClick = { record ->
        //     deleteRecord(record.id)
        // }
        
        // TODO: '홈으로' 버튼 클릭 시 IntroActivity로 이동하는 로직을 구현합니다.
        // 예: binding.homeButton.setOnClickListener {
        //     val intent = Intent(this, IntroActivity::class.java)
        //     startActivity(intent)
        //     finish()
        // }
    }
    
    /**
     * 기록을 삭제하는 함수입니다.
     */
    private fun deleteRecord(recordId: Long) {
        // TODO: PreferenceManager를 통해 해당 기록을 삭제합니다.
        // 예: val updatedRecords = winRecords.filter { it.id != recordId }
        // preferenceManager.saveWinRecords(updatedRecords)
        // loadWinRecords()
        // setupRecyclerView()
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
