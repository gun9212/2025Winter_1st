package com.example.foodworldcup.utils

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import android.util.Log

/**
 * 비트맵 처리 관련 유틸리티 함수들을 모아놓은 객체입니다.
 * 여러 Activity와 Adapter에서 공통으로 사용되는 비트맵 처리 로직을 중앙화합니다.
 */
object BitmapUtils {

    /**
     * 비트맵에서 투명하지 않은 실질적 영역을 구합니다.
     * 여러 곳에서 중복되던 getContentBounds 함수를 통합했습니다.
     *
     * @param bitmap 분석할 비트맵
     * @return 실질적 내용이 있는 영역의 Rect
     */
    fun getContentBounds(bitmap: Bitmap): Rect {
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
     * 실질적 내용 영역을 기준으로 이미지를 스케일링합니다.
     * 여러 곳에서 중복되던 scaleBitmapToFitContent 함수를 통합했습니다.
     *
     * @param originalBitmap 원본 비트맵
     * @param targetWidth 목표 너비
     * @param targetHeight 목표 높이
     * @param targetAreaRatio 타겟 영역 비율 (기본값 0.9f, 접시용은 0.75f)
     * @param centerYRatio 수직 정렬 비율 (기본값 0.5f = 중앙)
     * @param alignBottom true면 하단 정렬, false면 중앙 정렬 (기본값 false)
     * @return 스케일링된 비트맵 (실패 시 null)
     */
    fun scaleBitmapToFitContent(
        originalBitmap: Bitmap,
        targetWidth: Int,
        targetHeight: Int,
        targetAreaRatio: Float = 0.9f,
        centerYRatio: Float = 0.5f,
        alignBottom: Boolean = false
    ): Bitmap? {
        return try {
            // 이미지의 실질적 내용(누끼 부분)의 크기를 구함
            val contentBounds = getContentBounds(originalBitmap)
            val contentWidth = contentBounds.width()
            val contentHeight = contentBounds.height()

            // 타겟 영역 설정
            val targetAreaWidth = targetWidth * targetAreaRatio
            val targetAreaHeight = targetHeight * targetAreaRatio

            // 스케일 계산 (비율 유지)
            val scale = minOf(targetAreaWidth / contentWidth, targetAreaHeight / contentHeight)

            // Matrix를 사용하여 스케일링
            val matrix = Matrix()
            // 먼저 내용 영역의 왼쪽 상단을 원점으로 이동
            matrix.postTranslate(-contentBounds.left.toFloat(), -contentBounds.top.toFloat())
            // 스케일 적용
            matrix.postScale(scale, scale)

            // 수평 중앙 정렬
            val scaledContentWidth = contentWidth * scale
            val scaledContentHeight = contentHeight * scale
            val tx = (targetWidth - scaledContentWidth) / 2f

            // 수직 정렬 (하단 정렬 또는 중앙 정렬)
            val ty = if (alignBottom) {
                // 하단 정렬: centerYRatio 위치에 이미지 하단이 오도록
                (targetHeight * centerYRatio) - scaledContentHeight
            } else {
                // 중앙 정렬: centerYRatio 위치에 이미지 중앙이 오도록
                (targetHeight * centerYRatio) - (scaledContentHeight / 2f)
            }

            matrix.postTranslate(tx, ty)

            // 스케일링된 비트맵 생성
            val scaledBitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(scaledBitmap)
            val paint = Paint().apply {
                isAntiAlias = true
                isFilterBitmap = true
            }

            canvas.drawBitmap(originalBitmap, matrix, paint)

            scaledBitmap
        } catch (e: Exception) {
            Log.e("BitmapUtils", "비트맵 스케일링 실패: ${e.message}", e)
            null
        }
    }
}
