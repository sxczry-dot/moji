package com.autobook.app.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Balance
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.autobook.app.data.localTime
import com.autobook.app.util.formatAmountGrouped

/**
 * 余额对账：基于银行短信余额快照校验流水完整性，
 * 列出"预计余额 ≠ 实际余额"的日期，提示可能漏记/错记。
 */
@Composable
fun ReconciliationDialog(onDismiss: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var mismatches by remember {
        mutableStateOf<List<ReconciliationCalculator.Mismatch>>(emptyList())
    }
    var snapshotDays by remember { mutableStateOf(0) }
    var loaded by remember { mutableStateOf(false) }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        val txs = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            com.autobook.app.AutoBookApp.instance.repository.getAllOnce()
        }
        val snapshots = txs.count { it.balanceAfter > 0 }
        snapshotDays = txs
            .filter { it.balanceAfter > 0 }
            .map {
                java.time.Instant.ofEpochMilli(it.localTime())
                    .atZone(java.time.ZoneId.systemDefault()).toLocalDate()
            }
            .distinct().size
        mismatches = ReconciliationCalculator.analyze(txs)
        loaded = true
    }

    AlertDialog(
        shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
        onDismissRequest = onDismiss,
        title = { Text("余额对账") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = if (!loaded) {
                        "分析中…"
                    } else if (snapshotDays < 2) {
                        "还没有足够的余额快照。\n\n银行短信里带「余额XXX元」的流水会自动记录快照，积累几天后即可对账。"
                    } else if (mismatches.isEmpty()) {
                        "✅ 已校验 $snapshotDays 天的余额快照，账目全部对得上。"
                    } else {
                        "已校验 $snapshotDays 天的余额快照，发现 ${mismatches.size} 天账目对不上（可能漏记或错记）："
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                mismatches.forEach { m ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        androidx.compose.material3.Icon(
                            Icons.Outlined.Balance,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.width(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "${m.day.monthValue}月${m.day.dayOfMonth}日：预计 ¥${formatAmountGrouped(m.expected)}，实际 ¥${formatAmountGrouped(m.actual)}",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = "差异 ${if (m.diff > 0) "+" else ""}¥${formatAmountGrouped(m.diff)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
                if (loaded && snapshotDays >= 2) {
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        androidx.compose.material3.Icon(
                            Icons.Outlined.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.width(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "小提示：差异为正数通常是漏记了支出，负数通常是漏记了收入。",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("关闭") }
        }
    )
}
