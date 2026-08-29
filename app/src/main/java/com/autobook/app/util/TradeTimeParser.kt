package com.autobook.app.util

import java.util.Calendar

object TradeTimeParser {

    // 支持格式：08月24日09:23、8月24日 09:23、09:23、08-24 09:23、08/24 09:23
    private val fullRegex = Regex("""(\d{1,2})[月\-/.](\d{1,2})[日]?[\s\-]*(\d{1,2})[:：](\d{1,2})""")
    private val shortRegex = Regex("""(?:^|[\s，,（(])(\d{1,2})[:：](\d{1,2})(?=[\s，,）)：]|$)""")

    fun parse(text: String, reference: Long = System.currentTimeMillis()): Long {
        val cal = Calendar.getInstance().apply { timeInMillis = reference }
        val nowYear = cal.get(Calendar.YEAR)
        val nowMonth = cal.get(Calendar.MONTH)
        val nowDay = cal.get(Calendar.DAY_OF_MONTH)

        val full = fullRegex.find(text)
        if (full != null) {
            val month = full.groupValues[1].toIntOrNull() ?: return 0L
            val day = full.groupValues[2].toIntOrNull() ?: return 0L
            val hour = full.groupValues[3].toIntOrNull() ?: return 0L
            val minute = full.groupValues[4].toIntOrNull() ?: return 0L
            if (month !in 1..12 || day !in 1..31 || hour !in 0..23 || minute !in 0..59) return 0L
            var year = nowYear
            if (month > nowMonth + 1 || (month == nowMonth + 1 && day > nowDay)) {
                year -= 1
            }
            val result = Calendar.getInstance().apply {
                clear()
                isLenient = false
                try {
                    set(year, month - 1, day, hour, minute, 0)
                } catch (_: Exception) {
                    return@apply
                }
            }
            val dayValid = try {
                result.get(Calendar.DAY_OF_MONTH) == day
            } catch (_: Exception) {
                false
            }
            if (!dayValid) return 0L
            return result.timeInMillis
        }

        val short = shortRegex.find(text)
        if (short != null) {
            val hour = short.groupValues[1].toIntOrNull() ?: return 0L
            val minute = short.groupValues[2].toIntOrNull() ?: return 0L
            if (hour !in 0..23 || minute !in 0..59) return 0L
            val result = Calendar.getInstance().apply {
                timeInMillis = reference
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            if (result.timeInMillis > reference + 60_000) {
                result.add(Calendar.DAY_OF_YEAR, -1)
            }
            return result.timeInMillis
        }
        return 0L
    }
}
