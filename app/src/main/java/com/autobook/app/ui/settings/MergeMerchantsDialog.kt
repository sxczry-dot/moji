package com.autobook.app.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.autobook.app.AutoBookApp
import com.autobook.app.util.AppSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MergeMerchantsDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var merchants by remember { mutableStateOf<List<Pair<String, Int>>>(emptyList()) }
    var main by remember { mutableStateOf<String?>(null) }
    var selected by remember { mutableStateOf<Set<String>>(emptySet()) }
    var result by remember { mutableStateOf<String?>(null) }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        merchants = withContext(Dispatchers.IO) {
            AutoBookApp.instance.repository.getMerchantCounts(80)
                .map { it.merchant to it.count }
        }
    }

    AlertDialog(
        shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
        onDismissRequest = onDismiss,
        title = { Text("商户合并") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "同一个商户出现多个名字时，选一个主名，把其他名字并入。合并后统计、排行都按主名计算。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "主商户名（点选保留的名字）",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                FlowRow {
                    merchants.forEach { (name, _) ->
                        FilterChip(
                            selected = main == name,
                            onClick = {
                                main = name
                                selected = selected - name
                            },
                            label = { Text(name, fontSize = 11.sp) },
                            modifier = Modifier.padding(end = 4.dp)
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "勾选要并入的其他名字",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                merchants.filter { it.first != main }.forEach { (name, count) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(androidx.compose.foundation.shape.RoundedCornerShape(10.dp))
                            .background(
                                if (name in selected) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    androidx.compose.ui.graphics.Color.Transparent
                                }
                            )
                            .clickable {
                                selected = if (name in selected) {
                                    selected - name
                                } else {
                                    selected + name
                                }
                            }
                            .padding(horizontal = 4.dp, vertical = 1.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = name in selected,
                            onCheckedChange = { checked ->
                                selected = if (checked) selected + name else selected - name
                            }
                        )
                        Text(
                            text = name,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "$count 笔",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = main != null && selected.isNotEmpty(),
                onClick = {
                    val target = main ?: return@TextButton
                    val froms = selected.filter { it != target }
                    if (froms.isEmpty()) return@TextButton
                    scope.launch(Dispatchers.IO) {
                        if (AppSettings.getLearnedCategory(context, target) == null) {
                            froms.firstNotNullOfOrNull {
                                AppSettings.getLearnedCategory(context, it)
                            }?.let { cat -> AppSettings.learnCategory(context, target, com.autobook.app.data.Categories.primaryOf(cat)) }
                        }
                        froms.forEach { from ->
                            AppSettings.removeLearned(context, from)
                        }
                        AutoBookApp.instance.repository.mergeMerchants(froms, target)
                        merchants = AutoBookApp.instance.repository.getMerchantCounts(80)
                            .map { it.merchant to it.count }
                        selected = emptySet()
                        result = "已把 ${froms.size} 个商户名并入「$target」"
                    }
                }
            ) {
                Text(if (selected.isNotEmpty()) "合并 ${selected.size} 个名字" else "合并")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("关闭") }
        }
    )

    result?.let { msg ->
        AlertDialog(
            shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
            onDismissRequest = { result = null },
            title = { Text("合并完成") },
            text = { Text(msg) },
            confirmButton = {
                TextButton(onClick = { result = null }) { Text("好的") }
            }
        )
    }
}
