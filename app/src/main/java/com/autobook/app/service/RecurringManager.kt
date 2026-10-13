package com.autobook.app.service

import android.content.Context
import com.autobook.app.AutoBookApp
import com.autobook.app.data.Transaction
import com.autobook.app.util.AppSettings
import java.util.Calendar

object RecurringManager {

    suspend fun checkAndRun(context: Context) {
        // 与"暂停自动记账"开关语义一致：暂停期间周期记账同样不执行
        if (AppSettings.isAutoPaused(context)) return
        val rules = AppSettings.getRecurringRules(context)
        if (rules.isEmpty()) return
        val books = AppSettings.getBooks(context)
        val cal = Calendar.getInstance()
        val now = cal.timeInMillis
        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH)
        val day = cal.get(Calendar.DAY_OF_MONTH)
        val nowDayOfWeek = cal.get(Calendar.DAY_OF_WEEK)

        val repo = AutoBookApp.instance.repository
        for (rule in rules) {
            if (rule.paused) continue
            // 账本已被删除的规则不再向幽灵账本写记录（重命名/删除账本时已同步迁移规则，这里是兜底）
            if (rule.book !in books) continue
            // 规则 day：每月=几号；每周=1周一..7周日 → Calendar.DAY_OF_WEEK（2周一..1周日）；
            // 每年=月*100+日（如903=9月3日）；月末=忽略 day
            val calDay = (rule.day % 7) + 1
            when (rule.period) {
                0 -> if (day < rule.day) continue
                // 只有"今天就是目标星期几"才执行，避免周日(calDay=1)规则因最小值判定失效而每天触发
                1 -> if ((calDay - nowDayOfWeek + 7) % 7 != 0) continue
                2 -> if (day != cal.getActualMaximum(Calendar.DAY_OF_MONTH)) continue
                3 -> {
                    val ruleMonth = rule.day / 100
                    val ruleDayOfMonth = rule.day % 100
                    if (month + 1 < ruleMonth) continue
                    if (month + 1 == ruleMonth && day < ruleDayOfMonth) continue
                }
                else -> continue
            }
            val periodKey = when (rule.period) {
                0, 2 -> month
                1 -> java.time.LocalDate.now()
                    .with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY))
                    .toEpochDay().toInt()
                else -> 0
            }
            val key = AppSettings.recurringKey(rule)
            if (AppSettings.isRecurringDone(context, key, year, periodKey)) continue
            val ts = ruleTimestamp(cal, rule, calDay, now)
            repo.add(
                Transaction(
                    amount = rule.amount,
                    type = rule.type,
                    category = rule.category,
                    merchant = rule.merchant.ifEmpty { rule.name },
                    source = Transaction.SOURCE_RECURRING,
                    timestamp = ts,
                    localTimestamp = ts,
                    book = rule.book
                )
            )
            AppSettings.markRecurringDone(context, key, year, periodKey)
            MonthSync.refreshCurrentMonth(context, AppSettings.getCurrentBook(context))
        }
    }

    private fun ruleTimestamp(
        cal: Calendar,
        rule: AppSettings.RecurringRule,
        calDay: Int,
        now: Long
    ): Long {
        val ts = Calendar.getInstance().apply {
            clear()
            set(Calendar.YEAR, cal.get(Calendar.YEAR))
            set(Calendar.MONTH, cal.get(Calendar.MONTH))
            set(Calendar.DAY_OF_MONTH, cal.get(Calendar.DAY_OF_MONTH))
            set(Calendar.HOUR_OF_DAY, 8)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        when (rule.period) {
            0 -> ts.set(Calendar.DAY_OF_MONTH, rule.day)
            2 -> ts.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
            3 -> {
                val m = rule.day / 100 - 1
                val d = rule.day % 100
                // 每年规则（如 2月29）：非闰年等不存在的日期钳制到该月最后一天，避免 lenient 滚动到次月
                ts.isLenient = false
                ts.set(Calendar.MONTH, m)
                ts.set(Calendar.DAY_OF_MONTH, d)
                try {
                    ts.timeInMillis
                } catch (_: Exception) {
                    ts.isLenient = true
                    ts.set(Calendar.MONTH, m)
                    ts.set(Calendar.DAY_OF_MONTH, 1)
                    ts.set(Calendar.DAY_OF_MONTH, ts.getActualMaximum(Calendar.DAY_OF_MONTH))
                }
            }
            else -> ts.add(Calendar.DAY_OF_YEAR, -(cal.get(Calendar.DAY_OF_WEEK) - calDay))
        }
        return minOf(ts.timeInMillis, now)
    }
}
