import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

// local.properties에서 Kakao API Key 읽기
val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(localPropertiesFile.inputStream())
}
val kakaoRestApiKey = localProperties.getProperty("KAKAO_REST_API_KEY", "")
val kakaoMapKey = localProperties.getProperty("KAKAO_MAP_KEY", "")

android {
    namespace = "com.example.foodworldcup"
    compileSdk = 36  // Android 15 (의존성 라이브러리 요구사항)

    defaultConfig {
        applicationId = "com.example.foodworldcup"
        minSdk = 26  // Android 8.0 Oreo
        targetSdk = 36  // Android 15
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        
        buildConfigField("String", "KAKAO_REST_API_KEY", "\"$kakaoRestApiKey\"")
        buildConfigField("String", "KAKAO_MAP_KEY", "\"$kakaoMapKey\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    
    // ViewBinding 및 BuildConfig 활성화
    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
}

dependencies {
    // Kakao Maps SDK
    implementation("com.kakao.maps.open:android:2.13.0")
    
    // Kakao SDK v2-all (Utility.getKeyHash() 사용을 위해 필요)
    implementation("com.kakao.sdk:v2-all:2.11.0")
    
    // Retrofit (Kakao Local API 사용)
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    
    // 이미지 로딩 라이브러리 (Glide)
    implementation(libs.glide)
    
    // JSON 직렬화/역직렬화 (SharedPreferences에 객체 저장 시 사용)
    implementation(libs.gson)
    
    // 카드 스와이프 뷰 (게임 화면에서 음식 선택용)
    implementation(libs.cardstackview)
    
    // 리사이클러뷰 (마이페이지에서 우승 기록 리스트 표시용)
    implementation(libs.androidx.recyclerview)
    
    // 카드뷰 (검색 결과 리스트 아이템 표시용)
    implementation("androidx.cardview:cardview:1.0.0")
    
    // Google Maps (지도 표시용) - Kakao Maps로 대체되지만 호환성을 위해 유지
    implementation(libs.google.maps)
    
    // Google Places API (음식점 검색용) - Kakao Local API로 대체되지만 호환성을 위해 유지
    implementation(libs.google.places)
    
    // Google Location Services (현재 위치 가져오기용)
    implementation(libs.google.location)
    
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}