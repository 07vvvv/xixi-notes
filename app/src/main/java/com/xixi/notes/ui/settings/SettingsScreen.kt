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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import com.xixi.notes.ui.theme.ThemeMode
import com.xixi.notes.ui.theme.XixiTheme
import com.xixi.notes.ui.util.GentleEasing

/**
 * 设置页。
 *
 * 已完成显示方式 / assignee 排序 / 全局默认提醒分钟数 / 动态取色开关 /
 * 通知点击行为（原地展开 3 选项）/ 引导页入口。
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
    var notificationMenuOpen by remember { mutableStateOf(false) }
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
                    .height(56.dp)
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = stringResource(R.string.settings_title),
                    color = XixiTheme.colors.textPrimary,
                    fontSize = 20.sp,
                    letterSpacing = 0.5.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 120.dp)
            ) {
                // ---------------------------------------------------- 显示
                SectionTitle(stringResource(R.string.settings_section_display))

                SettingCard {
                    SettingLabel(
                        title = stringResource(R.string.settings_completed_style),
                        description = null
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    SegmentedOptions(
                        options = listOf(
                            CompletedStyle.IN_PLACE to stringResource(R.string.settings_completed_in_place),
                            CompletedStyle.HIDDEN to stringResource(R.string.settings_completed_hidden)
                        ),
                        selected = prefs.completedStyle,
                        onSelect = viewModel::setCompletedStyle
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                SettingCard {
                    SettingLabel(
                        title = stringResource(R.string.settings_assignee_mode),
                        description = null
                    )
                    Spacer(modifier = Modifier.height(10.dp))
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

                Spacer(modifier = Modifier.height(20.dp))

                // ---------------------------------------------------- 默认值
                SectionTitle(stringResource(R.string.settings_section_defaults))

                SettingCard {
                    SettingLabel(
                        title = stringResource(R.string.settings_default_reminder),
                        description = null
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ReminderMinuteOptions.forEach { minutes ->
                            MinuteChip(
                                minutes = minutes,
                                selected = prefs.defaultReminderMinutes == minutes,
                                onClick = { viewModel.setDefaultReminderMinutes(minutes) }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // ------------------------------------------------------ 主题
                SectionTitle(stringResource(R.string.settings_section_theme))

                SettingCard {
                    SettingLabel(
                        title = stringResource(R.string.cd_theme_mode),
                        description = stringResource(
                            when (prefs.themeMode) {
                                ThemeMode.FOLLOW_SYSTEM -> R.string.theme_follow_system
                                ThemeMode.DARK -> R.string.theme_dark
                                ThemeMode.LIGHT -> R.string.theme_light
                            }
                        )
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    SegmentedOptions(
                        options = listOf(
                            ThemeMode.FOLLOW_SYSTEM to stringResource(R.string.theme_follow_system),
                            ThemeMode.DARK to stringResource(R.string.theme_dark),
                            ThemeMode.LIGHT to stringResource(R.string.theme_light)
                        ),
                        selected = prefs.themeMode,
                        onSelect = viewModel::setThemeMode
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    SwitchRow(
                        title = stringResource(R.string.settings_dynamic_color),
                        description = stringResource(R.string.settings_dynamic_color_desc),
                        checked = prefs.dynamicColor,
                        onCheckedChange = viewModel::setDynamicColor
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // ------------------------------------------------------ 通知
                SectionTitle(stringResource(R.string.settings_section_notification))

                SettingCard(modifier = Modifier.animateContentSize(tween(280, easing = GentleEasing))) {
                    // 通知点击行为：原地展开 3 选项
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickableNoRipple { notificationMenuOpen = !notificationMenuOpen }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.settings_notification_action),
                                color = XixiTheme.colors.textPrimary,
                                fontSize = 14.sp,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = stringResource(
                                    when (prefs.notificationAction) {
                                        NotificationActionMode.OPEN_EDIT ->
                                            R.string.settings_notification_open_edit
                                        NotificationActionMode.OPEN_HOME ->
                                            R.string.settings_notification_open_home
                                        NotificationActionMode.HIGHLIGHT ->
                                            R.string.settings_notification_highlight
                                    }
                                ),
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 13.sp,
                                letterSpacing = 0.5.sp
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(
                                    if (notificationMenuOpen) XixiTheme.colors.textPrimary
                                    else XixiTheme.colors.textSecondary
                                )
                        )
                    }

                    AnimatedVisibility(
                        visible = notificationMenuOpen,
                        enter = fadeIn(tween(180, easing = GentleEasing)) +
                            expandVertically(tween(260, easing = GentleEasing)),
                        exit = fadeOut(tween(140, easing = GentleEasing)) +
                            shrinkVertically(tween(220, easing = GentleEasing))
                    ) {
                        Column(modifier = Modifier.padding(top = 10.dp)) {
                            NotificationActionMode.entries.forEach { mode ->
                                val selected = prefs.notificationAction == mode
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickableNoRipple {
                                            viewModel.setNotificationAction(mode)
                                            notificationMenuOpen = false
                                        }
                                        .padding(horizontal = 10.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (selected) XixiTheme.colors.textPrimary
                                                else Color.Transparent
                                            )
                                    )
                                    Spacer(modifier = Modifier.size(10.dp))
                                    Text(
                                        text = stringResource(
                                            when (mode) {
                                                NotificationActionMode.OPEN_EDIT ->
                                                    R.string.settings_notification_open_edit
                                                NotificationActionMode.OPEN_HOME ->
                                                    R.string.settings_notification_open_home
                                                NotificationActionMode.HIGHLIGHT ->
                                                    R.string.settings_notification_highlight
                                            }
                                        ),
                                        color = XixiTheme.colors.textPrimary,
                                        fontSize = 14.sp,
                                        letterSpacing = 0.5.sp,
                                        fontWeight = if (selected) FontWeight.SemiBold
                                        else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // ------------------------------------------------------ 关于
                SectionTitle(stringResource(R.string.settings_section_about))

                SettingCard {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickableNoRipple { onboardingConfirm = true }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.settings_onboarding),
                            color = XixiTheme.colors.textPrimary,
                            fontSize = 14.sp,
                            letterSpacing = 0.5.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = stringResource(R.string.settings_version, BuildConfig.VERSION_NAME),
                        color = XixiTheme.colors.textSecondary,
                        fontSize = 12.sp,
                        letterSpacing = 0.5.sp
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
                .padding(16.dp)
                .padding(bottom = 72.dp)
        )
    }
}

/** 分组标题 */
@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        color = XixiTheme.colors.textSecondary,
        fontSize = 12.sp,
        letterSpacing = 0.5.sp,
        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
    )
}

