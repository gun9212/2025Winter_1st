package com.example.foodworldcup.ui.adapter

import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.foodworldcup.R
import com.example.foodworldcup.data.Food

/**
 * 음식 리스트를 표시하는 RecyclerView 어댑터입니다.
 * 카테고리 내부의 음식들을 표시할 때 사용됩니다.
 */
class FoodAdapter(
    private val foods: List<Food>,
    private val selectedFoodIds: Set<Int>,
    private val onFoodCheckedChanged: (Int, Boolean) -> Unit
) : RecyclerView.Adapter<FoodAdapter.FoodViewHolder>() {

    class FoodViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val checkBox: CheckBox = itemView.findViewById(R.id.foodCheckBox)
        val foodImageView: ImageView = itemView.findViewById(R.id.foodImageView)
        val foodNameTextView: TextView = itemView.findViewById(R.id.foodNameTextView)
        val foodDescriptionTextView: TextView = itemView.findViewById(R.id.foodDescriptionTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FoodViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_food, parent, false)
        return FoodViewHolder(view)
    }

    override fun onBindViewHolder(holder: FoodViewHolder, position: Int) {
        val food = foods[position]
        val isSelected = food.id in selectedFoodIds

        holder.foodNameTextView.text = food.name
        holder.checkBox.isChecked = isSelected

        // 음식 설명은 표시하지 않음 (이름만 표시)
        holder.foodDescriptionTextView.visibility = View.GONE

        // assets 폴더의 이미지 로드
        if (!food.imagePath.isNullOrEmpty()) {
            loadImageWithFallback(holder, food.imagePath, food.name, food.category)
        } else {
            // 이미지 경로가 없으면 기본 이미지 표시
            holder.foodImageView.setImageResource(R.drawable.ic_launcher_background)
        }

        holder.checkBox.setOnCheckedChangeListener(null) // 기존 리스너 제거
        holder.checkBox.isChecked = isSelected
        holder.checkBox.setOnCheckedChangeListener { _, isChecked ->
            onFoodCheckedChanged(food.id, isChecked)
        }

        // 아이템 클릭 시 체크박스 토글
        holder.itemView.setOnClickListener {
            holder.checkBox.isChecked = !holder.checkBox.isChecked
        }
    }
    
    /**
     * 이미지를 로드하고, 실패 시 여러 경로를 시도하는 함수
     */
    private fun loadImageWithFallback(
        holder: FoodViewHolder,
        imagePath: String,
        foodName: String,
        category: String
    ) {
        // 시도할 경로 리스트
        val pathsToTry = mutableListOf<String>()
        
        // 1. 원본 경로
        pathsToTry.add(imagePath)
        
        // 2. 확장자 변경 (.png <-> .jpg)
        if (imagePath.endsWith(".png")) {
            pathsToTry.add(imagePath.replace(".png", ".jpg"))
        } else if (imagePath.endsWith(".jpg")) {
            pathsToTry.add(imagePath.replace(".jpg", ".png"))
        }
        
        // 3. 음식 이름으로 직접 찾기
        pathsToTry.add("food_images/$category/$foodName.png")
        pathsToTry.add("food_images/$category/$foodName.jpg")
        
        // 각 경로를 시도
        for (path in pathsToTry) {
            try {
                val inputStream = holder.itemView.context.assets.open(path)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream.close()
                
                if (bitmap != null) {
                    Glide.with(holder.itemView.context)
                        .load(bitmap)
                        .placeholder(R.drawable.ic_launcher_background)
                        .error(R.drawable.ic_launcher_background)
                        .centerCrop()
                        .into(holder.foodImageView)
                    return // 성공하면 종료
                }
            } catch (e: Exception) {
                // 다음 경로 시도
                continue
            }
        }
        
        // 모든 시도 실패 시 기본 이미지 표시
        holder.foodImageView.setImageResource(R.drawable.ic_launcher_background)
    }

    override fun getItemCount(): Int = foods.size
}
