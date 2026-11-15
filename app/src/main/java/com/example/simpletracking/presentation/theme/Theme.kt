package com.example.simpletracking.presentation.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * 항상 라이트. 카카오 지도 SDK에는 다크 테마가 없어(바탕 지도는 map/skyview뿐) 폰이 다크모드면
 * 밝은 지도 위에 어두운 UI가 떠서 어울리지 않았다. 배경화면 색(dynamic color)도 따르지 않는다
 * (검색 화면이 연보라색이 되어 지도·경로선 파랑과 맞지 않았다). iOS는 Info.plist UIUserInterfaceStyle=Light.
 */
private val LightColors = lightColorScheme(
    primary = Brand,
    onPrimary = Color.White,
    primaryContainer = BrandContainer,
    onPrimaryContainer = Ink,
    secondary = Brand,
    onSecondary = Color.White,
    background = Color.White,
    onBackground = Ink,
    surface = Color.White,
    onSurface = Ink,
    surfaceVariant = Color(0xFFF1F3F4),
    onSurfaceVariant = InkSecondary,
    surfaceContainer = Color.White,
    surfaceContainerLow = Color.White,
    surfaceContainerHigh = Color(0xFFF5F6F7),
    surfaceContainerHighest = Color(0xFFEFF1F3),
    outline = Outline,
    outlineVariant = Outline,
)

@Composable
fun MapPilotTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = LightColors, typography = Typography, content = content)
}
