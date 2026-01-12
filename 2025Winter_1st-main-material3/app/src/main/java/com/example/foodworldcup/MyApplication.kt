package com.example.foodworldcup

import android.app.Application
import com.kakao.vectormap.KakaoMapSdk
import com.example.foodworldcup.BuildConfig

/**
 * Application 클래스
 * 앱 시작 시 Kakao Map SDK 초기화
 * 참고: https://apis.map.kakao.com/android_v2/docs/getting-started/quickstart/
 */
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // Kakao Map SDK 초기화
        // local.properties에서 KAKAO_MAP_KEY를 읽어서 초기화
        val kakaoMapKey = BuildConfig.KAKAO_MAP_KEY
        android.util.Log.d("MyApplication", "KAKAO_MAP_KEY from BuildConfig: $kakaoMapKey")
        android.util.Log.d("MyApplication", "KAKAO_MAP_KEY length: ${kakaoMapKey.length}")
        
        if (kakaoMapKey.isNotEmpty()) {
            try {
                KakaoMapSdk.init(this, kakaoMapKey)
                android.util.Log.d("MyApplication", "KakaoMapSdk 초기화 성공")
            } catch (e: Exception) {
                android.util.Log.e("MyApplication", "KakaoMapSdk 초기화 실패", e)
            }
        } else {
            android.util.Log.e("MyApplication", "KAKAO_MAP_KEY가 설정되지 않았습니다. local.properties를 확인해주세요.")
        }
    }

}
