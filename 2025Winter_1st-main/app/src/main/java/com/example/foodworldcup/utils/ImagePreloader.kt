package com.example.foodworldcup.utils

import android.content.Context
import android.graphics.BitmapFactory
import com.bumptech.glide.Glide
import com.example.foodworldcup.data.Food

/**
 * 이미지 프리로딩을 관리하는 클래스입니다.
 * Glide를 사용하여 다음에 표시될 이미지를 미리 메모리에 로드합니다.
 * 
 * 주요 기능:
 * - 다음 N장의 이미지를 미리 로드하여 스와이프 시 부드러운 전환 제공
 * - assets 폴더의 이미지를 Glide로 프리로드
 */
class ImagePreloader(private val context: Context) {

    /**
     * 다음 N장의 이미지를 미리 로드합니다.
     * 
     * @param foods 프리로드할 음식 리스트
     * @param count 프리로드할 이미지 개수 (기본값: 3)
     */
    fun preloadNext(foods: List<Food>, count: Int = 3) {
        if (foods.isEmpty()) {
            return
        }

        // 최대 count개만큼 프리로드
        val preloadCount = minOf(count, foods.size)
        
        for (i in 0 until preloadCount) {
            val food = foods[i]
            preloadFood(food)
        }
    }

    /**
     * 단일 음식 이미지를 프리로드합니다.
     * 
     * @param food 프리로드할 음식
     */
    fun preloadFood(food: Food) {
        if (food.imagePath.isNullOrEmpty()) {
            return
        }

        try {
            // assets 폴더에서 이미지를 Bitmap으로 읽어옴
            val inputStream = context.assets.open(food.imagePath)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()

            if (bitmap != null) {
                // Glide로 프리로드
                Glide.with(context)
                    .load(bitmap)
                    .preload()
            }
        } catch (e: Exception) {
            // 이미지 로드 실패 시 로그만 출력 (에러 처리)
            android.util.Log.e("ImagePreloader", "이미지 프리로드 실패: ${food.name}, 경로: ${food.imagePath}", e)
        }
    }

    /**
     * 여러 음식의 이미지를 순차적으로 프리로드합니다.
     * 
     * @param foods 프리로드할 음식 리스트
     */
    fun preloadFoods(foods: List<Food>) {
        foods.forEach { food ->
            preloadFood(food)
        }
    }
}
