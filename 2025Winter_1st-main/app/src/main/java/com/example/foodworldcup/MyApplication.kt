package com.example.foodworldcup

import android.app.Application
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.Signature
import com.example.foodworldcup.BuildConfig
import com.kakao.vectormap.KakaoMapSdk
import java.security.MessageDigest

/**
 * Application 클래스
 * 앱 시작 시 Kakao Map SDK 초기화
 * 참고: https://apis.map.kakao.com/android_v2/docs/getting-started/quickstart/
 */
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // 카카오맵 API 등록용 해시값 출력
        printKakaoMapHash()
        
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
    
    /**
     * 카카오맵 API 등록에 필요한 해시값을 로그로 출력합니다.
     * 카카오 개발자 콘솔(https://developers.kakao.com)에서 앱 등록 시 사용합니다.
     */
    private fun printKakaoMapHash() {
        try {
            val packageName = packageName
            val packageInfo: PackageInfo = packageManager.getPackageInfo(
                packageName,
                PackageManager.GET_SIGNATURES
            )
            
            android.util.Log.d("MyApplication", "========================================")
            android.util.Log.d("MyApplication", "카카오맵 API 등록용 정보")
            android.util.Log.d("MyApplication", "========================================")
            android.util.Log.d("MyApplication", "패키지명: $packageName")
            
            val signatures = packageInfo.signatures
            if (signatures != null) {
                for (signature in signatures) {
                    val md: MessageDigest = MessageDigest.getInstance("SHA")
                    md.update(signature.toByteArray())
                    val hash = md.digest()
                    
                    // SHA-1 해시를 16진수 문자열로 변환
                    val hashString = hash.joinToString("") { "%02X".format(it) }
                    
                    android.util.Log.d("MyApplication", "SHA-1: $hashString")
                    android.util.Log.d("MyApplication", "해시값 (패키지명 + SHA-1): $packageName;$hashString")
                }
            } else {
                android.util.Log.w("MyApplication", "서명 정보를 가져올 수 없습니다.")
            }
            android.util.Log.d("MyApplication", "========================================")
            android.util.Log.d("MyApplication", "위 해시값을 카카오 개발자 콘솔에 등록하세요.")
            android.util.Log.d("MyApplication", "https://developers.kakao.com > 내 애플리케이션 > 플랫폼 > Android 플랫폼 추가")
            android.util.Log.d("MyApplication", "========================================")
        } catch (e: Exception) {
            android.util.Log.e("MyApplication", "해시값 출력 실패", e)
        }
    }
}
