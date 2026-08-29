package com.autobook.app.service

import android.content.Context
import com.autobook.app.AutoBookApp
import com.autobook.app.data.Transaction
import com.autobook.app.util.AppSettings
import java.util.Calendar

object RecurringManager {

    suspend fun checkAndRun(context: Context) {
        val rules = AppSettings.getRecurringRules(context)
        if (rules.isEmpty()) return
        val cal = Calendar.getInstance()
        val now = cal.timeInMillis
        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH)
        val day = cal.get(Calendar.DAY_OF_MONTH)
        val nowDayOfWeek = cal.get(Calendar.DAY_OF_WEEK)

        val repo = AutoBookApp.instance.repository
        for (rule in rules) {
            if (rule.paused) continue
            // 规则 day：每月=几号；每周=1周一..7周日 → Calendar.DAY_OF_WEEK（2周一..1周日）；
            // 每年=月*100+日（如903=9月3日）；月末=忽略 day
            val calDay = (rule.day % 7) + 1
            when (rule.period) {
                0 -> if (day < rule.day) continue
                1 -> if (nowDayOfWeek < calDay) continue
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
            val key = "${rule.name}_${rule.day}_${rule.period}_${rule.book}"
            if (AppSettings.isRecurringDone(context, key, year, periodKey)) continue
            repo.add(
                Transaction(
                    amount = rule.amount,
                    type = rule.type,
                    category = rule.category,
                    merchant = rule.merchant.ifEmpty { rule.name },
                    source = Transaction.SOURCE_RECURRING,
                    timestamp = ruleTimestamp(cal, rule, calDay, now),
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
                ts.set(Calendar.MONTH, rule.day / 100 - 1)
                ts.set(Calendar.DAY_OF_MONTH, rule.day % 100)
            }
            else -> ts.add(Calendar.DAY_OF_YEAR, -(cal.get(Calendar.DAY_OF_WEEK) - calDay))
        }
        return minOf(ts.timeInMillis, now)
    }
}
