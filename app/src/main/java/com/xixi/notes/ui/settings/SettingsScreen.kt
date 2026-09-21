package com.xixi.notes.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xixi.notes.BuildConfig
import com.xixi.notes.R
import com.xixi.notes.data.preferences.NotificationActionMode
import com.xixi.notes.di.LocalAppContainer
import com.xixi.notes.ui.board.AssigneeMode
import com.xixi.notes.ui.board.CompletedStyle
import com.xixi.notes.ui.components.InlineConfirm
import com.xixi.notes.ui.components.clickableNoRipple
import com.xixi.notes.ui.main.canScheduleExactAlarms
import com.xixi.notes.ui.main.hasNotificationPermission
import com.xixi.notes.ui.main.openExactAlarmSettings
import com.xixi.notes.ui.main.openNotificationSettings
import com.xixi.notes.ui.theme.Spacing
import com.xixi.notes.ui.theme.ThemeMode
import com.xixi.notes.ui.theme.XixiElevation
import com.xixi.notes.ui.theme.XixiItemShape
import com.xixi.notes.ui.theme.XixiThumbnailShape
import com.xixi.notes.ui.theme.XixiTheme
import com.xixi.notes.ui.util.GentleEasing

/** 卡片间距 */
private val CardGap = Spacing.md

/** 主题模式色点（跟随系统 / 深色 / 浅色）——用于强调色板三档色 */
@Composable
private fun themeModeDotColor(mode: ThemeMode): Color = when (mode) {
    ThemeMode.FOLLOW_SYSTEM -> XixiTheme.accent.followSystem
    ThemeMode.DARK -> XixiTheme.accent.dark
    ThemeMode.LIGHT -> XixiTheme.accent.light
}

/**
 * 设置页。
 *
 * 布局约定：
 * - 每个设置项独立成一张白色圆角卡片（16dp 圆角 + 柔和阴影 + 20dp 内边距），
 *   同类设置不再挤在同一张卡里，靠"分组标题 + 卡片间距"建立层级
 * - 提醒分钟数：横向滚动 + 选中项高亮（强调色实心胶囊）
 * - 主题切换：点击卡片头部原地展开 3 个选项（不跳页、不弹窗）
 *
 * 数据与写入口径全部来自 SettingsViewModel，本文件只负责展示与交互转发。
 */
