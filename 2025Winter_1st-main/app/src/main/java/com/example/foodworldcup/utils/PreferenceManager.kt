package com.example.foodworldcup.utils

import android.content.Context
import android.content.SharedPreferences
import com.example.foodworldcup.data.MapSelectedFood
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * SharedPreferences를 쉽게 사용하기 위한 헬퍼(Helper) 클래스입니다. SharedPreferences는 앱 내부에 간단한 데이터(문자열, 숫자,
 * boolean 등)를 key-value 형태로 저장할 때 사용합니다. 앱 설정이나 간단한 사용자 데이터 저장에 유용합니다.
 */
class PreferenceManager(context: Context) {

    private val prefs: SharedPreferences =
            context.getSharedPreferences("food_world_cup_prefs", Context.MODE_PRIVATE)

    private val gson = Gson()

    // SharedPreferences에 저장할 키 이름들
    companion object {
        private const val KEY_SELECTED_FOOD_IDS = "selected_food_ids" // 스와이프 게임용
        private const val KEY_FINAL_FOOD_IDS = "final_food_ids" // 최종 선택된 음식 ID (지도 검색용)
        private const val KEY_MAP_SELECTED_FOOD_IDS = "map_selected_food_ids" // 지도에서 선택한 음식용 (레거시)
        private const val KEY_MAP_SELECTED_FOODS = "map_selected_foods" // 지도에서 선택한 음식 상세 정보
        private const val KEY_PLACE_MEMOS = "place_memos" // 가게 이름별 메모 (placeName -> memo 매핑)
    }

