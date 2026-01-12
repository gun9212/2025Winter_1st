package com.example.foodworldcup.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.foodworldcup.R
import com.example.foodworldcup.data.Food
import com.example.foodworldcup.data.FoodRepository

/**
 * 카테고리 리스트를 표시하는 RecyclerView 어댑터입니다.
 * Food Genres 화면에서 사용됩니다.
 * 카테고리를 클릭하면 해당 카테고리의 음식 리스트가 펼쳐집니다.
 */
class CategoryAdapter(
    private val categories: List<CategoryItem>,
    private val selectedFoodIds: Set<Int>,
    private val onCategoryCheckedChanged: (String, Boolean) -> Unit,
    private val onFoodCheckedChanged: (Int, Boolean) -> Unit,
    private val onCategoryExpanded: (Int) -> Unit
) : RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder>() {

    /**
     * 카테고리 아이템 데이터 클래스
     */
    data class CategoryItem(
        val categoryName: String,      // 한글 카테고리 이름 (예: "한식")
        val categoryNameEn: String,    // 영문 카테고리 이름 (예: "Korean")
        val isChecked: Boolean,         // 체크 상태
        val isExpanded: Boolean = false // 펼쳐짐 상태
    )

    class CategoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val checkBox: CheckBox = itemView.findViewById(R.id.categoryCheckBox)
        val categoryNameTextView: TextView = itemView.findViewById(R.id.categoryNameTextView)
        val categoryNameEnTextView: TextView = itemView.findViewById(R.id.categoryNameEnTextView)
        val expandIcon: ImageView = itemView.findViewById(R.id.expandIcon)
        val categoryHeader: View = itemView.findViewById(R.id.categoryHeader)
        val foodRecyclerView: RecyclerView = itemView.findViewById(R.id.foodRecyclerView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_category, parent, false)
        return CategoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        val category = categories[position]
        
        holder.categoryNameTextView.text = category.categoryName
        holder.categoryNameEnTextView.text = category.categoryNameEn
        holder.checkBox.isChecked = category.isChecked
        
        // 펼쳐짐 상태에 따라 화살표 아이콘과 음식 리스트 표시
        if (category.isExpanded) {
            holder.expandIcon.setImageResource(android.R.drawable.arrow_up_float)
            holder.foodRecyclerView.visibility = View.VISIBLE
            
            // 해당 카테고리의 음식 리스트 가져오기
            val foods = FoodRepository.getFoodListByCategory(category.categoryName)
            
            // LayoutManager가 없으면 설정 (한 번만)
            if (holder.foodRecyclerView.layoutManager == null) {
                holder.foodRecyclerView.layoutManager = LinearLayoutManager(holder.itemView.context)
            }
            
            // 어댑터 생성 또는 업데이트
            // 이미지 로딩 로직이 개선되어 깜빡임이 방지되므로 어댑터를 재생성해도 안전함
            val foodAdapter = FoodAdapter(foods, selectedFoodIds) { foodId, isChecked ->
                onFoodCheckedChanged(foodId, isChecked)
            }
            holder.foodRecyclerView.adapter = foodAdapter
        } else {
            holder.expandIcon.setImageResource(android.R.drawable.arrow_down_float)
            holder.foodRecyclerView.visibility = View.GONE
        }
        
        // 체크박스 클릭 시 카테고리 전체 선택/해제
        holder.checkBox.setOnCheckedChangeListener { _, isChecked ->
            onCategoryCheckedChanged(category.categoryName, isChecked)
        }
        
        // 카테고리 헤더 클릭 시 펼쳐지기/접히기
        holder.categoryHeader.setOnClickListener {
            onCategoryExpanded(position)
        }
    }

    override fun getItemCount(): Int = categories.size
}
