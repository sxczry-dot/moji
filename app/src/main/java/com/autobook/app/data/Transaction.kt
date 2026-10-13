package com.autobook.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(
    tableName = "transactions",
    indices = [
        androidx.room.Index("timestamp"),
        androidx.room.Index("book", "timestamp"),
        androidx.room.Index("book", "localTimestamp")
    ]
)
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amount: Double,
    val type: Int,
    val category: String,
    val merchant: String,
    val source: String,
    val timestamp: Long,
    val tags: String = "",
    val book: String = com.autobook.app.util.AppSettings.DEFAULT_BOOK,
    val refNo: String = "",
    val insertedAt: Long = 0L,
    /** 交易后余额（银行短信"余额XXX元"），对账用；无则为 0 */
    val balanceAfter: Double = 0.0,
    /** 记录时按当时本地时区解析的墙钟时间：按月/日归类与展示一律用它，换时区不串月 */
    val localTimestamp: Long = 0L
) {
    companion object {
        const val TYPE_EXPENSE = 0
        const val TYPE_INCOME = 1

        const val MAX_AMOUNT = 1_000_000.0
        const val AMOUNT_EPSILON = 0.02
        const val MERCHANT_FALLBACK = "手动记账"

        const val SOURCE_MANUAL = "手动"
        const val SOURCE_WECHAT = "微信"
        const val SOURCE_ALIPAY = "支付宝"
        const val SOURCE_UNIONPAY = "云闪付"
        const val SOURCE_SMS = "短信"
        const val SOURCE_RECURRING = "周期"

        // 退款标记（存 tags）：识别出的退款收入 + 已关联到原消费的退款
        const val TAG_REFUND = "退款"
        const val TAG_REFUND_LINKED = "关联退款"

        // 跨渠道去重合并标记：同一笔交易经两个渠道（如微信通知+银行短信）分别到达
        const val TAG_DUAL_SOURCE = "双渠道"

        }
}

fun Transaction.isRefund(): Boolean =
    tags.split(Regex("\\s+")).any { it == Transaction.TAG_REFUND }

/** 墙钟时间：localTimestamp 优先（换时区不串月），旧数据兜底 timestamp */
fun Transaction.localTime(): Long = if (localTimestamp > 0) localTimestamp else timestamp

fun List<Transaction>.refundSum(): Double =
    filter { it.type == Transaction.TYPE_INCOME && it.isRefund() }.sumOf { it.amount }

fun List<Transaction>.expenseSum(): Double =
    filter { it.type == Transaction.TYPE_EXPENSE }.sumOf { it.amount }

fun List<Transaction>.incomeSum(): Double =
    filter { it.type == Transaction.TYPE_INCOME }.sumOf { it.amount }
