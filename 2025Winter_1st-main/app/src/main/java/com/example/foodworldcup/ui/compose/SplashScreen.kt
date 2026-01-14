package com.example.foodworldcup.ui.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.example.foodworldcup.R

/**
 * 스플래시 스크린 화면
 * 앱 시작 시 표시되는 로딩 화면
 */
@Composable
fun SplashScreen() {
    val context = LocalContext.current
    val density = LocalDensity.current
    
    BoxWithConstraints(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // 화면 크기를 픽셀로 변환
        val screenWidthPx = with(density) { maxWidth.toPx().toInt() }
        val screenHeightPx = with(density) { maxHeight.toPx().toInt() }
        
        // 배경 색상
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF5F0E8))
        )
        
        // 스플래시 이미지 (중앙에 크게 표시, 크기 조정하여 로드)
        SubcomposeAsyncImage(
            model = ImageRequest.Builder(context)
                .data(R.drawable.splash_fullscreen)
                .size(screenWidthPx, screenHeightPx) // 화면 크기에 맞춰 리사이즈
                .build(),
            contentDescription = "Splash Screen",
            modifier = Modifier
                .fillMaxWidth(1.0f) // 화면 전체 너비
                .fillMaxHeight(1.0f), // 화면 전체 높이
            contentScale = ContentScale.Fit
        )
    }
}

@Preview(showBackground = true)
@Composable
fun SplashScreenPreview() {
    SplashScreen()
}
