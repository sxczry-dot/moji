package com.autobook.app.util

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun formatAmount(amount: Double): String =
    String.format(Locale.CHINA, "%.2f", amount)

fun smallSymbolAmount(
    amount: String,
    numberFontSize: TextUnit,
    color: Color
): AnnotatedString {
    return buildAnnotatedString {
        withStyle(SpanStyle(fontSize = numberFontSize * 0.72f, color = color)) { append("¥") }
        withStyle(SpanStyle(color = color)) { append(amount) }
    }
}

fun formatAmountGrouped(amount: Double): String =
    String.format(Locale.CHINA, "%,.2f", amount)

private val timeFormat = SimpleDateFormat("MM月dd日 HH:mm", Locale.CHINA)
private val dayFormat = SimpleDateFormat("MM月dd日", Locale.CHINA)

fun formatTime(timestamp: Long): String = timeFormat.format(Date(timestamp))

fun formatDay(timestamp: Long): String = dayFormat.format(Date(timestamp))

fun formatDayWithWeek(timestamp: Long): String {
    val cal = java.util.Calendar.getInstance().apply { timeInMillis = timestamp }
    val weekday = when (cal.get(java.util.Calendar.DAY_OF_WEEK)) {
        java.util.Calendar.MONDAY -> "周一"
        java.util.Calendar.TUESDAY -> "周二"
        java.util.Calendar.WEDNESDAY -> "周三"
        java.util.Calendar.THURSDAY -> "周四"
        java.util.Calendar.FRIDAY -> "周五"
        java.util.Calendar.SATURDAY -> "周六"
        else -> "周日"
    }
    return dayFormat.format(Date(timestamp)) + " " + weekday
}

fun filterAmountInput(raw: String): String {
    val filtered = raw.filter { it.isDigit() || it == '.' }
    val firstDot = filtered.indexOf('.')
    return if (firstDot == -1) {
        filtered
    } else {
        filtered.substring(0, firstDot + 1) + filtered.substring(firstDot + 1).replace(".", "")
    }
}

fun formatCategory(category: String): String = com.autobook.app.data.Categories.primaryOf(category)
