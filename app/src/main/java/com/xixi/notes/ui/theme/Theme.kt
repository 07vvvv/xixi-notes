package com.xixi.notes.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * 应用主题（第一步：只改 token 的组织方式，不改任何功能逻辑）。
 *
 * - 颜色常量统一放在 Color.kt，排版 / 圆角 / 间距 / 阴影放在 Type.kt
 * - [XixiTheme] 的对外 API 与 [XixiNotesTheme] 的签名保持不变，页面无需改动即可编译
 * - 不使用系统动态取色（已移除 Android 12+ 的 dynamicColorScheme）
 */

/** 主题模式：跟随系统 / 强制深色 / 强制浅色（顺序不可调整，强调色板按下标取值） */
enum class ThemeMode {
    FOLLOW_SYSTEM,
    DARK,
    LIGHT
}

val LocalXixiColors = staticCompositionLocalOf { XixiColorsDark }
val LocalQuadrantColors = staticCompositionLocalOf { XixiQuadrantColors }
val LocalAccentPalette = staticCompositionLocalOf { XixiAccentPalette }

/** 便捷访问：主题自定义色板 + 四象限固定色 + 强调色板 */
object XixiTheme {
    val colors: XixiColors
        @Composable
        @ReadOnlyComposable
        get() = LocalXixiColors.current

    val quadrant: QuadrantColors
        @Composable
        @ReadOnlyComposable
        get() = LocalQuadrantColors.current

    val accent: AccentPalette
        @Composable
        @ReadOnlyComposable
        get() = LocalAccentPalette.current

    /** 语义形状集合（不随深浅色变化） */
    val shapes: XixiShapesToken
        @Composable
        @ReadOnlyComposable
        get() = XixiShapesSet
}

/**
 * Material3 角色映射（供 Material3 组件与少量仍直接读取 colorScheme 的页面使用）：
 * primary      -> 统一强调色（Dock / FAB / 主按钮 / 光标 / 开关）
 * secondary    -> 强调色的柔和变体
 * tertiary     -> 四象限"重要不紧急"色（少量装饰）
 * background   -> 纯色背景（深色近黑 / 浅色近白）
 * surface      -> 一级卡片底色（深色比背景亮一档 / 浅色纯白）
 * surfaceVariant -> 内嵌底（输入框、选中底）
 * outline      -> 细描边
 */
private val DarkScheme: ColorScheme = darkColorScheme(
    primary = XixiColorsDark.accent,
    onPrimary = XixiColorsDark.onAccent,
    primaryContainer = XixiColorsDark.accentSoft,
    onPrimaryContainer = XixiColorsDark.accent,
    secondary = XixiColorsDark.accent,
    onSecondary = XixiColorsDark.onAccent,
    tertiary = QuadrantImportantOnly,
    onTertiary = XixiColorsDark.onAccent,
    background = XixiColorsDark.background,
    onBackground = XixiColorsDark.textPrimary,
    surface = XixiColorsDark.card,
    onSurface = XixiColorsDark.textPrimary,
    surfaceVariant = XixiColorsDark.sunken,
    onSurfaceVariant = XixiColorsDark.textSecondary,
    surfaceContainer = XixiColorsDark.cardElevated,
    surfaceContainerHigh = XixiColorsDark.cardElevated,
    outline = XixiColorsDark.outline,
    outlineVariant = XixiColorsDark.outline,
    error = XixiColorsDark.danger,
    onError = XixiColorsDark.onDangerContainer,
    errorContainer = XixiColorsDark.dangerContainer,
    onErrorContainer = XixiColorsDark.onDangerContainer
)

private val LightScheme: ColorScheme = lightColorScheme(
    primary = XixiColorsLight.accent,
    onPrimary = XixiColorsLight.onAccent,
    primaryContainer = XixiColorsLight.accentSoft,
    onPrimaryContainer = XixiColorsLight.accent,
    secondary = XixiColorsLight.accent,
    onSecondary = XixiColorsLight.onAccent,
    tertiary = QuadrantImportantOnly,
    onTertiary = XixiColorsLight.onAccent,
    background = XixiColorsLight.background,
    onBackground = XixiColorsLight.textPrimary,
    surface = XixiColorsLight.card,
    onSurface = XixiColorsLight.textPrimary,
    surfaceVariant = XixiColorsLight.sunken,
    onSurfaceVariant = XixiColorsLight.textSecondary,
    surfaceContainer = XixiColorsLight.cardElevated,
    surfaceContainerHigh = XixiColorsLight.cardElevated,
    outline = XixiColorsLight.outline,
    outlineVariant = XixiColorsLight.outline,
    error = XixiColorsLight.danger,
    onError = XixiColorsLight.onDangerContainer,
    errorContainer = XixiColorsLight.dangerContainer,
    onErrorContainer = XixiColorsLight.onDangerContainer
)

/**
 * 应用主题。
 *
 * - 默认跟随系统主题；[mode] 可强制深色 / 浅色
 * - 固定使用本应用配色；四象限色与强调色始终固定，不随动态取色变化
 */
@Composable
fun XixiNotesTheme(
    mode: ThemeMode = ThemeMode.FOLLOW_SYSTEM,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val dark = when (mode) {
        ThemeMode.FOLLOW_SYSTEM -> systemDark
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
    }

    val colorScheme = if (dark) DarkScheme else LightScheme
    val xixiColors = if (dark) XixiColorsDark else XixiColorsLight

    CompositionLocalProvider(
        LocalXixiColors provides xixiColors,
        LocalQuadrantColors provides XixiQuadrantColors,
        LocalAccentPalette provides XixiAccentPalette
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = XixiTypography,
            shapes = XixiShapes,
            content = content
        )
    }
}
