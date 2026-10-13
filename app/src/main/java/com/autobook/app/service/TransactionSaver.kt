package com.autobook.app.service

import android.content.Context
import com.autobook.app.AutoBookApp
import com.autobook.app.data.Transaction
import com.autobook.app.parser.CategoryClassifier
import com.autobook.app.parser.ParsedResult

object TransactionSaver {

    val redPacketDeductionRegex = Regex("""使用\d+(?:\.\d{1,2})?元红包|红包抵扣|抵扣红包|红包立减""")

    /** 纯函数：识别退款类收入（含"退款/退回"的入账；"退款支出"方向已由解析器判为支出，不会被误标） */
    fun isRefundTransaction(type: Int, rawText: String, merchant: String): Boolean =
        type == Transaction.TYPE_INCOME &&
            (rawText.contains("退款") || rawText.contains("退回") || merchant.contains("退款"))

    /** 查找近 30 天内同账本、同金额（±0.02）的支出记录，用于退款关联 */
    private suspend fun hasRefundTarget(parsed: ParsedResult): Boolean {
        val now = System.currentTimeMillis()
        val txs = AutoBookApp.instance.repository.getByRangeOnce(
            com.autobook.app.util.AppSettings.DEFAULT_BOOK,
            now - 30L * 24 * 3600 * 1000,
            now
        )
        return txs.any {
            it.type == Transaction.TYPE_EXPENSE &&
                kotlin.math.abs(it.amount - parsed.amount) < Transaction.AMOUNT_EPSILON
        }
    }

    sealed class Outcome {
        data object Inserted : Outcome()
        data object Duplicate : Outcome()
        data object Blocked : Outcome()
        data object Failed : Outcome()
        data object Paused : Outcome()
    }

    suspend fun save(context: Context, parsed: ParsedResult, source: String): Outcome {
        if (com.autobook.app.util.AppSettings.isAutoPaused(context)) return Outcome.Paused
        try {
            val blockWords = com.autobook.app.util.AppSettings.getBlockWords(context)
            if (blockWords.any { parsed.rawText.contains(it) }) return Outcome.Blocked

            val repo = AutoBookApp.instance.repository
            val now = System.currentTimeMillis()

            val learned = if (parsed.merchant.isNotBlank()) {
                com.autobook.app.util.AppSettings.getLearnedCategory(context, parsed.merchant)
                    ?.let { com.autobook.app.data.Categories.primaryOf(it) }
            } else null
            val classifyText = listOf(parsed.merchant, parsed.rawText, source)
                .filter { it.isNotBlank() }
                .joinToString(" ")
                // 剔除抵扣类红包文案，避免"使用5元红包，实付38元"被"红包"关键词误分人情社交
                .replace(com.autobook.app.service.TransactionSaver.redPacketDeductionRegex, "")
            val category = learned ?: if (parsed.type == Transaction.TYPE_EXPENSE) {
                CategoryClassifier.classifyExpense(classifyText)
            } else {
                CategoryClassifier.classifyIncome(classifyText)
            }
            // 退款净口径：识别退款并关联原消费（标记存 tags，不改变金额与方向）
            var tags = ""
            if (isRefundTransaction(parsed.type, parsed.rawText, parsed.merchant)) {
                tags = Transaction.TAG_REFUND
                if (hasRefundTarget(parsed)) tags += " " + Transaction.TAG_REFUND_LINKED
            }
            val tx = Transaction(
                amount = parsed.amount,
                type = parsed.type,
                category = category,
                merchant = parsed.merchant.ifEmpty { source },
                source = source,
                timestamp = if (parsed.tradeTime in 1..now) parsed.tradeTime else now,
                book = com.autobook.app.util.AppSettings.DEFAULT_BOOK,
                refNo = parsed.refNo,
                tags = tags,
                balanceAfter = parsed.balance,
                localTimestamp = if (parsed.tradeTime in 1..now) parsed.tradeTime else now
            )

            when (val result = repo.insertAndGetMonth(tx, now)) {
                is com.autobook.app.data.SaveResult.Duplicate -> {
                    val existing = result.existing
                    val existingIsFallback = existing.merchant.isBlank() ||
                        existing.merchant == existing.source
                    val merchantEnriched = existingIsFallback &&
                        tx.merchant.isNotBlank() && tx.merchant != tx.source
                    // 跨渠道去重合并：同笔交易经两个渠道到达，标注来源并富集余额
                    val crossSource = existing.source.isNotEmpty() && existing.source != tx.source
                    val balanceEnriched = existing.balanceAfter <= 0 && tx.balanceAfter > 0
                    // 新到的商户名更准确时替换（现有是兜底/通用标题，或新名更长更具体）
                    val existingGeneric = existing.merchant in com.autobook.app.parser.ParseRules.genericTitles
                    val betterMerchant = crossSource && tx.merchant.isNotBlank() &&
                        tx.merchant != tx.source &&
                        (existingIsFallback || existingGeneric || tx.merchant.length > existing.merchant.length)
                    if (merchantEnriched || betterMerchant || crossSource || balanceEnriched) {
                        val hasTag = existing.tags.split(Regex("\\s+")).any { it == Transaction.TAG_DUAL_SOURCE }
                        val newTags = if (crossSource && !hasTag) {
                            listOf(existing.tags, Transaction.TAG_DUAL_SOURCE)
                                .filter { it.isNotBlank() }.joinToString(" ")
                        } else {
                            existing.tags
                        }
                        repo.update(
                            existing.copy(
                                merchant = if (merchantEnriched || betterMerchant) tx.merchant else existing.merchant,
                                balanceAfter = if (balanceEnriched) tx.balanceAfter else existing.balanceAfter,
                                tags = newTags
                            )
                        )
                    }
                    val time = java.text.SimpleDateFormat("MM-dd HH:mm", java.util.Locale.CHINA)
                        .format(java.util.Date())
                    com.autobook.app.util.AppSettings.addNotifLog(
                        context,
                        "$time | 去重 | 已合并：${result.existing.source} ${com.autobook.app.util.formatAmount(result.existing.amount)} = $source ${com.autobook.app.util.formatAmount(tx.amount)}"
                    )
                    return Outcome.Duplicate
                }
                is com.autobook.app.data.SaveResult.Inserted -> {
                    // 还款入账 → 核销对应信用卡账单提醒
                    if (parsed.rawText.contains("还款")) {
                        com.autobook.app.util.AppSettings.clearBillDuesMatching(context, parsed.amount)
                    }
                    val monthTxs = result.monthTxs
                    val currentBook = com.autobook.app.util.AppSettings.getCurrentBook(context)
                    if (currentBook == tx.book) {
                        MonthSync.refresh(context, monthTxs)
                    } else {
                        MonthSync.refreshCurrentMonth(context, currentBook)
                    }
                    return Outcome.Inserted
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("TransactionSaver", "save failed", e)
        }
        return Outcome.Failed
    }
}
