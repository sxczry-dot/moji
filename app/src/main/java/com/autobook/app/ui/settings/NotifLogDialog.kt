package com.autobook.app.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.autobook.app.util.AppSettings

@Composable
fun NotifLogDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val clipboard = androidx.compose.ui.platform.LocalClipboardManager.current
    var logs by remember { mutableStateOf(AppSettings.getNotifLog(context)) }
    var logFilter by remember { mutableIntStateOf(0) }

    val visibleLogs = when (logFilter) {
        1 -> logs.filter { it.contains("未匹配") }
        2 -> logs.filter { it.contains("去重") }
        3 -> logs.filter { it.contains("已记账") }
        4 -> logs.filter { it.contains("已屏蔽") }
        else -> logs
    }

    AlertDialog(
        shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
        onDismissRequest = onDismiss,
        title = { Text("通知日志") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                androidx.compose.foundation.layout.Row {
                    listOf("全部", "未匹配", "去重", "已记账", "已屏蔽").forEachIndexed { index, label ->
                        androidx.compose.material3.FilterChip(
                            selected = logFilter == index,
                            onClick = { logFilter = index },
                            label = { Text(label) },
                            modifier = Modifier.padding(end = 4.dp)
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
                if (logs.isEmpty()) {
                    Text(
                        text = "还没有收到微信/支付宝/云闪付的通知。\n付款后回来这里查看。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    SelectionContainer {
                        Column {
                            visibleLogs.groupBy { it.take(5) }.forEach { (day, lines) ->
                                Text(
                                    text = day,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
                                )
                                lines.forEach { line ->
                                    Text(
                                        text = line,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                        ),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("关闭") }
        },
        dismissButton = {
            Row {
                TextButton(
                    onClick = {
                        // 复制当前筛选下的内容，与所见一致
                        clipboard.setText(
                            androidx.compose.ui.text.AnnotatedString(visibleLogs.joinToString("\n"))
                        )
                    }
                ) { Text("复制") }
                TextButton(
                    onClick = {
                        AppSettings.clearNotifLog(context)
                        logs = emptyList()
                    }
                ) { Text("清空") }
            }
        }
    )
}
