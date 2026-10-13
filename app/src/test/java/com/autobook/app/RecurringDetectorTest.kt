package com.autobook.app

import com.autobook.app.data.Transaction
import com.autobook.app.util.RecurringDetector
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class RecurringDetectorTest {

    private fun tx(merchant: String, amount: Double, day: LocalDate) = Transaction(
        amount = amount,
        type = Transaction.TYPE_EXPENSE,
        category = "娱乐休闲",
        merchant = merchant,
        source = "微信",
        timestamp = day.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli(),
        localTimestamp = day.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
    )

    @Test
    fun `月度订阅识别为候选`() {
        val txs = listOf(
            tx("爱奇艺会员", 25.0, LocalDate.of(2026, 6, 1)),
            tx("爱奇艺会员", 25.0, LocalDate.of(2026, 7, 1)),
            tx("爱奇艺会员", 25.0, LocalDate.of(2026, 8, 1))
        )
        val candidates = RecurringDetector.findCandidates(txs, today = LocalDate.of(2026, 8, 5))
        assertEquals(1, candidates.size)
        assertEquals("爱奇艺会员", candidates[0].merchant)
        assertEquals(25.0, candidates[0].amount, 0.001)
        assertEquals(3, candidates[0].count)
        assertTrue(candidates[0].intervalDays in 26..34)
    }

    @Test
    fun `每周订阅识别为候选`() {
        val txs = listOf(
            tx("健身房周卡", 39.9, LocalDate.of(2026, 7, 6)),
            tx("健身房周卡", 39.9, LocalDate.of(2026, 7, 13)),
            tx("健身房周卡", 39.9, LocalDate.of(2026, 7, 20))
        )
        val candidates = RecurringDetector.findCandidates(txs, today = LocalDate.of(2026, 7, 25))
        assertEquals(1, candidates.size)
        assertTrue(candidates[0].intervalDays in 6..8)
        assertEquals(1, candidates[0].lastDayOfWeek) // 7月20日是周一
    }

    @Test
    fun `不规律支出不识别`() {
        val txs = listOf(
            tx("美团外卖", 25.0, LocalDate.of(2026, 6, 1)),
            tx("美团外卖", 30.5, LocalDate.of(2026, 6, 12)),
            tx("美团外卖", 25.0, LocalDate.of(2026, 7, 20))
        )
        assertTrue(RecurringDetector.findCandidates(txs, today = LocalDate.of(2026, 8, 1)).isEmpty())
    }

    @Test
    fun `停扣超两月不再推荐`() {
        val txs = listOf(
            tx("旧订阅", 15.0, LocalDate.of(2026, 2, 1)),
            tx("旧订阅", 15.0, LocalDate.of(2026, 3, 1))
        )
        assertTrue(RecurringDetector.findCandidates(txs, today = LocalDate.of(2026, 8, 1)).isEmpty())
    }
}
