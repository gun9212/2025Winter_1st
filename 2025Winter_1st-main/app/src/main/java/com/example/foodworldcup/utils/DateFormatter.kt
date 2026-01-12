package com.example.foodworldcup.utils

import java.text.SimpleDateFormat
import java.util.Locale

/**
 * 날짜 포맷터를 재사용하기 위한 유틸리티 객체입니다.
 * SimpleDateFormat은 스레드 안전하지 않으므로 각 스레드별로 인스턴스를 생성합니다.
 */
object DateFormatter {

    /**
     * "yyyy/MM/dd" 형식의 날짜 포맷터 (접시 날짜 표시용)
     */
    val dateFormatShort: SimpleDateFormat
        get() = SimpleDateFormat("yyyy/MM/dd", Locale.KOREAN)

    /**
     * "yyyy년 MM월 dd일" 형식의 날짜 포맷터 (상세 정보 표시용)
     */
    val dateFormatLong: SimpleDateFormat
        get() = SimpleDateFormat("yyyy년 MM월 dd일", Locale.KOREAN)
}
