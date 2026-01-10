package com.example.foodworldcup

import android.app.Application
import com.example.foodworldcup.BuildConfig
import com.kakao.vectormap.KakaoMapSdk

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
        if (kakaoMapKey.isNotEmpty()) {
            KakaoMapSdk.init(this, kakaoMapKey)
        } else {
            android.util.Log.e("MyApplication", "KAKAO_MAP_KEY가 설정되지 않았습니다. local.properties를 확인해주세요.")
        }
    }
}
