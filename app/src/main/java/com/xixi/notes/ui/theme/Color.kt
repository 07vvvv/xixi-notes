package com.xixi.notes.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * 颜色体系（第一步：只定义 token，不改任何功能逻辑）。
 *
 * 设计规则：
 * 1. 背景一律纯色，不使用渐变；深色近黑、浅色近白。
 * 2. 深色卡片比背景稍亮一档；浅色卡片为纯白。
 * 3. 强调色全 App 统一（Dock 选中、FAB、主按钮、指示器、光标、开关等）。
 * 4. 四象限强调色彼此和谐区分，且深浅色下保持一致（固定色，不随动态取色变化）。
 * 5. 语义色（成功 / 危险 / 警告）独立成组，避免各页面零散硬编码。
 *
 * 注意：本文件只新增常量，Material3 ColorScheme 的组装仍在 Theme.kt。
 */

// ---------------------------------------------------------------------------
// 深色模式基础色（纯色，无渐变）
// ---------------------------------------------------------------------------

/** 深色背景：近黑深灰 */
val DarkBackgroundN2 = Color(0xFF0B0B0D)

/** 深色一级卡片：比背景稍亮一档 */
val DarkSurfaceN1 = Color(0xFF17171A)

/** 深色二级卡片 / 悬浮层：再亮一档，用于弹层、选中态底色 */
val DarkSurfaceN2 = Color(0xFF202024)

/** 深色输入框 / 内嵌凹槽底色 */
val DarkSurfaceSunken = Color(0xFF111114)

/** 深色模式细描边 */
val DarkOutline = Color(0xFF2B2B31)

/** 文字：主 / 次 / 辅助（辅助信息更小更淡） */
val DarkTextPrimary = Color(0xFFF7F7F8)
val DarkTextSecondary = Color(0xFF9C9CA6)
val DarkTextTertiary = Color(0xFF6B6B75)

/** 深色按下态高亮覆盖（配合强调色使用） */
val DarkPressedOverlay = Color(0xFF27272C)

// ---------------------------------------------------------------------------
// 浅色模式基础色（纯色，无渐变）
// ---------------------------------------------------------------------------

/** 浅色背景：近白浅灰 */
val LightBackgroundN2 = Color(0xFFF5F5F7)

/** 浅色一级卡片：纯白 */
val LightSurfaceN1 = Color(0xFFFFFFFF)

/** 浅色二级卡片 / 悬浮层：极浅灰，用于弹层、选中态底色 */
val LightSurfaceN2 = Color(0xFFF0F0F2)

/** 浅色输入框 / 内嵌凹槽底色 */
val LightSurfaceSunken = Color(0xFFEFEFF1)

/** 浅色模式细描边 */
val LightOutline = Color(0xFFE5E5E9)

/** 文字：主 / 次 / 辅助 */
val LightTextPrimary = Color(0xFF15151A)
val LightTextSecondary = Color(0xFF5C5C66)
val LightTextTertiary = Color(0xFF8A8A94)

/** 浅色按下态高亮覆盖 */
val LightPressedOverlay = Color(0xFFE9E9EE)

/** 纯白 */
val PureWhite = Color(0xFFFFFFFF)

// ---------------------------------------------------------------------------
// 统一强调色
// ---------------------------------------------------------------------------

/**
 * 全 App 唯一强调色（薄荷青绿）。
 * Dock 选中、FAB、主按钮、指示器、输入光标、开关、进度等统一使用。
 * 深色底用亮一档的 [AccentDark]，浅色底用深一档的 [AccentLight] 以保证对比度。
 */
val AccentDark = Color(0xFF4FD1B0)
val AccentLight = Color(0xFF0E9E82)

/** 强调色上的文字 / 图标（深色底用近黑，浅色底用纯白） */
val OnAccentDark = Color(0xFF04231C)
val OnAccentLight = PureWhite

