package com.autobook.app.util

import java.time.LocalDate

/**
 * 信用卡账单短信解析：应还款金额、最低还款、还款日、卡尾号。
 * 覆盖常见模板："应还款人民币5,000.00元，最低还款500.00元，最后还款日8月23日"。
 * 纯函数，可单测。
 */
object BillDueParser {

    data class BillDue(
        val amount: Double,
        val minPayment: Double,
        val dueDate: LocalDate,
        val cardTail: String = ""
    )

    private val dueAmountRegex = Regex(
        """应还款(?:金额)?(?:人民币)?[为：:]?\s*[¥￥]?\s*(\d+(?:,\d{3})*(?:\.\d{1,2})?)(?=[^\d.,]|$)"""
    )
    private val minPaymentRegex = Regex(
        """最低还款(?:额)?(?:人民币)?[为：:]?\s*[¥￥]?\s*(\d+(?:,\d{3})*(?:\.\d{1,2})?)(?=[^\d.,]|$)"""
    )
    private val dueDateRegex = Regex("""(?:最后)?还款日(?:为|是|：|:)?\s*(\d{1,2})月(\d{1,2})日""")
    private val dueDateShortRegex = Regex("""(?:最后)?还款日(?:为|是|：|:)?\s*(\d{1,2})日""")
    private val cardTailRegex = Regex("""尾号(\d{3,4})""")

    fun parse(body: String, now: LocalDate = LocalDate.now()): BillDue? {
        val dueDate = parseDueDate(body, now) ?: return null
        val amount = dueAmountRegex.find(body)?.groupValues?.get(1)
            ?.replace(",", "")?.toDoubleOrNull() ?: 0.0
        val minPayment = minPaymentRegex.find(body)?.groupValues?.get(1)
            ?.replace(",", "")?.toDoubleOrNull() ?: 0.0
        val cardTail = cardTailRegex.find(body)?.groupValues?.get(1) ?: ""
        return BillDue(amount, minPayment, dueDate, cardTail)
    }

    fun hasBillInfo(body: String): Boolean =
        body.contains("还款日") || body.contains("应还款") || body.contains("最低还款")

    /** 非法日期（如2月30日）钳制到当月最后一天，避免整条账单被丢弃 */
    private fun safeDate(year: Int, month: Int, day: Int): LocalDate? {
        if (month !in 1..12 || day !in 1..31) return null
        val base = try {
            LocalDate.of(year, month, 1)
        } catch (_: Exception) {
            return null
        }
        val clamped = (day - 1).coerceAtMost(base.lengthOfMonth() - 1)
        return try {
            base.plusDays(clamped.toLong())
        } catch (_: Exception) {
            null
        }
    }

    private fun parseDueDate(body: String, now: LocalDate): LocalDate? {
        dueDateRegex.find(body)?.let { m ->
            val month = m.groupValues[1].toIntOrNull()
            val day = m.groupValues[2].toIntOrNull()
            if (month != null && day != null) {
                // 年份推断：月份远在半年以后（如1月收到"12月23日"）视为上一年逾期账单；
                // 其余按本年，若已过期超过3天则顺延到明年同一日期
                var year = now.year
                val fromLastYear = month > now.monthValue + 6
                if (fromLastYear) year -= 1
                var d = safeDate(year, month, day)
                if (!fromLastYear && d != null && d.isBefore(now.minusDays(3))) {
                    d = safeDate(now.year + 1, month, day) ?: d
                }
                return d
            }
        }
        dueDateShortRegex.find(body)?.let { m ->
            val day = m.groupValues[1].toIntOrNull() ?: return null
            var d = safeDate(now.year, now.monthValue, day)
            if (d != null && d.isBefore(now.minusDays(3))) {
                d = safeDate(now.year, now.monthValue + 1, day) ?: d
            }
            return d
        }
        return null
    }
}
