package com.linjiang.command.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = Accent,
    onPrimary = TextPrimary,
    secondary = CrimsonGrey80,
    tertiary = Pink80,
    background = BgDeep,
    surface = BgDeep,
    surfaceVariant = BgCard,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    onSurfaceVariant = TextSecondary,
    primaryContainer = BgCard,
    onPrimaryContainer = TextPrimary,
    secondaryContainer = BgCardHover,
    onSecondaryContainer = TextPrimary,
    inverseSurface = TextPrimary,
    inverseOnSurface = BgDeep,
    outline = TextDim,
    outlineVariant = GlassBorder  // 暖金色微发光边框
)

private val LightColorScheme = lightColorScheme(
    primary = Crimson40,
    secondary = CrimsonGrey40,
    tertiary = Pink40
)

@Composable
fun LinjiangCommandTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    // 强制暗色主题 — 指挥中心不需要亮色模式
    val colorScheme = DarkColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = BgDeep.toArgb()
            window.navigationBarColor = BgNavBar.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