@Composable
fun SettingsScreen(
    onOpenOnboarding: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModel.Factory(LocalAppContainer.current.preferences)
    )
) {
    val prefs by viewModel.prefs.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var notificationMenuOpen by remember { mutableStateOf(false) }
    var themeMenuOpen by remember { mutableStateOf(false) }
    var onboardingConfirm by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(XixiTheme.colors.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
        ) {
            // 顶部栏：只显示标题
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .padding(horizontal = Spacing.screenH),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = stringResource(R.string.settings_title),
                    color = XixiTheme.colors.textPrimary,
                    fontSize = 26.sp,
                    letterSpacing = 0.3.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Spacing.screenH)
                    .padding(bottom = 120.dp)
            ) {
                // ---------------------------------------------------- 显示
                SectionTitle(stringResource(R.string.settings_section_display))

                SettingCard {
                    SettingLabel(title = stringResource(R.string.settings_completed_style))
                    Spacer(modifier = Modifier.height(Spacing.md))
                    SegmentedOptions(
                        options = listOf(
                            CompletedStyle.IN_PLACE to stringResource(R.string.settings_completed_in_place),
                            CompletedStyle.HIDDEN to stringResource(R.string.settings_completed_hidden)
                        ),
                        selected = prefs.completedStyle,
                        onSelect = viewModel::setCompletedStyle
                    )
                }

                Spacer(modifier = Modifier.height(CardGap))

                SettingCard {
                    SettingLabel(title = stringResource(R.string.settings_assignee_mode))
                    Spacer(modifier = Modifier.height(Spacing.md))
                    SegmentedOptions(
                        options = listOf(
                            AssigneeMode.ASSIGNEE_FIRST to stringResource(R.string.settings_assignee_first),
                            AssigneeMode.VISUAL_ONLY to stringResource(R.string.settings_assignee_visual),
                            AssigneeMode.SEPARATED to stringResource(R.string.settings_assignee_separated)
                        ),
                        selected = prefs.assigneeMode,
                        onSelect = viewModel::setAssigneeMode
                    )
                }

                SectionGap()

                // ---------------------------------------------------- 默认值
                SectionTitle(stringResource(R.string.settings_section_defaults))

                SettingCard {
                    SettingLabel(title = stringResource(R.string.settings_default_reminder))
                    Spacer(modifier = Modifier.height(Spacing.md))
                    // 横向可滚动，每项固定 76dp × 40dp，不换行；选中项为强调色实心胶囊
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ReminderMinuteOptions.forEach { minutes ->
                            MinuteChip(
                                minutes = minutes,
                                selected = prefs.defaultReminderMinutes == minutes,
                                onClick = { viewModel.setDefaultReminderMinutes(minutes) }
                            )
                        }
                    }
                }

                SectionGap()

                // ------------------------------------------------------ 主题
                SectionTitle(stringResource(R.string.settings_section_theme))

                ThemeModeCard(
                    current = prefs.themeMode,
                    open = themeMenuOpen,
                    onToggle = { themeMenuOpen = !themeMenuOpen },
                    onSelect = { mode ->
                        viewModel.setThemeMode(mode)
                        themeMenuOpen = false
                    }
                )

                SectionGap()

                // ------------------------------------------------------ 通知
                SectionTitle(stringResource(R.string.settings_section_notification))

                SettingCard(modifier = Modifier.animateContentSize(tween(280, easing = GentleEasing))) {
                    // 通知点击行为：点击卡片头部原地展开 3 选项
                    ExpandableHeader(
                        title = stringResource(R.string.settings_notification_action),
                        value = stringResource(
                            when (prefs.notificationAction) {
                                NotificationActionMode.OPEN_EDIT ->
                                    R.string.settings_notification_open_edit
                                NotificationActionMode.OPEN_HOME ->
                                    R.string.settings_notification_open_home
                                NotificationActionMode.HIGHLIGHT ->
                                    R.string.settings_notification_highlight
                            }
                        ),
                        open = notificationMenuOpen,
                        onToggle = { notificationMenuOpen = !notificationMenuOpen }
                    )

                    AnimatedVisibility(
                        visible = notificationMenuOpen,
                        enter = fadeIn(tween(180, easing = GentleEasing)) +
                            expandVertically(tween(260, easing = GentleEasing)),
                        exit = fadeOut(tween(140, easing = GentleEasing)) +
                            shrinkVertically(tween(220, easing = GentleEasing))
                    ) {
                        Column(modifier = Modifier.padding(top = Spacing.sm)) {
                            NotificationActionMode.entries.forEach { mode ->
                                val selected = prefs.notificationAction == mode
                                ExpandOptionRow(
                                    label = stringResource(
                                        when (mode) {
                                            NotificationActionMode.OPEN_EDIT ->
                                                R.string.settings_notification_open_edit
                                            NotificationActionMode.OPEN_HOME ->
                                                R.string.settings_notification_open_home
                                            NotificationActionMode.HIGHLIGHT ->
                                                R.string.settings_notification_highlight
                                        }
                                    ),
                                    accent = XixiTheme.colors.accent,
                                    selected = selected,
                                    onClick = {
                                        viewModel.setNotificationAction(mode)
                                        notificationMenuOpen = false
                                    }
                                )
                            }
                        }
                    }
                }

                SectionGap()

                // ------------------------------------------------------ 权限（每项独立卡片）
                SectionTitle(stringResource(R.string.settings_section_permission))

                SettingCard {
                    PermissionRow(
                        title = stringResource(R.string.settings_notification_permission),
                        granted = hasNotificationPermission(context),
                        onOpen = { openNotificationSettings(context) }
                    )
                }

                Spacer(modifier = Modifier.height(CardGap))

                SettingCard {
                    PermissionRow(
                        title = stringResource(R.string.settings_exact_alarm_permission),
                        granted = canScheduleExactAlarms(context),
                        onOpen = { openExactAlarmSettings(context) }
                    )
                }

                SectionGap()

                // ------------------------------------------------------ 关于
                SectionTitle(stringResource(R.string.settings_section_about))

                SettingCard {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickableNoRipple { onboardingConfirm = true }
                            .padding(vertical = Spacing.xs),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.settings_onboarding),
                            color = XixiTheme.colors.textPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 0.3.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(Spacing.sm))
                    Text(
                        text = stringResource(R.string.settings_version, BuildConfig.VERSION_NAME),
                        color = XixiTheme.colors.textTertiary,
                        fontSize = 12.sp,
                        letterSpacing = 0.3.sp
                    )
                }
            }
        }

        // 引导页确认（避免误触）
        InlineConfirm(
            visible = onboardingConfirm,
            title = stringResource(R.string.settings_onboarding),
            message = stringResource(R.string.onboarding_page1_body),
            confirmLabel = stringResource(R.string.onboarding_start),
            cancelLabel = stringResource(R.string.action_cancel),
            onConfirm = {
                onboardingConfirm = false
                onOpenOnboarding()
            },
            onCancel = { onboardingConfirm = false },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(Spacing.lg)
                .padding(bottom = 72.dp)
        )
    }
}

