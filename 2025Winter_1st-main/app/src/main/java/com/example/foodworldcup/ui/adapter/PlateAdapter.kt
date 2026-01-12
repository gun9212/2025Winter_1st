package com.example.foodworldcup.ui.adapter

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Rect
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.foodworldcup.R
import com.example.foodworldcup.data.Food
import com.example.foodworldcup.data.FoodRepository
import com.example.foodworldcup.data.MapSelectedFood
import java.text.SimpleDateFormat
import java.util.Locale

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
                val dateFormat = SimpleDateFormat("yyyy/MM/dd", Locale.KOREAN)
                holder.dateTextView.text = dateFormat.format(selectedFood.getDate())
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

    /** 음식 캐릭터 이미지를 로드하는 함수입니다. 접시 위에 오버레이로 표시됩니다. MapActivity와 동일한 방식으로 이미지 크기를 통일합니다. */
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
                val originalBitmap = BitmapFactory.decodeStream(inputStream)
                inputStream.close()

                if (originalBitmap != null) {
                    // ImageView의 크기를 측정한 후 이미지 스케일링
                    val viewTreeObserver = holder.foodImageView.viewTreeObserver
                    viewTreeObserver.addOnPreDrawListener(
                            object : ViewTreeObserver.OnPreDrawListener {
                                override fun onPreDraw(): Boolean {
                                    holder.foodImageView.viewTreeObserver.removeOnPreDrawListener(
                                            this
                                    )

                                    // ImageView의 실제 크기 가져오기
                                    val targetWidth = holder.foodImageView.width
                                    val targetHeight = holder.foodImageView.height

                                    if (targetWidth > 0 && targetHeight > 0) {
                                        // MapActivity와 동일한 방식으로 이미지 크기 통일
                                        val scaledBitmap =
                                                scaleBitmapToFitContent(
                                                        originalBitmap,
                                                        targetWidth,
                                                        targetHeight
                                                )

                                        if (scaledBitmap != null) {
                                            try {
                                                holder.foodImageView.setImageBitmap(scaledBitmap)
                                                holder.foodImageView.scaleType =
                                                        ImageView.ScaleType.FIT_CENTER
                                                holder.foodImageView.setBackgroundColor(
                                                        Color.TRANSPARENT
                                                )
                                            } catch (e: Exception) {
                                                Log.e("PlateAdapter", "스케일된 Bitmap 설정 실패", e)
                                                // 폴백: 원본 Bitmap 사용
                                                holder.foodImageView.setImageBitmap(originalBitmap)
                                                holder.foodImageView.scaleType =
                                                        ImageView.ScaleType.FIT_CENTER
                                                holder.foodImageView.setBackgroundColor(
                                                        Color.TRANSPARENT
                                                )
                                            }
                                        } else {
                                            // 스케일링 실패 시 원본 사용
                                            holder.foodImageView.setImageBitmap(originalBitmap)
                                            holder.foodImageView.scaleType =
                                                    ImageView.ScaleType.FIT_CENTER
                                            holder.foodImageView.setBackgroundColor(
                                                    Color.TRANSPARENT
                                            )
                                        }
                                    } else {
                                        // 크기를 측정할 수 없으면 원본 사용
                                        holder.foodImageView.setImageBitmap(originalBitmap)
                                        holder.foodImageView.scaleType =
                                                ImageView.ScaleType.FIT_CENTER
                                        holder.foodImageView.setBackgroundColor(Color.TRANSPARENT)
                                    }

                                    return true
                                }
                            }
                    )

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

    /** MapActivity의 getContentBounds와 동일한 방식으로 비트맵에서 투명하지 않은 실질적 영역을 구합니다. */
    private fun getContentBounds(bitmap: Bitmap): Rect {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        var minX = width
        var maxX = -1
        var minY = height
        var maxY = -1

        for (y in 0 until height) {
            for (x in 0 until width) {
                // Alpha 값 확인 (MSB 8bit)
                val alpha = (pixels[y * width + x] shr 24) and 0xFF
                if (alpha > 50) { // 약간의 투명도는 무시 (노이즈 방지)
                    if (x < minX) minX = x
                    if (x > maxX) maxX = x
                    if (y < minY) minY = y
                    if (y > maxY) maxY = y
                }
            }
        }

        if (maxX < minX || maxY < minY) {
            // 내용이 없으면 전체 반환
            return Rect(0, 0, width, height)
        }

        return Rect(minX, minY, maxX + 1, maxY + 1)
    }

    /**
     * MapActivity와 동일한 방식으로 이미지를 스케일링합니다. 실질적 내용 영역을 기준으로 크기를 통일합니다.
     *
     * @param originalBitmap 원본 비트맵
     * @param targetWidth 목표 너비
     * @param targetHeight 목표 높이
     * @return 스케일링된 비트맵
     */
    private fun scaleBitmapToFitContent(
            originalBitmap: Bitmap,
            targetWidth: Int,
            targetHeight: Int
    ): Bitmap? {
        try {
            // 이미지의 실질적 내용(누끼 부분)의 크기를 구함
            val contentBounds = getContentBounds(originalBitmap)
            val contentWidth = contentBounds.width()
            val contentHeight = contentBounds.height()

            // 타겟 영역 설정 (접시 크기의 75% 정도 사용)
            val targetAreaWidth = targetWidth * 0.75f
            val targetAreaHeight = targetHeight * 0.75f

            // 스케일 계산 (비율 유지)
            val scale = Math.min(targetAreaWidth / contentWidth, targetAreaHeight / contentHeight)

            // Matrix를 사용하여 스케일링
            val matrix = Matrix()
            // 먼저 내용 영역의 왼쪽 상단을 원점으로 이동
            matrix.postTranslate(-contentBounds.left.toFloat(), -contentBounds.top.toFloat())
            // 스케일 적용
            matrix.postScale(scale, scale)

            // 수평 중앙 정렬, 수직은 하단 정렬
            val scaledContentWidth = contentWidth * scale
            val scaledContentHeight = contentHeight * scale
            val tx = (targetWidth - scaledContentWidth) / 2f

            // ⭐ 모든 음식 이미지의 하단을 일정한 위치에 정렬
            // 접시 중앙보다 아래에 음식 하단 배치 (접시 높이의 68% 위치)
            val plateBottomAnchor = targetHeight * 0.68f
            val ty = plateBottomAnchor - scaledContentHeight

            matrix.postTranslate(tx, ty)

            // 스케일링된 비트맵 생성
            val scaledBitmap =
                    Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)

            val canvas = android.graphics.Canvas(scaledBitmap)
            val paint = android.graphics.Paint()
            paint.isAntiAlias = true
            paint.isFilterBitmap = true

            canvas.drawBitmap(originalBitmap, matrix, paint)

            return scaledBitmap
        } catch (e: Exception) {
            Log.e("PlateAdapter", "비트맵 스케일링 실패: ${e.message}", e)
            return null
        }
    }
}