/** 强调色柔和底（选中胶囊、标签底、轻量高亮） */
val AccentSoftDark = Color(0x334FD1B0)
val AccentSoftLight = Color(0x1F0E9E82)

/** 强调色描边（选中卡片边框） */
val AccentOutlineDark = Color(0x804FD1B0)
val AccentOutlineLight = Color(0x660E9E82)

// ---------------------------------------------------------------------------
// 四象限强调色（固定色，深浅模式保持一致）
// ---------------------------------------------------------------------------

/** 重要且紧急 —— 珊瑚红 */
val QuadrantImportantUrgent = Color(0xFFF87171)

/** 紧急不重要 —— 琥珀橙 */
val QuadrantUrgentOnly = Color(0xFFFBBF24)

/** 重要不紧急 —— 天青蓝 */
val QuadrantImportantOnly = Color(0xFF60A5FA)

/** 都不 —— 中性灰 */
val QuadrantNeither = Color(0xFF9CA3AF)

/** 四象限固定配色 */
@Immutable
data class QuadrantColors(
    val importantUrgent: Color,
    val urgentOnly: Color,
    val importantOnly: Color,
    val neither: Color
)

val XixiQuadrantColors = QuadrantColors(
    importantUrgent = QuadrantImportantUrgent,
    urgentOnly = QuadrantUrgentOnly,
    importantOnly = QuadrantImportantOnly,
    neither = QuadrantNeither
)

// ---------------------------------------------------------------------------
// 语义色（成功 / 危险 / 警告）
// ---------------------------------------------------------------------------

/** 深色模式：完成、已授权、通过 */
val SuccessDark = Color(0xFF4ADE80)

/** 浅色模式：完成、已授权、通过 */
val SuccessLight = Color(0xFF16A34A)

/** 深色模式：删除、逾期、未授权 */
val DangerDark = Color(0xFFF87171)

/** 浅色模式：删除、逾期、未授权 */
val DangerLight = Color(0xFFDC2626)

/** 危险操作底色（底部固定删除按钮、二次确认面板） */
val DangerContainerDark = Color(0xFF3A1416)
val DangerContainerLight = Color(0xFFFDE8E8)

/** 危险操作前景文字 */
val OnDangerContainerDark = Color(0xFFFCA5A5)
val OnDangerContainerLight = Color(0xFFB91C1C)

/** 警告 / 临期 */
val WarningDark = Color(0xFFFBBF24)
val WarningLight = Color(0xFFD97706)

// ---------------------------------------------------------------------------
// 「完成」专用的独立绿色（刻意不并入统一强调色）
// ---------------------------------------------------------------------------

/**
 * 勾选框 / 勾选按钮 / 完成高亮的专用绿。
 * 「完成」是特殊交互状态，用独立绿色比跟强调色统一更容易识别；
 * 深浅模式共用同一个绿，保证勾选态在两种主题下观感一致。
 */
val CheckGreen = Color(0xFF22C55E)

/** 绿色勾选框内对勾的颜色（深一档的墨绿，保证在绿底上的对比度） */
val OnCheckGreen = Color(0xFF062814)

// ---------------------------------------------------------------------------
// 全屏图片查看器专用色（固定深色，不随主题切换）
// ---------------------------------------------------------------------------

/**
 * 图片查看页始终是沉浸式深色场景（隐藏系统栏、背景近黑），
 * 因此单独定义一组固定色，避免浅色主题下出现"浅底白字"的错配。
 */
val ViewerBackground = Color(0xFF0A0A0A)
val ViewerSurface = Color(0xFF141414)
val ViewerTextPrimary = Color(0xFFFAFAFA)
val ViewerTextSecondary = Color(0xFF71717A)

/** 查看页上的强调色（加载指示器）：深底上使用亮一档的强调色 */
val ViewerAccent = AccentDark

// ---------------------------------------------------------------------------
// 应用自定义色板
// ---------------------------------------------------------------------------

