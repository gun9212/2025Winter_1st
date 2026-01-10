package com.example.foodworldcup.ui.adapter

import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.foodworldcup.R
import com.example.foodworldcup.data.Food

/**
 * 통과한 음식 리스트를 표시하는 RecyclerView 어댑터입니다.
 * 결과 화면에서 사용됩니다.
 */
class PassedFoodAdapter(
    private val foods: List<Food>
) : RecyclerView.Adapter<PassedFoodAdapter.PassedFoodViewHolder>() {

    class PassedFoodViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val foodImageView: ImageView = itemView.findViewById(R.id.foodImageView)
        val foodNameTextView: TextView = itemView.findViewById(R.id.foodNameTextView)
        val passedLabelTextView: TextView = itemView.findViewById(R.id.passedLabelTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PassedFoodViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_passed_food, parent, false)
        return PassedFoodViewHolder(view)
    }

    override fun onBindViewHolder(holder: PassedFoodViewHolder, position: Int) {
        val food = foods[position]

        // 음식 이름 표시
        holder.foodNameTextView.text = food.name

        // 통과 레이블 표시 (선택사항이므로 숨김 처리 가능)
        holder.passedLabelTextView.visibility = View.VISIBLE

        // assets 폴더의 이미지 로드
        loadImageWithFallback(holder, food)
    }

    override fun getItemCount(): Int = foods.size

    /**
     * 이미지를 로드하고, 실패 시 여러 경로를 시도합니다.
     */
    private fun loadImageWithFallback(holder: PassedFoodViewHolder, food: Food) {
        if (food.imagePath.isNullOrEmpty()) {
            holder.foodImageView.setImageResource(R.drawable.ic_launcher_background)
            return
        }

        // 시도할 경로 리스트
        val pathsToTry = mutableListOf<String>()

        // 1. 원본 경로
        pathsToTry.add(food.imagePath)

        // 2. 확장자 변경 (.png <-> .jpg)
        if (food.imagePath.endsWith(".png")) {
            pathsToTry.add(food.imagePath.replace(".png", ".jpg"))
        } else if (food.imagePath.endsWith(".jpg")) {
            pathsToTry.add(food.imagePath.replace(".jpg", ".png"))
        }

        // 3. 음식 이름으로 직접 찾기
        pathsToTry.add("food_images/${food.category}/${food.name}.png")
        pathsToTry.add("food_images/${food.category}/${food.name}.jpg")

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
}
