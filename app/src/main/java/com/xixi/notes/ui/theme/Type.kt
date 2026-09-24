package com.xixi.notes.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 排版 / 圆角 / 间距 / 阴影 token（第一步：只定义 token，不改任何功能逻辑）。
 *
 * 设计规则：
 * 1. 排版层级分明：大标题粗、正文正常、辅助信息小且淡、统计数字大且粗。
 * 2. 主圆角 20dp，大卡片 24dp，弹层 28dp，小元件 10~14dp。
 * 3. 留白充足：卡片内边距 20dp，区块间距 16dp，页面边距 20dp。
 * 4. 阴影柔和：卡片默认 2dp 低透明度阴影，禁止重投影。
 */

/** 中文 UI 统一字间距 */
private const val LETTER_SPACING = 0.3f

/** 中文 UI 统一行高倍数（配合 LineHeightStyle 居中裁切，避免上下留白不均） */
private const val LINE_HEIGHT_MULTIPLIER = 1.35f

/** 居中裁切策略：中文字形上下留白一致，视觉更稳 */
private val CenteredLineHeightStyle = LineHeightStyle(
    alignment = LineHeightStyle.Alignment.Center,
    trim = LineHeightStyle.Trim.None
)

/**
 * 给任意文本样式套用中文 UI 统一处理：
 * 字间距 / 行高倍数 / 关闭字体自身额外行距 / 行高居中裁切。
 */
private fun TextStyle.cn(): TextStyle = copy(
    letterSpacing = LETTER_SPACING.sp,
    lineHeight = (fontSize.value * LINE_HEIGHT_MULTIPLIER).sp,
    platformStyle = PlatformTextStyle(includeFontPadding = false),
    lineHeightStyle = CenteredLineHeightStyle
)

private val Base = Typography()

/**
 * 应用排版：
 * - display / headline：统计页核心数字、引导页大标题（粗）
 * - title：页面标题、卡片标题（加粗，字重递减）
 * - body：正文（正常字重）
 * - label：按钮、标签、辅助信息（小 / 淡）
 */
val XixiTypography = Typography(
    displayLarge = Base.displayLarge.cn().copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp),
    displayMedium = Base.displayMedium.cn().copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp),
    displaySmall = Base.displaySmall.cn().copy(fontWeight = FontWeight.Bold),
    headlineLarge = Base.headlineLarge.cn().copy(fontWeight = FontWeight.Bold),
    headlineMedium = Base.headlineMedium.cn().copy(fontWeight = FontWeight.Bold),
    headlineSmall = Base.headlineSmall.cn().copy(fontWeight = FontWeight.SemiBold),
    titleLarge = Base.titleLarge.cn().copy(fontWeight = FontWeight.Bold),
    titleMedium = Base.titleMedium.cn().copy(fontWeight = FontWeight.SemiBold),
    titleSmall = Base.titleSmall.cn().copy(fontWeight = FontWeight.SemiBold),
    bodyLarge = Base.bodyLarge.cn().copy(fontWeight = FontWeight.Normal),
    bodyMedium = Base.bodyMedium.cn().copy(fontWeight = FontWeight.Normal),
    bodySmall = Base.bodySmall.cn().copy(fontWeight = FontWeight.Normal),
    labelLarge = Base.labelLarge.cn().copy(fontWeight = FontWeight.SemiBold),
    labelMedium = Base.labelMedium.cn().copy(fontWeight = FontWeight.Medium),
    labelSmall = Base.labelSmall.cn().copy(fontWeight = FontWeight.Medium)
)

/**
 * 额外文本样式。
 *
 * - 统计数字：大且粗，负字距更紧凑
 * - 通用字阶 token（pageTitle / inputTitle / subtitle / rowTitle / body /
 *   bodyReading / caption / minor / micro）：全应用唯一的字号来源，
 *   页面不得再写 `fontSize = X.sp` 这类魔法值；字重与颜色在调用处按语义覆盖
 */
