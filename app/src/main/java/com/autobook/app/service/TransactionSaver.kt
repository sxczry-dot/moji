package com.autobook.app.service

import android.content.Context
import com.autobook.app.AutoBookApp
import com.autobook.app.data.Transaction
import com.autobook.app.parser.CategoryClassifier
import com.autobook.app.parser.ParsedResult

object TransactionSaver {

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
            } else null
            val category = learned ?: if (parsed.type == Transaction.TYPE_EXPENSE) {
                CategoryClassifier.classifyExpense(parsed.merchant + " " + source)
            } else {
                CategoryClassifier.classifyIncome(parsed.merchant + " " + source)
            }
            val tx = Transaction(
                amount = parsed.amount,
                type = parsed.type,
                category = category,
                merchant = parsed.merchant.ifEmpty { source },
                source = source,
                timestamp = if (parsed.tradeTime in 1..now) parsed.tradeTime else now,
                book = com.autobook.app.util.AppSettings.DEFAULT_BOOK,
                refNo = parsed.refNo
            )

            when (val result = repo.insertAndGetMonth(tx, now)) {
                is com.autobook.app.data.SaveResult.Duplicate -> {
                    val existing = result.existing
                    val existingIsFallback = existing.merchant.isBlank() ||
                        existing.merchant == existing.source
                    if (existingIsFallback && tx.merchant.isNotBlank() && tx.merchant != tx.source) {
                        repo.update(existing.copy(merchant = tx.merchant))
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
