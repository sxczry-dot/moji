package com.autobook.app.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.EventRepeat
import androidx.compose.material.icons.outlined.LastPage
import androidx.compose.material.icons.outlined.ViewWeek
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.autobook.app.data.Categories
import com.autobook.app.data.Transaction
import com.autobook.app.util.filterAmountInput
import com.autobook.app.util.AppSettings
import com.autobook.app.util.AppSettings.RecurringRule

@Composable
fun RecurringDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    var rules by remember { mutableStateOf(AppSettings.getRecurringRules(context)) }
    var showAdd by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<RecurringRule?>(null) }
    var editingIndex by remember { mutableIntStateOf(-1) }

    AlertDialog(
        shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
        onDismissRequest = onDismiss,
        title = { Text("周期记账") },
        text = {
            Column {
                if (rules.isEmpty()) {
                    Text(
                        text = "还没有周期规则。添加后，每月到日会自动记一笔（如每月15日房租）。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    rules.forEachIndexed { index, rule ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    editing = rule
                                    editingIndex = index
                                }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = when (rule.period) {
                                    0 -> androidx.compose.material.icons.Icons.Outlined.CalendarMonth
                                    1 -> androidx.compose.material.icons.Icons.Outlined.ViewWeek
                                    2 -> androidx.compose.material.icons.Icons.Outlined.LastPage
                                    else -> androidx.compose.material.icons.Icons.Outlined.EventRepeat
                                },
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Column(Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = when {
                                            rule.period == 0 -> "${rule.name}：每月${rule.day}日 ¥" + com.autobook.app.util.formatAmountGrouped(rule.amount)
                                            rule.period == 1 -> "${rule.name}：每周星期${rule.day} ¥" + com.autobook.app.util.formatAmountGrouped(rule.amount)
                                            rule.period == 2 -> "${rule.name}：每月最后一天 ¥" + com.autobook.app.util.formatAmountGrouped(rule.amount)
                                            else -> "${rule.name}：每年${rule.day / 100}月${rule.day % 100}日 ¥" + com.autobook.app.util.formatAmountGrouped(rule.amount)
                                        },
                                        style = MaterialTheme.typography.bodyMedium,
                                        maxLines = 1,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                    if (rule.paused) {
                                        Spacer(Modifier.width(6.dp))
                                        Box(
                                            modifier = Modifier
                                                .clip(androidx.compose.foundation.shape.RoundedCornerShape(50))
                                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                                .padding(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = "已暂停",
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = if (rule.book != AppSettings.DEFAULT_BOOK) {
                                        "${rule.category} · ${rule.book}"
                                    } else {
                                        rule.category
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = com.autobook.app.util.RecurringNext.nextRunText(rule),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            TextButton(
                                onClick = {
                                    rules = rules.toMutableList().apply {
                                        set(index, rule.copy(paused = !rule.paused))
                                    }
                                    AppSettings.saveRecurringRules(context, rules)
                                },
                                modifier = Modifier.height(32.dp)
                            ) { Text(if (rule.paused) "继续" else "暂停", fontSize = 12.sp) }
                            TextButton(
                                onClick = {
                                    rules = rules.filterIndexed { i, _ -> i != index }
                                    AppSettings.saveRecurringRules(context, rules)
                                },
                                modifier = Modifier.height(32.dp)
                            ) { Text("删除", color = MaterialTheme.colorScheme.error, fontSize = 12.sp) }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = { showAdd = true }) { Text("+ 添加周期规则") }
                    if (rules.isNotEmpty()) {
                        Spacer(Modifier.weight(1f))
                        val allPaused = rules.all { it.paused }
                        TextButton(
                            onClick = {
                                rules = rules.map { it.copy(paused = !allPaused) }
                                AppSettings.saveRecurringRules(context, rules)
                            }
                        ) {
                            Text(
                                if (allPaused) "全部恢复" else "全部暂停",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("关闭") }
        }
    )

    if (showAdd) {
        AddRecurringRuleDialog(
            onConfirm = { rule ->
                rules = rules + rule
                AppSettings.saveRecurringRules(context, rules)
                showAdd = false
            },
            onDismiss = { showAdd = false }
        )
    }

    if (editing != null) {
        AddRecurringRuleDialog(
            initial = editing,
            onConfirm = { rule ->
                rules = rules.toMutableList().apply { set(editingIndex, rule) }
                AppSettings.saveRecurringRules(context, rules)
                editing = null
                editingIndex = -1
            },
            onDismiss = {
                editing = null
                editingIndex = -1
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddRecurringRuleDialog(
    onConfirm: (RecurringRule) -> Unit,
    onDismiss: () -> Unit,
    initial: RecurringRule? = null
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var amountText by remember {
        mutableStateOf(initial?.let { com.autobook.app.util.formatAmount(it.amount) } ?: "")
    }
    var dayText by remember {
        mutableStateOf(
            initial?.day?.takeIf { initial.period != 3 }?.toString() ?: ""
        )
    }
    var type by remember { mutableIntStateOf(initial?.type ?: Transaction.TYPE_EXPENSE) }
    var period by remember { mutableIntStateOf(initial?.period ?: 0) }
    var category by remember {
        mutableStateOf(initial?.category ?: Categories.FOOD)
    }
    var merchant by remember { mutableStateOf(initial?.merchant ?: "") }
    var book by remember {
        mutableStateOf(initial?.book ?: AppSettings.getCurrentBook(context))
    }
    var annualMonth by remember {
        mutableIntStateOf(initial?.takeIf { it.period == 3 }?.let { it.day / 100 } ?: 1)
    }
    var annualDay by remember {
        mutableIntStateOf(initial?.takeIf { it.period == 3 }?.let { it.day % 100 } ?: 1)
    }

    val amount = amountText.toDoubleOrNull() ?: 0.0
    val day = dayText.toIntOrNull() ?: 0
    val valid = name.isNotBlank() && amount > 0 && amount <= Transaction.MAX_AMOUNT && when (period) {
        0 -> day in 1..28
        1 -> day in 1..7
        2 -> true
        else -> annualMonth in 1..12 && annualDay in 1..31
    }

    AlertDialog(
        shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "添加周期规则" else "编辑周期规则") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("名称（如：房租）") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = filterAmountInput(it) },
                        label = { Text("金额") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.padding(4.dp))
                    when (period) {
                        2 -> {}
                        3 -> {
                            OutlinedTextField(
                                value = annualMonth.toString(),
                                onValueChange = {
                                    val v = filterAmountInput(it).toIntOrNull() ?: 0
                                    annualMonth = if (v in 1..12) v else annualMonth
                                },
                                label = { Text("几月") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.weight(0.6f)
                            )
                            Spacer(Modifier.padding(4.dp))
                            OutlinedTextField(
                                value = annualDay.toString(),
                                onValueChange = {
                                    val v = filterAmountInput(it).toIntOrNull() ?: 0
                                    annualDay = if (v in 1..31) v else annualDay
                                },
                                label = { Text("几日") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.weight(0.6f)
                            )
                        }
                        else -> {
                            OutlinedTextField(
                                value = dayText,
                                onValueChange = { dayText = filterAmountInput(it) },
                                label = { Text(if (period == 0) "每月几日" else "星期几(1-7)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                FlowRow {
                    listOf("每月" to 0, "每周" to 1, "月末" to 2, "每年" to 3).forEach { (label, p) ->
                        FilterChip(
                            selected = period == p,
                            onClick = {
                                period = p
                                dayText = ""
                            },
                            label = { Text(label) },
                            modifier = Modifier.padding(end = 4.dp)
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = type == Transaction.TYPE_EXPENSE,
                        onClick = { type = Transaction.TYPE_EXPENSE },
                        shape = SegmentedButtonDefaults.itemShape(0, 2)
                    ) { Text("支出") }
                    SegmentedButton(
                        selected = type == Transaction.TYPE_INCOME,
                        onClick = {
                            type = Transaction.TYPE_INCOME
                            category = Categories.INCOME
                        },
                        shape = SegmentedButtonDefaults.itemShape(1, 2)
                    ) { Text("收入") }
                }
                Spacer(Modifier.height(8.dp))
                if (type == Transaction.TYPE_EXPENSE) {
                    FlowRow {
                        Categories.expenseGroups.forEach { group ->
                            FilterChip(
                                selected = category == group.name,
                                onClick = { category = group.name },
                                label = { Text(group.name) },
                                modifier = Modifier.padding(end = 4.dp)
                            )
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = merchant,
                    onValueChange = { merchant = it },
                    label = { Text("备注（可选）") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "记入账本",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FlowRow {
                    AppSettings.getBooks(context).forEach { b ->
                        FilterChip(
                            selected = book == b,
                            onClick = { book = b },
                            label = { Text(b) },
                            modifier = Modifier.padding(end = 4.dp)
                        )
                    }
                }
                Text(
                    text = when (period) {
                        0 -> "提示：每月 29-31 日请选 28 日，或选「月末」模式"
                        1 -> "提示：1=周一 … 7=周日，错过当天会在本周内补记"
                        2 -> "提示：每月最后一天自动记，自动适配 28/29/30/31 天"
                        else -> "提示：每年这一天自动记一次（如 9 月 3 日）"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (valid) {
                        val ruleDay = when (period) {
                            3 -> annualMonth * 100 + annualDay
                            2 -> 0
                            else -> day
                        }
                        onConfirm(
                            RecurringRule(
                                name = name.trim(),
                                amount = amount,
                                type = type,
                                category = category,
                                day = ruleDay,
                                merchant = merchant.trim(),
                                period = period,
                                book = book,
                                paused = initial?.paused ?: false
                            )
                        )
                    }
                },
                enabled = valid
            ) { Text(if (initial == null) "添加" else "保存") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}
