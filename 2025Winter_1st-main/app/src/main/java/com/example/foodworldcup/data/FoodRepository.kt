package com.example.foodworldcup.data

import com.example.foodworldcup.R // 이미지 리소스 ID(R.drawable.*)를 사용하기 위해 import

/**
 * 음식 데이터 목록을 관리하고 제공하는 싱글톤(Singleton) 객체입니다.
 * 'object'로 선언하면 앱 전체에서 이 객체는 단 하나만 생성되어,
 * 어디서든 동일한 데이터 소스를 참조할 수 있습니다. (메모리 효율적)
 */
object FoodRepository {

    // 앱에서 사용할 전체 음식 목록을 하드코딩으로 미리 만들어둡니다.
    // 서버가 없기 때문에 앱 내부에 데이터를 직접 저장하는 방식입니다.
    private val foodList = listOf(
        // TODO: 실제 프로젝트에서는 여기에 더 많은 음식 데이터를 추가해야 합니다.
        // TODO: R.drawable.food_kimchi_stew와 같은 이름의 이미지 파일을 res/drawable 폴더에 추가해야 합니다.
        Food(id = 1, name = "김치찌개", category = "한식", imageResId = 0 /* R.drawable.food_kimchi_stew */),
        Food(id = 2, name = "짜장면", category = "중식", imageResId = 0 /* R.drawable.food_jajangmyeon */),
        Food(id = 3, name = "피자", category = "양식", imageResId = 0 /* R.drawable.food_pizza */)
    )

    /**
     * 전체 음식 리스트를 반환하는 함수입니다.
     */
    fun getFoodList(): List<Food> {
        return foodList
    }

    /**
     * 특정 카테고리(한식, 중식, 양식 등)로 필터링된 음식 리스트를 반환하는 함수입니다.
     * GameActivity의 상단 토글에서 사용됩니다.
     *
     * @param category 필터링할 카테고리 이름 (예: "한식", "중식")
     * @return 해당 카테고리에 속한 음식 리스트
     */
    fun getFoodListByCategory(category: String): List<Food> {
        // TODO: foodList에서 category가 일치하는 음식만 필터링하여 반환합니다.
        return emptyList()
    }

    /**
     * 사용 가능한 모든 카테고리 목록을 반환하는 함수입니다.
     * 상단 토글 버튼을 동적으로 생성할 때 사용됩니다.
     *
     * @return 카테고리 이름 리스트 (예: ["한식", "중식", "양식"])
     */
    fun getAllCategories(): List<String> {
        // TODO: foodList에서 중복을 제거한 카테고리 목록을 반환합니다.
        return emptyList()
    }
}