    /**
     * 최종 선택된 음식 ID 리스트를 저장하는 함수입니다. Gson을 사용하여 List<Int>를 JSON 문자열로 변환한 후 SharedPreferences에 저장합니다.
     * 지도 검색 시 사용할 음식 목록을 저장합니다.
     *
     * @param foodIds 저장할 음식 ID 리스트
     */
    fun saveFinalFoodIds(foodIds: List<Int>) {
        try {
            val json = gson.toJson(foodIds)
            prefs.edit().putString(KEY_FINAL_FOOD_IDS, json).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 저장된 최종 선택된 음식 ID 리스트를 불러오는 함수입니다. SharedPreferences에서 JSON 문자열을 읽어와 Gson으로 List<Int>로 변환합니다.
     *
     * @return 저장된 음식 ID 리스트 (저장된 리스트가 없으면 빈 리스트 반환)
     */
    fun getFinalFoodIds(): List<Int> {
        return try {
            val json = prefs.getString(KEY_FINAL_FOOD_IDS, null)
            if (json == null || json.isEmpty()) {
                return emptyList()
            }
            val type = object : TypeToken<List<Int>>() {}.type
            gson.fromJson<List<Int>>(json, type) ?: emptyList()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    /**
     * 선택된 음식 ID 리스트를 저장하는 함수입니다. Gson을 사용하여 List<Int>를 JSON 문자열로 변환한 후 SharedPreferences에 저장합니다.
     *
     * @param foodIds 저장할 음식 ID 리스트
     */
    fun saveSelectedFoodIds(foodIds: List<Int>) {
        try {
            val json = gson.toJson(foodIds)
            prefs.edit().putString(KEY_SELECTED_FOOD_IDS, json).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 저장된 선택된 음식 ID 리스트를 불러오는 함수입니다. SharedPreferences에서 JSON 문자열을 읽어와 Gson으로 List<Int>로 변환합니다.
     *
     * @return 저장된 음식 ID 리스트 (저장된 리스트가 없으면 빈 리스트 반환)
     */
    fun getSelectedFoodIds(): List<Int> {
        return try {
            val json = prefs.getString(KEY_SELECTED_FOOD_IDS, null)
            if (json == null || json.isEmpty()) {
                return emptyList()
            }
            val type = object : TypeToken<List<Int>>() {}.type
            gson.fromJson<List<Int>>(json, type) ?: emptyList()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    /**
     * 지도에서 선택한 음식 상세 정보 리스트를 저장합니다.
     *
     * @param foods 저장할 MapSelectedFood 리스트
     */
    fun saveMapSelectedFoods(foods: List<MapSelectedFood>) {
        try {
            val json = gson.toJson(foods)
            prefs.edit().putString(KEY_MAP_SELECTED_FOODS, json).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 저장된 지도에서 선택한 음식 상세 정보 리스트를 불러옵니다.
     *
     * @return 저장된 MapSelectedFood 리스트 (없으면 빈 리스트)
     */
    fun getMapSelectedFoods(): List<MapSelectedFood> {
        return try {
            val json = prefs.getString(KEY_MAP_SELECTED_FOODS, null)
            if (json.isNullOrEmpty()) {
                return emptyList()
            }
            val type = object : TypeToken<List<MapSelectedFood>>() {}.type
            gson.fromJson<List<MapSelectedFood>>(json, type) ?: emptyList()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    /**
     * 지도에서 선택한 음식을 추가합니다.
     *
     * @param food 추가할 MapSelectedFood
     */
    fun addMapSelectedFood(food: MapSelectedFood) {
        try {
            val existingFoods = getMapSelectedFoods().toMutableList()
            existingFoods.add(food)
            saveMapSelectedFoods(existingFoods)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 지도에서 선택한 음식을 삭제합니다.
     *
     * @param id 삭제할 MapSelectedFood의 고유 ID
     */
    fun removeMapSelectedFood(id: Long) {
        try {
            val existingFoods = getMapSelectedFoods().toMutableList()
            existingFoods.removeAll { it.id == id }
            saveMapSelectedFoods(existingFoods)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 특정 음식의 메모를 업데이트합니다. 같은 placeName을 가진 모든 음식의 메모가 함께 업데이트됩니다.
     *
     * @param id 업데이트할 MapSelectedFood의 고유 ID
     * @param memo 새로운 메모 내용
     */
    fun updateMapSelectedFoodMemo(id: Long, memo: String) {
        try {
            val existingFoods = getMapSelectedFoods().toMutableList()
            val targetFood = existingFoods.find { it.id == id } ?: return
            val placeName = targetFood.placeName

            // placeName 기준으로 가게별 메모 저장
            if (placeName.isNotEmpty() && placeName != "정보 없음") {
                savePlaceMemo(placeName, memo)
            }

            // 같은 placeName을 가진 모든 항목의 메모 업데이트
            val updatedFoods =
                    existingFoods.map { food ->
                        if (food.placeName == placeName) {
                            food.copy(memo = memo)
                        } else {
                            food
                        }
                    }

            saveMapSelectedFoods(updatedFoods)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 가게별 메모를 저장합니다.
     *
     * @param placeName 가게 이름
     * @param memo 메모 내용
     */
    private fun savePlaceMemo(placeName: String, memo: String) {
        try {
            val placeMemosJson = prefs.getString(KEY_PLACE_MEMOS, null)
            val placeMemos =
                    if (placeMemosJson.isNullOrEmpty()) {
                        mutableMapOf<String, String>()
                    } else {
                        val type = object : TypeToken<Map<String, String>>() {}.type
                        gson.fromJson<Map<String, String>>(placeMemosJson, type)?.toMutableMap()
                                ?: mutableMapOf()
                    }

            placeMemos[placeName] = memo
            val json = gson.toJson(placeMemos)
            prefs.edit().putString(KEY_PLACE_MEMOS, json).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 가게별 메모를 조회합니다.
     *
     * @param placeName 가게 이름
     * @return 메모 내용 (없으면 null)
     */
    fun getPlaceMemo(placeName: String): String? {
        return try {
            val placeMemosJson = prefs.getString(KEY_PLACE_MEMOS, null)
            if (placeMemosJson.isNullOrEmpty()) {
                return null
            }
            val type = object : TypeToken<Map<String, String>>() {}.type
            val placeMemos = gson.fromJson<Map<String, String>>(placeMemosJson, type)
            placeMemos?.get(placeName)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
