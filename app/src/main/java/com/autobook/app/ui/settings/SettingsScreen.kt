package com.autobook.app.ui.settings

import android.content.Context
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Handyman
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CallMerge
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.FactCheck
import androidx.compose.material.icons.outlined.FormatListBulleted
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Loop
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.PauseCircle
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Timeline
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.Sell
import androidx.compose.material.icons.outlined.Upload
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.autobook.app.AutoBookApp
import com.autobook.app.data.CsvCodec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
    val clipboard = androidx.compose.ui.platform.LocalClipboardManager.current

    var listenerEnabled by remember { mutableStateOf(com.autobook.app.util.Permissions.isNotificationListenerEnabled(context)) }
    var smsGranted by remember { mutableStateOf(com.autobook.app.util.Permissions.isSmsGranted(context)) }
    var batteryIgnored by remember { mutableStateOf(com.autobook.app.util.Permissions.isIgnoringBatteryOptimizations(context)) }
    var exactAlarm by remember { mutableStateOf(com.autobook.app.util.Permissions.canScheduleExactAlarm(context)) }
    var resultMessage by remember { mutableStateOf<String?>(null) }
    var showRecurring by remember { mutableStateOf(false) }
    var showBookManager by remember { mutableStateOf(false) }
    var showBlockWords by remember { mutableStateOf(false) }
    var showNotifLog by remember { mutableStateOf(false) }
    var showYearReport by remember { mutableStateOf(false) }
    var showExport by remember { mutableStateOf(false) }
    var showCategoryManage by remember { mutableStateOf(false) }
    var showLearnedRules by remember { mutableStateOf(false) }
    var showCustomColor by remember { mutableStateOf(false) }
    var showMergeMerchants by remember { mutableStateOf(false) }
    var showSelfCheck by remember { mutableStateOf(false) }
    var showCrashLog by remember { mutableStateOf(false) }
    var showClearAll by remember { mutableStateOf(false) }
    var showClearAllFinal by remember { mutableStateOf(false) }
    var expandedPermissions by rememberSaveable { mutableStateOf(false) }
    var expandedData by rememberSaveable { mutableStateOf(false) }
    var expandedPrefs by rememberSaveable { mutableStateOf(false) }
    var expandedTools by rememberSaveable { mutableStateOf(false) }
    var expandedStats by rememberSaveable { mutableStateOf(false) }
    var expandedHelp by rememberSaveable { mutableStateOf(false) }
    var exportPendingCsv by remember { mutableStateOf("") }
    var exportPendingCount by remember { mutableIntStateOf(0) }
    var exportedUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var persistentNotif by remember {
        mutableStateOf(com.autobook.app.util.AppSettings.isPersistentNotification(context))
    }
    var autoPaused by remember {
        mutableStateOf(com.autobook.app.util.AppSettings.isAutoPaused(context))
    }
    var lockEnabled by remember {
        mutableStateOf(com.autobook.app.util.AppSettings.isAppLockEnabled(context))
    }
    var showSetPin by remember { mutableStateOf(false) }
    var showVerifyPin by remember { mutableStateOf(false) }
    var notifGranted by remember { mutableStateOf(com.autobook.app.util.Permissions.isNotifGranted(context)) }

    val viewModel: com.autobook.app.viewmodel.MainViewModel = viewModel()
    val hideAmount by viewModel.hideAmount.collectAsStateWithLifecycle()
    val fontScale by viewModel.fontScale.collectAsStateWithLifecycle()
    val themeColor by viewModel.themeColor.collectAsStateWithLifecycle()
    val darkMode by viewModel.darkMode.collectAsStateWithLifecycle()

    val scope = rememberCoroutineScope()

    val lifecycleOwner = LocalLifecycleOwner.current
    LifecycleResumeEffect(Unit) {
        listenerEnabled = com.autobook.app.util.Permissions.isNotificationListenerEnabled(context)
        smsGranted = com.autobook.app.util.Permissions.isSmsGranted(context)
        batteryIgnored = com.autobook.app.util.Permissions.isIgnoringBatteryOptimizations(context)
        notifGranted = com.autobook.app.util.Permissions.isNotifGranted(context)
        exactAlarm = com.autobook.app.util.Permissions.canScheduleExactAlarm(context)
        onPauseOrDispose { }
    }

    val smsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        smsGranted = com.autobook.app.util.Permissions.isSmsGranted(context)
    }
    val notifLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
    }

    var restorePending by remember { mutableStateOf<String?>(null) }

    val backupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        if (uri != null) {
            scope.launch(Dispatchers.IO) {
                try {
                    val json = org.json.JSONObject()
                    json.put(
                        "settings",
                        org.json.JSONObject(
                            com.autobook.app.util.AppSettings.exportAllSettings(context)
                        )
                    )
                    json.put(
                        "csv",
                        CsvCodec.encode(AutoBookApp.instance.repository.getAllOnce())
                    )
                    val data = ("ABK1\n" + json.toString()).toByteArray(Charsets.UTF_8)
                    context.contentResolver.openOutputStream(uri)?.use { it.write(data) }
                    resultMessage = "完整备份已保存"
                } catch (e: Exception) {
                    resultMessage = "备份失败：${e.message}"
                }
            }
        }
    }

    val restoreLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch(Dispatchers.IO) {
                try {
                    val text = context.contentResolver.openInputStream(uri)
                        ?.bufferedReader()?.readText()
                    if (text != null && text.startsWith("ABK1\n")) {
                        restorePending = text.removePrefix("ABK1\n")
                    } else {
                        resultMessage = "不是有效的默记备份文件"
                    }
                } catch (e: Exception) {
                    resultMessage = "读取失败：${e.message}"
                }
            }
        }
    }

    val createDocLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            scope.launch(Dispatchers.IO) {
                try {
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        out.write(exportPendingCsv.toByteArray(Charsets.UTF_8))
                    }
                    exportedUri = uri
                    resultMessage = "已导出 ${exportPendingCount} 笔记录"
                } catch (e: Exception) {
                    resultMessage = "导出失败：${e.message}"
                }
            }
        }
    }

    val openDocLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch(Dispatchers.IO) {
                try {
                    val bytes = readLimited(context, uri) ?: run {
                        resultMessage = "导入失败：文件过大（超过 10MB）"
                        return@launch
                    }
                    val content = decodeBytes(bytes)
                    val txs = CsvCodec.decode(content)
                    val existing = AutoBookApp.instance.repository.getAllOnce()
                    val existingKeys = existing.map {
                        Triple(it.timestamp, it.amount, it.merchant)
                    }.toHashSet()
                    val newTxs = txs.filter {
                        Triple(it.timestamp, it.amount, it.merchant) !in existingKeys
                    }
                    AutoBookApp.instance.repository.addAll(newTxs)
                    val skipped = txs.size - newTxs.size
                    com.autobook.app.service.MonthSync.refreshCurrentMonth(
                        context,
                        com.autobook.app.util.AppSettings.getCurrentBook(context)
                    )
                    resultMessage = if (skipped > 0) {
                        "成功导入 ${newTxs.size} 笔，跳过重复 ${skipped} 笔"
                    } else {
                        "成功导入 ${newTxs.size} 笔记录"
                    }
                } catch (e: Exception) {
                    resultMessage = "导入失败：${e.message}"
                }
            }
        }
    }

    val settingsScrollState = rememberScrollState()
    Box(modifier = modifier.fillMaxSize()) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(settingsScrollState)
            .padding(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
            )
        ) {
            Column(
                Modifier
                    .padding(16.dp)
            ) {
                val arrowPermissions by androidx.compose.animation.core.animateFloatAsState(
                    targetValue = if (expandedPermissions) 180f else 0f,
                    label = "arrow"
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { expandedPermissions = !expandedPermissions },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Filled.Security,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "默记权限",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        Icons.Filled.KeyboardArrowDown,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .size(18.dp)
                            .rotate(arrowPermissions)
                    )
                }
                androidx.compose.animation.AnimatedVisibility(
                    visible = expandedPermissions,
                    enter = androidx.compose.animation.expandVertically(
                        animationSpec = androidx.compose.animation.core.tween(
                            260, easing = androidx.compose.animation.core.FastOutSlowInEasing
                        )
                    ) + androidx.compose.animation.fadeIn(
                        animationSpec = androidx.compose.animation.core.tween(260)
                    ),
                    exit = androidx.compose.animation.shrinkVertically(
                        animationSpec = androidx.compose.animation.core.tween(
                            220, easing = androidx.compose.animation.core.FastOutSlowInEasing
                        )
                    ) + androidx.compose.animation.fadeOut(
                        animationSpec = androidx.compose.animation.core.tween(180)
                    )
                ) {
                Column {
                Spacer(Modifier.height(12.dp))

                PermissionRow(
                    title = "通知使用权（微信/支付宝/云闪付自动记账）",
                    granted = listenerEnabled,
                    actionLabel = if (listenerEnabled) null else "去开启"
                ) {
                    context.startActivity(
                        android.content.Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                            .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                }

                PermissionRow(
                    title = "短信权限（银行及消费短信自动记账）",
                    granted = smsGranted,
                    actionLabel = if (smsGranted) null else "去授权"
                ) {
                    val perms = com.autobook.app.util.Permissions.missingRuntimePermissions(context)
                    if (perms.isNotEmpty()) {
                        smsLauncher.launch(perms.toTypedArray())
                    }
                }

                PermissionRow(
                    title = "通知权限（常驻运行状态显示）",
                    granted = notifGranted,
                    actionLabel = if (notifGranted) null else "去授权"
                ) {
                    val perms = com.autobook.app.util.Permissions.missingRuntimePermissions(context)
                    if (perms.isNotEmpty()) {
                        smsLauncher.launch(perms.toTypedArray())
                    }
                }

                PermissionRow(
                    title = "后台运行保护（防止自动记账被系统关闭）",
                    granted = batteryIgnored,
                    actionLabel = if (batteryIgnored) null else "去开启"
                ) {
                    context.startActivity(
                        android.content.Intent(
                            Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                            android.net.Uri.parse("package:${context.packageName}")
                        ).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                }

                PermissionRow(
                    title = "精确闹钟（让自愈检查在深睡时也准时）",
                    granted = exactAlarm,
                    actionLabel = if (exactAlarm) null else "去开启"
                ) {
                    context.startActivity(
                        android.content.Intent(
                            Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                            android.net.Uri.parse("package:${context.packageName}")
                        ).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                }

                Spacer(Modifier.height(4.dp))
                androidx.compose.material3.HorizontalDivider(
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Outlined.PhoneAndroid,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "厂商后台设置（华为/小米/OPPO/vivo 建议检查）",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(
                        onClick = {
                            context.startActivity(
                                android.content.Intent(
                                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                    android.net.Uri.parse("package:${context.packageName}")
                                ).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                        }
                    ) { Text("去设置", fontSize = 11.sp) }
                }
                Text(
                    text = "在打开的页面里找「自启动」「省电策略」「后台管理」，把默记设为允许，自动记账更稳定。",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            }
            }
        }

        Spacer(Modifier.height(16.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
            )
        ) {
            Column(
                Modifier
                    .padding(16.dp)
            ) {
                val arrowData by androidx.compose.animation.core.animateFloatAsState(
                    targetValue = if (expandedData) 180f else 0f,
                    label = "arrow"
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { expandedData = !expandedData },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Filled.Folder,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "数据管理",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        Icons.Filled.KeyboardArrowDown,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .size(18.dp)
                            .rotate(arrowData)
                    )
                }
                androidx.compose.animation.AnimatedVisibility(
                    visible = expandedData,
                    enter = androidx.compose.animation.expandVertically(
                        animationSpec = androidx.compose.animation.core.tween(
                            260, easing = androidx.compose.animation.core.FastOutSlowInEasing
                        )
                    ) + androidx.compose.animation.fadeIn(
                        animationSpec = androidx.compose.animation.core.tween(260)
                    ),
                    exit = androidx.compose.animation.shrinkVertically(
                        animationSpec = androidx.compose.animation.core.tween(
                            220, easing = androidx.compose.animation.core.FastOutSlowInEasing
                        )
                    ) + androidx.compose.animation.fadeOut(
                        animationSpec = androidx.compose.animation.core.tween(180)
                    )
                ) {
                Column {
                Spacer(Modifier.height(12.dp))
                androidx.compose.foundation.layout.FlowRow {
                    OutlinedButton(onClick = { showExport = true }) {
                        Icon(
                            androidx.compose.material.icons.Icons.Outlined.Upload,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("导出全部账单")
                    }
                    Spacer(Modifier.width(12.dp))
                    OutlinedButton(
                        onClick = { openDocLauncher.launch(arrayOf("text/*", "*/*")) }
                    ) {
                        Icon(
                            androidx.compose.material.icons.Icons.Outlined.Download,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("导入账单")
                    }
                }
                Spacer(Modifier.height(12.dp))
                androidx.compose.material3.HorizontalDivider(
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                )
                Spacer(Modifier.height(10.dp))
                androidx.compose.foundation.layout.FlowRow {
                    OutlinedButton(
                        onClick = {
                            backupLauncher.launch(
                                "默记备份_" +
                                    java.text.SimpleDateFormat(
                                        "yyyyMMdd_HHmm",
                                        java.util.Locale.CHINA
                                    ).format(java.util.Date()) + ".abk"
                            )
                        }
                    ) {
                        Icon(
                            Icons.Outlined.Backup,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("完整备份")
                    }
                    Spacer(Modifier.width(12.dp))
                    OutlinedButton(
                        onClick = { restoreLauncher.launch(arrayOf("*/*")) }
                    ) {
                        Icon(
                            Icons.Outlined.Restore,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("恢复备份")
                    }
                }
                Spacer(Modifier.height(12.dp))
                OutlinedButton(onClick = { showClearAll = true }) {
                    Icon(
                        Icons.Outlined.DeleteForever,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("清空全部数据", color = MaterialTheme.colorScheme.error)
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "完整备份包含全部账单、分类规则、屏蔽词、预算、应用锁等所有设置，换手机时用「恢复备份」一键还原。",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "导出为 CSV 文件，可用 Excel/WPS 打开编辑；导入时选择同格式的 CSV 文件即可。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            }
            }
        }

        Spacer(Modifier.height(16.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
            )
        ) {
            Column(
                Modifier
                    .padding(16.dp)
            ) {
                val arrowPrefs by androidx.compose.animation.core.animateFloatAsState(
                    targetValue = if (expandedPrefs) 180f else 0f,
                    label = "arrow"
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { expandedPrefs = !expandedPrefs },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Filled.Palette,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "偏好设置",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        Icons.Filled.KeyboardArrowDown,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .size(18.dp)
                            .rotate(arrowPrefs)
                    )
                }
                androidx.compose.animation.AnimatedVisibility(
                    visible = expandedPrefs,
                    enter = androidx.compose.animation.expandVertically(
                        animationSpec = androidx.compose.animation.core.tween(
                            260, easing = androidx.compose.animation.core.FastOutSlowInEasing
                        )
                    ) + androidx.compose.animation.fadeIn(
                        animationSpec = androidx.compose.animation.core.tween(260)
                    ),
                    exit = androidx.compose.animation.shrinkVertically(
                        animationSpec = androidx.compose.animation.core.tween(
                            220, easing = androidx.compose.animation.core.FastOutSlowInEasing
                        )
                    ) + androidx.compose.animation.fadeOut(
                        animationSpec = androidx.compose.animation.core.tween(180)
                    )
                ) {
                Column {
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        androidx.compose.material.icons.Icons.Outlined.VisibilityOff,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "隐藏金额（账单列表显示 ****）",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(
                        checked = hideAmount,
                        onCheckedChange = { viewModel.setHideAmount(it) },
                        colors = androidx.compose.material3.SwitchDefaults.colors(
                            uncheckedTrackColor = MaterialTheme.colorScheme.outline
                        )
                    )
                }
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Outlined.PauseCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "暂停自动记账（暂停后不再自动记录新账单）",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(
                        checked = autoPaused,
                        colors = androidx.compose.material3.SwitchDefaults.colors(
                            uncheckedTrackColor = MaterialTheme.colorScheme.outline
                        ),
                        onCheckedChange = {
                            autoPaused = it
                            com.autobook.app.util.AppSettings.setAutoPaused(context, it)
                        }
                    )
                }
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Outlined.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "应用锁（打开 App 需要 4 位密码）",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(
                        checked = lockEnabled,
                        colors = androidx.compose.material3.SwitchDefaults.colors(
                            uncheckedTrackColor = MaterialTheme.colorScheme.outline
                        ),
                        onCheckedChange = { on ->
                            if (on) {
                                showSetPin = true
                            } else {
                                showVerifyPin = true
                            }
                        }
                    )
                }
                if (lockEnabled) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "已开启，切换后台再回来会重新上锁",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = { showSetPin = true }) {
                            Text("修改密码", fontSize = 11.sp)
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        androidx.compose.material.icons.Icons.Outlined.Notifications,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "常驻通知（通知栏显示运行状态，防止被杀）",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(
                        checked = persistentNotif,
                        colors = androidx.compose.material3.SwitchDefaults.colors(
                            uncheckedTrackColor = MaterialTheme.colorScheme.outline
                        ),
                        onCheckedChange = { on ->
                            persistentNotif = on
                            com.autobook.app.util.AppSettings.setPersistentNotification(context, on)
                            if (on) {
                                val perms = com.autobook.app.util.Permissions.missingRuntimePermissions(context)
                                if (perms.isNotEmpty()) {
                                    smsLauncher.launch(perms.toTypedArray())
                                }
                            }
                            com.autobook.app.service.ServiceGuard.requestRebind(context)
                        }
                    )
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "字号大小",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(4.dp))
                androidx.compose.foundation.layout.FlowRow {
                    listOf(
                        "特小" to com.autobook.app.util.AppSettings.FONT_TINY,
                        "小" to com.autobook.app.util.AppSettings.FONT_SMALL,
                        "标准" to com.autobook.app.util.AppSettings.FONT_NORMAL,
                        "大" to com.autobook.app.util.AppSettings.FONT_LARGE,
                        "特大" to com.autobook.app.util.AppSettings.FONT_HUGE
                    ).forEach { (label, scale) ->
                        FilterChip(
                            selected = kotlin.math.abs(fontScale - scale) < 0.01f,
                            onClick = { viewModel.setFontScale(scale) },
                            label = { Text(label) },
                            modifier = Modifier.padding(end = 8.dp)
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "主题颜色",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(4.dp))
                androidx.compose.foundation.layout.FlowRow {
                    listOf(
                        0xFF00B578, 0xFF3B82F6, 0xFFF59E0B, 0xFF8B5CF6, 0xFFEF4444,
                        0xFF06B6D4, 0xFFEC4899, 0xFF64748B, 0xFF92400E, 0xFF6366F1
                    ).forEach { c ->
                        androidx.compose.foundation.layout.Box(
                            modifier = Modifier
                                .padding(end = 10.dp)
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(androidx.compose.ui.graphics.Color(c))
                                .clickable { viewModel.setThemeColor(c) },
                            contentAlignment = Alignment.Center
                        ) {
                            if (themeColor == c) {
                                Icon(
                                    Icons.Filled.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                    androidx.compose.foundation.layout.Box(
                        modifier = Modifier
                            .padding(end = 10.dp)
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(
                                androidx.compose.ui.graphics.Brush.sweepGradient(
                                    listOf(
                                        Color.Red, Color.Yellow, Color.Green,
                                        Color.Cyan, Color.Blue, Color.Magenta, Color.Red
                                    )
                                )
                            )
                            .clickable { showCustomColor = true }
                    )
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "背景模式",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(4.dp))
                androidx.compose.foundation.layout.FlowRow {
                    listOf(
                        "跟随系统" to com.autobook.app.util.AppSettings.DARK_FOLLOW_SYSTEM,
                        "白天" to com.autobook.app.util.AppSettings.DARK_ALWAYS_LIGHT,
                        "夜晚" to com.autobook.app.util.AppSettings.DARK_ALWAYS_DARK
                    ).forEach { (label, mode) ->
                        FilterChip(
                            selected = darkMode == mode,
                            onClick = { viewModel.setDarkMode(mode) },
                            label = { Text(label) },
                            modifier = Modifier.padding(end = 8.dp)
                        )
                    }
                }
            }
            }
            }
        }

        Spacer(Modifier.height(16.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
            )
        ) {
            Column(
                Modifier
                    .padding(16.dp)
            ) {
                val arrowTools by androidx.compose.animation.core.animateFloatAsState(
                    targetValue = if (expandedTools) 180f else 0f,
                    label = "arrow"
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { expandedTools = !expandedTools },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Filled.Handyman,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "工具",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        Icons.Filled.KeyboardArrowDown,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .size(18.dp)
                            .rotate(arrowTools)
                    )
                }
                androidx.compose.animation.AnimatedVisibility(
                    visible = expandedTools,
                    enter = androidx.compose.animation.expandVertically(
                        animationSpec = androidx.compose.animation.core.tween(
                            260, easing = androidx.compose.animation.core.FastOutSlowInEasing
                        )
                    ) + androidx.compose.animation.fadeIn(
                        animationSpec = androidx.compose.animation.core.tween(260)
                    ),
                    exit = androidx.compose.animation.shrinkVertically(
                        animationSpec = androidx.compose.animation.core.tween(
                            220, easing = androidx.compose.animation.core.FastOutSlowInEasing
                        )
                    ) + androidx.compose.animation.fadeOut(
                        animationSpec = androidx.compose.animation.core.tween(180)
                    )
                ) {
                Column {
                Spacer(Modifier.height(12.dp))
                androidx.compose.foundation.layout.FlowRow {
                    OutlinedButton(onClick = { showRecurring = true }, modifier = Modifier.width(150.dp)) {
                        Icon(
                            androidx.compose.material.icons.Icons.Outlined.Loop,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("周期记账")
                    }
                    Spacer(Modifier.width(12.dp))
                    OutlinedButton(onClick = { showBookManager = true }, modifier = Modifier.width(150.dp)) {
                        Icon(
                            androidx.compose.material.icons.Icons.Outlined.MenuBook,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("账本管理")
                    }
                    Spacer(Modifier.width(12.dp))
                    OutlinedButton(onClick = { showBlockWords = true }, modifier = Modifier.width(150.dp)) {
                        Icon(
                            androidx.compose.material.icons.Icons.Outlined.Block,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("记账屏蔽词")
                    }
                    Spacer(Modifier.width(12.dp))
                    OutlinedButton(onClick = { showNotifLog = true }, modifier = Modifier.width(150.dp)) {
                        Icon(
                            androidx.compose.material.icons.Icons.Outlined.FormatListBulleted,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("通知日志")
                    }
                    Spacer(Modifier.width(12.dp))
                    OutlinedButton(onClick = { showYearReport = true }, modifier = Modifier.width(150.dp)) {
                        Icon(
                            androidx.compose.material.icons.Icons.Outlined.Assessment,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("年度报告")
                    }
                    Spacer(Modifier.width(12.dp))
                    OutlinedButton(onClick = { showCategoryManage = true }, modifier = Modifier.width(150.dp)) {
                        Icon(
                            androidx.compose.material.icons.Icons.Outlined.Category,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("分类管理")
                    }
                    Spacer(Modifier.width(12.dp))
                    OutlinedButton(onClick = { showLearnedRules = true }, modifier = Modifier.width(150.dp)) {
                        Icon(
                            androidx.compose.material.icons.Icons.Outlined.Lightbulb,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("分类规则")
                    }
                    Spacer(Modifier.width(12.dp))
                    OutlinedButton(onClick = { showSelfCheck = true }, modifier = Modifier.width(150.dp)) {
                        Icon(
                            androidx.compose.material.icons.Icons.Outlined.FactCheck,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("自动记账自检")
                    }
                    Spacer(Modifier.width(12.dp))
                    OutlinedButton(onClick = { showMergeMerchants = true }, modifier = Modifier.width(150.dp)) {
                        Icon(
                            androidx.compose.material.icons.Icons.Outlined.CallMerge,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("商户合并")
                    }
                    Spacer(Modifier.width(12.dp))
                    OutlinedButton(onClick = { showCrashLog = true }, modifier = Modifier.width(150.dp)) {
                        Icon(
                            Icons.Outlined.BugReport,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("崩溃日志")
                    }
                }
            }
            }
            }
        }

        Spacer(Modifier.height(16.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
            )
        ) {
            Column(
                Modifier
                    .padding(16.dp)
            ) {
                val arrowStats by androidx.compose.animation.core.animateFloatAsState(
                    targetValue = if (expandedStats) 180f else 0f,
                    label = "arrow"
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { expandedStats = !expandedStats },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Filled.Insights,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "我的记账数据",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        Icons.Filled.KeyboardArrowDown,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .size(18.dp)
                            .rotate(arrowStats)
                    )
                }
                androidx.compose.animation.AnimatedVisibility(
                    visible = expandedStats,
                    enter = androidx.compose.animation.expandVertically(
                        animationSpec = androidx.compose.animation.core.tween(
                            260, easing = androidx.compose.animation.core.FastOutSlowInEasing
                        )
                    ) + androidx.compose.animation.fadeIn(
                        animationSpec = androidx.compose.animation.core.tween(260)
                    ),
                    exit = androidx.compose.animation.shrinkVertically(
                        animationSpec = androidx.compose.animation.core.tween(
                            220, easing = androidx.compose.animation.core.FastOutSlowInEasing
                        )
                    ) + androidx.compose.animation.fadeOut(
                        animationSpec = androidx.compose.animation.core.tween(180)
                    )
                ) {
                Column {
                Spacer(Modifier.height(10.dp))
                val allTxs = com.autobook.app.AutoBookApp.instance
                    .let { app ->
                        val txs = remember { mutableStateOf<List<com.autobook.app.data.Transaction>>(emptyList()) }
                        androidx.compose.runtime.LaunchedEffect(Unit, resultMessage) {
                            txs.value = kotlinx.coroutines.withContext(Dispatchers.IO) {
                                app.repository.getAllOnce()
                            }
                        }
                        txs.value
                    }
                if (allTxs.isEmpty()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 12.dp)
                    ) {
                        Icon(
                            Icons.Outlined.Inbox,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "暂无数据",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    val totalCount = allTxs.size
                    val maxAmount = allTxs.maxOf { it.amount }
                    val earliest = allTxs.minOf { it.timestamp }
                    val days = ((System.currentTimeMillis() - earliest) / 86_400_000L).toInt()
                    val monthRange = com.autobook.app.util.TimeUtils.currentMonthRange()
                    val monthTxs = allTxs.filter {
                        it.timestamp in monthRange.first until monthRange.second
                    }
                    val monthCount = monthTxs.size
                    val monthExpense = monthTxs
                        .filter { it.type == com.autobook.app.data.Transaction.TYPE_EXPENSE }
                        .sumOf { it.amount }
                    val dayOfMonth = java.util.Calendar.getInstance()
                        .get(java.util.Calendar.DAY_OF_MONTH)
                    val avgDaily = if (dayOfMonth > 0) monthExpense / dayOfMonth else 0.0
                    val dbSize = remember { mutableStateOf(0L) }
                    androidx.compose.runtime.LaunchedEffect(Unit) {
                        dbSize.value = kotlinx.coroutines.withContext(Dispatchers.IO) {
                            context.getDatabasePath("autobook.db").length()
                        }
                    }
                    androidx.compose.foundation.layout.Row(Modifier.fillMaxWidth()) {
                        StatItem(
                            label = "累计记录",
                            value = "$totalCount 笔",
                            icon = Icons.Outlined.FormatListBulleted,
                            modifier = Modifier.weight(1f)
                        )
                        StatItem(
                            label = "覆盖天数",
                            value = "$days 天",
                            icon = Icons.Outlined.CalendarMonth,
                            modifier = Modifier.weight(1f)
                        )
                        StatItem(
                            label = "最大单笔",
                            value = "¥${com.autobook.app.util.formatAmount(maxAmount)}",
                            icon = Icons.Outlined.TrendingUp,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    androidx.compose.foundation.layout.Row(Modifier.fillMaxWidth()) {
                        StatItem(
                            label = "本月笔数",
                            value = "$monthCount 笔",
                            icon = Icons.Outlined.ReceiptLong,
                            modifier = Modifier.weight(1f)
                        )
                        StatItem(
                            label = "本月日均支出",
                            value = "¥${com.autobook.app.util.formatAmount(avgDaily)}",
                            icon = Icons.Outlined.Timeline,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Outlined.Storage,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "数据体积：${"%.1f".format(dbSize.value / 1024.0)} KB",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            }
            }
        }

        Spacer(Modifier.height(16.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
            )
        ) {
            Column(
                Modifier
                    .padding(16.dp)
            ) {
                val arrowHelp by androidx.compose.animation.core.animateFloatAsState(
                    targetValue = if (expandedHelp) 180f else 0f,
                    label = "arrow"
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { expandedHelp = !expandedHelp },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Filled.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "使用说明与隐私",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        Icons.Filled.KeyboardArrowDown,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .size(18.dp)
                            .rotate(arrowHelp)
                    )
                }
                androidx.compose.animation.AnimatedVisibility(
                    visible = expandedHelp,
                    enter = androidx.compose.animation.expandVertically(
                        animationSpec = androidx.compose.animation.core.tween(
                            260, easing = androidx.compose.animation.core.FastOutSlowInEasing
                        )
                    ) + androidx.compose.animation.fadeIn(
                        animationSpec = androidx.compose.animation.core.tween(260)
                    ),
                    exit = androidx.compose.animation.shrinkVertically(
                        animationSpec = androidx.compose.animation.core.tween(
                            220, easing = androidx.compose.animation.core.FastOutSlowInEasing
                        )
                    ) + androidx.compose.animation.fadeOut(
                        animationSpec = androidx.compose.animation.core.tween(180)
                    )
                ) {
                Column {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "1. 微信/支付宝/云闪付：开启通知使用权后，付款通知会自动记入账单。\n" +
                        "2. 银行及各类消费短信：授权短信权限后自动记入账单。\n" +
                        "3. 现金消费：点首页右下角 + 手动记一笔。\n" +
                        "4. 桌面小组件：长按手机桌面空白处 → 添加小组件 → 找到「默记」（不同手机有所差别）。\n" +
                        "5. 本应用不联网，所有数据仅保存在本机，删除前记得导出账单。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(10.dp))
                androidx.compose.material3.HorizontalDivider(
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                )
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.Security,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "隐私承诺",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "· 本应用没有联网权限，技术上无法上传任何数据\n" +
                        "· 全部账单只保存在你的手机里，卸载即彻底删除\n" +
                        "· 数据随时可导出，随时可删除\n" +
                        "· 换手机用「完整备份」一键迁移",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "遇到问题？点这里复制诊断信息发给开发者",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(
                        onClick = {
                            scope.launch(Dispatchers.IO) {
                                val count = AutoBookApp.instance.repository.getAllOnce().size
                                val info =
                                    "设备：${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}\n" +
                                        "系统：Android ${android.os.Build.VERSION.RELEASE}\n" +
                                        "应用版本：4.88\n" +
                                        "账单记录：$count 笔"
                                clipboard.setText(
                                    androidx.compose.ui.text.AnnotatedString(info)
                                )
                                resultMessage = "诊断信息已复制，请粘贴发给开发者"
                            }
                        }
                    ) { Text("复制", fontSize = 11.sp) }
                }
            }
            }
            }
        }

        Spacer(Modifier.height(24.dp))
        Text(
            text = "默记 v4.88",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "开发者：xiuchenshen.com",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .clickable { uriHandler.openUri("https://xiuchenshen.com/") }
        )
    }

    if (settingsScrollState.value > 800) {
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface)
                .border(
                    1.dp,
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                    CircleShape
                )
                .clickable {
                    scope.launch { settingsScrollState.animateScrollTo(0) }
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                androidx.compose.material.icons.Icons.Filled.KeyboardArrowUp,
                contentDescription = "回到顶部",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
    }
    resultMessage?.let { msg ->
        AlertDialog(
            shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
            onDismissRequest = {
                resultMessage = null
                exportedUri = null
            },
            title = { Text("提示") },
            text = { Text(msg) },
            confirmButton = {
                if (exportedUri != null) {
                    TextButton(
                        onClick = {
                            val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                type = "text/csv"
                                putExtra(android.content.Intent.EXTRA_STREAM, exportedUri)
                                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(
                                android.content.Intent.createChooser(shareIntent, "分享账单文件")
                            )
                        }
                    ) { Text("分享") }
                }
                TextButton(
                    onClick = {
                        resultMessage = null
                        exportedUri = null
                    }
                ) { Text("好的") }
            }
        )
    }

    if (showRecurring) {
        RecurringDialog(onDismiss = { showRecurring = false })
    }

    if (showBookManager) {
        BookManagerDialog(onDismiss = { showBookManager = false })
    }

    if (showBlockWords) {
        BlockWordsDialog(onDismiss = { showBlockWords = false })
    }

    if (showNotifLog) {
        NotifLogDialog(onDismiss = { showNotifLog = false })
    }

    if (showYearReport) {
        YearReportDialog(onDismiss = { showYearReport = false })
    }

    if (showCategoryManage) {
        CategoryManageDialog(onDismiss = { showCategoryManage = false })
    }

    if (showLearnedRules) {
        LearnedRulesDialog(onDismiss = { showLearnedRules = false })
    }

    if (showMergeMerchants) {
        MergeMerchantsDialog(onDismiss = { showMergeMerchants = false })
    }

    if (showSelfCheck) {
        SelfCheckDialog(onDismiss = { showSelfCheck = false })
    }

    if (showClearAll) {
        androidx.compose.material3.AlertDialog(
            shape = RoundedCornerShape(24.dp),
            onDismissRequest = { showClearAll = false },
            title = { Text("清空全部数据") },
            text = {
                Text("将删除全部账单记录和所有设置（分类规则、屏蔽词、预算、账本、应用锁等），无法恢复。\n\n建议先做一次「完整备份」，留个后路。")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearAll = false
                        showClearAllFinal = true
                    }
                ) { Text("继续") }
            },
            dismissButton = {
                TextButton(onClick = { showClearAll = false }) { Text("取消") }
            }
        )
    }

    if (showClearAllFinal) {
        androidx.compose.material3.AlertDialog(
            shape = RoundedCornerShape(24.dp),
            onDismissRequest = { showClearAllFinal = false },
            title = { Text("最后确认") },
            text = {
                Text("真的要清空吗？此操作无法撤销。")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearAllFinal = false
                        scope.launch(Dispatchers.IO) {
                            try {
                                AutoBookApp.instance.repository.replaceAll(emptyList())
                                com.autobook.app.util.AppSettings.clearAllSettings(context)
                                resultMessage = "已清空全部数据"
                            } catch (e: Exception) {
                                resultMessage = "清空失败：${e.message}"
                            }
                        }
                    }
                ) { Text("清空", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showClearAllFinal = false }) { Text("取消") }
            }
        )
    }

    if (showCrashLog) {
        var crashText by remember { mutableStateOf<String?>(null) }
        androidx.compose.runtime.LaunchedEffect(Unit) {
            crashText = kotlinx.coroutines.withContext(Dispatchers.IO) {
                val dir = context.getExternalFilesDir(null) ?: context.filesDir
                val file = java.io.File(dir, "crash_log.txt")
                if (file.exists()) file.readText().take(4000) else null
            }
        }
        androidx.compose.material3.AlertDialog(
            shape = RoundedCornerShape(24.dp),
            onDismissRequest = { showCrashLog = false },
            title = { Text("崩溃日志") },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    val content = crashText
                    if (content == null) {
                        Text(
                            text = "没有崩溃记录",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        androidx.compose.foundation.text.selection.SelectionContainer {
                            Text(
                                text = content,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Row {
                    crashText?.let { text ->
                        TextButton(
                            onClick = {
                                clipboard.setText(
                                    androidx.compose.ui.text.AnnotatedString(text)
                                )
                                showCrashLog = false
                                resultMessage = "崩溃日志已复制"
                            }
                        ) { Text("复制", fontSize = 11.sp) }
                    }
                    TextButton(onClick = { showCrashLog = false }) { Text("关闭") }
                }
            }
        )
    }

    if (showCustomColor) {
        CustomColorDialog(
            initial = themeColor,
            onConfirm = { c ->
                viewModel.setThemeColor(c)
                showCustomColor = false
            },
            onDismiss = { showCustomColor = false }
        )
    }

    if (showSetPin) {
        var pinStep by remember { mutableIntStateOf(0) }
        var pinFirst by remember { mutableStateOf("") }
        var pinSecond by remember { mutableStateOf("") }
        androidx.compose.material3.AlertDialog(
            shape = RoundedCornerShape(24.dp),
            onDismissRequest = { showSetPin = false },
            title = {
                Text(if (pinStep == 0) "设置 4 位密码" else "再次输入确认")
            },
            text = {
                Column {
                    androidx.compose.material3.OutlinedTextField(
                        value = if (pinStep == 0) pinFirst else pinSecond,
                        onValueChange = { v ->
                            val filtered = v.filter { it.isDigit() }.take(4)
                            if (pinStep == 0) pinFirst = filtered else pinSecond = filtered
                        },
                        label = { Text("4 位数字密码") },
                        singleLine = true,
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.NumberPassword
                        )
                    )
                    if (pinStep == 1 && pinSecond.length == 4 && pinFirst != pinSecond) {
                        Text(
                            text = "两次输入不一致",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = if (pinStep == 0) {
                        pinFirst.length == 4
                    } else {
                        pinSecond.length == 4 && pinFirst == pinSecond
                    },
                    onClick = {
                        if (pinStep == 0) {
                            pinStep = 1
                        } else {
                            com.autobook.app.util.AppSettings.setLockPin(context, pinFirst)
                            com.autobook.app.util.AppSettings.setAppLockEnabled(context, true)
                            lockEnabled = true
                            showSetPin = false
                        }
                    }
                ) { Text(if (pinStep == 0) "下一步" else "开启") }
            },
            dismissButton = {
                TextButton(onClick = { showSetPin = false }) { Text("取消") }
            }
        )
    }

    if (showVerifyPin) {
        var verifyInput by remember { mutableStateOf("") }
        var verifyError by remember { mutableStateOf(false) }
        androidx.compose.material3.AlertDialog(
            shape = RoundedCornerShape(24.dp),
            onDismissRequest = { showVerifyPin = false },
            title = { Text("关闭应用锁") },
            text = {
                Column {
                    Text(
                        text = "输入当前密码以关闭",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    androidx.compose.material3.OutlinedTextField(
                        value = verifyInput,
                        onValueChange = { v ->
                            verifyInput = v.filter { it.isDigit() }.take(4)
                            verifyError = false
                        },
                        label = { Text("4 位数字密码") },
                        singleLine = true,
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.NumberPassword
                        )
                    )
                    if (verifyError) {
                        Text(
                            text = "密码错误",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = verifyInput.length == 4,
                    onClick = {
                        if (com.autobook.app.util.AppSettings.verifyLockPin(context, verifyInput)) {
                            com.autobook.app.util.AppSettings.setAppLockEnabled(context, false)
                            lockEnabled = false
                            showVerifyPin = false
                        } else {
                            verifyError = true
                            verifyInput = ""
                        }
                    }
                ) { Text("关闭") }
            },
            dismissButton = {
                TextButton(onClick = { showVerifyPin = false }) { Text("取消") }
            }
        )
    }

    restorePending?.let { pending ->
        androidx.compose.material3.AlertDialog(
            shape = RoundedCornerShape(24.dp),
            onDismissRequest = { restorePending = null },
            title = { Text("恢复备份") },
            text = {
                Text("恢复会覆盖当前全部账单和设置（分类规则、预算、应用锁等），确定继续吗？")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch(Dispatchers.IO) {
                            try {
                                val json = org.json.JSONObject(pending)
                                val txs = CsvCodec.decode(json.getString("csv"))
                                AutoBookApp.instance.repository.replaceAll(txs)
                                com.autobook.app.util.AppSettings.importAllSettings(
                                    context,
                                    json.getString("settings")
                                )
                                resultMessage = "恢复完成：${txs.size} 笔记录"
                            } catch (e: Exception) {
                                resultMessage = "恢复失败：${e.message}"
                            }
                            restorePending = null
                        }
                    }
                ) { Text("恢复", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { restorePending = null }) { Text("取消") }
            }
        )
    }

    if (showExport) {
        ExportDialog(
            onDismiss = { showExport = false },
            onExport = { book, year ->
                showExport = false
                scope.launch(Dispatchers.IO) {
                    try {
                        val all = AutoBookApp.instance.repository.getAllOnce()
                        val filtered = all.filter { tx ->
                            (book == null || tx.book == book) &&
                                (year == null ||
                                    java.util.Calendar.getInstance().apply {
                                        timeInMillis = tx.timestamp
                                    }.get(java.util.Calendar.YEAR) == year)
                        }
                        val csv = CsvCodec.encode(filtered)
                        val date = java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.CHINA)
                            .format(java.util.Date())
                        val name = when {
                            book != null -> "记账数据_${book}_$date.csv"
                            year != null -> "记账数据_${year}年_$date.csv"
                            else -> "记账数据_$date.csv"
                        }
                        exportPendingCsv = csv
                        exportPendingCount = filtered.size
                        kotlinx.coroutines.withContext(Dispatchers.Main) {
                            createDocLauncher.launch(name)
                        }
                    } catch (e: Exception) {
                        resultMessage = "导出失败：${e.message}"
                    }
                }
            }
        )
    }
}

@Composable
private fun CustomColorDialog(
    initial: Long,
    onConfirm: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    var r by remember { mutableStateOf(((initial shr 16) and 0xFFL).toInt().toFloat()) }
    var g by remember { mutableStateOf(((initial shr 8) and 0xFFL).toInt().toFloat()) }
    var b by remember { mutableStateOf((initial and 0xFFL).toInt().toFloat()) }
    val preview = androidx.compose.ui.graphics.Color(r / 255f, g / 255f, b / 255f)

    AlertDialog(
        shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
        onDismissRequest = onDismiss,
        title = { Text("自定义颜色") },
        text = {
            Column {
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(preview)
                )
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("红", modifier = Modifier.width(32.dp))
                    androidx.compose.material3.Slider(
                        value = r,
                        onValueChange = { r = it },
                        valueRange = 0f..255f,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("绿", modifier = Modifier.width(32.dp))
                    androidx.compose.material3.Slider(
                        value = g,
                        onValueChange = { g = it },
                        valueRange = 0f..255f,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("蓝", modifier = Modifier.width(32.dp))
                    androidx.compose.material3.Slider(
                        value = b,
                        onValueChange = { b = it },
                        valueRange = 0f..255f,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val color = 0xFF000000L or
                        (r.toInt() shl 16).toLong() or
                        (g.toInt() shl 8).toLong() or
                        b.toInt().toLong()
                    onConfirm(color)
                }
            ) { Text("确定") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

@Composable
private fun PermissionRow(
    title: String,
    granted: Boolean,
    actionLabel: String?,
    onAction: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = if (granted) "✓" else "✗",
            color = if (granted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(24.dp)
        )
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        if (actionLabel != null) {
            OutlinedButton(onClick = onAction) {
                Text(actionLabel)
            }
        } else {
            Text(
                text = "已开启",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}


private fun decodeBytes(bytes: ByteArray): String {
    if (bytes.isEmpty()) return ""
    val text = try {
        Charsets.UTF_8.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
            .decode(ByteBuffer.wrap(bytes))
            .toString()
    } catch (_: Exception) {
        String(bytes, charset("GBK"))
    }
    return text.removePrefix("\uFEFF")
}

@Composable
private fun StatItem(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Column(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(12.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold
        )
    }
}

private const val MAX_IMPORT_BYTES = 10 * 1024 * 1024

private fun readLimited(context: Context, uri: android.net.Uri): ByteArray? {
    return context.contentResolver.openInputStream(uri)?.use { input ->
        val buffer = java.io.ByteArrayOutputStream()
        val chunk = ByteArray(8192)
        var total = 0
        while (true) {
            val n = input.read(chunk)
            if (n < 0) break
            total += n
            if (total > MAX_IMPORT_BYTES) return null
            buffer.write(chunk, 0, n)
        }
        buffer.toByteArray()
    }
}


