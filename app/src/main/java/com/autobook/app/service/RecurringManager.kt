package com.autobook.app.service

import android.content.Context
import com.autobook.app.AutoBookApp
import com.autobook.app.data.Transaction
import com.autobook.app.util.AppSettings
import java.util.Calendar
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

object RecurringManager {

    /** 对账回查窗口：周期扣款的支付通知通常同日到达，留 3 天余量覆盖延迟 */
    private const val RECONCILE_WINDOW_MS = 3 * 24 * 60 * 60 * 1000L

    /**
     * 商户名双向包含匹配（"爱奇艺" vs "爱奇艺会员"）。
     * 任一方没有真实商户名（空，或通知解析失败时用渠道名兜底）时不敢按名称跳，交给同日同额兜底判断。
     */
    private fun merchantMatches(txMerchant: String, txSource: String, ruleMerchant: String): Boolean {
        if (ruleMerchant.isEmpty()) return false
        if (txMerchant.isEmpty() || txMerchant == txSource) return false
        return txMerchant.contains(ruleMerchant) || ruleMerchant.contains(txMerchant)
    }

    private fun isSameDay(a: Long, b: Long): Boolean {
        val ca = Calendar.getInstance().apply { timeInMillis = a }
        val cb = Calendar.getInstance().apply { timeInMillis = b }
        return ca.get(Calendar.YEAR) == cb.get(Calendar.YEAR) &&
            ca.get(Calendar.DAY_OF_YEAR) == cb.get(Calendar.DAY_OF_YEAR)
    }

    /** 多入口（10分钟闹钟/15分钟Worker/开机/打开App）并发调用时串行执行，防重复入账 */
    private val runMutex = Mutex()

    suspend fun checkAndRun(context: Context) {
        runMutex.withLock {
            try {
                runLocked(context)
            } catch (e: Exception) {
                android.util.Log.e("RecurringManager", "checkAndRun failed", e)
            }
        }
    }

    private suspend fun runLocked(context: Context) {
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
                // day>当月天数（如一键生成的31号规则）钳制到月末执行，短月不漏
                0 -> {
                    val targetDay = minOf(rule.day, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
                    if (day < targetDay) continue
                }
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
            // 对账：近 3 天内已有同额同类型且商户名匹配的真实记录（通知/短信/手动），
            // 说明这笔周期扣款已被自动记账捕获，跳过避免重复入账。
            // 跳过时不打 done 标记——用户若删掉那条已记账目，下次触发仍会补记。
            val merchant = rule.merchant.ifEmpty { rule.name }
            val recent = repo.getByRangeOnce(rule.book, ts - RECONCILE_WINDOW_MS, now)
            val alreadyRecorded = recent.any { tx ->
                if (tx.source == Transaction.SOURCE_RECURRING ||
                    tx.type != rule.type ||
                    kotlin.math.abs(tx.amount - rule.amount) >= Transaction.AMOUNT_EPSILON
                ) return@any false
                if (merchantMatches(tx.merchant, tx.source, merchant)) return@any true
                // 通知没解析出商户名（用渠道名兜底）时：同额同方向且当天，也视为同一笔扣款
                val isFallbackMerchant = tx.merchant.isEmpty() || tx.merchant == tx.source
                isFallbackMerchant && isSameDay(tx.timestamp, ts)
            }
            if (alreadyRecorded) {
                AppSettings.addNotifLog(
                    context,
                    "周期 | 已有同额记录，跳过：$merchant ¥${rule.amount}"
                )
                continue
            }
            repo.add(
                Transaction(
                    amount = rule.amount,
                    type = rule.type,
                    category = rule.category,
                    merchant = merchant,
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
