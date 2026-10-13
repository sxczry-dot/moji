package com.autobook.app.ui.stats

import com.autobook.app.data.Categories
import com.autobook.app.data.Transaction
import com.autobook.app.data.expenseSum
import com.autobook.app.data.incomeSum
import com.autobook.app.util.TimeUtils
import java.time.YearMonth

/**
 * 统计聚合纯函数：与 UI 解耦、可在 Dispatchers.Default 上执行、可单测。
 */
object StatsCalculator {

    /** 近 N 个月（含当月往前）的支出/收入统计，按给定账本过滤后的数据计算 */
    fun monthStats(
        bookTxs: List<Transaction>,
        current: YearMonth,
        monthsBack: Int = 6
    ): List<com.autobook.app.viewmodel.MainViewModel.MonthStat> =
        (monthsBack - 1 downTo 0).map { offset ->
            val m = current.minusMonths(offset.toLong())
            val (start, end) = TimeUtils.monthRange(m.year, m.monthValue - 1)
            val inMonth = bookTxs.filter { it.timestamp in start until end }
            com.autobook.app.viewmodel.MainViewModel.MonthStat(
                yearMonth = m,
                expense = inMonth.expenseSum(),
                income = inMonth.incomeSum()
            )
        }

    /** 分类支出聚合（按大分类，降序） */
    fun categoryExpense(txs: List<Transaction>): List<Pair<String, Double>> =
        txs.filter { it.type == Transaction.TYPE_EXPENSE }
            .groupBy { Categories.primaryOf(it.category) }
            .map { (cat, list) -> cat to list.sumOf { it.amount } }
            .sortedByDescending { it.second }

    /** 商户支出排行（按金额降序，取前 limit 名） */
    fun merchantRanking(txs: List<Transaction>, limit: Int): List<Pair<String, Double>> =
        txs.filter { it.type == Transaction.TYPE_EXPENSE && it.merchant.isNotBlank() }
            .groupBy { it.merchant }
            .map { (m, list) -> m to list.sumOf { it.amount } }
            .sortedByDescending { it.second }
            .take(limit)

    /** 每日支出（时间戳→当天支出合计） */
    fun dailyExpense(txs: List<Transaction>): Map<Long, Double> =
        txs.filter { it.type == Transaction.TYPE_EXPENSE }
            .groupBy { TimeUtils.dayStartOf(it.timestamp) }
            .mapValues { (_, list) -> list.sumOf { it.amount } }
}
