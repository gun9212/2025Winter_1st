package com.example.foodworldcup.data

import android.content.Context
import com.google.gson.Gson
import java.io.InputStream

/**
 * 음식 데이터 목록을 관리하고 제공하는 싱글톤(Singleton) 객체입니다.
 * 'object'로 선언하면 앱 전체에서 이 객체는 단 하나만 생성되어,
 * 어디서든 동일한 데이터 소스를 참조할 수 있습니다. (메모리 효율적)
 * 
 * final_foods.json 파일에서 음식 데이터를 로드합니다.
 */
object FoodRepository {

    /**
     * JSON 파일의 음식 데이터 구조
     */
    private data class FoodJson(
        val id: String,
        val name: String,
        val cuisine: String,
        val attributes: Map<String, Any>? = null,
        val img: String? = null,
        val character_img: String? = null
    )

    // 앱에서 사용할 전체 음식 목록
    private var foodList: List<Food> = emptyList()

    /**
     * Context를 받아서 JSON 파일에서 음식 데이터를 로드합니다.
     * 앱 시작 시 한 번만 호출하면 됩니다.
     */
    fun initialize(context: Context) {
        if (foodList.isNotEmpty()) {
            return // 이미 로드됨
        }

        try {
            val inputStream: InputStream = context.assets.open("final_foods.json")
            val jsonString = inputStream.bufferedReader().use { it.readText() }
            
            val gson = Gson()
            val foodJsonList: List<FoodJson> = gson.fromJson(jsonString, Array<FoodJson>::class.java).toList()
            
            // JSON 데이터를 Food 객체로 변환
            foodList = foodJsonList.mapIndexed { index, foodJson ->
                // 이미지 경로 생성: JSON의 음식 이름을 사용하여 실제 파일 찾기
                // 예: "비빔밥" -> "food_images/한식/비빔밥.png"
                val imagePath = if (foodJson.img != null) {
                    // JSON의 img 경로에서 파일명 추출 시도
                    val imgPath = foodJson.img
                    var path = if (imgPath.startsWith("./")) {
                        imgPath.substring(2) // "./" 제거
                    } else {
                        imgPath
                    }
                    
                    // 확장자 제거 후 .png로 변환
                    val pathWithoutExt = if (path.contains(".")) {
                        path.substring(0, path.lastIndexOf("."))
                    } else {
                        path
                    }
                    
                    "food_images/$pathWithoutExt.png"
                } else {
                    // img가 없으면 음식 이름으로 직접 찾기
                    "food_images/${foodJson.cuisine}/${foodJson.name}.png"
                }
                
                // 캐릭터 이미지 경로 생성
                val characterImagePath = if (foodJson.character_img != null) {
                    // JSON의 character_img 경로에서 파일명 추출
                    val charImgPath = foodJson.character_img
                    val path = if (charImgPath.startsWith("./")) {
                        charImgPath.substring(2) // "./" 제거
                    } else {
                        charImgPath
                    }
                    path
                } else {
                    // character_img가 없으면 음식 이름으로 직접 찾기
                    "food_character_images/${foodJson.cuisine}/${foodJson.name}_캐릭터누끼.png"
                }
                
                Food(
                    id = index + 1, // 순차적인 숫자 ID 부여
                    name = foodJson.name,
                    category = foodJson.cuisine,
                    imageResId = 0,
                    imagePath = imagePath,
                    characterImagePath = characterImagePath
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // 오류 발생 시 빈 리스트 반환
            foodList = emptyList()
        }
    }

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
