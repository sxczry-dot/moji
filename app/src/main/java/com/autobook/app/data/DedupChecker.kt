package com.autobook.app.data

import kotlin.math.abs

object DedupChecker {

    const val DEDUP_WINDOW_MS = 15 * 60 * 1000L
    const val MERCHANT_MATCH_WINDOW_MS = 90_000L
    const val CROSS_SOURCE_WINDOW_MS = 180_000L
    const val SAME_SOURCE_WINDOW_MS = 10_000L

    // 通知没提取到商户名时，入库用渠道名兜底（如"微信"），这种不算真实商户名
    private fun merchantKey(tx: Transaction): String {
        val m = tx.merchant
        return if (m.isNotEmpty() && m != tx.source) m else ""
    }

    fun isDuplicateOf(existing: Transaction, tx: Transaction): Boolean {
        if (existing.refNo.isNotEmpty() && existing.refNo == tx.refNo &&
            abs(existing.amount - tx.amount) < Transaction.AMOUNT_EPSILON
        ) return true
        if (abs(existing.amount - tx.amount) >= Transaction.AMOUNT_EPSILON) return false
        if (existing.type != tx.type) return false
        if (existing.source.isEmpty() || tx.source.isEmpty()) return false
        val existingKey = merchantKey(existing)
        val txKey = merchantKey(tx)
        if (existing.source == tx.source) {
            if (existingKey.isNotEmpty() && txKey.isNotEmpty()) {
                return existingKey == txKey &&
                    abs(existing.timestamp - tx.timestamp) < SAME_SOURCE_WINDOW_MS
            }
            return abs(existing.timestamp - tx.timestamp) < SAME_SOURCE_WINDOW_MS
        }
        if (existingKey.isNotEmpty() && txKey.isNotEmpty()) {
            return existingKey == txKey &&
                abs(existing.timestamp - tx.timestamp) < MERCHANT_MATCH_WINDOW_MS
        }
        return abs(existing.timestamp - tx.timestamp) < CROSS_SOURCE_WINDOW_MS
    }
}
