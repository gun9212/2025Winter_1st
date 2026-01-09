package com.example.foodworldcup.data

/**
 * 음식 하나의 정보를 담는 데이터 클래스(Data Class)입니다.
 * 데이터를 보관하는 목적의 클래스를 간결하게 정의할 수 있습니다.
 *
 * @property id 각 음식을 구분하기 위한 고유한 번호
 * @property name 음식 이름 (예: "김치찌개")
 * @property category 음식 카테고리 (예: "한식")
 * @property imageResId res/drawable 폴더에 저장된 음식 이미지 파일의 고유 ID (사용 안 함)
 * @property imagePath assets 폴더 내 이미지 파일 경로 (예: "food_images/한식/비빔밥.jpg")
 */
data class Food(
    val id: Int,
    val name: String,
    val category: String,
    val imageResId: Int = 0,
    val imagePath: String? = null
)
