package com.example.foodworldcup.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.foodworldcup.ui.compose.FoodWorldCupTheme
import com.example.foodworldcup.ui.compose.StartScreen

/**
 * 앱의 메인 Activity입니다.
 * StartScreen을 호출하여 앱을 시작합니다.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Intent에서 특정 탭으로 이동할지 확인
        val navigateTo = intent.getStringExtra("navigate_to")

        setContent {
            FoodWorldCupTheme {
                StartScreen(navigateTo = navigateTo)
            }
        }
    }
}
