package com.example.foodworldcup.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.foodworldcup.R
import com.example.foodworldcup.data.Food
import com.example.foodworldcup.data.FoodRepository
import com.example.foodworldcup.data.WinRecord
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * 우승 기록을 갤러리 형식으로 표시하는 RecyclerView 어댑터입니다.
 * 마이페이지에서 사용됩니다.
 * 각 WinRecord는 하나의 카드로 표시되며, 카드 내부에 해당 기록의 음식들을 GridLayout으로 표시합니다.
 */
class WinRecordGalleryAdapter(
    private val winRecords: List<WinRecord>,
    private val onRecordClick: (WinRecord) -> Unit = {},
    private val onDeleteClick: (WinRecord) -> Unit = {}
) : RecyclerView.Adapter<WinRecordGalleryAdapter.WinRecordViewHolder>() {

    // 날짜 포맷터
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

    class WinRecordViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val winDateTextView: TextView = itemView.findViewById(R.id.winDateTextView)
        val memoTextView: TextView = itemView.findViewById(R.id.memoTextView)
        val deleteButton: ImageView = itemView.findViewById(R.id.deleteButton)
        val foodRecyclerView: RecyclerView = itemView.findViewById(R.id.foodRecyclerView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WinRecordViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_win_record, parent, false)
        return WinRecordViewHolder(view)
    }

    override fun onBindViewHolder(holder: WinRecordViewHolder, position: Int) {
        val record = winRecords[position]

        // 날짜 표시
        holder.winDateTextView.text = dateFormat.format(record.winDate)

        // 메모 표시 (메모가 있을 때만)
        if (record.memo.isNotEmpty()) {
            holder.memoTextView.text = record.memo
            holder.memoTextView.visibility = View.VISIBLE
        } else {
            holder.memoTextView.visibility = View.GONE
        }

        // 삭제 버튼 설정
        holder.deleteButton.visibility = View.VISIBLE
        holder.deleteButton.isClickable = true
        holder.deleteButton.isFocusable = true
        holder.deleteButton.setOnClickListener {
            // 삭제 버튼 클릭 시 카드 클릭 이벤트가 발생하지 않도록 처리
            // (Android는 자식 뷰가 클릭 가능하면 부모의 클릭 이벤트를 차단하지만,
            //  명시적으로 처리하여 더 안전하게 함)
            onDeleteClick(record)
        }

        // 음식 ID 리스트를 Food 객체 리스트로 변환
        val foods = record.selectedFoods.mapNotNull { foodId ->
            FoodRepository.getFoodById(foodId)
        }

        // 내부 RecyclerView 설정 (PassedFoodAdapter 재사용)
        if (holder.foodRecyclerView.layoutManager == null) {
            holder.foodRecyclerView.layoutManager = GridLayoutManager(holder.itemView.context, 2)
        }
        holder.foodRecyclerView.adapter = PassedFoodAdapter(foods)

        // 카드 클릭 시 (메모 편집 등)
        // 삭제 버튼은 자식 뷰이므로 클릭 가능하면 자동으로 부모 클릭 이벤트가 차단됨
        holder.itemView.setOnClickListener {
            onRecordClick(record)
        }
    }

    override fun getItemCount(): Int = winRecords.size
}
