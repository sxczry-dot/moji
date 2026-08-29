package com.autobook.app.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.autobook.app.util.AppSettings

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BlockWordsDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    var words by remember { mutableStateOf(AppSettings.getBlockWords(context)) }
    var input by remember { mutableStateOf("") }

    AlertDialog(
        shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
        onDismissRequest = onDismiss,
        title = { Text("自动记账屏蔽词") },
        text = {
            Column {
                Text(
                    text = "自动记账时，通知或短信内容包含以下任一词的记录将被跳过，不记入账单。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                if (words.isEmpty()) {
                    Text(
                        text = "还没有屏蔽词",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    androidx.compose.foundation.layout.FlowRow {
                        words.forEach { word ->
                            androidx.compose.material3.InputChip(
                                selected = false,
                                onClick = { },
                                label = { Text(word, fontSize = 12.sp) },
                                trailingIcon = {
                                    androidx.compose.material3.Icon(
                                        Icons.Filled.Close,
                                        contentDescription = "删除",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier
                                            .size(14.dp)
                                            .clickable {
                                                words = words.filter { it != word }
                                                AppSettings.saveBlockWords(context, words)
                                            }
                                    )
                                },
                                modifier = Modifier.padding(end = 4.dp, bottom = 4.dp)
                            )
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = input,
                        onValueChange = { input = it },
                        label = { Text("输入屏蔽词") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(
                        onClick = {
                            val w = input.trim()
                            if (w.isNotEmpty() && w !in words) {
                                words = words + w
                                AppSettings.saveBlockWords(context, words)
                                input = ""
                            }
                        }
                    ) { Text("添加") }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("关闭") }
        }
    )
}
