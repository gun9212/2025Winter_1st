package com.example.foodworldcup.ui.compose

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.example.foodworldcup.R

/**
 * ColorScheme 확장 속성 - 추가 색상에 접근하기 위한 헬퍼
 */
val ColorScheme.orangeGradientEnd: Color
    get() = AppColors.OrangeGradientEnd

/**
 * 카페24 써라운드 체 폰트 패밀리
 */
val Cafe24SurroundFontFamily = FontFamily(
    Font(R.font.cafe24_surround, FontWeight.Normal)
)

/**
 * 커스텀 Typography - 카페24 써라운드 체 적용
 * Material3의 기본 Typography를 기반으로 폰트만 변경
 */
private val defaultTypography = Typography()

private val AppTypography = Typography(
    displayLarge = defaultTypography.displayLarge.copy(fontFamily = Cafe24SurroundFontFamily),
    displayMedium = defaultTypography.displayMedium.copy(fontFamily = Cafe24SurroundFontFamily),
    displaySmall = defaultTypography.displaySmall.copy(fontFamily = Cafe24SurroundFontFamily),
    headlineLarge = defaultTypography.headlineLarge.copy(fontFamily = Cafe24SurroundFontFamily),
    headlineMedium = defaultTypography.headlineMedium.copy(fontFamily = Cafe24SurroundFontFamily),
    headlineSmall = defaultTypography.headlineSmall.copy(fontFamily = Cafe24SurroundFontFamily),
    titleLarge = defaultTypography.titleLarge.copy(fontFamily = Cafe24SurroundFontFamily),
    titleMedium = defaultTypography.titleMedium.copy(fontFamily = Cafe24SurroundFontFamily),
    titleSmall = defaultTypography.titleSmall.copy(fontFamily = Cafe24SurroundFontFamily),
    bodyLarge = defaultTypography.bodyLarge.copy(fontFamily = Cafe24SurroundFontFamily),
    bodyMedium = defaultTypography.bodyMedium.copy(fontFamily = Cafe24SurroundFontFamily),
    bodySmall = defaultTypography.bodySmall.copy(fontFamily = Cafe24SurroundFontFamily),
    labelLarge = defaultTypography.labelLarge.copy(fontFamily = Cafe24SurroundFontFamily),
    labelMedium = defaultTypography.labelMedium.copy(fontFamily = Cafe24SurroundFontFamily),
    labelSmall = defaultTypography.labelSmall.copy(fontFamily = Cafe24SurroundFontFamily)
)

/**
 * Light Color Scheme - 앱의 기본 테마
 */
private val LightColorScheme = lightColorScheme(
    primary = AppColors.PrimaryOrange,
    onPrimary = AppColors.White,
    primaryContainer = AppColors.PrimaryOrange.copy(alpha = 0.2f),
    onPrimaryContainer = AppColors.PrimaryOrange,
    
    secondary = AppColors.Gray,
    onSecondary = AppColors.White,
    secondaryContainer = AppColors.Gray.copy(alpha = 0.2f),
    onSecondaryContainer = AppColors.Gray,
    
    tertiary = AppColors.LightIvory,
    onTertiary = AppColors.TextBlack,
    
    background = AppColors.BackgroundBeige,
    onBackground = AppColors.TextBlack,
    
    surface = AppColors.White,
    onSurface = AppColors.TextBlack,
    surfaceVariant = AppColors.LightIvory,
    onSurfaceVariant = AppColors.Gray,
    
    outline = AppColors.Gray.copy(alpha = 0.5f),
    outlineVariant = AppColors.LightGray,
    
    error = Color(0xFFBA1A1A),
    onError = AppColors.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    
    inverseSurface = AppColors.TextBlack,
    inverseOnSurface = AppColors.White,
    inversePrimary = AppColors.PrimaryOrange.copy(alpha = 0.8f),
    
    scrim = Color.Black,
    surfaceTint = AppColors.PrimaryOrange
)

/**
 * Dark Color Scheme - 다크 모드용 (선택적)
 */
private val DarkColorScheme = darkColorScheme(
    primary = AppColors.PrimaryOrange,
    onPrimary = AppColors.TextBlack,
    primaryContainer = AppColors.PrimaryOrange.copy(alpha = 0.3f),
    onPrimaryContainer = AppColors.PrimaryOrange,
    
    secondary = AppColors.Gray,
    onSecondary = AppColors.TextBlack,
    secondaryContainer = AppColors.Gray.copy(alpha = 0.3f),
    onSecondaryContainer = AppColors.Gray,
    
    tertiary = AppColors.LightIvory.copy(alpha = 0.2f),
    onTertiary = AppColors.White,
    
    background = Color(0xFF1C1C1C),
    onBackground = AppColors.White,
    
    surface = Color(0xFF2C2C2C),
    onSurface = AppColors.White,
    surfaceVariant = Color(0xFF3C3C3C),
    onSurfaceVariant = AppColors.Gray,
    
    outline = AppColors.Gray,
    outlineVariant = AppColors.Gray.copy(alpha = 0.5f),
    
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    
    inverseSurface = AppColors.White,
    inverseOnSurface = AppColors.TextBlack,
    inversePrimary = AppColors.PrimaryOrange,
    
    scrim = Color.Black,
    surfaceTint = AppColors.PrimaryOrange
)

/**
 * 앱의 MaterialTheme 설정
 * 
 * @param darkTheme 다크 모드 사용 여부 (기본값: 시스템 설정 따름)
 * @param content 테마가 적용될 콘텐츠
 */
@Composable
fun FoodWorldCupTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) {
        DarkColorScheme
    } else {
        LightColorScheme
    }
    
    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}
