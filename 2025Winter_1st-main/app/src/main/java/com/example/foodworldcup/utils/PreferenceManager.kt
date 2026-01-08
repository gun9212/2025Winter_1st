package com.example.foodworldcup.utils

import android.content.Context
import android.content.SharedPreferences
import com.example.foodworldcup.data.WinRecord
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

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
    }

    /**
     * 우승 기록을 저장하는 함수입니다.
     * Gson을 사용하여 WinRecord 객체 리스트를 JSON 문자열로 변환한 후 저장합니다.
     *
     * @param records 저장할 우승 기록 리스트
     */
    fun saveWinRecords(records: List<WinRecord>) {
        // TODO: Gson을 사용하여 records를 JSON 문자열로 변환하고 SharedPreferences에 저장합니다.
        // 예: val json = gson.toJson(records)
        //     prefs.edit().putString(KEY_WIN_RECORDS, json).apply()
    }

    /**
     * 저장된 우승 기록을 불러오는 함수입니다.
     * SharedPreferences에서 JSON 문자열을 읽어와 Gson으로 WinRecord 리스트로 변환합니다.
     *
     * @return 저장된 우승 기록 리스트 (저장된 기록이 없으면 빈 리스트 반환)
     */
    fun getWinRecords(): List<WinRecord> {
        // TODO: SharedPreferences에서 JSON 문자열을 읽어와 Gson으로 WinRecord 리스트로 변환합니다.
        // 예: val json = prefs.getString(KEY_WIN_RECORDS, null)
        //     if (json == null) return emptyList()
        //     val type = object : TypeToken<List<WinRecord>>() {}.type
        //     return gson.fromJson(json, type) ?: emptyList()
        return emptyList()
    }

    /**
     * 새로운 우승 기록을 추가하는 함수입니다.
     * 기존 기록을 불러온 후, 새로운 기록을 추가하고 다시 저장합니다.
     *
     * @param record 추가할 우승 기록
     */
    fun addWinRecord(record: WinRecord) {
        // TODO: getWinRecords()로 기존 기록을 불러온 후, record를 추가하고 saveWinRecords()로 저장합니다.
    }

    /**
     * 모든 우승 기록을 삭제하는 함수입니다.
     */
    fun clearWinRecords() {
        // TODO: SharedPreferences에서 KEY_WIN_RECORDS 키를 삭제합니다.
        // 예: prefs.edit().remove(KEY_WIN_RECORDS).apply()
    }
}
