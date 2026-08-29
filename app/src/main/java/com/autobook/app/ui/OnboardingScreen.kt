package com.autobook.app.ui

import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.outlined.BatteryChargingFull
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Sms
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.runtime.getValue
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
import com.autobook.app.util.AppSettings
import com.autobook.app.util.Permissions

@Composable
fun OnboardingScreen(onDone: () -> Unit) {
    val context = LocalContext.current
    var listenerEnabled by remember { mutableStateOf(Permissions.isNotificationListenerEnabled(context)) }
    var smsGranted by remember { mutableStateOf(Permissions.isSmsGranted(context)) }
    var notifGranted by remember { mutableStateOf(Permissions.isNotifGranted(context)) }
    var batteryIgnored by remember { mutableStateOf(Permissions.isIgnoringBatteryOptimizations(context)) }

    LifecycleResumeEffect(Unit) {
        listenerEnabled = Permissions.isNotificationListenerEnabled(context)
        smsGranted = Permissions.isSmsGranted(context)
        notifGranted = Permissions.isNotifGranted(context)
        batteryIgnored = Permissions.isIgnoringBatteryOptimizations(context)
        onPauseOrDispose { }
    }

    val smsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        smsGranted = Permissions.isSmsGranted(context)
        notifGranted = Permissions.isNotifGranted(context)
    }

    val allGranted = listenerEnabled && smsGranted && notifGranted && batteryIgnored
    LaunchedEffect(allGranted) {
        if (allGranted) {
            AppSettings.setOnboarded(context, true)
            onDone()
        }
    }
    var showDemo by androidx.compose.runtime.remember {
        androidx.compose.runtime.mutableStateOf(false)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "默记",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "付款自动记账，安静不打扰",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(32.dp))

        PermissionItem(
            title = "通知使用权",
            desc = "读取微信、支付宝、银行 App 的付款通知",
            granted = listenerEnabled,
            icon = androidx.compose.material.icons.Icons.Outlined.Security
        ) {
            context.startActivity(
                Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
        PermissionItem(
            title = "短信权限",
            desc = "读取银行消费短信，刷卡消费也能自动记账",
            granted = smsGranted,
            icon = androidx.compose.material.icons.Icons.Outlined.Sms
        ) {
            val perms = Permissions.missingRuntimePermissions(context)
            if (perms.isNotEmpty()) {
                smsLauncher.launch(perms.toTypedArray())
            }
        }
        PermissionItem(
            title = "通知权限",
            desc = "显示记账确认通知和运行状态",
            granted = notifGranted,
            icon = androidx.compose.material.icons.Icons.Outlined.Notifications
        ) {
            val perms = Permissions.missingRuntimePermissions(context)
            if (perms.isNotEmpty()) {
                smsLauncher.launch(perms.toTypedArray())
            }
        }
        PermissionItem(
            title = "后台运行保护",
            desc = "防止手机系统关闭默记，自动记账持续工作",
            granted = batteryIgnored,
            icon = androidx.compose.material.icons.Icons.Outlined.BatteryChargingFull
        ) {
            context.startActivity(
                Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = android.net.Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            )
        }

        Spacer(Modifier.height(24.dp))
        val glow = androidx.compose.animation.core.rememberInfiniteTransition(
            label = "onboard_glow"
        )
        val glowT by glow.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = androidx.compose.animation.core.infiniteRepeatable(
                animation = androidx.compose.animation.core.tween(
                    1400,
                    easing = androidx.compose.animation.core.FastOutSlowInEasing
                ),
                repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
            ),
            label = "glow_t"
        )
        val glowPrimary = MaterialTheme.colorScheme.primary
        val glowDeep = com.autobook.app.ui.theme.darker(glowPrimary, 0.75f)
        Button(
            onClick = {
                AppSettings.setOnboarded(context, true)
                onDone()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(26.dp),
            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                containerColor = androidx.compose.ui.graphics.Color.Transparent,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        androidx.compose.ui.graphics.Brush.verticalGradient(
                            colors = listOf(
                                androidx.compose.ui.graphics.lerp(glowPrimary, glowDeep, glowT),
                                androidx.compose.ui.graphics.lerp(glowDeep, glowPrimary, glowT)
                            )
                        ),
                        RoundedCornerShape(26.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(if (allGranted) "进入默记" else "回头再说，先进入")
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = "权限全部开启后会自动进入；以后也能在「设置」里随时补开。",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = { showDemo = true }) {
            Text("看看自动记账是什么样", fontSize = 12.sp)
        }
    }

    if (showDemo) {
        var step by androidx.compose.runtime.remember {
            androidx.compose.runtime.mutableIntStateOf(0)
        }
        LaunchedEffect(Unit) {
            kotlinx.coroutines.delay(900)
            step = 1
            kotlinx.coroutines.delay(900)
            step = 2
        }
        androidx.compose.material3.AlertDialog(
            shape = RoundedCornerShape(24.dp),
            onDismissRequest = { showDemo = false },
            title = { Text("自动记账演示") },
            text = {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "1",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(
                            "收到付款通知：微信支付 ¥25.00",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    androidx.compose.animation.AnimatedVisibility(visible = step >= 1) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "2",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Text(
                                "自动识别：餐饮美食 · 微信渠道",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    androidx.compose.animation.AnimatedVisibility(visible = step >= 2) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "3",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Text(
                                "已记入账单 ✓（全程不用打开 App）",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDemo = false }) { Text("知道了") }
            }
        )
    }
}

@Composable
private fun PermissionItem(
    title: String,
    desc: String,
    granted: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onAction: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            androidx.compose.material3.Icon(
                icon,
                contentDescription = null,
                tint = if (granted) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.padding(6.dp))
            Text(
                text = if (granted) "✓" else "✗",
                color = if (granted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.padding(4.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            if (granted) {
                Text(
                    text = "已开启",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            } else {
                OutlinedButton(onClick = onAction) { Text("去开启") }
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = desc,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
