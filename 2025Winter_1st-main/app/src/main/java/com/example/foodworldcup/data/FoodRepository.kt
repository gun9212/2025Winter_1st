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
    // TODO: R.drawable.food_*와 같은 이름의 이미지 파일을 res/drawable 폴더에 추가해야 합니다.
    private val foodList = listOf(
        // 한식
        Food(id = 1, name = "김치찌개", category = "한식", imageResId = 0 /* R.drawable.food_kimchi_stew */),
        Food(id = 2, name = "육회비빔밥", category = "한식", imageResId = 0 /* R.drawable.food_yukhoe_bibimbap */),
        Food(id = 3, name = "제육볶음", category = "한식", imageResId = 0 /* R.drawable.food_jeyuk_bokkeum */),
        Food(id = 4, name = "된장찌개", category = "한식", imageResId = 0 /* R.drawable.food_doenjang_jjigae */),
        Food(id = 5, name = "불고기", category = "한식", imageResId = 0 /* R.drawable.food_bulgogi */),
        Food(id = 6, name = "비빔밥", category = "한식", imageResId = 0 /* R.drawable.food_bibimbap */),
        
        // 중식
        Food(id = 7, name = "짜장면", category = "중식", imageResId = 0 /* R.drawable.food_jajangmyeon */),
        Food(id = 8, name = "짬뽕", category = "중식", imageResId = 0 /* R.drawable.food_jjamppong */),
        Food(id = 9, name = "탕수육", category = "중식", imageResId = 0 /* R.drawable.food_tangsuyuk */),
        Food(id = 10, name = "양장피", category = "중식", imageResId = 0 /* R.drawable.food_yangjangpi */),
        Food(id = 11, name = "마파두부", category = "중식", imageResId = 0 /* R.drawable.food_mapadubu */),
        Food(id = 12, name = "볶음밥", category = "중식", imageResId = 0 /* R.drawable.food_bokkeumbap */),
        
        // 양식
        Food(id = 13, name = "피자", category = "양식", imageResId = 0 /* R.drawable.food_pizza */),
        Food(id = 14, name = "파스타", category = "양식", imageResId = 0 /* R.drawable.food_pasta */),
        Food(id = 15, name = "햄버거", category = "양식", imageResId = 0 /* R.drawable.food_hamburger */),
        Food(id = 16, name = "스테이크", category = "양식", imageResId = 0 /* R.drawable.food_steak */),
        Food(id = 17, name = "리조또", category = "양식", imageResId = 0 /* R.drawable.food_risotto */),
        Food(id = 18, name = "샐러드", category = "양식", imageResId = 0 /* R.drawable.food_salad */),
        
        // 일식
        Food(id = 19, name = "초밥", category = "일식", imageResId = 0 /* R.drawable.food_sushi */),
        Food(id = 20, name = "라멘", category = "일식", imageResId = 0 /* R.drawable.food_ramen */),
        Food(id = 21, name = "우동", category = "일식", imageResId = 0 /* R.drawable.food_udon */),
        Food(id = 22, name = "돈까스", category = "일식", imageResId = 0 /* R.drawable.food_tonkatsu */),
        Food(id = 23, name = "규동", category = "일식", imageResId = 0 /* R.drawable.food_gyudon */),
        Food(id = 24, name = "오므라이스", category = "일식", imageResId = 0 /* R.drawable.food_omurice */)
    )

    /**
     * 전체 음식 리스트를 반환하는 함수입니다.
     */
    fun getFoodList(): List<Food> {
        return foodList
    }

    /**
     * 특정 카테고리(한식, 중식, 양식 등)로 필터링된 음식 리스트를 반환하는 함수입니다.
     * FoodListActivity와 GameActivity의 상단 토글에서 사용됩니다.
     *
     * @param category 필터링할 카테고리 이름 (예: "한식", "중식")
     * @return 해당 카테고리에 속한 음식 리스트
     */
    fun getFoodListByCategory(category: String): List<Food> {
        return foodList.filter { it.category == category }
    }

    /**
     * 사용 가능한 모든 카테고리 목록을 반환하는 함수입니다.
     * 상단 토글 버튼을 동적으로 생성할 때 사용됩니다.
     *
     * @return 카테고리 이름 리스트 (예: ["한식", "중식", "양식", "일식"])
     */
    fun getAllCategories(): List<String> {
        return foodList.map { it.category }.distinct().sorted()
    }

    /**
     * 음식 ID로 음식을 찾는 함수입니다.
     *
     * @param id 찾을 음식의 ID
     * @return 해당 ID를 가진 음식 (없으면 null)
     */
    fun getFoodById(id: Int): Food? {
        return foodList.find { it.id == id }
    }
}
