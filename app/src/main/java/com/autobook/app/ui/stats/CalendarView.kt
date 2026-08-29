package com.autobook.app.ui.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.autobook.app.util.formatCategory
import androidx.compose.ui.unit.sp
import com.autobook.app.data.Transaction
import com.autobook.app.data.expenseSum
import com.autobook.app.data.incomeSum
import com.autobook.app.util.formatAmount
import com.autobook.app.util.formatDay
import com.autobook.app.ui.theme.ExpenseRed
import com.autobook.app.ui.theme.IncomeBlue
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId

private fun monthOf(timestamp: Long): Int =
    Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).monthValue

private fun dayOf(timestamp: Long): Int =
    Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).dayOfMonth

@Composable
fun CalendarMonthView(
    txs: List<Transaction>,
    yearMonth: YearMonth,
    yearMode: Boolean = false
) {
    var selectedDay by remember { mutableStateOf<Int?>(null) }
    var selectedMonth by remember { mutableStateOf<Int?>(null) }

    if (yearMode) {
        val monthGrids = remember(txs) {
            (1..12).map { m -> m to txs.filter { monthOf(it.timestamp) == m } }
        }
        Column {
            monthGrids.forEach { (m, monthList) ->
                Text(
                    text = "${m}月",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                )
                MonthGrid(
                    txs = monthList,
                    yearMonth = YearMonth.of(yearMonth.year, m),
                    onDayClick = { day ->
                        selectedDay = day
                        selectedMonth = m
                    }
                )
            }
        }
    } else {
        MonthGrid(
            txs = txs,
            yearMonth = yearMonth,
            onDayClick = { day -> selectedDay = day }
        )
    }

    if (selectedDay != null) {
        val list = if (yearMode && selectedMonth != null) {
            txs.filter { monthOf(it.timestamp) == selectedMonth && dayOf(it.timestamp) == selectedDay }
        } else {
            txs.filter { dayOf(it.timestamp) == selectedDay }
        }
        DayDetailDialog(
            dayTxs = list,
            onDismiss = {
                selectedDay = null
                selectedMonth = null
            }
        )
    }
}

@Composable
private fun MonthGrid(
    txs: List<Transaction>,
    yearMonth: YearMonth,
    onDayClick: (Int) -> Unit
) {
    val daysInMonth = yearMonth.lengthOfMonth()
    val firstDayOfWeek = yearMonth.atDay(1).dayOfWeek.value
    val offset = (firstDayOfWeek + 6) % 7
    val today = java.time.LocalDate.now()
    val isCurrentMonth = today.year == yearMonth.year && today.monthValue == yearMonth.monthValue

    val dayExpense = remember(txs) {
        txs.filter { it.type == Transaction.TYPE_EXPENSE }
            .groupBy { dayOf(it.timestamp) }
            .mapValues { (_, list) -> list.sumOf { it.amount } }
    }
    val dayIncome = remember(txs) {
        txs.filter { it.type == Transaction.TYPE_INCOME }
            .groupBy { dayOf(it.timestamp) }
            .mapValues { (_, list) -> list.sumOf { it.amount } }
    }
    val hasData = dayExpense.keys + dayIncome.keys
    val maxExpense = dayExpense.values.maxOrNull() ?: 0.0

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Row(Modifier.fillMaxWidth()) {
            listOf("一", "二", "三", "四", "五", "六", "日").forEach { label ->
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        for (week in 0 until 6) {
            Row(Modifier.fillMaxWidth()) {
                for (d in 0 until 7) {
                    val dayNum = week * 7 + d - offset + 1
                    Box(Modifier.weight(1f)) {
                        if (dayNum in 1..daysInMonth) {
                            val isToday = isCurrentMonth && today.dayOfMonth == dayNum
                            DayCell(
                                day = dayNum,
                                expense = dayExpense[dayNum] ?: 0.0,
                                income = dayIncome[dayNum] ?: 0.0,
                                isToday = isToday,
                                heat = if (maxExpense > 0) {
                                    ((dayExpense[dayNum] ?: 0.0) / maxExpense).toFloat()
                                } else 0f,
                                onClick = { if (dayNum in hasData) onDayClick(dayNum) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DayDetailDialog(
    dayTxs: List<Transaction>,
    onDismiss: () -> Unit
) {
    AlertDialog(
        shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
        onDismissRequest = onDismiss,
        title = { Text(formatDay(dayTxs.firstOrNull()?.timestamp ?: System.currentTimeMillis())) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                dayTxs.sortedByDescending { it.timestamp }.forEach { tx ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = tx.merchant,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = com.autobook.app.util.formatCategory(tx.category),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = (if (tx.type == Transaction.TYPE_EXPENSE) "-" else "+") +
                                formatAmount(tx.amount),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = if (tx.type == Transaction.TYPE_EXPENSE) ExpenseRed else IncomeBlue
                        )
                    }
                }
                Text(
                    text = "支出 ¥${formatAmount(dayTxs.expenseSum())} · 收入 ¥${formatAmount(dayTxs.incomeSum())}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("关闭") }
        }
    )
}

@Composable
private fun DayCell(
    day: Int,
    expense: Double,
    income: Double,
    isToday: Boolean,
    heat: Float = 0f,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(
                when {
                    isToday -> MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
                    heat > 0f -> MaterialTheme.colorScheme.primary.copy(
                        alpha = 0.10f + heat * 0.55f
                    )
                    income > 0 -> MaterialTheme.colorScheme.surfaceVariant
                    else -> Color.Transparent
                }
            )
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = day.toString(),
            fontSize = 11.sp,
            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal
        )
        if (expense > 0) {
            Text(
                text = formatAmount(expense),
                fontSize = 8.sp,
                color = ExpenseRed,
                maxLines = 1
            )
        }
        if (income > 0) {
            Text(
                text = formatAmount(income),
                fontSize = 8.sp,
                color = IncomeBlue,
                maxLines = 1
            )
        }
    }
}
