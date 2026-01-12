package com.example.foodworldcup.ui.adapter

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.example.foodworldcup.R
import com.example.foodworldcup.data.Food
import com.example.foodworldcup.data.FoodRepository

/**
 * 접시와 음식을 표시하는 RecyclerView 어댑터입니다.
 * 마이페이지에서 사용되며, 접시 배경 위에 선택된 음식 이미지를 오버레이합니다.
 */
class PlateAdapter(
    private val foodIds: List<Int?>, // null이면 빈 접시, Int면 해당 음식 ID
    private val onItemClick: ((Int) -> Unit)? = null // 클릭 리스너 (foodId 전달)
) : RecyclerView.Adapter<PlateAdapter.PlateViewHolder>() {

    class PlateViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val plateImageView: ImageView = itemView.findViewById(R.id.plateImageView)
        val foodImageView: ImageView = itemView.findViewById(R.id.foodImageView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlateViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_plate, parent, false)
        return PlateViewHolder(view)
    }

    override fun onBindViewHolder(holder: PlateViewHolder, position: Int) {
        val foodId = foodIds[position]

        // 접시 이미지 로드 (assets에서)
        loadPlateImage(holder)

        // 음식이 있으면 표시, 없으면 숨김
        if (foodId != null) {
            val food = FoodRepository.getFoodById(foodId)
            if (food != null) {
                holder.foodImageView.visibility = View.VISIBLE
                loadFoodImage(holder, food)
                
                // 클릭 리스너 설정 (음식이 있을 때만)
                holder.itemView.setOnClickListener {
                    onItemClick?.invoke(foodId)
                }
            } else {
                holder.foodImageView.visibility = View.GONE
                holder.itemView.setOnClickListener(null)
            }
        } else {
            holder.foodImageView.visibility = View.GONE
            holder.itemView.setOnClickListener(null)
        }
    }

    override fun getItemCount(): Int = foodIds.size

    /**
     * 접시 이미지를 로드하는 함수입니다.
     */
    private fun loadPlateImage(holder: PlateViewHolder) {
        try {
            // assets에서 접시 이미지 로드
            // 큰 이미지 파일이므로 BitmapFactory.Options로 메모리 최적화
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = false
                inSampleSize = 1 // 필요시 조정 가능
                inPreferredConfig = Bitmap.Config.RGB_565 // 메모리 절약
            }
            
            // 접시 이미지 로드 (영문 파일명 사용)
            val inputStream = holder.itemView.context.assets.open("plate.png")
            val bitmap = BitmapFactory.decodeStream(inputStream, null, options)
            inputStream.close()

            if (bitmap != null && !bitmap.isRecycled) {
                Log.d("PlateAdapter", "접시 이미지 로드 성공: ${bitmap.width}x${bitmap.height}")
                
                // BitmapFactory로 직접 로드한 이미지를 ImageView에 직접 설정
                // (Glide 캐시 문제를 피하기 위해 직접 설정)
                holder.plateImageView.setImageBitmap(bitmap)
                holder.plateImageView.scaleType = ImageView.ScaleType.FIT_CENTER
                holder.plateImageView.adjustViewBounds = true
            } else {
                Log.e("PlateAdapter", "접시 이미지 Bitmap이 null이거나 재활용됨")
                // Bitmap이 null인 경우 기본 이미지 표시
                holder.plateImageView.setImageResource(R.drawable.ic_launcher_background)
            }
        } catch (e: OutOfMemoryError) {
            Log.e("PlateAdapter", "메모리 부족으로 접시 이미지 로드 실패", e)
            // 메모리 부족 시 기본 이미지 표시
            holder.plateImageView.setImageResource(R.drawable.ic_launcher_background)
        } catch (e: Exception) {
            Log.e("PlateAdapter", "접시 이미지 로드 실패: ${e.message}", e)
            e.printStackTrace()
            // 예외 발생 시 기본 이미지 표시
            holder.plateImageView.setImageResource(R.drawable.ic_launcher_background)
        }
    }

    /**
     * 음식 캐릭터 이미지를 로드하는 함수입니다.
     * 접시 위에 오버레이로 표시됩니다.
     */
    private fun loadFoodImage(holder: PlateViewHolder, food: Food) {
        if (food.characterImagePath.isNullOrEmpty()) {
            holder.foodImageView.setImageResource(R.drawable.ic_launcher_background)
            holder.foodImageView.setBackgroundColor(Color.TRANSPARENT)
            return
        }

        // 시도할 경로 리스트
        val pathsToTry = mutableListOf<String>()

        // 1. 원본 경로 (캐릭터 이미지 경로)
        pathsToTry.add(food.characterImagePath)

        // 2. 확장자 변경 (.png <-> .jpg)
        if (food.characterImagePath.endsWith(".png")) {
            pathsToTry.add(food.characterImagePath.replace(".png", ".jpg"))
        } else if (food.characterImagePath.endsWith(".jpg")) {
            pathsToTry.add(food.characterImagePath.replace(".jpg", ".png"))
        }

        // 3. 음식 이름으로 직접 찾기 (캐릭터 이미지 경로)
        pathsToTry.add("food_character_images/${food.category}/${food.name}_캐릭터누끼.png")
        pathsToTry.add("food_character_images/${food.category}/${food.name}_캐릭터누끼.jpg")

        // 각 경로를 시도
        for (path in pathsToTry) {
            try {
                val inputStream = holder.itemView.context.assets.open(path)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream.close()

                if (bitmap != null) {
                    try {
                        // ImageView에 직접 설정 (동기적, 즉시 표시 - 깜빡임 방지)
                        holder.foodImageView.setImageBitmap(bitmap)
                        holder.foodImageView.scaleType = ImageView.ScaleType.FIT_CENTER
                        holder.foodImageView.setBackgroundColor(Color.TRANSPARENT)
                    } catch (e: Exception) {
                        // Bitmap 직접 설정 실패 시 Glide 사용 (폴백)
                        val requestOptions = RequestOptions()
                            .placeholder(null)
                            .error(ColorDrawable(Color.TRANSPARENT))
                            .centerCrop()
                            .dontAnimate()
                            .skipMemoryCache(false)
                            .diskCacheStrategy(DiskCacheStrategy.NONE)

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
        holder.foodImageView.setBackgroundColor(Color.TRANSPARENT)
    }
}
