package com.autobook.app.util

import java.time.LocalDate

/**
 * 信用卡账单短信解析：应还款金额、最低还款、还款日。
 * 覆盖常见模板："应还款人民币5,000.00元，最低还款500.00元，最后还款日8月23日"。
 * 纯函数，可单测。
 */
object BillDueParser {

    data class BillDue(
        val amount: Double,
        val minPayment: Double,
        val dueDate: LocalDate
    )

    private val dueAmountRegex = Regex(
        """应还款(?:金额)?(?:人民币)?[为：:]?\s*[¥￥]?\s*(\d{1,3}(?:,\d{3})*(?:\.\d{1,2})?)"""
    )
    private val minPaymentRegex = Regex(
        """最低还款(?:额)?(?:人民币)?[为：:]?\s*[¥￥]?\s*(\d{1,3}(?:,\d{3})*(?:\.\d{1,2})?)"""
    )
    private val dueDateRegex = Regex("""(?:最后)?还款日(?:为|是|：|:)?\s*(\d{1,2})月(\d{1,2})日""")
    private val dueDateShortRegex = Regex("""(?:最后)?还款日(?:为|是|：|:)?\s*(\d{1,2})日""")

    fun parse(body: String, now: LocalDate = LocalDate.now()): BillDue? {
        val dueDate = parseDueDate(body, now) ?: return null
        val amount = dueAmountRegex.find(body)?.groupValues?.get(1)
            ?.replace(",", "")?.toDoubleOrNull() ?: 0.0
        val minPayment = minPaymentRegex.find(body)?.groupValues?.get(1)
            ?.replace(",", "")?.toDoubleOrNull() ?: 0.0
        return BillDue(amount, minPayment, dueDate)
    }

    fun hasBillInfo(body: String): Boolean =
        body.contains("还款日") || body.contains("应还款") || body.contains("最低还款")

    private fun parseDueDate(body: String, now: LocalDate): LocalDate? {
        dueDateRegex.find(body)?.let { m ->
            val month = m.groupValues[1].toIntOrNull()
            val day = m.groupValues[2].toIntOrNull()
            if (month != null && day != null) {
                var d = try {
                    LocalDate.of(now.year, month, day)
                } catch (_: Exception) {
                    null
                }
                // 已过还款日超过3天：视为下一年的同一日期（年末账单）
                if (d != null && d.isBefore(now.minusDays(3))) {
                    d = try {
                        LocalDate.of(now.year + 1, month, day)
                    } catch (_: Exception) {
                        null
                    }
                }
                return d
            }
        }
        dueDateShortRegex.find(body)?.let { m ->
            val day = m.groupValues[1].toIntOrNull() ?: return null
            var d = try {
                LocalDate.of(now.year, now.monthValue, day)
            } catch (_: Exception) {
                null
            }
            if (d != null && d.isBefore(now.minusDays(3))) d = d.plusMonths(1)
            return d
        }
        return null
    }
}
