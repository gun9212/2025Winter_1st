package com.example.foodworldcup.data

import java.util.Date

/**
 * 우승 기록을 저장하기 위한 데이터 클래스입니다.
 * 마이페이지에서 사용자가 이전에 우승한 음식과 날짜를 보여주기 위해 사용됩니다.
 *
 * @property id 기록의 고유 번호
 * @property foodName 우승한 음식 이름
 * @property winDate 우승한 날짜 및 시간
 */
data class WinRecord(
    val id: Long,
    val foodName: String,
    val winDate: Date
)
