package com.autobook.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.autobook.app.util.AppSettings

@Composable
fun LockScreen(onUnlocked: () -> Unit) {
    val context = LocalContext.current
    val view = LocalView.current
    var input by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }
    var bioError by remember { mutableStateOf(false) }

    val hasBiometric = remember {
        try {
            androidx.biometric.BiometricManager.from(context)
                .canAuthenticate(androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG) ==
                androidx.biometric.BiometricManager.BIOMETRIC_SUCCESS
        } catch (_: Exception) {
            false
        }
    }

    fun showBiometric() {
        val activity = context as? androidx.fragment.app.FragmentActivity ?: return
        val prompt = androidx.biometric.BiometricPrompt(
            activity,
            androidx.core.content.ContextCompat.getMainExecutor(context),
            object : androidx.biometric.BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(
                    result: androidx.biometric.BiometricPrompt.AuthenticationResult
                ) {
                    onUnlocked()
                }

                override fun onAuthenticationFailed() {
                    bioError = true
                }

                override fun onAuthenticationError(
                    errorCode: Int,
                    errString: CharSequence
                ) {
                    if (errorCode != androidx.biometric.BiometricPrompt.ERROR_NEGATIVE_BUTTON &&
                        errorCode != androidx.biometric.BiometricPrompt.ERROR_USER_CANCELED
                    ) {
                        bioError = true
                    }
                }
            }
        )
        val info = androidx.biometric.BiometricPrompt.PromptInfo.Builder()
            .setTitle("解锁默记")
            .setSubtitle("使用指纹快速解锁")
            .setNegativeButtonText("取消")
            .build()
        try {
            prompt.authenticate(info)
        } catch (_: Exception) {
        }
    }

    LaunchedEffect(input) {
        if (input.length == 4) {
            if (AppSettings.verifyLockPin(context, input)) {
                input = ""
                onUnlocked()
            } else {
                error = true
                input = ""
                try {
                    val vib = context.getSystemService(android.content.Context.VIBRATOR_SERVICE)
                            as android.os.Vibrator
                    vib.vibrate(
                        android.os.VibrationEffect.createOneShot(
                            80,
                            android.os.VibrationEffect.DEFAULT_AMPLITUDE
                        )
                    )
                } catch (_: Exception) {
                }
            }
        } else if (input.isNotEmpty()) {
            // 重新开始输入才清除错误提示（输错后 input 置空不会立刻抹掉"密码错误"）
            error = false
        }
    }

    fun press(key: String) {
        if (key == "del") {
            input = input.dropLast(1)
        } else if (input.length < 4) {
            input += key
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Outlined.Lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(30.dp)
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(
            text = "默记已锁定",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = if (error) "密码错误，请重试" else "输入 4 位密码",
            style = MaterialTheme.typography.bodySmall,
            color = if (error) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
        Spacer(Modifier.height(20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            repeat(4) { i ->
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(
                            if (i < input.length) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.outline
                            }
                        )
                )
            }
        }
        Spacer(Modifier.height(30.dp))
        if (hasBiometric) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface)
                    .clickable { showBiometric() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.Fingerprint,
                    contentDescription = "指纹解锁",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(34.dp)
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = if (bioError) "指纹不匹配，再试一次" else "点按指纹图标快捷解锁",
                style = MaterialTheme.typography.labelSmall,
                color = if (bioError) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
        Spacer(Modifier.height(if (hasBiometric) 24.dp else 36.dp))
        val keys = listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9"),
            listOf("", "0", "del")
        )
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            keys.forEach { rowKeys ->
                Row(horizontalArrangement = Arrangement.spacedBy(26.dp)) {
                    rowKeys.forEach { key ->
                        if (key.isEmpty()) {
                            Spacer(Modifier.size(68.dp))
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(68.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surface)
                                    .clickable { press(key) },
                                contentAlignment = Alignment.Center
                            ) {
                                if (key == "del") {
                                    Icon(
                                        Icons.AutoMirrored.Filled.Backspace,
                                        contentDescription = "删除",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(22.dp)
                                    )
                                } else {
                                    Text(
                                        text = key,
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Text(
            text = "账单数据只保存在本机",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
