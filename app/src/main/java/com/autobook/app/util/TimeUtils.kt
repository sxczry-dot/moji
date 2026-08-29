package com.autobook.app.util

import java.util.Calendar

object TimeUtils {

    fun monthRange(year: Int, month: Int): Pair<Long, Long> {
        val cal = Calendar.getInstance().apply {
            clear()
            set(year, month, 1, 0, 0, 0)
        }
        val start = cal.timeInMillis
        cal.add(Calendar.MONTH, 1)
        return start to cal.timeInMillis
    }

    fun currentMonthRange(): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        return monthRange(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH))
    }

    fun monthRangeOf(timestamp: Long): Pair<Long, Long> {
        val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
        return monthRange(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH))
    }

    fun dayStartOf(timestamp: Long): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = timestamp
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    fun formatShortTime(timestamp: Long): String {
        val f = java.text.SimpleDateFormat("MM-dd HH:mm", java.util.Locale.CHINA)
        return f.format(java.util.Date(timestamp))
    }
}
