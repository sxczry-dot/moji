package com.autobook.app.util

import com.autobook.app.data.Transaction
import com.autobook.app.data.localTime
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * 循环扣款识别：同商户+同金额（±2%）且间隔稳定（约7天或约30天）出现≥2次的支出，
 * 建议一键生成周期规则（订阅/水电/会员等）。
 * 纯函数，可单测。
 */
object RecurringDetector {

    const val WEEKLY_MIN_DAYS = 6
    const val WEEKLY_MAX_DAYS = 8
    const val MONTHLY_MIN_DAYS = 26
    const val MONTHLY_MAX_DAYS = 34

    data class Candidate(
        val merchant: String,
        val amount: Double,
        val count: Int,
        val intervalDays: Int,
        val lastDayOfMonth: Int,
        val lastDayOfWeek: Int,
        val book: String,
        val category: String
    )

    fun findCandidates(
        txs: List<Transaction>,
        minOccurrences: Int = 2,
        today: LocalDate = LocalDate.now()
    ): List<Candidate> {
        val grouped = txs
            .filter {
                it.type == Transaction.TYPE_EXPENSE &&
                    it.merchant.isNotBlank() && it.amount > 0
            }
            .groupBy { Triple(it.book, it.merchant, (it.amount * 100).roundToInt()) }

        val candidates = mutableListOf<Candidate>()
        for ((key, list) in grouped) {
            if (list.size < minOccurrences) continue
            val dates = list.map { toLocalDate(it.localTime()) }.sorted()
            if (dates.distinct().size < minOccurrences) continue
            val intervals = dates.zipWithNext { a, b ->
                java.time.temporal.ChronoUnit.DAYS.between(a, b).toInt()
            }
            val intervalDays = when {
                intervals.all { it in WEEKLY_MIN_DAYS..WEEKLY_MAX_DAYS } ->
                    intervals.average().roundToInt()
                intervals.all { it in MONTHLY_MIN_DAYS..MONTHLY_MAX_DAYS } ->
                    intervals.average().roundToInt()
                else -> continue
            }
            // 最后一笔距今不能太远（超过 2 个周期说明可能已停扣）
            val last = dates.max()
            val gap = java.time.temporal.ChronoUnit.DAYS.between(last, today)
            if (gap > MONTHLY_MAX_DAYS * 2) continue
            val amounts = list.map { it.amount }
            val medianAmount = amounts.sorted()[amounts.size / 2]
            // 金额波动 ±2% 校验（以中位数为基准）
            if (amounts.any { abs(it - medianAmount) > medianAmount * 0.02 }) continue
            candidates += Candidate(
                merchant = key.second,
                amount = medianAmount,
                count = list.size,
                intervalDays = intervalDays,
                lastDayOfMonth = last.dayOfMonth,
                lastDayOfWeek = last.dayOfWeek.value, // ISO: 1=周一..7=周日
                book = key.first,
                category = list.first().category
            )
        }
        return candidates.sortedByDescending { it.count }
    }

    private fun toLocalDate(timestamp: Long): LocalDate =
        Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).toLocalDate()
}
