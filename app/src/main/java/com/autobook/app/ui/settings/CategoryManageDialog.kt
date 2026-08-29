package com.autobook.app.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
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
import com.autobook.app.data.Categories
import com.autobook.app.util.AppSettings

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CategoryManageDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    var selected by remember { mutableStateOf(Categories.FOOD) }
    var input by remember { mutableStateOf("") }
    var custom by remember { mutableStateOf(AppSettings.getCustomChildren(context, selected)) }

    AlertDialog(
        shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
        onDismissRequest = onDismiss,
        title = { Text("分类管理") },
        text = {
            Column {
                Text(
                    text = "选择一级分类，管理其二级子分类",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                FlowRow {
                    Categories.expenseGroups.forEach { group ->
                        FilterChip(
                            selected = selected == group.name,
                            onClick = {
                                selected = group.name
                                custom = AppSettings.getCustomChildren(context, group.name)
                            },
                            label = { Text(group.name) },
                            modifier = Modifier.padding(end = 4.dp)
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "内置子分类",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                FlowRow {
                    Categories.childrenOf(selected).forEach { child ->
                        FilterChip(
                            selected = false,
                            onClick = {},
                            label = { Text(child) },
                            modifier = Modifier.padding(end = 4.dp)
                        )
                    }
                }
                if (custom.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "自定义子分类",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(4.dp))
                    FlowRow {
                        custom.forEach { child ->
                            FilterChip(
                                selected = true,
                                onClick = {
                                    AppSettings.removeCustomChild(context, selected, child)
                                    custom = AppSettings.getCustomChildren(context, selected)
                                },
                                label = { Text("$child ✕") },
                                modifier = Modifier.padding(end = 4.dp)
                            )
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = input,
                        onValueChange = { input = it },
                        label = { Text("添加子分类") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(
                        onClick = {
                            val name = input.trim()
                            if (name.isNotEmpty()) {
                                AppSettings.addCustomChild(context, selected, name)
                                custom = AppSettings.getCustomChildren(context, selected)
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
