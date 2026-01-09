plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

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
    
    // ViewBinding 활성화
    buildFeatures {
        viewBinding = true
    }
}

dependencies {

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
    // implementation(libs.cardstackview)  // TODO: 스와이프 기능 구현 시 주석 해제
    
    // 리사이클러뷰 (마이페이지에서 우승 기록 리스트 표시용)
    implementation(libs.androidx.recyclerview)
    
    // Google Maps (지도 표시용)
    implementation(libs.google.maps)
    
    // Google Places API (음식점 검색용)
    implementation(libs.google.places)
    
    // Google Location Services (현재 위치 가져오기용)
    implementation(libs.google.location)
    
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}