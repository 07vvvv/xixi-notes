package com.xixi.notes.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import android.os.Build

/** 主题模式：跟随系统 / 强制深色 / 强制浅色 */
enum class ThemeMode {
    FOLLOW_SYSTEM,
    DARK,
    LIGHT
}

/** 四象限色（固定色，不随动态取色变化） */
@Immutable
data class QuadrantColors(
    val importantUrgent: Color,
    val urgentOnly: Color,
    val importantOnly: Color,
    val neither: Color
)

private val QuadrantColorsDark = QuadrantColors(
    importantUrgent = Color(0xFF6EE7B7),
    urgentOnly = Color(0xFFFB923C),
    importantOnly = Color(0xFFA1A1AA),
    neither = Color(0xFFA78BFA)
)

// 四象限色在深浅色下保持一致（规范要求固定）
private val QuadrantColorsLight = QuadrantColorsDark

private val XixiColorsDark = XixiColors(
    background = Color(0xFF0A0A0A),
    card = Color(0xFF141414),
    textPrimary = Color(0xFFFAFAFA),
    textSecondary = Color(0xFF71717A),
    isDark = true
)

private val XixiColorsLight = XixiColors(
    background = Color(0xFFFAFAFA),
    card = Color(0xFFFFFFFF),
    textPrimary = Color(0xFF18181B),
    textSecondary = Color(0xFF52525B),
    isDark = false
)

/** 应用自定义色板：背景 / 卡片 / 主文字 / 次文字 */
@Immutable
data class XixiColors(
    val background: Color,
    val card: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val isDark: Boolean
)

val LocalXixiColors = staticCompositionLocalOf { XixiColorsDark }
val LocalQuadrantColors = staticCompositionLocalOf { QuadrantColorsDark }

/** 便捷访问：主题自定义色板 */
object XixiTheme {
    val colors: XixiColors
        @Composable
        @ReadOnlyComposable
        get() = LocalXixiColors.current

    val quadrant: QuadrantColors
        @Composable
        @ReadOnlyComposable
        get() = LocalQuadrantColors.current
}

/**
 * 应用主题。
 *
 * - 默认跟随系统主题；[mode] 可强制深色 / 浅色
 * - [dynamicColor] 为 true 且 Android 12+ 时使用系统动态取色；
 *   四象限色与 magnet-select 颜色始终固定
 */
@Composable
fun XixiNotesTheme(
    mode: ThemeMode = ThemeMode.FOLLOW_SYSTEM,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val dark = when (mode) {
        ThemeMode.FOLLOW_SYSTEM -> systemDark
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
    }

    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        dark -> darkColorScheme(
            primary = Color(0xFF6EE7B7),
            onPrimary = Color(0xFF052E22),
            secondary = Color(0xFFA78BFA),
            background = XixiColorsDark.background,
            onBackground = XixiColorsDark.textPrimary,
            surface = XixiColorsDark.card,
            onSurface = XixiColorsDark.textPrimary,
            surfaceVariant = Color(0xFF1F1F23),
            onSurfaceVariant = XixiColorsDark.textSecondary,
            outline = Color(0xFF2A2A2E)
        )
        else -> lightColorScheme(
            primary = Color(0xFF0F766E),
            onPrimary = Color(0xFFFFFFFF),
            secondary = Color(0xFF7C3AED),
            background = XixiColorsLight.background,
            onBackground = XixiColorsLight.textPrimary,
            surface = XixiColorsLight.card,
            onSurface = XixiColorsLight.textPrimary,
            surfaceVariant = Color(0xFFF4F4F5),
            onSurfaceVariant = XixiColorsLight.textSecondary,
            outline = Color(0xFFE4E4E7)
        )
    }

    val xixiColors = if (dark) XixiColorsDark else XixiColorsLight
    val quadrantColors = if (dark) QuadrantColorsDark else QuadrantColorsLight

    CompositionLocalProvider(
        LocalXixiColors provides xixiColors,
        LocalQuadrantColors provides quadrantColors
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = XixiTypography,
            shapes = XixiShapes,
            content = content
        )
    }
}
