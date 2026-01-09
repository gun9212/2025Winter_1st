package com.example.foodworldcup.data

import java.util.Date

/**
 * 우승 기록을 저장하기 위한 데이터 클래스입니다.
 * 마이페이지에서 사용자가 이전에 우승한 음식과 날짜를 보여주기 위해 사용됩니다.
 *
 * @property id 기록의 고유 번호
 * @property selectedFoods 합격된 음식 리스트 (음식 ID 리스트)
 * @property winDate 우승한 날짜 및 시간
 * @property memo 사용자가 작성한 메모 (선택사항)
 */
data class WinRecord(
    val id: Long,
    val selectedFoods: List<Int>, // 합격된 음식 ID 리스트
    val winDate: Date,
    val memo: String = "" // 사용자가 작성한 메모
)
