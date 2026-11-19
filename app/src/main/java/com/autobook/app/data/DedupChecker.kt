package com.autobook.app.data

import kotlin.math.abs

object DedupChecker {

    const val DEDUP_WINDOW_MS = 15 * 60 * 1000L
    // 去重候选池拉取窗口：比判重窗口宽得多，覆盖跨渠道延迟到达（如短信比通知晚到几小时）的场景；
    // 是否真判重仍由 isDuplicateOf 内部的各时间窗口决定，候选池放宽不会误吞
    const val DEDUP_CANDIDATE_WINDOW_MS = 24 * 60 * 60 * 1000L
    const val MERCHANT_MATCH_WINDOW_MS = 90_000L
    // 跨渠道、双方商户名都具体且一致：银行短信常延迟数分钟~数小时到达，窗口放宽到 2 小时。
    // 同店同额两笔真实交易通常不会挤在 2 小时内；通用标题不适用（碰撞率太高）
    const val MERCHANT_MATCH_SPECIFIC_WINDOW_MS = 2 * 60 * 60 * 1000L
    const val CROSS_SOURCE_WINDOW_MS = 180_000L
    const val SAME_SOURCE_WINDOW_MS = 10_000L
    // 同渠道且双方都未识别到商户名时，只按 3 秒判重（避免吞掉两笔同金额的不同商户真实交易）
    const val SAME_SOURCE_UNKNOWN_MERCHANT_WINDOW_MS = 3_000L
    // 跨渠道商户名不同但同金额且时间极近（如短信=公司全称、通知=通用标题）：判重后由合并逻辑保留更准确的商户并打"双渠道"标签
    const val DIFFERENT_MERCHANT_DEDUP_WINDOW_MS = 120_000L
    // 双方商户名都很具体但不同（如公司全称 vs 品牌名）：仅 1 分钟内判重
    const val DIFFERENT_SPECIFIC_MERCHANT_DEDUP_WINDOW_MS = 60_000L

    // 通知没提取到商户名时，入库用渠道名兜底（如"微信"），这种不算真实商户名
    private fun merchantKey(tx: Transaction): String {
        val m = tx.merchant
        return if (m.isNotEmpty() && m != tx.source) m else ""
    }

    fun isDuplicateOf(existing: Transaction, tx: Transaction): Boolean {
        if (existing.refNo.isNotEmpty() && existing.refNo == tx.refNo &&
            existing.type == tx.type &&
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
            return abs(existing.timestamp - tx.timestamp) < SAME_SOURCE_UNKNOWN_MERCHANT_WINDOW_MS
        }
        if (existingKey.isNotEmpty() && txKey.isNotEmpty()) {
            if (existingKey == txKey) {
                // 商户名具体（非"交易提醒"类通用标题）且一致：延迟到达的银行短信也能判重
                val window = if (existingKey in com.autobook.app.parser.ParseRules.genericTitles) {
                    MERCHANT_MATCH_WINDOW_MS
                } else {
                    MERCHANT_MATCH_SPECIFIC_WINDOW_MS
                }
                return abs(existing.timestamp - tx.timestamp) < window
            }
            // 商户名不同：一方是通用标题（交易提醒/动账通知等）时 2 分钟内判重；
            // 双方都很具体（如公司全称 vs 品牌名）时仅 1 分钟内判重，避免吞掉两笔真实同金额交易
            val oneSideGeneric =
                existing.merchant in com.autobook.app.parser.ParseRules.genericTitles ||
                    tx.merchant in com.autobook.app.parser.ParseRules.genericTitles
            val window = if (oneSideGeneric) {
                DIFFERENT_MERCHANT_DEDUP_WINDOW_MS
            } else {
                DIFFERENT_SPECIFIC_MERCHANT_DEDUP_WINDOW_MS
            }
            return abs(existing.timestamp - tx.timestamp) < window
        }
        return abs(existing.timestamp - tx.timestamp) < CROSS_SOURCE_WINDOW_MS
    }
}
