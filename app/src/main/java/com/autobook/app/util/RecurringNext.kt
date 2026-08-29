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
            2 -> "下次：${nowYear}年${nowMonth + 1}月${now.getActualMaximum(Calendar.DAY_OF_MONTH)}日"
            else -> {
                val m = rule.day / 100
                val d = rule.day % 100
                val year = if (nowMonth + 1 > m || (nowMonth + 1 == m && nowDay > d)) {
                    nowYear + 1
                } else {
                    nowYear
                }
                "下次：${year}年${m}月${d}日"
            }
        }
    }
}
