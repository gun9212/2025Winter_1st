package com.example.foodworldcup.data

import java.util.Date

/**
 * 지도에서 선택한 음식 정보를 담는 데이터 클래스입니다.
 * 
 * @property foodId 선택한 음식의 ID
 * @property selectedDate 선택한 날짜
 * @property placeName 음식점 이름
 * @property placeAddress 음식점 주소 (도로명 주소 우선, 없으면 지번 주소)
 * @property memo 사용자가 작성한 메모
 */
data class MapSelectedFood(
    val foodId: Int,
    val selectedDate: Long, // Date를 Long 타임스탬프로 저장
    val placeName: String,
    val placeAddress: String,
    val memo: String = "" // 기본값은 빈 문자열
) {
    /**
     * Date 객체로 변환하는 헬퍼 함수
     */
    fun getDate(): Date {
        return Date(selectedDate)
    }
}
