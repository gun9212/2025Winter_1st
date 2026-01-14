package com.example.foodworldcup.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import com.example.foodworldcup.data.MapSelectedFood

/** 카카오맵 관련 유틸리티 함수들을 모아놓은 객체입니다. */
object KakaoMapHelper {

    /**
     * 카카오맵에서 상세 정보 보기 (리뷰, 사진 등 확인 가능)
     *
     * @param context 컨텍스트
     * @param placeId 카카오맵 장소 ID (선택사항)
     * @param latitude 위도 (선택사항)
     * @param longitude 경도 (선택사항)
     * @param placeName 가게 이름 (선택사항)
     */
    fun openKakaoMapDetail(
            context: Context,
            placeId: String? = null,
            latitude: Double? = null,
            longitude: Double? = null,
            placeName: String? = null
    ) {
        // 1. placeId가 있으면 카카오맵 앱으로 직접 열기 (가장 정확)
        if (!placeId.isNullOrBlank()) {
            try {
                android.util.Log.d("KakaoMapHelper", "placeId로 앱 열기 시도: $placeId")
                val kakaoMapUri = "kakaomap://place?id=$placeId"
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(kakaoMapUri))

                if (intent.resolveActivity(context.packageManager) != null) {
                    context.startActivity(intent)
                    return
                } else {
                    // 앱이 없으면 placeId를 이용한 웹으로 열기
                    android.util.Log.d("KakaoMapHelper", "앱 없음 - placeId로 웹 열기 시도")
                    try {
                        val webUrl = "https://place.map.kakao.com/$placeId"
                        val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse(webUrl))
                        context.startActivity(webIntent)
                        return
                    } catch (e: Exception) {
                        Log.e("KakaoMapHelper", "placeId 웹 열기 실패: ${e.message}", e)
                        // placeId 웹 실패 시 다음 로직으로 계속 진행
                    }
                }
            } catch (e: Exception) {
                Log.e("KakaoMapHelper", "카카오맵 앱 열기 실패: ${e.message}", e)
            }
        }

        // 2. 좌표가 있으면 좌표로 카카오맵 앱 열기
        if (latitude != null && longitude != null) {
            try {
                android.util.Log.d("KakaoMapHelper", "좌표로 앱 열기 시도: $latitude, $longitude")
                val kakaoMapUri = "kakaomap://place?lat=$latitude&lng=$longitude"
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(kakaoMapUri))

                if (intent.resolveActivity(context.packageManager) != null) {
                    context.startActivity(intent)
                    return
                }
            } catch (e: Exception) {
                Log.e("KakaoMapHelper", "카카오맵 앱 좌표 열기 실패: ${e.message}", e)
            }
        }

        // 3. placeName으로 카카오맵 앱 검색 시도
        if (!placeName.isNullOrBlank() && placeName != "정보 없음") {
            try {
                android.util.Log.d("KakaoMapHelper", "placeName으로 앱 검색 시도: $placeName")
                val kakaoMapUri = "kakaomap://search?q=${Uri.encode(placeName)}"
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(kakaoMapUri))

                if (intent.resolveActivity(context.packageManager) != null) {
                    context.startActivity(intent)
                    return
                } else {
                    // 앱이 없으면 placeName을 이용한 웹 검색으로 열기
                    android.util.Log.d("KakaoMapHelper", "앱 없음 - placeName으로 웹 검색")
                    try {
                        val webUrl = "https://map.kakao.com/link/search/${Uri.encode(placeName)}"
                        val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse(webUrl))
                        context.startActivity(webIntent)
                        return
                    } catch (e: Exception) {
                        Log.e("KakaoMapHelper", "placeName 웹 검색 열기 실패: ${e.message}", e)
                        Toast.makeText(context, "카카오맵을 열 수 없습니다.", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Log.e("KakaoMapHelper", "카카오맵 앱 검색 열기 실패: ${e.message}", e)
                Toast.makeText(context, "카카오맵을 열 수 없습니다.", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "위치 정보가 없어 카카오맵을 열 수 없습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * MapSelectedFood 객체를 사용하여 카카오맵 상세 페이지를 엽니다.
     *
     * @param context 컨텍스트
     * @param selectedFood MapSelectedFood 객체
     */
    fun openKakaoMapDetail(context: Context, selectedFood: MapSelectedFood) {
        openKakaoMapDetail(
                context = context,
                placeId = selectedFood.placeId,
                latitude = selectedFood.latitude,
                longitude = selectedFood.longitude,
                placeName = selectedFood.placeName.takeIf { it.isNotEmpty() && it != "정보 없음" }
        )
    }
}
