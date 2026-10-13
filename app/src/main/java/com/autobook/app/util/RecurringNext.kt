package com.autobook.app.util

import com.autobook.app.util.AppSettings.RecurringRule
import java.util.Calendar

object RecurringNext {

    fun nextRunText(rule: RecurringRule, now: Calendar = Calendar.getInstance()): String {
        if (rule.paused) return "已暂停"
        val nowYear = now.get(Calendar.YEAR)
        val nowMonth = now.get(Calendar.MONTH)
        val nowDay = now.get(Calendar.DAY_OF_MONTH)
        return when (rule.period) {
            0 -> {
                val targetMonth = if (nowDay >= rule.day) nowMonth + 1 else nowMonth
                val cal = (now.clone() as Calendar).apply {
                    clear()
                    set(nowYear, targetMonth, 1)
                }
                val m = cal.get(Calendar.MONTH) + 1
                "下次：${cal.get(Calendar.YEAR)}年${m}月${rule.day}日"
            }
            1 -> {
                val calDay = (rule.day % 7) + 1
                val nowDow = now.get(Calendar.DAY_OF_WEEK)
                val diff = (calDay - nowDow + 7) % 7
                val target = (now.clone() as Calendar).apply {
                    add(Calendar.DAY_OF_YEAR, diff)
                }
                "下次：${target.get(Calendar.YEAR)}年${target.get(Calendar.MONTH) + 1}月${target.get(Calendar.DAY_OF_MONTH)}日"
            }
            2 -> {
                // 今天已过月末 → 显示下月最后一天；否则本月最后一天
                val thisMax = now.getActualMaximum(Calendar.DAY_OF_MONTH)
                val isLastDay = nowDay >= thisMax
                val target = (now.clone() as Calendar).apply {
                    if (isLastDay) {
                        add(Calendar.MONTH, 1)
                        set(Calendar.DAY_OF_MONTH, 1)
                    } else {
                        set(Calendar.DAY_OF_MONTH, thisMax)
                    }
                }
                val d = if (isLastDay) target.getActualMaximum(Calendar.DAY_OF_MONTH) else thisMax
                "下次：${target.get(Calendar.YEAR)}年${target.get(Calendar.MONTH) + 1}月${d}日"
            }
            else -> {
                val m = rule.day / 100
                val d = rule.day % 100
                var year = nowYear
                if (nowMonth + 1 > m || (nowMonth + 1 == m && nowDay >= d)) {
                    year = nowYear + 1
                }
                // 跳过不存在的日期（如非闰年 2 月 29 日顺延到下一个闰年）
                while (!dateValid(year, m, d)) year++
                "下次：${year}年${m}月${d}日"
            }
        }
    }

    private fun dateValid(year: Int, month: Int, day: Int): Boolean {
        val cal = Calendar.getInstance().apply {
            isLenient = false
            clear()
            set(year, month - 1, day)
        }
        return try {
            cal.timeInMillis
            true
        } catch (_: Exception) {
            false
        }
    }
}
