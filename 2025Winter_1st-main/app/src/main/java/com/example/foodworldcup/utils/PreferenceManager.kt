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
        private const val KEY_WIN_RECORDS = "win_records"
        private const val KEY_SELECTED_FOOD_IDS = "selected_food_ids"
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
     */
    fun clearWinRecords() {
        // TODO: SharedPreferences에서 KEY_WIN_RECORDS 키를 삭제합니다.
        // 예: prefs.edit().remove(KEY_WIN_RECORDS).apply()
    }

    /**
     * 선택된 음식 ID 리스트를 저장하는 함수입니다.
     *
     * @param foodIds 저장할 음식 ID 리스트
     */
    fun saveSelectedFoodIds(foodIds: List<Int>) {
        // TODO: Gson을 사용하여 foodIds를 JSON 문자열로 변환하고 SharedPreferences에 저장합니다.
        // 예: val json = gson.toJson(foodIds)
        //     prefs.edit().putString(KEY_SELECTED_FOOD_IDS, json).apply()
    }

    /**
     * 저장된 선택된 음식 ID 리스트를 불러오는 함수입니다.
     *
     * @return 저장된 음식 ID 리스트 (저장된 리스트가 없으면 빈 리스트 반환)
     */
    fun getSelectedFoodIds(): List<Int> {
        // TODO: SharedPreferences에서 JSON 문자열을 읽어와 Gson으로 음식 ID 리스트로 변환합니다.
        // 예: val json = prefs.getString(KEY_SELECTED_FOOD_IDS, null)
        //     if (json == null) return emptyList()
        //     val type = object : TypeToken<List<Int>>() {}.type
        //     return gson.fromJson(json, type) ?: emptyList()
        return emptyList()
    }
}
