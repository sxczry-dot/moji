package com.autobook.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.YearMonth

@Composable
fun MonthPickerLabel(
    yearMonth: YearMonth,
    onSelect: (YearMonth) -> Unit,
    modifier: Modifier = Modifier,
    twoLines: Boolean = false,
    yearOnly: Boolean = false,
    textColor: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color.Unspecified,
    iconColor: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color.Unspecified
) {
    var showDialog by remember { mutableStateOf(false) }

    Row(
        modifier = modifier.clickable { showDialog = true },
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (yearOnly) {
            Text(
                text = "${yearMonth.year}年",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
        } else if (twoLines) {
            Column {
                Text(
                    text = "${yearMonth.year}年",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
                Text(
                    text = "${yearMonth.monthValue}月",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
            }
        } else {
            Text(
                text = "${yearMonth.year}年${yearMonth.monthValue}月",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
        }
        Icon(
            Icons.Filled.KeyboardArrowDown,
            contentDescription = "选择月份",
            tint = if (iconColor == androidx.compose.ui.graphics.Color.Unspecified) {
                MaterialTheme.colorScheme.onSurfaceVariant
            } else {
                iconColor
            },
            modifier = Modifier.size(20.dp)
        )
    }

    if (showDialog) {
        MonthPickerDialog(
            current = yearMonth,
            onSelect = {
                onSelect(it)
                showDialog = false
            },
            onDismiss = { showDialog = false },
            yearOnly = yearOnly
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MonthPickerDialog(
    current: YearMonth,
    onSelect: (YearMonth) -> Unit,
    onDismiss: () -> Unit,
    yearOnly: Boolean = false
) {
    val now = YearMonth.now()
    var year by remember { mutableIntStateOf(current.year) }
    var month by remember { mutableIntStateOf(current.monthValue) }

    AlertDialog(
        shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
        onDismissRequest = onDismiss,
        title = { Text("选择月份") },
        text = {
            Column {
                FlowRow {
                    FilterChip(
                        selected = year == now.year && month == now.monthValue,
                        onClick = {
                            year = now.year
                            month = now.monthValue
                            onSelect(YearMonth.of(year, month))
                        },
                        label = { Text("本月") },
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.padding(end = 6.dp)
                    )
                    (2019..now.year).reversed().forEach { y ->
                        FilterChip(
                            selected = year == y,
                            onClick = {
                                year = y
                                if (yearOnly) onSelect(YearMonth.of(y, 1))
                            },
                            label = { Text("$y") },
                            shape = RoundedCornerShape(50),
                            modifier = Modifier.padding(end = 6.dp)
                        )
                    }
                }
                if (!yearOnly) {
                    Spacer(Modifier.height(12.dp))
                    FlowRow {
                        (1..12).forEach { m ->
                            FilterChip(
                                selected = month == m,
                                onClick = { month = m },
                                label = { Text("${m}月") },
                                shape = RoundedCornerShape(50),
                                modifier = Modifier.padding(end = 6.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSelect(YearMonth.of(year, month)) }) { Text("确定") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}