object XixiTextStyles {
    val statNumber = TextStyle(
        fontSize = 40.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.5).sp,
        lineHeight = 44.sp,
        platformStyle = PlatformTextStyle(includeFontPadding = false),
        lineHeightStyle = CenteredLineHeightStyle
    )

    /** 小一号的统计数字（网格卡片内） */
    val statNumberSmall = TextStyle(
        fontSize = 28.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.5).sp,
        lineHeight = 32.sp,
        platformStyle = PlatformTextStyle(includeFontPadding = false),
        lineHeightStyle = CenteredLineHeightStyle
    )

    /** 行内统计数字（单行卡片右侧） */
    val statNumberLine = TextStyle(
        fontSize = 24.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.5).sp,
        lineHeight = 28.sp,
        platformStyle = PlatformTextStyle(includeFontPadding = false),
        lineHeightStyle = CenteredLineHeightStyle
    )

    /** 页面大标题（26sp Bold）：统计 / 设置 / 归档顶栏、引导页标题 */
    val pageTitle = TextStyle(
        fontSize = 26.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = LETTER_SPACING.sp,
        lineHeight = 35.sp,
        platformStyle = PlatformTextStyle(includeFontPadding = false),
        lineHeightStyle = CenteredLineHeightStyle
    )

    /** 大号输入文本（17sp）：编辑页标题输入框与占位符 */
    val inputTitle = TextStyle(
        fontSize = 17.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = LETTER_SPACING.sp,
        lineHeight = 23.sp,
        platformStyle = PlatformTextStyle(includeFontPadding = false),
        lineHeightStyle = CenteredLineHeightStyle
    )

    /** 强调小标题（16sp SemiBold）：编辑页顶栏标题、空状态标题 */
    val subtitle = TextStyle(
        fontSize = 16.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = LETTER_SPACING.sp,
        lineHeight = 22.sp,
        platformStyle = PlatformTextStyle(includeFontPadding = false),
        lineHeightStyle = CenteredLineHeightStyle
    )

    /** 行标题（15sp）：任务标题、设置行标题、按钮文字（字重按调用处覆盖） */
    val rowTitle = TextStyle(
        fontSize = 15.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = LETTER_SPACING.sp,
        lineHeight = 20.sp,
        platformStyle = PlatformTextStyle(includeFontPadding = false),
        lineHeightStyle = CenteredLineHeightStyle
    )

    /** 正文（14sp）：菜单项、说明文字（字重按调用处覆盖） */
    val body = TextStyle(
        fontSize = 14.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = LETTER_SPACING.sp,
        lineHeight = 19.sp,
        platformStyle = PlatformTextStyle(includeFontPadding = false),
        lineHeightStyle = CenteredLineHeightStyle
    )

    /** 多行阅读文本（14sp，行高更松）：编辑页描述、引导页正文 */
    val bodyReading = TextStyle(
        fontSize = 14.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = LETTER_SPACING.sp,
        lineHeight = 23.sp,
        platformStyle = PlatformTextStyle(includeFontPadding = false),
        lineHeightStyle = CenteredLineHeightStyle
    )

    /** 辅助说明（13sp）：提示条、次要信息、小按钮 */
    val caption = TextStyle(
        fontSize = 13.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = LETTER_SPACING.sp,
        lineHeight = 18.sp,
        platformStyle = PlatformTextStyle(includeFontPadding = false),
        lineHeightStyle = CenteredLineHeightStyle
    )

    /** 次要小字（12sp）：时间戳、状态值、徽标说明（字重按调用处覆盖） */
    val minor = TextStyle(
        fontSize = 12.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = LETTER_SPACING.sp,
        lineHeight = 16.sp,
        platformStyle = PlatformTextStyle(includeFontPadding = false),
        lineHeightStyle = CenteredLineHeightStyle
    )

    /** 微标签（11sp）：徽章、扇区标签、图片角标文字（字重按调用处覆盖） */
    val micro = TextStyle(
        fontSize = 11.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = LETTER_SPACING.sp,
        lineHeight = 15.sp,
        platformStyle = PlatformTextStyle(includeFontPadding = false),
        lineHeightStyle = CenteredLineHeightStyle
    )
}

// ---------------------------------------------------------------------------
// 圆角
// ---------------------------------------------------------------------------

/** 圆角 token：主圆角 20dp，大卡片 24dp，弹层 28dp */
object XixiRadius {
    /** 小标签 / 勾选框等微小元件 */
    val tiny = 6.dp

    /** 缩略图 / 内嵌小面板 */
    val thumbnail = 10.dp

    /** 小卡片 / 输入框 / 列表内嵌块 */
    val small = 12.dp

    /** 条目卡片 / 中号容器 */
    val medium = 16.dp

    /** 主卡片圆角（默认） */
    val card = 20.dp

    /** 大卡片圆角（统计页顶部大卡、编辑页分组大卡） */
    val largeCard = 24.dp

    /** 弹层 / 底部面板 */
    val sheet = 28.dp

