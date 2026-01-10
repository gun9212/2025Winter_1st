package com.example.foodworldcup.game

import com.example.foodworldcup.data.Food
import com.yuyakaido.android.cardstackview.Direction

/**
 * 게임 상태를 관리하는 클래스입니다.
 * 스와이프 히스토리를 관리하여 Rewind 기능을 지원합니다.
 * 
 * 주요 기능:
 * - 남은 음식, 합격된 음식, 탈락된 음식 리스트 관리
 * - 스와이프 히스토리 관리 (Rewind 기능을 위해)
 * - 게임 상태 조회 및 업데이트
 */
class GameStateManager(initialFoods: List<Food>) {

    /**
     * 스와이프 액션을 나타내는 데이터 클래스입니다.
     * 
     * @property direction 스와이프 방향 (Left, Right)
     * @property food 스와이프된 음식
     * @property timestamp 스와이프 시간 (밀리초)
     */
    data class SwipeAction(
        val direction: Direction,
        val food: Food,
        val timestamp: Long = System.currentTimeMillis()
    )

    // 남은 음식 리스트
    private val remainingFoods: MutableList<Food> = initialFoods.toMutableList()

    // 합격된 음식 리스트 (오른쪽 스와이프)
    private val passedFoods: MutableList<Food> = mutableListOf()

    // 탈락된 음식 리스트 (왼쪽 스와이프)
    private val rejectedFoods: MutableList<Food> = mutableListOf()

    // Rewind를 위한 스와이프 히스토리 (Stack)
    private val swipeHistory: ArrayDeque<SwipeAction> = ArrayDeque()

    /**
     * 스와이프 시 상태를 업데이트하고 히스토리에 저장합니다.
     * 
     * @param direction 스와이프 방향 (Left 또는 Right)
     * @param food 스와이프된 음식
     */
    fun swipeCard(direction: Direction, food: Food) {
        // 남은 음식 리스트에서 제거
        remainingFoods.remove(food)

        // 방향에 따라 합격 또는 탈락 리스트에 추가
        when (direction) {
            Direction.Right -> {
                passedFoods.add(food)
            }
            Direction.Left -> {
                rejectedFoods.add(food)
            }
            else -> {
                // Left, Right가 아닌 경우는 처리하지 않음
                return
            }
        }

        // 히스토리에 저장
        swipeHistory.addLast(SwipeAction(direction, food))
    }

    /**
     * Rewind 가능 여부를 확인합니다.
     * 
     * @return 히스토리가 비어있지 않으면 true, 그렇지 않으면 false
     */
    fun canRewind(): Boolean {
        return swipeHistory.isNotEmpty()
    }

    /**
     * 마지막 스와이프를 취소하고 상태를 복구합니다.
     * 
     * @return 복구된 SwipeAction (히스토리가 비어있으면 null)
     */
    fun rewind(): SwipeAction? {
        if (swipeHistory.isEmpty()) {
            return null
        }

        // 마지막 액션 가져오기
        val lastAction = swipeHistory.removeLast()

        // 남은 음식 리스트에 복구 (맨 앞에 추가)
        remainingFoods.add(0, lastAction.food)

        // 방향에 따라 합격 또는 탈락 리스트에서 제거
        when (lastAction.direction) {
            Direction.Right -> {
                passedFoods.remove(lastAction.food)
            }
            Direction.Left -> {
                rejectedFoods.remove(lastAction.food)
            }
            else -> {
                // 처리하지 않음
            }
        }

        return lastAction
    }

    /**
     * 게임 종료 여부를 확인합니다.
     * 
     * @return 남은 음식 리스트가 비어있으면 true, 그렇지 않으면 false
     */
    fun isGameFinished(): Boolean {
        return remainingFoods.isEmpty()
    }

    /**
     * 남은 음식 리스트를 반환합니다.
     * 
     * @return 남은 음식 리스트 (읽기 전용)
     */
    fun getRemainingFoods(): List<Food> {
        return remainingFoods.toList()
    }

    /**
     * 합격된 음식 리스트를 반환합니다.
     * 
     * @return 합격된 음식 리스트 (읽기 전용)
     */
    fun getPassedFoods(): List<Food> {
        return passedFoods.toList()
    }

    /**
     * 탈락된 음식 리스트를 반환합니다.
     * 
     * @return 탈락된 음식 리스트 (읽기 전용)
     */
    fun getRejectedFoods(): List<Food> {
        return rejectedFoods.toList()
    }

    /**
     * 현재 남은 음식 개수를 반환합니다.
     * 
     * @return 남은 음식 개수
     */
    fun getRemainingCount(): Int {
        return remainingFoods.size
    }

    /**
     * 전체 음식 개수를 반환합니다.
     * 
     * @return 전체 음식 개수
     */
    fun getTotalCount(): Int {
        return remainingFoods.size + passedFoods.size + rejectedFoods.size
    }
}
