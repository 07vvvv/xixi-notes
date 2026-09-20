package com.xixi.notes.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** 中文 UI 统一字间距 0.5sp */
private const val LETTER_SPACING = 0.5f

/** 给任意文本样式套用中文 UI 字间距 */
private fun TextStyle.cn(): TextStyle = copy(letterSpacing = LETTER_SPACING.sp)

private val Base = Typography()

val XixiTypography = Typography(
    displayLarge = Base.displayLarge.cn(),
    displayMedium = Base.displayMedium.cn(),
    displaySmall = Base.displaySmall.cn(),
    headlineLarge = Base.headlineLarge.cn(),
    headlineMedium = Base.headlineMedium.cn(),
    headlineSmall = Base.headlineSmall.cn(),
    titleLarge = Base.titleLarge.cn().copy(fontWeight = FontWeight.SemiBold),
    titleMedium = Base.titleMedium.cn().copy(fontWeight = FontWeight.SemiBold),
    titleSmall = Base.titleSmall.cn().copy(fontWeight = FontWeight.Medium),
    bodyLarge = Base.bodyLarge.cn(),
    bodyMedium = Base.bodyMedium.cn(),
    bodySmall = Base.bodySmall.cn(),
    labelLarge = Base.labelLarge.cn().copy(fontWeight = FontWeight.Medium),
    labelMedium = Base.labelMedium.cn(),
    labelSmall = Base.labelSmall.cn()
)

/** 卡片圆角 16dp，弹层 20dp，超大弹层 26dp */
val XixiShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(26.dp)
)

/** 统一的卡片内边距 */
val ContentPadding = 16.dp
