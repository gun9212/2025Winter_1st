package com.example.foodworldcup.data

import java.util.Date

/**
 * 지도에서 선택한 음식 정보를 담는 데이터 클래스입니다.
 * 
 * @property id 고유 ID (각 선택마다 고유한 값)
 * @property foodId 선택한 음식의 ID
 * @property selectedDate 선택한 날짜
 * @property placeName 음식점 이름
 * @property placeAddress 음식점 주소 (도로명 주소 우선, 없으면 지번 주소)
 * @property placeId 카카오맵 장소 ID (카카오맵 앱으로 직접 이동하기 위해 필요)
 * @property latitude 위도 (카카오맵 앱으로 직접 이동하기 위해 필요)
 * @property longitude 경도 (카카오맵 앱으로 직접 이동하기 위해 필요)
 * @property memo 사용자가 작성한 메모
 */
data class MapSelectedFood(
    val id: Long, // 고유 ID (각 선택마다 고유한 값)
    val foodId: Int,
    val selectedDate: Long, // Date를 Long 타임스탬프로 저장
    val placeName: String,
    val placeAddress: String,
    val placeId: String? = null, // 카카오맵 장소 ID
    val latitude: Double? = null, // 위도
    val longitude: Double? = null, // 경도
    val memo: String = "" // 기본값은 빈 문자열
) {
    /**
     * Date 객체로 변환하는 헬퍼 함수
     */
    fun getDate(): Date {
        return Date(selectedDate)
    }
}