    /** 胶囊（chips / 标签 / 提醒分钟数） */
    val pill = 999.dp
}

/**
 * Material3 Shapes 映射：
 * - extraSmall 4dp：菜单 / 下拉
 * - small 12dp：小卡片 / 输入框
 * - medium 20dp：主卡片（Material3 卡片默认取 medium）
 * - large 24dp：大卡片 / 分组卡片
 * - extraLarge 28dp：底部面板
 */
val XixiShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(XixiRadius.small),
    medium = RoundedCornerShape(XixiRadius.card),
    large = RoundedCornerShape(XixiRadius.largeCard),
    extraLarge = RoundedCornerShape(XixiRadius.sheet)
)

/** 便捷形状：主卡片（20dp） */
val XixiCardShape = RoundedCornerShape(XixiRadius.card)

/** 便捷形状：大卡片（24dp） */
val XixiLargeCardShape = RoundedCornerShape(XixiRadius.largeCard)

/** 便捷形状：中号条目卡片（16dp） */
val XixiItemShape = RoundedCornerShape(XixiRadius.medium)

/** 便捷形状：小卡片 / 输入框（12dp） */
val XixiSmallShape = RoundedCornerShape(XixiRadius.small)

/** 便捷形状：缩略图（10dp） */
val XixiThumbnailShape = RoundedCornerShape(XixiRadius.thumbnail)

/** 便捷形状：胶囊 */
val XixiPillShape = RoundedCornerShape(percent = 50)

/** 便捷形状：底部面板（28dp） */
val XixiSheetShape = RoundedCornerShape(XixiRadius.sheet)

/**
 * 语义形状集合：供组件通过 `XixiTheme.shapes.xxx` 取用，
 * 避免各页面继续硬编码 `RoundedCornerShape(12.dp)` 这类魔法值。
 */
@Immutable
data class XixiShapesToken(
    /** 缩略图 / 内嵌小面板（10dp） */
    val thumbnail: Shape = XixiThumbnailShape,
    /** 输入框 / 小容器（12dp） */
    val small: Shape = XixiSmallShape,
    /** 条目卡片 / 中号容器（16dp） */
    val item: Shape = XixiItemShape,
    /** 主卡片（20dp） */
    val card: Shape = XixiCardShape,
    /** 大卡片 / 分组卡片（24dp） */
    val largeCard: Shape = XixiLargeCardShape,
    /** 弹层 / 底部面板（28dp） */
    val sheet: Shape = XixiSheetShape,
    /** 胶囊 */
    val pill: Shape = XixiPillShape,
    /** Dock 与底部悬浮条（24dp，上圆角更明显） */
    val dock: Shape = XixiLargeCardShape
)

/** 唯一形状集合实例（形状不随深浅色变化） */
val XixiShapesSet = XixiShapesToken()

// ---------------------------------------------------------------------------
// 间距
// ---------------------------------------------------------------------------

/**
 * 间距 token（4 的倍数，保证节奏统一）。
 * 页面横向边距用 [Spacing.screenH]，卡片内边距用 [Spacing.cardPadding]。
 */
object Spacing {
    /** 2dp：极细分隔 */
    val hairline = 2.dp

    /** 4dp：图标与文字的最小间隙 */
    val xs = 4.dp

    /** 8dp：紧密元素之间 */
    val sm = 8.dp

    /** 12dp：卡片内部小节之间 */
    val md = 12.dp

    /** 16dp：卡片之间、区块之间 */
    val lg = 16.dp

    /** 20dp：页面横向边距 / 卡片内边距 */
    val xl = 20.dp

    /** 24dp：大区块之间 */
    val xxl = 24.dp

    /** 32dp：页面顶部大留白 */
    val huge = 32.dp

    /** 页面横向边距 */
    val screenH = 20.dp

    /** 卡片内边距（留白充足） */
    val cardPadding = 20.dp

    /** 大卡片内边距 */
    val largeCardPadding = 24.dp
}

// ---------------------------------------------------------------------------
// 阴影
// ---------------------------------------------------------------------------

/**
 * 阴影 token：柔和、低透明度，深色模式下更弱。
 * 使用 Material3 [androidx.compose.material3.CardDefaults] 时传入对应 elevation 即可。
 */
object XixiElevation {
    /** 普通条目卡片（2dp） */
    val card = 2.dp

    /** 主卡片 / 统计大卡（3dp） */
    val cardStrong = 3.dp

    /** FAB 展开态（8dp） */
    val fab = 8.dp
}