/** 分组之间的间距 */
@Composable
private fun SectionGap() {
    Spacer(modifier = Modifier.height(Spacing.xxl))
}

/** 分组标题：小号、次要色、前缀留白 */
@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        color = XixiTheme.colors.textSecondary,
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.3.sp,
        modifier = Modifier.padding(start = Spacing.xs, bottom = Spacing.sm)
    )
}

/** 设置卡片：独立白色圆角卡片（16dp 圆角 + 柔和阴影 + 20dp 内边距） */
@Composable
private fun SettingCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(XixiElevation.card, XixiItemShape)
            .clip(XixiItemShape)
            .background(XixiTheme.colors.card)
            .padding(Spacing.cardPadding)
    ) {
        content()
    }
}

/** 卡片内的设置项标题（可选说明文字） */
@Composable
private fun SettingLabel(title: String, description: String? = null) {
    Column {
        Text(
            text = title,
            color = XixiTheme.colors.textPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.3.sp
        )
        if (description != null) {
            Spacer(modifier = Modifier.height(Spacing.xs))
            Text(
                text = description,
                color = XixiTheme.colors.textSecondary,
                fontSize = 12.sp,
                letterSpacing = 0.3.sp
            )
        }
    }
}

/**
 * 可原地展开的卡片头部：标题 + 当前值 + 右侧指示圆点。
 * 指示圆点在展开时用强调色，收起时为次要色。
 */
@Composable
private fun ExpandableHeader(
    title: String,
    value: String,
    open: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickableNoRipple(onClick = onToggle)
            .padding(vertical = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = XixiTheme.colors.textPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.3.sp
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = value,
                color = XixiTheme.colors.accent,
                fontSize = 13.sp,
                letterSpacing = 0.3.sp
            )
        }
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(
                    if (open) XixiTheme.colors.accent else XixiTheme.colors.outline
                )
        )
    }
}

/** 主题模式卡片：头部显示当前模式 + 模式色点，点击原地展开 3 个选项 */
@Composable
private fun ThemeModeCard(
    current: ThemeMode,
    open: Boolean,
    onToggle: () -> Unit,
    onSelect: (ThemeMode) -> Unit
) {
    SettingCard(modifier = Modifier.animateContentSize(tween(280, easing = GentleEasing))) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickableNoRipple(onClick = onToggle)
                .padding(vertical = Spacing.xs),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.settings_theme_mode),
                    color = XixiTheme.colors.textPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.3.sp
                )
                Spacer(modifier = Modifier.height(3.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // 当前模式色点（强调色板三档色）
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(themeModeDotColor(current))
                    )
                    Spacer(modifier = Modifier.width(Spacing.sm))
                    Text(
                        text = stringResource(
                            when (current) {
                                ThemeMode.FOLLOW_SYSTEM -> R.string.theme_follow_system
                                ThemeMode.DARK -> R.string.theme_dark
                                ThemeMode.LIGHT -> R.string.theme_light
                            }
                        ),
                        color = XixiTheme.colors.accent,
                        fontSize = 13.sp,
                        letterSpacing = 0.3.sp
                    )
                }
            }
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(
                        if (open) XixiTheme.colors.accent else XixiTheme.colors.outline
                    )
            )
        }

        AnimatedVisibility(
            visible = open,
            enter = fadeIn(tween(180, easing = GentleEasing)) +
                expandVertically(tween(260, easing = GentleEasing)),
            exit = fadeOut(tween(140, easing = GentleEasing)) +
                shrinkVertically(tween(220, easing = GentleEasing))
        ) {
            Column(modifier = Modifier.padding(top = Spacing.sm)) {
                ThemeMode.entries.forEach { mode ->
                    ExpandOptionRow(
                        label = stringResource(
                            when (mode) {
                                ThemeMode.FOLLOW_SYSTEM -> R.string.theme_follow_system
                                ThemeMode.DARK -> R.string.theme_dark
                                ThemeMode.LIGHT -> R.string.theme_light
                            }
                        ),
                        accent = themeModeDotColor(mode),
                        selected = current == mode,
                        onClick = { onSelect(mode) }
                    )
                }
            }
        }
    }
}

