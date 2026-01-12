package com.example.foodworldcup.ui.adapter

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.foodworldcup.R
import com.example.foodworldcup.data.Food
import com.example.foodworldcup.data.FoodRepository
import com.example.foodworldcup.data.MapSelectedFood
import com.example.foodworldcup.utils.DateFormatter
import com.example.foodworldcup.utils.ImageLoader

/** 접시와 음식을 표시하는 RecyclerView 어댑터입니다. 마이페이지에서 사용되며, 접시 배경 위에 선택된 음식 이미지를 오버레이합니다. */
class PlateAdapter(
        private val selectedFoods: List<MapSelectedFood?>, // null이면 빈 접시, MapSelectedFood면 해당 음식 정보
        private val onItemClick: ((Long) -> Unit)? = null // 클릭 리스너 (MapSelectedFood의 id 전달)
) : RecyclerView.Adapter<PlateAdapter.PlateViewHolder>() {

    class PlateViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val plateImageView: ImageView = itemView.findViewById(R.id.plateImageView)
        val foodImageView: ImageView = itemView.findViewById(R.id.foodImageView)
        val dateTextView: TextView = itemView.findViewById(R.id.dateTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlateViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_plate, parent, false)
        return PlateViewHolder(view)
    }

    override fun onBindViewHolder(holder: PlateViewHolder, position: Int) {
        val selectedFood = selectedFoods[position]

        // 접시 이미지 로드 (assets에서)
        loadPlateImage(holder)

        // 음식이 있으면 표시, 없으면 숨김
        if (selectedFood != null) {
            val food = FoodRepository.getFoodById(selectedFood.foodId)
            if (food != null) {
                holder.foodImageView.visibility = View.VISIBLE
                loadFoodImage(holder, food)

                // 날짜 표시 (YYYY/MM/dd 형식)
                holder.dateTextView.text = DateFormatter.dateFormatShort.format(selectedFood.getDate())
                holder.dateTextView.visibility = View.VISIBLE

                // 클릭 리스너 설정 (음식이 있을 때만, MapSelectedFood의 id 전달)
                holder.itemView.setOnClickListener { onItemClick?.invoke(selectedFood.id) }
            } else {
                holder.foodImageView.visibility = View.GONE
                holder.dateTextView.visibility = View.GONE
                holder.itemView.setOnClickListener(null)
            }
        } else {
            holder.foodImageView.visibility = View.GONE
            holder.dateTextView.visibility = View.GONE
            holder.itemView.setOnClickListener(null)
        }
    }

    override fun getItemCount(): Int = selectedFoods.size

    /** 접시 이미지를 로드하는 함수입니다. */
    private fun loadPlateImage(holder: PlateViewHolder) {
        try {
            // assets에서 접시 이미지 로드
            // 큰 이미지 파일이므로 BitmapFactory.Options로 메모리 최적화
            val options =
                    BitmapFactory.Options().apply {
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

    /** 음식 캐릭터 이미지를 로드하는 함수입니다. 접시 위에 오버레이로 표시됩니다. */
    private fun loadFoodImage(holder: PlateViewHolder, food: Food) {
        // ImageLoader 유틸리티 사용 (접시용: targetAreaRatio=0.75f, centerYRatio=0.68f, alignBottom=true)
        ImageLoader.loadCharacterImage(
            context = holder.itemView.context,
            imageView = holder.foodImageView,
            food = food,
            targetAreaRatio = 0.75f, // 접시 크기의 75% 사용
            centerYRatio = 0.68f, // 접시 높이의 68% 위치에 하단 정렬
            alignBottom = true // 하단 정렬 사용 (접시 위에 올라오도록)
        )
    }
}
