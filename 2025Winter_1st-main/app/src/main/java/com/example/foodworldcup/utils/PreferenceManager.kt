package com.example.foodworldcup.utils

import android.content.Context
import android.content.SharedPreferences
import com.example.foodworldcup.data.WinRecord
import com.example.foodworldcup.data.MapSelectedFood
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
        private const val KEY_WIN_RECORDS = "win_records"
        private const val KEY_SELECTED_FOOD_IDS = "selected_food_ids" // 스와이프 게임용
        private const val KEY_MAP_SELECTED_FOOD_IDS = "map_selected_food_ids" // 지도에서 선택한 음식용 (레거시)
        private const val KEY_MAP_SELECTED_FOODS = "map_selected_foods" // 지도에서 선택한 음식 상세 정보
    }

    /**
     * 우승 기록을 저장하는 함수입니다.
     * Gson을 사용하여 WinRecord 객체 리스트를 JSON 문자열로 변환한 후 SharedPreferences에 저장합니다.
     *
     * @param records 저장할 우승 기록 리스트
     */
    fun saveWinRecords(records: List<WinRecord>) {
        try {
            // WinRecord를 직렬화 가능한 형태로 변환 (Date를 Long으로 변환)
            val recordsJson = records.map { record ->
                mapOf(
                    "id" to record.id,
                    "selectedFoods" to record.selectedFoods,
                    "winDate" to record.winDate.time, // Date를 Long 타임스탬프로 변환
                    "memo" to record.memo
                )
            }
            val json = gson.toJson(recordsJson)
            prefs.edit().putString(KEY_WIN_RECORDS, json).apply()
        } catch (e: Exception) {
            // 에러 발생 시 로그 출력 (필요시 Log 사용)
            e.printStackTrace()
        }
    }

    /**
     * 저장된 우승 기록을 불러오는 함수입니다.
     * SharedPreferences에서 JSON 문자열을 읽어와 Gson으로 WinRecord 리스트로 변환합니다.
     *
     * @return 저장된 우승 기록 리스트 (저장된 기록이 없으면 빈 리스트 반환)
     */
    fun getWinRecords(): List<WinRecord> {
        return try {
            val json = prefs.getString(KEY_WIN_RECORDS, null)
            if (json == null || json.isEmpty()) {
                return emptyList()
            }
            
            // JSON을 Map 리스트로 파싱
            val type = object : TypeToken<List<Map<String, Any>>>() {}.type
            val recordsJson: List<Map<String, Any>> = gson.fromJson(json, type) ?: return emptyList()
            
            // Map을 WinRecord로 변환 (Long 타임스탬프를 Date로 변환)
            recordsJson.mapNotNull { recordMap ->
                try {
                    WinRecord(
                        id = (recordMap["id"] as? Double)?.toLong() ?: (recordMap["id"] as? Long) ?: 0L,
                        selectedFoods = (recordMap["selectedFoods"] as? List<*>)?.mapNotNull { 
                            when (it) {
                                is Double -> it.toInt()
                                is Int -> it
                                else -> null
                            }
                        } ?: emptyList(),
                        winDate = Date((recordMap["winDate"] as? Double)?.toLong() ?: (recordMap["winDate"] as? Long) ?: 0L),
                        memo = (recordMap["memo"] as? String) ?: ""
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    /**
     * 새로운 우승 기록을 추가하는 함수입니다.
     * 기존 기록을 불러온 후, 새로운 기록을 추가하고 다시 저장합니다.
     *
     * @param record 추가할 우승 기록
     */
    fun addWinRecord(record: WinRecord) {
        try {
            val existingRecords = getWinRecords().toMutableList()
            existingRecords.add(record)
            saveWinRecords(existingRecords)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 모든 우승 기록을 삭제하는 함수입니다.
     * SharedPreferences에서 KEY_WIN_RECORDS 키를 삭제합니다.
     */
    fun clearWinRecords() {
        try {
            prefs.edit().remove(KEY_WIN_RECORDS).apply()
        } catch (e: Exception) {
            e.printStackTrace()
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

    /**
     * 지도에서 선택한 음식 ID 리스트를 저장하는 함수입니다.
     * Gson을 사용하여 List<Int>를 JSON 문자열로 변환한 후 SharedPreferences에 저장합니다.
     *
     * @param foodIds 저장할 음식 ID 리스트
     */
    fun saveMapSelectedFoodIds(foodIds: List<Int>) {
        try {
            val json = gson.toJson(foodIds)
            prefs.edit().putString(KEY_MAP_SELECTED_FOOD_IDS, json).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 저장된 지도에서 선택한 음식 ID 리스트를 불러오는 함수입니다.
     * SharedPreferences에서 JSON 문자열을 읽어와 Gson으로 List<Int>로 변환합니다.
     *
     * @return 저장된 음식 ID 리스트 (저장된 리스트가 없으면 빈 리스트 반환)
     */
    fun getMapSelectedFoodIds(): List<Int> {
        return try {
            val json = prefs.getString(KEY_MAP_SELECTED_FOOD_IDS, null)
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
     * 지도에서 선택한 음식 ID를 추가하는 함수입니다.
     * 기존 리스트에 새로운 음식 ID를 추가하고 저장합니다.
     *
     * @param foodId 추가할 음식 ID
     */
    fun addMapSelectedFoodId(foodId: Int) {
        try {
            val existingIds = getMapSelectedFoodIds().toMutableList()
            if (!existingIds.contains(foodId)) {
                existingIds.add(foodId)
                saveMapSelectedFoodIds(existingIds)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 지도에서 선택한 음식 상세 정보 리스트를 저장하는 함수입니다.
     * Gson을 사용하여 List<MapSelectedFood>를 JSON 문자열로 변환한 후 SharedPreferences에 저장합니다.
     *
     * @param foods 저장할 MapSelectedFood 리스트
     */
    fun saveMapSelectedFoods(foods: List<MapSelectedFood>) {
        try {
            val json = gson.toJson(foods)
            prefs.edit().putString(KEY_MAP_SELECTED_FOODS, json).apply()
            
            // 호환성을 위해 ID 리스트도 함께 저장
            val foodIds = foods.map { it.foodId }
            saveMapSelectedFoodIds(foodIds)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 저장된 지도에서 선택한 음식 상세 정보 리스트를 불러오는 함수입니다.
     * SharedPreferences에서 JSON 문자열을 읽어와 Gson으로 List<MapSelectedFood>로 변환합니다.
     *
     * @return 저장된 MapSelectedFood 리스트 (저장된 리스트가 없으면 빈 리스트 반환)
     */
    fun getMapSelectedFoods(): List<MapSelectedFood> {
        return try {
            val json = prefs.getString(KEY_MAP_SELECTED_FOODS, null)
            if (json == null || json.isEmpty()) {
                // 레거시 데이터가 있으면 마이그레이션
                val legacyIds = getMapSelectedFoodIds()
                if (legacyIds.isNotEmpty()) {
                    // 레거시 ID 리스트를 MapSelectedFood로 변환 (기본값 사용)
                    val migrated = legacyIds.map { foodId ->
                        MapSelectedFood(
                            foodId = foodId,
                            selectedDate = System.currentTimeMillis(),
                            placeName = "",
                            placeAddress = "",
                            memo = ""
                        )
                    }
                    saveMapSelectedFoods(migrated)
                    return migrated
                }
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
     * 지도에서 선택한 음식 상세 정보를 추가하는 함수입니다.
     * 기존 리스트에 새로운 MapSelectedFood를 추가하고 저장합니다.
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
     * 지도에서 선택한 음식 상세 정보를 삭제하는 함수입니다.
     * foodId로 해당 항목을 찾아 삭제합니다.
     *
     * @param foodId 삭제할 음식 ID
     */
    fun removeMapSelectedFood(foodId: Int) {
        try {
            val existingFoods = getMapSelectedFoods().toMutableList()
            existingFoods.removeAll { it.foodId == foodId }
            saveMapSelectedFoods(existingFoods)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 지도에서 선택한 음식 상세 정보의 메모를 업데이트하는 함수입니다.
     *
     * @param foodId 업데이트할 음식 ID
     * @param memo 새로운 메모 내용
     */
    fun updateMapSelectedFoodMemo(foodId: Int, memo: String) {
        try {
            val existingFoods = getMapSelectedFoods().toMutableList()
            val index = existingFoods.indexOfFirst { it.foodId == foodId }
            if (index >= 0) {
                val food = existingFoods[index]
                existingFoods[index] = food.copy(memo = memo)
                saveMapSelectedFoods(existingFoods)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
