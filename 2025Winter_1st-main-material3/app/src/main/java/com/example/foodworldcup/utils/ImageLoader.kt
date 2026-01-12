package com.example.foodworldcup.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.util.Log
import android.view.ViewTreeObserver
import android.widget.ImageView
import com.example.foodworldcup.R
import com.example.foodworldcup.data.Food

/**
 * 이미지 로딩 관련 유틸리티 함수들을 모아놓은 객체입니다.
 * 여러 곳에서 중복되던 이미지 경로 찾기 및 로딩 로직을 통합했습니다.
 */
object ImageLoader {

    /**
     * 캐릭터 이미지 경로 리스트를 생성합니다.
     * 여러 곳에서 중복되던 경로 찾기 로직을 통합했습니다.
     *
     * @param characterImagePath 원본 캐릭터 이미지 경로
     * @param foodName 음식 이름
     * @param category 음식 카테고리
     * @return 시도할 경로 리스트
     */
    fun getCharacterImagePaths(
        characterImagePath: String,
        foodName: String,
        category: String
    ): List<String> {
        val pathsToTry = mutableListOf<String>()

        // 1. 원본 경로 (캐릭터 이미지 경로)
        pathsToTry.add(characterImagePath)

        // 2. 확장자 변경 (.png <-> .jpg)
        if (characterImagePath.endsWith(".png")) {
            pathsToTry.add(characterImagePath.replace(".png", ".jpg"))
        } else if (characterImagePath.endsWith(".jpg")) {
            pathsToTry.add(characterImagePath.replace(".jpg", ".png"))
        }

        // 3. 음식 이름으로 직접 찾기 (캐릭터 이미지 경로)
        pathsToTry.add("food_character_images/$category/${foodName}_캐릭터누끼.png")
        pathsToTry.add("food_character_images/$category/${foodName}_캐릭터누끼.jpg")

        return pathsToTry
    }

    /**
     * 일반 음식 이미지 경로 리스트를 생성합니다.
     *
     * @param imagePath 원본 이미지 경로
     * @param foodName 음식 이름
     * @param category 음식 카테고리
     * @return 시도할 경로 리스트
     */
    fun getFoodImagePaths(
        imagePath: String,
        foodName: String,
        category: String
    ): List<String> {
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

        return pathsToTry
    }

    /**
     * assets에서 비트맵을 로드합니다.
     *
     * @param context 컨텍스트
     * @param path assets 내 경로
     * @return 로드된 비트맵 (실패 시 null)
     */
    fun loadBitmapFromAssets(context: Context, path: String): Bitmap? {
        return try {
            val inputStream = context.assets.open(path)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()
            bitmap
        } catch (e: Exception) {
            Log.d("ImageLoader", "이미지 로드 실패: $path - ${e.message}")
            null
        }
    }

    /**
     * 캐릭터 이미지를 로드하고 스케일링하여 ImageView에 설정합니다.
     *
     * @param context 컨텍스트
     * @param imageView 대상 ImageView
     * @param food 음식 정보
     * @param targetAreaRatio 타겟 영역 비율 (기본값 0.9f, 접시용은 0.75f)
     * @param centerYRatio 수직 정렬 비율 (기본값 0.5f = 중앙, 접시용은 0.68f = 하단 정렬)
     * @param alignBottom true면 하단 정렬, false면 중앙 정렬 (기본값 false)
     */
    fun loadCharacterImage(
        context: Context,
        imageView: ImageView,
        food: Food,
        targetAreaRatio: Float = 0.9f,
        centerYRatio: Float = 0.5f,
        alignBottom: Boolean = false
    ) {
        if (food.characterImagePath.isNullOrEmpty()) {
            imageView.setImageResource(R.drawable.ic_launcher_background)
            imageView.setBackgroundColor(Color.TRANSPARENT)
            return
        }

        val pathsToTry = getCharacterImagePaths(food.characterImagePath, food.name, food.category)

        // 각 경로를 시도
        for (path in pathsToTry) {
            val originalBitmap = loadBitmapFromAssets(context, path)
            if (originalBitmap != null) {
                // ImageView의 크기를 측정한 후 이미지 스케일링
                val viewTreeObserver = imageView.viewTreeObserver
                viewTreeObserver.addOnPreDrawListener(object : ViewTreeObserver.OnPreDrawListener {
                    override fun onPreDraw(): Boolean {
                        imageView.viewTreeObserver.removeOnPreDrawListener(this)

                        val targetWidth = imageView.width
                        val targetHeight = imageView.height

                        if (targetWidth > 0 && targetHeight > 0) {
                            val scaledBitmap = BitmapUtils.scaleBitmapToFitContent(
                                originalBitmap,
                                targetWidth,
                                targetHeight,
                                targetAreaRatio,
                                centerYRatio,
                                alignBottom
                            )

                            if (scaledBitmap != null) {
                                try {
                                    imageView.setImageBitmap(scaledBitmap)
                                    imageView.scaleType = ImageView.ScaleType.FIT_CENTER
                                    imageView.setBackgroundColor(Color.TRANSPARENT)
                                } catch (e: Exception) {
                                    Log.e("ImageLoader", "스케일된 Bitmap 설정 실패", e)
                                    // 폴백: 원본 Bitmap 사용
                                    imageView.setImageBitmap(originalBitmap)
                                    imageView.scaleType = ImageView.ScaleType.FIT_CENTER
                                    imageView.setBackgroundColor(Color.TRANSPARENT)
                                }
                            } else {
                                // 스케일링 실패 시 원본 사용
                                imageView.setImageBitmap(originalBitmap)
                                imageView.scaleType = ImageView.ScaleType.FIT_CENTER
                                imageView.setBackgroundColor(Color.TRANSPARENT)
                            }
                        } else {
                            // 크기를 측정할 수 없으면 원본 사용
                            imageView.setImageBitmap(originalBitmap)
                            imageView.scaleType = ImageView.ScaleType.FIT_CENTER
                            imageView.setBackgroundColor(Color.TRANSPARENT)
                        }

                        return true
                    }
                })

                return // 성공하면 종료
            }
        }

        // 모든 시도 실패 시 기본 이미지 표시
        imageView.setImageResource(R.drawable.ic_launcher_background)
        imageView.setBackgroundColor(Color.TRANSPARENT)
    }
}
