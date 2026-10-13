package com.autobook.app

import com.autobook.app.data.Transaction
import com.autobook.app.ui.settings.ReconciliationCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class ReconciliationCalculatorTest {

    private fun ts(day: LocalDate, hour: Int = 12): Long =
        day.atTime(hour, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

    private fun tx(
        amount: Double,
        type: Int = Transaction.TYPE_EXPENSE,
        day: LocalDate,
        balance: Double = 0.0,
        hour: Int = 12
    ) = Transaction(
        amount = amount, type = type, category = "餐饮美食", merchant = "测试",
        source = "短信", timestamp = ts(day, hour), localTimestamp = ts(day, hour),
        balanceAfter = balance
    )

    @Test
    fun `账目一致时无差异`() {
        val txs = listOf(
            tx(100.0, day = LocalDate.of(2026, 8, 1), balance = 900.0),
            tx(50.0, day = LocalDate.of(2026, 8, 2)),
            tx(20.0, type = Transaction.TYPE_INCOME, day = LocalDate.of(2026, 8, 2)),
            tx(200.0, day = LocalDate.of(2026, 8, 3), balance = 670.0) // 900-50+20-200=670 ✓
        )
        assertTrue(ReconciliationCalculator.analyze(txs).isEmpty())
    }

    @Test
    fun `漏记支出时检出差异`() {
        val txs = listOf(
            tx(100.0, day = LocalDate.of(2026, 8, 1), balance = 900.0),
            // 8月2日有一笔30元支出漏记（不在数据里）
            tx(200.0, day = LocalDate.of(2026, 8, 3), balance = 670.0) // 应为 600
        )
        val mismatches = ReconciliationCalculator.analyze(txs)
        assertEquals(1, mismatches.size)
        assertEquals(LocalDate.of(2026, 8, 3), mismatches[0].day)
        assertEquals(700.0, mismatches[0].expected, 0.001) // 900-200
        assertEquals(670.0, mismatches[0].actual, 0.001)
        assertEquals(30.0, mismatches[0].diff, 0.001) // 差异+30 = 漏记支出30
    }

    @Test
    fun `余额快照不足时不分析`() {
        val txs = listOf(
            tx(100.0, day = LocalDate.of(2026, 8, 1), balance = 900.0)
        )
        assertTrue(ReconciliationCalculator.analyze(txs).isEmpty())
    }
}
