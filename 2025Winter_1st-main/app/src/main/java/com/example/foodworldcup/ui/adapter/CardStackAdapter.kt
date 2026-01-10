package com.example.foodworldcup.ui.adapter

import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.foodworldcup.R
import com.example.foodworldcup.data.Food
import com.yuyakaido.android.cardstackview.Direction

/**
 * CardStackView를 위한 어댑터입니다.
 * 음식 카드를 표시하고, 스와이프 시 Overlay (Like/Nope)를 표시합니다.
 * 
 * 주요 기능:
 * - 음식 데이터를 카드로 표시
 * - 스와이프 방향에 따라 Overlay 표시/숨김
 * - Overlay 투명도 조절
 */
class CardStackAdapter(
    private var foods: MutableList<Food>
) : RecyclerView.Adapter<CardStackAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val foodImageView: ImageView = itemView.findViewById(R.id.foodImageView)
        val foodNameTextView: TextView = itemView.findViewById(R.id.foodNameTextView)
        val likeOverlay: TextView = itemView.findViewById(R.id.likeOverlay)
        val nopeOverlay: TextView = itemView.findViewById(R.id.nopeOverlay)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_food_card, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val food = foods[position]
        
        // 음식 이름 표시
        holder.foodNameTextView.text = food.name
        
        // 이미지 로드
        loadImageWithFallback(holder, food)
        
        // Overlay 초기 상태로 설정 (숨김, 투명)
        hideOverlay(holder)
    }

    override fun getItemCount(): Int = foods.size
    
    /**
     * 어댑터의 음식 리스트를 업데이트합니다.
     * 
     * @param newFoods 새로운 음식 리스트
     */
    fun setFoods(newFoods: List<Food>) {
        foods.clear()
        foods.addAll(newFoods)
        notifyDataSetChanged()
    }
    
    /**
     * 현재 음식 리스트를 반환합니다.
     */
    fun getFoods(): List<Food> = foods.toList()

    /**
     * 이미지를 로드하고, 실패 시 여러 경로를 시도합니다.
     * 깜빡임 방지를 위해 Bitmap을 직접 ImageView에 설정합니다.
     * 게임 화면에서는 기존 음식 이미지(imagePath)를 사용합니다.
     */
    private fun loadImageWithFallback(holder: ViewHolder, food: Food) {
        if (food.imagePath.isNullOrEmpty()) {
            holder.foodImageView.setImageResource(R.drawable.ic_launcher_background)
            holder.foodImageView.setBackgroundColor(android.graphics.Color.WHITE)
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
                    try {
                        // ImageView에 직접 설정 (동기적, 즉시 표시)
                        holder.foodImageView.setImageBitmap(bitmap)
                        holder.foodImageView.scaleType = android.widget.ImageView.ScaleType.CENTER_CROP
                        holder.foodImageView.setBackgroundColor(android.graphics.Color.WHITE)
                    } catch (e: Exception) {
                        // Bitmap 직접 설정 실패 시 Glide 사용 (폴백)
                        val requestOptions = RequestOptions()
                            .placeholder(null)
                            .error(ColorDrawable(android.graphics.Color.WHITE))
                            .centerCrop()
                            .dontAnimate()
                        
                        Glide.with(holder.itemView.context)
                            .load(bitmap)
                            .apply(requestOptions)
                            .into(holder.foodImageView)
                    }
                    
                    return // 성공하면 종료
                }
            } catch (e: Exception) {
                // 다음 경로 시도
                continue
            }
        }
        
        // 모든 시도 실패 시 기본 이미지 표시
        holder.foodImageView.setImageResource(R.drawable.ic_launcher_background)
        holder.foodImageView.setBackgroundColor(android.graphics.Color.WHITE)
    }
    

    /**
     * 스와이프 비율에 따라 Overlay 투명도를 조절합니다.
     * 
     * @param holder ViewHolder
     * @param ratio 스와이프 비율 (0.0 ~ 1.0)
     * @param direction 스와이프 방향 (Left, Right)
     */
    fun setOverlayAlpha(holder: ViewHolder, ratio: Float, direction: Direction) {
        val alpha = ratio.coerceIn(0f, 1f)
        
        when (direction) {
            Direction.Right -> {
                holder.likeOverlay.visibility = View.VISIBLE
                holder.likeOverlay.alpha = alpha
                holder.nopeOverlay.visibility = View.GONE
            }
            Direction.Left -> {
                holder.nopeOverlay.visibility = View.VISIBLE
                holder.nopeOverlay.alpha = alpha
                holder.likeOverlay.visibility = View.GONE
            }
            else -> {
                hideOverlay(holder)
            }
        }
    }

    /**
     * 특정 방향의 Overlay를 표시합니다.
     * 
     * @param holder ViewHolder
     * @param direction 스와이프 방향 (Left, Right)
     */
    fun showOverlay(holder: ViewHolder, direction: Direction) {
        when (direction) {
            Direction.Right -> {
                holder.likeOverlay.visibility = View.VISIBLE
                holder.likeOverlay.alpha = 1f
                holder.nopeOverlay.visibility = View.GONE
            }
            Direction.Left -> {
                holder.nopeOverlay.visibility = View.VISIBLE
                holder.nopeOverlay.alpha = 1f
                holder.likeOverlay.visibility = View.GONE
            }
            else -> {
                hideOverlay(holder)
            }
        }
    }

    /**
     * Overlay를 숨깁니다.
     * 
     * @param holder ViewHolder
     */
    fun hideOverlay(holder: ViewHolder) {
        holder.likeOverlay.visibility = View.GONE
        holder.likeOverlay.alpha = 0f
        holder.nopeOverlay.visibility = View.GONE
        holder.nopeOverlay.alpha = 0f
    }

}
