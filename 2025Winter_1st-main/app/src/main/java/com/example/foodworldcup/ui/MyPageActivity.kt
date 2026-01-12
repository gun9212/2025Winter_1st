package com.example.foodworldcup.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.GridLayoutManager
import com.example.foodworldcup.data.FoodRepository
import com.example.foodworldcup.data.MapSelectedFood
import com.example.foodworldcup.databinding.ActivityMypageBinding
import com.example.foodworldcup.databinding.BottomSheetFoodDetailBinding
import com.example.foodworldcup.ui.adapter.PlateAdapter
import com.google.android.material.bottomsheet.BottomSheetDialog
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * 마이페이지를 담당하는 Activity입니다.
 * 지도에서 선택한 음식들을 접시 위에 표시합니다.
 * 
 * 레이아웃 파일: res/layout/activity_mypage.xml
 */
class MyPageActivity : BaseActivity() {

    private lateinit var binding: ActivityMypageBinding
    
    // RecyclerView 어댑터 (접시 그리드)
    private lateinit var adapter: PlateAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMypageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 하단 네비게이션 바 설정
        setupBottomNavigation(BaseActivity.Screen.ACCEPTED)

        // RecyclerView를 설정하고 접시 그리드를 표시합니다.
        setupRecyclerView()
    }

    /**
     * RecyclerView를 설정하는 함수입니다. (접시 그리드 형식)
     */
    private fun setupRecyclerView() {
        // 지도에서 선택한 음식 상세 정보 리스트 불러오기
        // 최신순으로 정렬 (날짜 기준 내림차순)
        val selectedFoods = preferenceManager.getMapSelectedFoods()
            .sortedByDescending { it.selectedDate }

        // 접시 리스트 생성 (MapSelectedFood 객체 사용)
        // 선택된 음식이 있으면 해당 객체를, 없으면 null을 넣어서 빈 접시 표시
        val plateList = mutableListOf<MapSelectedFood?>()
        
        // 선택된 음식 추가 (최신순으로 정렬된 상태)
        plateList.addAll(selectedFoods)
        
        // 최소 12개 접시를 보장 (빈 접시는 null로 표시)
        // 12개 이상이면 그대로 유지 (동적으로 추가됨)
        while (plateList.size < 12) {
            plateList.add(null)
        }

        // RecyclerView에 GridLayoutManager 설정 (2열)
        if (binding.plateRecyclerView.layoutManager == null) {
            binding.plateRecyclerView.layoutManager = GridLayoutManager(this, 2)
        }

        // PlateAdapter 생성 및 연결 (클릭 리스너 추가, MapSelectedFood의 id 전달)
        adapter = PlateAdapter(plateList) { selectedFoodId ->
            showFoodDetailDialog(selectedFoodId)
        }
        binding.plateRecyclerView.adapter = adapter
    }

    /**
     * 음식 상세 정보를 표시하는 BottomSheetDialog를 보여주는 함수입니다.
     */
    private fun showFoodDetailDialog(selectedFoodId: Long) {
        val selectedFoods = preferenceManager.getMapSelectedFoods()
        // 고유 ID로 정확한 항목 찾기
        val selectedFood = selectedFoods.find { it.id == selectedFoodId } ?: return
        
        val food = FoodRepository.getFoodById(selectedFood.foodId) ?: return

        // BottomSheetDialog 생성
        val bottomSheetDialog = BottomSheetDialog(this)
        val bottomSheetBinding = BottomSheetFoodDetailBinding.inflate(LayoutInflater.from(this))
        bottomSheetDialog.setContentView(bottomSheetBinding.root)
        
        // 키보드가 올라올 때 BottomSheetDialog가 조정되도록 설정
        bottomSheetDialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)

        // 날짜 포맷터
        val dateFormat = SimpleDateFormat("yyyy년 MM월 dd일", Locale.KOREAN)

        // 정보 표시
        bottomSheetBinding.foodNameTextView.text = food.name
        bottomSheetBinding.dateTextView.text = dateFormat.format(selectedFood.getDate())
        bottomSheetBinding.placeNameTextView.text = selectedFood.placeName.ifEmpty { "정보 없음" }
        bottomSheetBinding.placeAddressTextView.text = selectedFood.placeAddress.ifEmpty { "주소 정보 없음" }
        bottomSheetBinding.memoEditText.setText(selectedFood.memo)
        
        // EditText가 자동으로 포커스를 받지 않도록 설정
        bottomSheetBinding.memoEditText.clearFocus()
        
        // 메모 EditText 클릭 시에만 키보드 표시
        bottomSheetBinding.memoEditText.setOnClickListener {
            bottomSheetBinding.memoEditText.isFocusableInTouchMode = true
            bottomSheetBinding.memoEditText.requestFocus()
            // 키보드 표시
            val imm = ContextCompat.getSystemService(this, InputMethodManager::class.java)
            imm?.showSoftInput(bottomSheetBinding.memoEditText, InputMethodManager.SHOW_IMPLICIT)
        }

        // 저장 버튼 클릭
        bottomSheetBinding.saveButton.setOnClickListener {
            val memo = bottomSheetBinding.memoEditText.text.toString()
            preferenceManager.updateMapSelectedFoodMemo(selectedFoodId, memo)
            Toast.makeText(this, "메모가 저장되었습니다.", Toast.LENGTH_SHORT).show()
            bottomSheetDialog.dismiss()
            setupRecyclerView() // 리스트 새로고침
        }

        // 삭제 버튼 클릭
        bottomSheetBinding.deleteButton.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("삭제 확인")
                .setMessage("정말 이 기록을 삭제하시겠습니까?")
                .setPositiveButton("삭제") { _, _ ->
                    preferenceManager.removeMapSelectedFood(selectedFoodId)
                    Toast.makeText(this, "기록이 삭제되었습니다.", Toast.LENGTH_SHORT).show()
                    bottomSheetDialog.dismiss()
                    setupRecyclerView() // 리스트 새로고침 (삭제 후 다음 음식이 자동으로 채워짐)
                }
                .setNegativeButton("취소", null)
                .show()
        }

        bottomSheetDialog.show()
    }
    override fun onResume() {
        super.onResume()
        // 마이페이지가 다시 보일 때 최신 선택된 음식을 다시 불러옵니다.
        // (지도에서 새로운 음식이 선택되었을 수 있으므로)
        setupRecyclerView()
    }
}
