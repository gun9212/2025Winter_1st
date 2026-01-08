package com.example.foodworldcup.data

/**
 * 음식점 정보를 담는 데이터 클래스입니다.
 * 지도 API에서 검색된 음식점의 정보를 저장합니다.
 *
 * @property id 음식점의 고유 ID (API에서 제공)
 * @property name 음식점 이름
 * @property address 음식점 주소
 * @property latitude 위도 (지도에 마커를 표시하기 위해 필요)
 * @property longitude 경도 (지도에 마커를 표시하기 위해 필요)
 * @property phoneNumber 전화번호 (선택사항)
 * @property rating 평점 (0.0 ~ 5.0)
 * @property distance 현재 위치로부터의 거리 (미터 단위, 선택사항)
 */
data class Restaurant(
    val id: String,
    val name: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val phoneNumber: String? = null,
    val rating: Double? = null,
    val distance: Int? = null
)
