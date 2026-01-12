package com.example.foodworldcup.utils

import android.content.Context
import android.content.SharedPreferences
import com.example.foodworldcup.data.WinRecord
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.Date

/**
 * SharedPreferences를 쉽게 사용하기 위한 헬퍼(Helper) 클래스입니다.
 * SharedPreferences는 앱 내부에 간단한 데이터(문자열, 숫자, boolean 등)를
 * key-value 형태로 저장할 때 사용합니다. 앱 설정이나 간단한 사용자 데이터 저장에 유용합니다.
 */
class PreferenceManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("food_world_cup_prefs", Context.MODE_PRIVATE)
    
    private val gson = Gson()
    
    // SharedPreferences에 저장할 키 이름들
    companion object {
        private const val KEY_FINAL_FOOD_IDS = "final_food_ids"
        private const val KEY_SELECTED_FOOD_IDS = "selected_food_ids"
    }
    /**
     * 최종 선택된 음식 ID 리스트를 저장하는 함수입니다.
     * Gson을 사용하여 List<Int>를 JSON 문자열로 변환한 후 SharedPreferences에 저장합니다.
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
     * 저장된 최종 선택된 음식 ID 리스트를 불러오는 함수입니다.
     * SharedPreferences에서 JSON 문자열을 읽어와 Gson으로 List<Int>로 변환합니다.
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
     * 선택된 음식 ID 리스트를 저장하는 함수입니다.
     * Gson을 사용하여 List<Int>를 JSON 문자열로 변환한 후 SharedPreferences에 저장합니다.
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
     * 저장된 선택된 음식 ID 리스트를 불러오는 함수입니다.
     * SharedPreferences에서 JSON 문자열을 읽어와 Gson으로 List<Int>로 변환합니다.
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
}
