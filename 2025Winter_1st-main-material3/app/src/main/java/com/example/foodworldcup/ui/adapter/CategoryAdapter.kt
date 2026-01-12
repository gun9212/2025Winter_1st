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
    private var categories: MutableList<CategoryItem>,
    private var selectedFoodIds: Set<Int>,
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
        
        // 체크박스 리스너를 먼저 제거하여 onBindViewHolder 중 리스너가 트리거되지 않도록 함
        holder.checkBox.setOnCheckedChangeListener(null)
        holder.checkBox.isChecked = category.isChecked
        
        // 펼쳐짐 상태에 따라 화살표 아이콘과 음식 리스트 표시
        if (category.isExpanded) {
            holder.expandIcon.setImageResource(android.R.drawable.arrow_up_float)
            holder.foodRecyclerView.visibility = View.VISIBLE
            
            // 해당 카테고리의 음식 리스트 가져오기
            val foods = FoodRepository.getFoodListByCategory(category.categoryName)
            
            // LayoutManager가 없으면 설정 (한 번만)
            if (holder.foodRecyclerView.layoutManager == null) {
                val layoutManager = LinearLayoutManager(holder.itemView.context)
                holder.foodRecyclerView.layoutManager = layoutManager
                // 성능 최적화 설정
                holder.foodRecyclerView.setHasFixedSize(true)
                holder.foodRecyclerView.itemAnimator = null // 애니메이션 비활성화
                holder.foodRecyclerView.setItemViewCacheSize(15) // 뷰 캐시 크기 증가
            }
            
            // 기존 어댑터 재사용 (성능 최적화)
            val existingAdapter = holder.foodRecyclerView.adapter as? FoodAdapter
            if (existingAdapter != null) {
                // 기존 어댑터가 있으면 데이터만 업데이트 (재생성 방지)
                existingAdapter.updateData(foods, selectedFoodIds)
            } else {
                // 어댑터가 없으면 새로 생성
                val foodAdapter = FoodAdapter(foods, selectedFoodIds) { foodId, isChecked ->
                    onFoodCheckedChanged(foodId, isChecked)
                }
                holder.foodRecyclerView.adapter = foodAdapter
            }
        } else {
            holder.expandIcon.setImageResource(android.R.drawable.arrow_down_float)
            holder.foodRecyclerView.visibility = View.GONE
        }
        
        // 체크박스 리스너를 마지막에 설정 (onBindViewHolder 완료 후)
        holder.checkBox.setOnCheckedChangeListener { _, isChecked ->
            onCategoryCheckedChanged(category.categoryName, isChecked)
        }
        
        // 카테고리 헤더 클릭 시 펼쳐지기/접히기
        holder.categoryHeader.setOnClickListener {
            onCategoryExpanded(position)
        }
    }

    override fun getItemCount(): Int = categories.size
    
    /**
     * 특정 카테고리 아이템을 업데이트하는 메서드입니다.
     * 성능 최적화를 위해 전체 리스트를 재생성하지 않고 특정 아이템만 업데이트합니다.
     */
    fun updateCategoryItem(position: Int, categoryItem: CategoryItem, recyclerView: RecyclerView? = null) {
        if (position in 0 until categories.size) {
            categories[position] = categoryItem
            // RecyclerView가 레이아웃 계산 중이 아닐 때만 업데이트
            if (recyclerView != null) {
                recyclerView.post {
                    if (position in 0 until categories.size) {
                        notifyItemChanged(position)
                    }
                }
            } else {
                notifyItemChanged(position)
            }
        }
    }
    
    /**
     * 선택된 음식 ID 목록을 업데이트하고, 펼쳐진 카테고리의 FoodAdapter를 직접 업데이트합니다.
     * 성능 최적화를 위해 CategoryAdapter를 다시 바인딩하지 않고 FoodAdapter만 업데이트합니다.
     */
    fun updateSelectedFoodIds(newSelectedFoodIds: Set<Int>, recyclerView: RecyclerView? = null) {
        selectedFoodIds = newSelectedFoodIds
        
        // RecyclerView가 있으면 직접 FoodAdapter를 업데이트 (더 효율적)
        if (recyclerView != null) {
            recyclerView.post {
                // 현재 화면에 보이는 ViewHolder들을 순회하며 FoodAdapter 업데이트
                for (i in 0 until recyclerView.childCount) {
                    val childView = recyclerView.getChildAt(i)
                    val viewHolder = recyclerView.getChildViewHolder(childView) as? CategoryViewHolder
                    viewHolder?.let { holder ->
                        val position = holder.adapterPosition
                        if (position != RecyclerView.NO_POSITION && 
                            position < categories.size && 
                            categories[position].isExpanded) {
                            // FoodAdapter 직접 업데이트 (CategoryAdapter 재바인딩 방지)
                            val foodAdapter = holder.foodRecyclerView.adapter as? FoodAdapter
                            foodAdapter?.updateSelectedFoodIds(newSelectedFoodIds)
                        }
                    }
                }
            }
        } else {
            // RecyclerView가 없으면 기존 방식 사용 (초기화 시)
            val expandedIndices = categories.mapIndexedNotNull { index, category ->
                if (category.isExpanded) index else null
            }
            if (expandedIndices.isNotEmpty()) {
                expandedIndices.forEach { index ->
                    notifyItemChanged(index)
                }
            }
        }
    }
    
    /**
     * 카테고리 아이템 리스트를 업데이트합니다.
     */
    fun updateCategories(newCategories: List<CategoryItem>) {
        categories.clear()
        categories.addAll(newCategories)
        notifyDataSetChanged()
    }
}
