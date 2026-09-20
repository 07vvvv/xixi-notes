package com.xixi.notes.ui.detail

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.xixi.notes.R
import com.xixi.notes.data.preferences.AppPrefs
import com.xixi.notes.di.LocalAppContainer
import com.xixi.notes.ui.components.InlineConfirm
import com.xixi.notes.ui.components.clickableNoRipple
import com.xixi.notes.ui.theme.XixiTheme
import com.xixi.notes.ui.util.GentleEasing
import com.xixi.notes.ui.util.imageModel
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.util.Calendar

/**
 * 任务编辑页。
 *
 * - 顶部栏：左「取消」右「保存」，标题非空且有变化时启用保存
 * - 日期时间选择器默认当前时间（精确到分钟），未保存不写入数据库
 * - 取消与返回手势行为一致：有修改弹 inline confirm；新建空内容直接退出
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun DetailScreen(
    taskId: Long,
    onClose: () -> Unit,
    onOpenImageViewer: (Int) -> Unit,
    initialQuadrant: com.xixi.notes.ui.board.Quadrant? = null,
    modifier: Modifier = Modifier,
    viewModel: DetailViewModel = viewModel(
        factory = DetailViewModel.Factory(
            LocalAppContainer.current.taskRepository,
            LocalAppContainer.current.imageManager,
            LocalAppContainer.current.preferences,
            LocalAppContainer.current.reminderScheduler
        )
    )
) {
    val container = LocalAppContainer.current
    val context = LocalContext.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    val prefs by container.preferences.prefs.collectAsStateWithLifecycle(initialValue = AppPrefs())

    // 载入
    LaunchedEffect(taskId, initialQuadrant) {
        viewModel.setInitialQuadrant(initialQuadrant)
        viewModel.load(taskId)
    }

    // 相册选图：两个 launcher，按 remaining 分发
    val multiPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(5)
    ) { uris -> viewModel.addFromGallery(uris) }

    val singlePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri -> uri?.let { viewModel.addFromGallery(listOf(it)) } }

    fun launchGallery() {
        val remaining = state.remainingImageSlots
        when {
            remaining >= 2 -> multiPicker.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
            remaining == 1 -> singlePicker.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
            else -> Unit
        }
    }

    // 拍照：temp 目录生成 camera_{uuid}.jpg -> FileProvider URI
    var pendingCameraFile by remember { mutableStateOf<File?>(null) }
    val takePictureLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        val file = pendingCameraFile
        pendingCameraFile = null
        if (success && file != null) {
            viewModel.addFromCamera(file)
        } else {
            file?.delete()
        }
    }

    var cameraRequested by remember { mutableStateOf(false) }
    val cameraAvailability = rememberCameraAvailability(
        context = context,
        requestGranted = cameraRequested,
        onRequestGranted = { cameraRequested = false }
    )

    fun takePhoto() {
        if (!cameraAvailability.canTakePhoto) {
            cameraRequested = true
            return
        }
        val tempFile = container.imageManager.createCameraTempFile()
        pendingCameraFile = tempFile
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            tempFile
        )
        takePictureLauncher.launch(uri)
    }

    // 权限提示
    val reminderPermissions = rememberReminderPermissions(context)
    val notificationMessage by reminderPermissions.notificationMessage
    val exactAlarmMessage by reminderPermissions.exactAlarmMessage

    // 日期 / 时间选择器
    var showDatePicker by remember { mutableStateOf(false) }
    /** 打开时间选择器的目标字段：DUE / REMINDER */
    var timePickerTarget by remember { mutableStateOf<String?>(null) }
    var pendingDateMillis by remember { mutableStateOf<Long?>(null) }
    // 选择器默认当前时间，精确到分钟
    val initialPickerTime = remember {
        LocalDateTime.now().withSecond(0).withNano(0)
    }

    // 有未保存修改时拦截返回手势
    BackHandler(enabled = true) {
        if (state.deleteConfirmVisible) {
            viewModel.hideDeleteConfirm()
        } else if (state.discardConfirmVisible) {
            viewModel.hideDiscardConfirm()
        } else if (state.imageMenuVisible) {
            viewModel.hideImageMenu()
        } else if (state.hasChanges) {
            viewModel.showDiscardConfirm()
        } else {
            viewModel.cancel(onDone = onClose)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(XixiTheme.colors.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .imePadding()
        ) {
            // -------------------------------------------------------- 顶部栏
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickableNoRipple {
                            if (state.hasChanges) {
                                viewModel.showDiscardConfirm()
                            } else {
                                viewModel.cancel(onDone = onClose)
                            }
                        }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.action_cancel),
                        color = XixiTheme.colors.textPrimary,
                        fontSize = 14.sp,
                        letterSpacing = 0.5.sp
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                Text(
                    text = stringResource(
                        if (state.isNew) R.string.editor_title_new else R.string.editor_title_edit
                    ),
                    color = XixiTheme.colors.textPrimary,
                    fontSize = 16.sp,
                    letterSpacing = 0.5.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.weight(1f))

                val saveAlpha by animateFloatAsState(
                    targetValue = if (state.canSave) 1f else 0.35f,
                    animationSpec = tween(durationMillis = 220, easing = GentleEasing),
                    label = "save_alpha"
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickableNoRipple(enabled = state.canSave) {
                            viewModel.save(onDone = { onClose() })
                        }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.action_save),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = saveAlpha),
                        fontSize = 14.sp,
                        letterSpacing = 0.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // ---------------------------------------------------------- 内容
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
            ) {
                // 标题
                FieldCard {
                    BasicTextField(
                        value = state.title,
                        onValueChange = viewModel::setTitle,
                        textStyle = androidx.compose.ui.text.TextStyle(
                            color = XixiTheme.colors.textPrimary,
                            fontSize = 17.sp,
                            letterSpacing = 0.5.sp
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Sentences,
                            imeAction = ImeAction.Next
                        ),
                        singleLine = true,
                        decorationBox = { inner ->
                            if (state.title.isEmpty()) {
                                Text(
                                    text = stringResource(R.string.editor_field_title_placeholder),
                                    color = XixiTheme.colors.textSecondary,
                                    fontSize = 17.sp,
                                    letterSpacing = 0.5.sp
                                )
                            }
                            inner()
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 描述
                FieldCard {
                    BasicTextField(
                        value = state.description,
                        onValueChange = viewModel::setDescription,
                        textStyle = androidx.compose.ui.text.TextStyle(
                            color = XixiTheme.colors.textPrimary,
                            fontSize = 14.sp,
                            lineHeight = 22.sp,
                            letterSpacing = 0.5.sp
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Sentences,
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Default
                        ),
                        decorationBox = { inner ->
                            if (state.description.isEmpty()) {
                                Text(
                                    text = stringResource(
                                        R.string.editor_field_description_placeholder
                                    ),
                                    color = XixiTheme.colors.textSecondary,
                                    fontSize = 14.sp,
                                    letterSpacing = 0.5.sp
                                )
                            }
                            inner()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 64.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 标签 + 状态
                FieldCard {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TagChip(
                                label = stringResource(R.string.editor_tag_important),
                                selected = state.isImportant,
                                color = XixiTheme.quadrant.importantUrgent,
                                onClick = viewModel::toggleImportant
                            )
                            TagChip(
                                label = stringResource(R.string.editor_tag_urgent),
                                selected = state.isUrgent,
                                color = XixiTheme.quadrant.urgentOnly,
                                onClick = viewModel::toggleUrgent
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TagChip(
                                label = stringResource(R.string.editor_assigned),
                                selected = state.isAssignedToMe,
                                color = MaterialTheme.colorScheme.primary,
                                onClick = viewModel::toggleAssigned
                            )
                            TagChip(
                                label = stringResource(R.string.editor_checklist),
                                selected = state.isCheckedOff,
                                color = Color(0xFF22C55E),
                                onClick = viewModel::toggleCheckedOff
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 截止日期
                FieldCard {
                    Column {
                        PickerRow(
                            icon = Icons.Default.CalendarMonth,
                            label = stringResource(R.string.editor_field_due_date),
                            value = state.dueDate?.let {
                                com.xixi.notes.ui.util.formatDateTime(it)
                            } ?: stringResource(R.string.editor_not_set),
                            onClear = if (state.dueDate != null) {
                                { viewModel.setDueDate(null) }
                            } else {
                                null
                            },
                            onClick = {
                                pendingDateMillis = null
                                showDatePicker = true
                            }
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        // 提醒时间：点击时一并处理通知与精确闹钟权限
                        PickerRow(
                            icon = Icons.Default.Notifications,
                            label = stringResource(R.string.editor_field_reminder),
                            value = state.reminderTime?.let {
                                com.xixi.notes.ui.util.formatDateTime(it)
                            } ?: stringResource(R.string.editor_not_set),
                            onClear = if (state.reminderTime != null) {
                                { viewModel.setReminderTime(null) }
                            } else {
                                null
                            },
                            onClick = {
                                // 点击提醒时间选择器时一并处理两个权限
                                reminderPermissions.request()
                                pendingDateMillis = null
                                timePickerTarget = TimeTarget.REMINDER
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 图片
                FieldCard {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = stringResource(R.string.editor_field_images),
                                color = XixiTheme.colors.textPrimary,
                                fontSize = 14.sp,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            Text(
                                text = stringResource(R.string.editor_image_limit),
                                color = XixiTheme.colors.textSecondary,
                                fontSize = 12.sp,
                                letterSpacing = 0.5.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))

                        Box {
                            ImageStrip(
                                state = state,
                                imageManager = container.imageManager,
                                onRemove = viewModel::removeImage,
                                onAdd = { viewModel.showImageMenu() },
                                onOpenViewer = { index ->
                                    if (state.images.isNotEmpty()) {
                                        val initial = container.imageViewerBridge.open(
                                            state.images.map { it.path },
                                            index
                                        )
                                        onOpenImageViewer(initial)
                                    }
                                }
                            )

                            // 压缩中只禁用图片区域
                            if (state.compressing) {
                                Box(
                                    modifier = Modifier
                                        .matchParentSize()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(XixiTheme.colors.background.copy(alpha = 0.72f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(22.dp),
                                            strokeWidth = 2.dp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = stringResource(
                                                R.string.editor_image_processing,
                                                state.compressionCurrent + 1,
                                                state.compressionTotal
                                            ),
                                            color = XixiTheme.colors.textPrimary,
                                            fontSize = 12.sp,
                                            letterSpacing = 0.5.sp
                                        )
                                    }
                                }
                            }
                        }

                        // 添加图片 inline 菜单
                        AnimatedVisibility(
                            visible = state.imageMenuVisible,
                            enter = fadeIn(tween(180, easing = GentleEasing)),
                            exit = fadeOut(tween(140, easing = GentleEasing))
                        ) {
                            Column(modifier = Modifier.padding(top = 10.dp)) {
                                MenuRow(
                                    icon = Icons.Default.PhotoLibrary,
                                    label = stringResource(R.string.editor_image_from_gallery),
                                    onClick = {
                                        viewModel.hideImageMenu()
                                        launchGallery()
                                    }
                                )
                                // 无相机或权限被拒时隐藏「拍照」
                                if (cameraAvailability.canTakePhoto) {
                                    MenuRow(
                                        icon = Icons.Default.AddPhotoAlternate,
                                        label = stringResource(R.string.editor_image_take_photo),
                                        onClick = {
                                            viewModel.hideImageMenu()
                                            takePhoto()
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // 删除任务
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF7F1D1D).copy(alpha = 0.18f))
                        .clickableNoRipple {
                            if (state.isImportant) {
                                viewModel.showDeleteConfirm()
                            } else if (!state.isNew) {
                                viewModel.deleteTask(onDone = onClose)
                            } else {
                                viewModel.cancel(onDone = onClose)
                            }
                        }
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.editor_delete_action),
                        color = Color(0xFFEF4444),
                        fontSize = 14.sp,
                        letterSpacing = 0.5.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                // 重要事项删除确认
                InlineConfirm(
                    visible = state.deleteConfirmVisible,
                    title = stringResource(R.string.editor_delete_title),
                    message = stringResource(R.string.editor_delete_message),
                    confirmLabel = stringResource(R.string.action_delete),
                    cancelLabel = stringResource(R.string.action_cancel),
                    onConfirm = {
                        viewModel.hideDeleteConfirm()
                        viewModel.deleteTask(onDone = onClose)
                    },
                    onCancel = { viewModel.hideDeleteConfirm() }
                )

                Spacer(modifier = Modifier.height(120.dp))
            }
        }

        // 未保存修改确认
        InlineConfirm(
            visible = state.discardConfirmVisible,
            title = stringResource(R.string.editor_discard_title),
            message = stringResource(R.string.editor_discard_message),
            confirmLabel = stringResource(R.string.action_cancel),
            cancelLabel = stringResource(R.string.action_save),
            onConfirm = {
                viewModel.hideDiscardConfirm()
                viewModel.cancel(onDone = onClose)
            },
            onCancel = {
                viewModel.hideDiscardConfirm()
                if (state.canSave) viewModel.save(onDone = { onClose() })
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        )

        // 保存失败 / 压缩失败提示
        val message = state.message
        AnimatedVisibility(
            visible = message != null,
            enter = fadeIn(tween(180, easing = GentleEasing)),
            exit = fadeOut(tween(160, easing = GentleEasing)),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 72.dp)
        ) {
            Box(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(XixiTheme.colors.card)
                    .clickableNoRipple { viewModel.clearMessage() }
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                Text(
                    text = message.orEmpty(),
                    color = XixiTheme.colors.textPrimary,
                    fontSize = 13.sp,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }

    // 权限被拒提示
    if (notificationMessage != null || exactAlarmMessage != null) {
        AlertDialog(
            onDismissRequest = { reminderPermissions.dismissMessages() },
            title = {
                Text(
                    text = stringResource(
                        if (notificationMessage != null) R.string.perm_notification_title
                        else R.string.perm_exact_alarm_title
                    )
                )
            },
            text = {
                Text(
                    text = notificationMessage
                        ?: stringResource(R.string.perm_exact_alarm_message)
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (exactAlarmMessage != null && notificationMessage == null) {
                        reminderPermissions.openExactAlarmSettings()
                    }
                    reminderPermissions.dismissMessages()
                }) {
                    Text(stringResource(R.string.perm_go_settings))
                }
            },
            dismissButton = {
                TextButton(onClick = { reminderPermissions.dismissMessages() }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    // 日期选择器（默认当前时间）
    if (showDatePicker) {
        val initialDate = pendingDateMillis ?: state.dueDate ?: System.currentTimeMillis()
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialDate)
        DatePickerDialog(
            onDismiss = {
                // 取消：不改变原值
                showDatePicker = false
                pendingDateMillis = null
            },
            onConfirm = {
                val picked = datePickerState.selectedDateMillis
                showDatePicker = false
                if (picked != null) {
                    pendingDateMillis = picked
                    // 紧接着选时间
                    timePickerTarget = TimeTarget.DUE
                } else {
                    pendingDateMillis = null
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // 时间选择器（跟随系统 24 小时制）
    val target = timePickerTarget
    if (target != null) {
        val existing = if (target == TimeTarget.DUE) state.dueDate else state.reminderTime
        val defaultTime = when {
            target == TimeTarget.DUE && pendingDateMillis != null ->
                Instant.ofEpochMilli(pendingDateMillis!!)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime()
                    .toLocalTime()
                    .withSecond(0)
                    .withNano(0)
            existing != null ->
                Instant.ofEpochMilli(existing)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime()
                    .toLocalTime()
                    .withSecond(0)
                    .withNano(0)
            // 默认当前时间，精确到分钟
            else -> initialPickerTime.toLocalTime()
        }

        val timePickerState = rememberTimePickerState(
            initialHour = defaultTime.hour,
            initialMinute = defaultTime.minute,
            is24Hour = android.text.format.DateFormat.is24HourFormat(context)
        )

        AlertDialog(
            onDismissRequest = {
                // 取消：不改变原值
                timePickerTarget = null
                pendingDateMillis = null
            },
            title = {
                Text(
                    stringResource(
                        if (target == TimeTarget.DUE) R.string.editor_field_due_date
                        else R.string.editor_field_reminder
                    )
                )
            },
            text = {
                TimePicker(
                    state = timePickerState,
                    colors = TimePickerDefaults.colors()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val zone = ZoneId.systemDefault()
                    val date = when {
                        target == TimeTarget.DUE && pendingDateMillis != null ->
                            Instant.ofEpochMilli(pendingDateMillis!!).atZone(zone).toLocalDate()
                        existing != null ->
                            Instant.ofEpochMilli(existing).atZone(zone).toLocalDate()
                        else -> LocalDate.now(zone)
                    }
                    val localTime = LocalTime.of(timePickerState.hour, timePickerState.minute)
                    val result = LocalDateTime.of(date, localTime)
                        .atZone(zone)
                        .toInstant()
                        .toEpochMilli()

                    if (target == TimeTarget.DUE) {
                        viewModel.setDueDate(result)
                    } else {
                        // 提醒时间：若无既有提醒，按全局默认提前分钟数推导
                        val previousReminder = state.reminderTime
                        if (previousReminder == null && prefs.defaultReminderMinutes > 0) {
                            val offset = prefs.defaultReminderMinutes * 60_000L
                            viewModel.setReminderTime((result - offset).coerceAtLeast(0L))
                        } else {
                            viewModel.setReminderTime(result)
                        }
                    }
                    timePickerTarget = null
                    pendingDateMillis = null
                }) {
                    Text(stringResource(R.string.action_save))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    // 取消：不改变原值
                    timePickerTarget = null
                    pendingDateMillis = null
                }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}

/** 时间选择器的目标字段 */
private object TimeTarget {
    const val DUE = "due"
    const val REMINDER = "reminder"
}

/** 日期 / 时间对话框（不使用默认的 DatePickerDialog 样式，改为自定义弹层） */
@Composable
private fun DatePickerDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    content: @Composable () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.editor_field_due_date)) },
        text = { content() },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        }
    )
}

/** 卡片容器 */
@Composable
private fun FieldCard(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(XixiTheme.colors.card)
            .padding(16.dp)
    ) {
        content()
    }
}

/** 标签开关（重要 / 紧急 / 我来做 / 已完成） */
@Composable
private fun TagChip(
    label: String,
    selected: Boolean,
    color: Color,
    onClick: () -> Unit
) {
    val background by animateColorAsState(
        targetValue = if (selected) color.copy(alpha = 0.22f) else Color.Transparent,
        animationSpec = tween(durationMillis = 220),
        label = "tag_bg"
    )
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.04f else 1f,
        animationSpec = tween(durationMillis = 220, easing = GentleEasing),
        label = "tag_scale"
    )
    Box(
        modifier = Modifier
            .scale(scale)
            .clip(RoundedCornerShape(12.dp))
            .background(background)
            .then(
                if (selected) {
                    Modifier.border(1.dp, color.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                } else {
                    Modifier
                }
            )
            .clickableNoRipple(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(
            text = label,
            color = if (selected) color else XixiTheme.colors.textSecondary,
            fontSize = 13.sp,
            letterSpacing = 0.5.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

/** 图标 + 标签 + 值的行 */
@Composable
private fun PickerRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    onClear: (() -> Unit)?,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickableNoRipple(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = XixiTheme.colors.textSecondary,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = label,
            color = XixiTheme.colors.textPrimary,
            fontSize = 14.sp,
            letterSpacing = 0.5.sp
        )
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = value,
            color = if (value == "未设置") XixiTheme.colors.textSecondary
            else MaterialTheme.colorScheme.primary,
            fontSize = 13.sp,
            letterSpacing = 0.5.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (onClear != null) {
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .clickableNoRipple(onClick = onClear),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(R.string.action_close),
                    tint = XixiTheme.colors.textSecondary,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

/** 图片区域：横向滚动 80dp × 80dp 缩略图 + 末尾添加按钮 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ImageStrip(
    state: DetailUiState,
    imageManager: com.xixi.notes.image.ImageManager,
    onRemove: (String) -> Unit,
    onAdd: () -> Unit,
    onOpenViewer: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        state.images.forEachIndexed { index, image ->
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .combinedClickable(
                        onClick = { },
                        onDoubleClick = { onOpenViewer(index) }
                    )
            ) {
                AsyncImage(
                    model = imageModel(image.path, imageManager),
                    contentDescription = stringResource(R.string.cd_view_image),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                // 右上角删除按钮 24dp（半透明黑底 + 白色 Close）
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(2.dp)
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.45f))
                        .clickableNoRipple { onRemove(image.path) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.cd_remove_image),
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }

        // 末尾「+ 添加」按钮：80dp × 80dp 虚线边框
        if (state.remainingImageSlots > 0) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .dashedBorder()
                    .clickableNoRipple(enabled = !state.compressing, onClick = onAdd),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.AddPhotoAlternate,
                        contentDescription = stringResource(R.string.action_add_image),
                        tint = XixiTheme.colors.textSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.action_add_image),
                        color = XixiTheme.colors.textSecondary,
                        fontSize = 11.sp,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }
    }
}

/** 虚线边框 */
private fun Modifier.dashedBorder(): Modifier = this.drawBehind {
    drawRoundRect(
        color = Color(0xFF71717A),
        size = size,
        cornerRadius = CornerRadius(8.dp.toPx()),
        style = Stroke(
            width = 1.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
        )
    )
}

/** 图片菜单行 */
@Composable
private fun MenuRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickableNoRipple(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = XixiTheme.colors.textPrimary,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = label,
            color = XixiTheme.colors.textPrimary,
            fontSize = 14.sp,
            letterSpacing = 0.5.sp
        )
    }
}