/** 展开后的单个选项行：左侧色条 + 文字（选中加粗并用强调色） */
@Composable
private fun ExpandOptionRow(
    label: String,
    accent: Color,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(XixiThumbnailShape)
            .clickableNoRipple(onClick = onClick)
            .padding(horizontal = Spacing.md, vertical = Spacing.md),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 选中项左侧彩色竖条
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(18.dp)
                .clip(RoundedCornerShape(percent = 50))
                .background(if (selected) accent else Color.Transparent)
        )
        Spacer(modifier = Modifier.width(Spacing.md))
        Text(
            text = label,
            color = if (selected) XixiTheme.colors.textPrimary
            else XixiTheme.colors.textSecondary,
            fontSize = 14.sp,
            letterSpacing = 0.3.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

/** 分段选择：轨道为内嵌底色，选中段为卡片色 */
@Composable
private fun <T> SegmentedOptions(
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(XixiTheme.shapes.small)
            .background(XixiTheme.colors.sunken)
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        options.forEach { (value, label) ->
            val isSelected = value == selected
            val background by animateColorAsState(
                targetValue = if (isSelected) XixiTheme.colors.card
                else Color.Transparent,
                animationSpec = tween(durationMillis = 220),
                label = "segment_bg"
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(XixiThumbnailShape)
                    .background(background)
                    .clickableNoRipple { onSelect(value) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    color = if (isSelected) XixiTheme.colors.textPrimary
                    else XixiTheme.colors.textSecondary,
                    fontSize = 12.sp,
                    letterSpacing = 0.3.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                )
            }
        }
    }
}

/** 提醒分钟数胶囊：固定 76dp × 40dp，选中为强调色实心 + 高对比文字 */
@Composable
private fun MinuteChip(
    minutes: Int,
    selected: Boolean,
    onClick: () -> Unit
) {
    val background by animateColorAsState(
        targetValue = if (selected) XixiTheme.colors.accent else XixiTheme.colors.sunken,
        animationSpec = tween(durationMillis = 220),
        label = "minute_bg"
    )
    Box(
        modifier = Modifier
            .size(width = 76.dp, height = 40.dp)
            .clip(XixiTheme.shapes.pill)
            .background(background)
            .clickableNoRipple(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = stringResource(R.string.settings_default_reminder_value, minutes),
            color = if (selected) XixiTheme.colors.onAccent else XixiTheme.colors.textSecondary,
            fontSize = 12.sp,
            letterSpacing = 0.3.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1
        )
    }
}

/** 权限状态行：已开启 / 未开启 + 「去开启」 */
@Composable
private fun PermissionRow(
    title: String,
    granted: Boolean,
    onOpen: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = XixiTheme.colors.textPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.3.sp
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = stringResource(
                    if (granted) R.string.settings_permission_granted
                    else R.string.settings_permission_denied
                ),
                // 已开启用成功绿，未开启用危险色
                color = if (granted) XixiTheme.colors.success else XixiTheme.colors.danger,
                fontSize = 12.sp,
                letterSpacing = 0.3.sp
            )
        }
        if (!granted) {
            Box(
                modifier = Modifier
                    .clip(XixiTheme.shapes.small)
                    .background(XixiTheme.colors.accent)
                    .clickableNoRipple(onClick = onOpen)
                    .padding(horizontal = Spacing.lg, vertical = Spacing.sm)
            ) {
                Text(
                    text = stringResource(R.string.settings_permission_open),
                    color = XixiTheme.colors.onAccent,
                    fontSize = 12.sp,
                    letterSpacing = 0.3.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
