package com.example.foodworldcup.utils

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.example.foodworldcup.data.Food

/**
 * 이미지 프리로딩을 관리하는 클래스입니다.
 * Glide를 사용하여 다음에 표시될 이미지를 미리 메모리에 로드합니다.
 * 
 * 주요 기능:
 * - 다음 N장의 이미지를 미리 로드하여 스와이프 시 부드러운 전환 제공
 * - assets 폴더의 이미지를 Glide로 프리로드
 * - 동일한 캐시 키를 사용하여 Adapter와 동일한 캐시 공유
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
     * Adapter와 동일한 방식으로 로드하여 캐시를 공유합니다.
     * 게임 화면에서는 기존 음식 이미지(imagePath)를 프리로드합니다.
     * 
     * @param food 프리로드할 음식
     */
    fun preloadFood(food: Food) {
        if (food.imagePath.isNullOrEmpty()) {
            return
        }

        // Adapter와 동일한 경로 순서로 시도
        val pathsToTry = mutableListOf<String>()
        pathsToTry.add(food.imagePath)
        if (food.imagePath.endsWith(".png")) {
            pathsToTry.add(food.imagePath.replace(".png", ".jpg"))
        } else if (food.imagePath.endsWith(".jpg")) {
            pathsToTry.add(food.imagePath.replace(".jpg", ".png"))
        }
        pathsToTry.add("food_images/${food.category}/${food.name}.png")
        pathsToTry.add("food_images/${food.category}/${food.name}.jpg")

        for (path in pathsToTry) {
            try {
                val inputStream = context.assets.open(path)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream.close()

                if (bitmap != null) {
                    // Adapter와 동일한 RequestOptions 사용하여 캐시 공유
                    // 같은 Bitmap 객체를 사용하면 Glide가 자동으로 메모리 캐시에 저장
                    val requestOptions = RequestOptions()
                        .placeholder(null)  // placeholder 제거 (깜빡임 방지)
                        .error(ColorDrawable(Color.WHITE))
                        .centerCrop()
                        .dontAnimate()  // 애니메이션 비활성화 (깜빡임 방지)
                        .skipMemoryCache(false) // 메모리 캐시 사용
                        .diskCacheStrategy(DiskCacheStrategy.NONE) // Bitmap은 메모리 캐시만
                    
                    // Glide로 프리로드 (메모리 캐시에 저장)
                    // Adapter에서 같은 Bitmap 객체를 로드하면 캐시에서 가져옴
                    Glide.with(context)
                        .load(bitmap)
                        .apply(requestOptions)
                        .preload()
                    
                    return // 성공하면 종료
                }
            } catch (e: Exception) {
                // 다음 경로 시도
                continue
            }
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