/** 设置卡片 */
@Composable
private fun SettingCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(XixiTheme.colors.card)
            .padding(16.dp)
    ) {
        content()
    }
}

/** 标题 + 说明 */
@Composable
private fun SettingLabel(title: String, description: String?) {
    Column {
        Text(
            text = title,
            color = XixiTheme.colors.textPrimary,
            fontSize = 14.sp,
            letterSpacing = 0.5.sp
        )
        if (description != null) {
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = description,
                color = XixiTheme.colors.textSecondary,
                fontSize = 12.sp,
                letterSpacing = 0.5.sp
            )
        }
    }
}

/** 分段选择 */
@Composable
private fun <T> SegmentedOptions(
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(XixiTheme.colors.background)
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
                    .clip(RoundedCornerShape(10.dp))
                    .background(background)
                    .clickableNoRipple { onSelect(value) }
                    .padding(vertical = 9.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    color = if (isSelected) XixiTheme.colors.textPrimary
                    else XixiTheme.colors.textSecondary,
                    fontSize = 12.sp,
                    letterSpacing = 0.5.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                )
            }
        }
    }
}

/** 提醒分钟数选项 */
@Composable
private fun MinuteChip(
    minutes: Int,
    selected: Boolean,
    onClick: () -> Unit
) {
    val background by animateColorAsState(
        targetValue = if (selected) XixiTheme.colors.textPrimary.copy(alpha = 0.12f)
        else Color.Transparent,
        animationSpec = tween(durationMillis = 220),
        label = "minute_bg"
    )
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(background)
            .clickableNoRipple(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp)
    ) {
        Text(
            text = stringResource(R.string.settings_default_reminder_value, minutes),
            color = if (selected) XixiTheme.colors.textPrimary
            else XixiTheme.colors.textSecondary,
            fontSize = 12.sp,
            letterSpacing = 0.5.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

/** 开关行 */
@Composable
private fun SwitchRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = XixiTheme.colors.textPrimary,
                fontSize = 14.sp,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = description,
                color = XixiTheme.colors.textSecondary,
                fontSize = 12.sp,
                letterSpacing = 0.5.sp
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = XixiTheme.colors.background,
                checkedTrackColor = XixiTheme.quadrant.importantUrgent
            )
        )
    }
}
