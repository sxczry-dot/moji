package com.autobook.app.ui.settings

import com.autobook.app.data.Transaction
import com.autobook.app.data.localTime
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.abs

/**
 * 余额对账：利用银行短信"余额XXX元"快照做流水校验。
 * 相邻两个有余额快照的日子：预计余额 = 前日余额 - 当日支出 + 当日收入，
 * 与实际快照差异超过 0.05 元的日列入"可能漏记/错记"清单。
 * 纯函数，可单测。
 */
object ReconciliationCalculator {

    data class Mismatch(
        val day: LocalDate,
        val expected: Double,
        val actual: Double,
        val diff: Double
    )

    fun analyze(txs: List<Transaction>): List<Mismatch> {
        // 快照：每自然日取当天最后一条带余额的记录（保存快照时间戳作为窗口边界）
        data class Snap(val day: LocalDate, val balance: Double, val ts: Long)
        val snapshots = txs
            .filter { it.balanceAfter > 0 }
            .groupBy { toDate(it.localTime()) }
            .mapValues { (_, list) ->
                val last = list.maxByOrNull { it.localTime() }!!
                Snap(toDate(last.localTime()), last.balanceAfter, last.localTime())
            }
        if (snapshots.size < 2) return emptyList()

        val days = snapshots.keys.sorted()
        val result = mutableListOf<Mismatch>()
        for (i in 1 until days.size) {
            val prevDay = days[i - 1]
            val day = days[i]
            val prevSnap = snapshots[prevDay]!!
            val daySnap = snapshots[day]!!
            // 以两次快照时间戳为界（而非自然日截断）：前日快照之后的流水到本日快照为止，
            // 避免"快照后又消费/快照前的跨日流水"被算错窗口
            val dayTxs = txs.filter {
                it.localTime() > prevSnap.ts && it.localTime() <= daySnap.ts
            }
            val expense = dayTxs.filter { it.type == Transaction.TYPE_EXPENSE }.sumOf { it.amount }
            val income = dayTxs.filter { it.type == Transaction.TYPE_INCOME }.sumOf { it.amount }
            val expected = prevSnap.balance - expense + income
            val actual = daySnap.balance
            if (abs(expected - actual) > 0.05) {
                result += Mismatch(day, expected, actual, expected - actual)
            }
        }
        return result
    }

    private fun toDate(timestamp: Long): LocalDate =
        Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).toLocalDate()
}
