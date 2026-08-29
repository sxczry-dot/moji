package com.autobook.app.ui.settings

import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.autobook.app.AutoBookApp
import com.autobook.app.service.NotificationListenerService
import com.autobook.app.util.AppSettings
import com.autobook.app.util.Permissions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun SelfCheckDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    var listenerEnabled by remember {
        mutableStateOf(Permissions.isNotificationListenerEnabled(context))
    }
    var listenerConnected by remember {
        mutableStateOf(NotificationListenerService.isConnected)
    }
    var smsGranted by remember { mutableStateOf(Permissions.isSmsGranted(context)) }
    var notifGranted by remember { mutableStateOf(Permissions.isNotifGranted(context)) }
    var batteryIgnored by remember {
        mutableStateOf(Permissions.isIgnoringBatteryOptimizations(context))
    }
    var exactAlarm by remember { mutableStateOf(Permissions.canScheduleExactAlarm(context)) }
    var ruleCount by remember { mutableIntStateOf(0) }
    var recordCount by remember { mutableIntStateOf(0) }
    var loaded by remember { mutableStateOf(false) }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            ruleCount = AppSettings.getRecurringRules(context).size
            recordCount = AutoBookApp.instance.repository.getAllOnce().size
        }
        loaded = true
    }

    LifecycleResumeEffect(Unit) {
        listenerEnabled = Permissions.isNotificationListenerEnabled(context)
        listenerConnected = NotificationListenerService.isConnected
        smsGranted = Permissions.isSmsGranted(context)
        notifGranted = Permissions.isNotifGranted(context)
        batteryIgnored = Permissions.isIgnoringBatteryOptimizations(context)
        exactAlarm = Permissions.canScheduleExactAlarm(context)
        onPauseOrDispose { }
    }

    val smsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        smsGranted = Permissions.isSmsGranted(context)
        notifGranted = Permissions.isNotifGranted(context)
    }

    val allOk = listenerEnabled && listenerConnected && smsGranted &&
        notifGranted && batteryIgnored && exactAlarm

    AlertDialog(
        shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
        onDismissRequest = onDismiss,
        title = { Text("自动记账自检") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                if (!loaded) {
                    Text(
                        text = "检查中…",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(
                        text = if (allOk) "全部正常，自动记账正在工作。" else "发现以下问题，点「修复」处理：",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (allOk) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                    Spacer(Modifier.height(8.dp))
                    val items = listOf(
                        Triple("通知使用权", listenerEnabled, {
                            context.startActivity(
                                Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                        }),
                        Triple("监听服务运行中", listenerConnected, {
                            com.autobook.app.service.ServiceGuard.requestRebind(context)
                        }),
                        Triple("短信权限", smsGranted, {
                            val perms = Permissions.missingRuntimePermissions(context)
                            if (perms.isNotEmpty()) {
                                smsLauncher.launch(perms.toTypedArray())
                            }
                        }),
                        Triple("通知权限", notifGranted, {
                            val perms = Permissions.missingRuntimePermissions(context)
                            if (perms.isNotEmpty()) {
                                smsLauncher.launch(perms.toTypedArray())
                            }
                        }),
                        Triple("后台运行保护", batteryIgnored, {
                            context.startActivity(
                                Intent(
                                    Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                                    android.net.Uri.parse("package:${context.packageName}")
                                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                        }),
                        Triple("精确闹钟", exactAlarm, {
                            context.startActivity(
                                Intent(
                                    Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                                    android.net.Uri.parse("package:${context.packageName}")
                                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                        })
                    )
                    val problems = items.filter { !it.second }
                    val oks = items.filter { it.second }
                    if (problems.isNotEmpty()) {
                        Text(
                            text = "需处理",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error
                        )
                        problems.forEach { (title, _, fix) ->
                            CheckRow(title, false, fix)
                        }
                        Spacer(Modifier.height(6.dp))
                    }
                    if (oks.isNotEmpty()) {
                        Text(
                            text = "正常",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        oks.forEach { (title, _, _) ->
                            OkRow(title)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "周期规则 $ruleCount 条 · 账单记录 $recordCount 笔",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("关闭") }
        }
    )
}

@Composable
private fun CheckRow(
    title: String,
    ok: Boolean,
    onFix: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "✗",
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(20.dp)
        )
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        OutlinedButton(
            onClick = onFix,
            modifier = Modifier.height(32.dp)
        ) { Text("修复", fontSize = 12.sp) }
    }
}

@Composable
private fun OkRow(title: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "✓",
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(20.dp)
        )
        Text(
            text = title,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
