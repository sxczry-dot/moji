package com.autobook.app

import com.autobook.app.data.Transaction
import com.autobook.app.ui.stats.StatsCalculator
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.YearMonth

class StatsCalculatorTest {

    private fun tx(
        amount: Double,
        type: Int = Transaction.TYPE_EXPENSE,
        category: String = "餐饮美食",
        merchant: String = "美团",
        timestamp: Long,
        book: String = "默认账本"
    ) = Transaction(
        amount = amount, type = type, category = category,
        merchant = merchant, source = "微信", timestamp = timestamp, book = book
    )

    // 2026-07-10 12:00 与 2026-08-10 12:00（本地时区无关的相对锚点用月首）
    private val jul10 = java.util.Calendar.getInstance().apply {
        clear(); set(2026, 6, 10, 12, 0, 0)
    }.timeInMillis

    private val aug10 = java.util.Calendar.getInstance().apply {
        clear(); set(2026, 7, 10, 12, 0, 0)
    }.timeInMillis

    @Test
    fun `monthStats 按月聚合`() {
        val txs = listOf(
            tx(100.0, timestamp = jul10),
            tx(50.0, type = Transaction.TYPE_INCOME, timestamp = jul10),
            tx(200.0, timestamp = aug10)
        )
        val stats = StatsCalculator.monthStats(txs, YearMonth.of(2026, 8), monthsBack = 2)
        assertEquals(2, stats.size)
        assertEquals(100.0, stats[0].expense, 0.001) // 7月
        assertEquals(50.0, stats[0].income, 0.001)
        assertEquals(200.0, stats[1].expense, 0.001) // 8月
    }

    @Test
    fun `categoryExpense 按大分类聚合降序`() {
        val txs = listOf(
            tx(30.0, category = "餐饮美食/早餐", timestamp = jul10),
            tx(50.0, category = "餐饮美食", timestamp = jul10),
            tx(100.0, category = "购物消费", timestamp = jul10),
            tx(10.0, type = Transaction.TYPE_INCOME, category = "收入", timestamp = jul10)
        )
        val result = StatsCalculator.categoryExpense(txs)
        assertEquals(listOf("购物消费" to 100.0, "餐饮美食" to 80.0), result)
    }

    @Test
    fun `merchantRanking 取前N名`() {
        val txs = listOf(
            tx(20.0, merchant = "A", timestamp = jul10),
            tx(30.0, merchant = "B", timestamp = jul10),
            tx(40.0, merchant = "C", timestamp = jul10)
        )
        val result = StatsCalculator.merchantRanking(txs, 2)
        assertEquals(listOf("C" to 40.0, "B" to 30.0), result)
    }
}