/**
 * 应用自定义色板：背景 / 卡片 / 主文字 / 次文字 / 辅助文字 / 描边 / 按下态 / 内嵌底。
 * 字段名保持向后兼容（原有 background、card、textPrimary、textSecondary 不变）。
 */
@Immutable
data class XixiColors(
    val background: Color,
    val card: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val isDark: Boolean,
    /** 二级卡片 / 弹层底色 */
    val cardElevated: Color,
    /** 辅助信息文字（更小更淡） */
    val textTertiary: Color,
    /** 细描边 / 分隔线 */
    val outline: Color,
    /** 按下态高亮覆盖 */
    val pressed: Color,
    /** 输入框 / 内嵌凹槽底色 */
    val sunken: Color,
    /** 统一强调色（当前深浅模式下的那一档） */
    val accent: Color,
    /** 强调色上的前景色 */
    val onAccent: Color,
    /** 强调色柔和底 */
    val accentSoft: Color,
    /** 强调色描边 */
    val accentOutline: Color,
    /** 成功语义色 */
    val success: Color,
    /** 危险语义色 */
    val danger: Color,
    /** 危险底色 */
    val dangerContainer: Color,
    /** 危险底上的前景文字 */
    val onDangerContainer: Color,
    /** 警告语义色 */
    val warning: Color,
    /** 「完成」专用绿（不并入强调色） */
    val check: Color,
    /** 绿底上的对勾颜色 */
    val onCheck: Color
)

/** 深色模式色板 */
val XixiColorsDark = XixiColors(
    background = DarkBackgroundN2,
    card = DarkSurfaceN1,
    textPrimary = DarkTextPrimary,
    textSecondary = DarkTextSecondary,
    isDark = true,
    cardElevated = DarkSurfaceN2,
    textTertiary = DarkTextTertiary,
    outline = DarkOutline,
    pressed = DarkPressedOverlay,
    sunken = DarkSurfaceSunken,
    accent = AccentDark,
    onAccent = OnAccentDark,
    accentSoft = AccentSoftDark,
    accentOutline = AccentOutlineDark,
    success = SuccessDark,
    danger = DangerDark,
    dangerContainer = DangerContainerDark,
    onDangerContainer = OnDangerContainerDark,
    warning = WarningDark,
    check = CheckGreen,
    onCheck = OnCheckGreen
)

/** 浅色模式色板 */
val XixiColorsLight = XixiColors(
    background = LightBackgroundN2,
    card = LightSurfaceN1,
    textPrimary = LightTextPrimary,
    textSecondary = LightTextSecondary,
    isDark = false,
    cardElevated = LightSurfaceN2,
    textTertiary = LightTextTertiary,
    outline = LightOutline,
    pressed = LightPressedOverlay,
    sunken = LightSurfaceSunken,
    accent = AccentLight,
    onAccent = OnAccentLight,
    accentSoft = AccentSoftLight,
    accentOutline = AccentOutlineLight,
    success = SuccessLight,
    danger = DangerLight,
    dangerContainer = DangerContainerLight,
    onDangerContainer = OnDangerContainerLight,
    warning = WarningLight,
    check = CheckGreen,
    onCheck = OnCheckGreen
)

// ---------------------------------------------------------------------------
// 强调色板
// ---------------------------------------------------------------------------

/**
 * 强调色随主题模式的取值集合。
 * 顺序固定为：跟随系统 / 深色 / 浅色，与 [ThemeMode] 的声明顺序一致，可直接用 ordinal 下标。
 */
@Immutable
data class AccentPalette(
    val followSystem: Color,
    val dark: Color,
    val light: Color
) {
    /** 当前模式下的强调色 */
    fun of(isDark: Boolean): Color = if (isDark) dark else light
}

val XixiAccentPalette = AccentPalette(
    followSystem = QuadrantNeither,
    dark = AccentDark,
    light = AccentLight
)
