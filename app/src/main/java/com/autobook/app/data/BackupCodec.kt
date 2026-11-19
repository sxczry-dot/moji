package com.autobook.app.data

import org.json.JSONArray
import org.json.JSONObject

/**
 * 完整备份编解码。
 *
 * ABK2（当前格式）：JSON 内含 settings（设置）+ txs（全字段流水数组），
 * 恢复后 refNo（单号去重）与 balanceAfter（对账余额）等字段全部保留。
 *
 * ABK1（历史格式）：JSON 内含 settings + csv（9 列 CSV，缺 refNo/balanceAfter），
 * 恢复时兼容读取，老备份文件仍可用。
 */
object BackupCodec {

    const val MAGIC_V1 = "ABK1\n"
    const val MAGIC_V2 = "ABK2\n"

    fun isBackup(text: String): Boolean =
        text.startsWith(MAGIC_V1) || text.startsWith(MAGIC_V2)

    fun encode(settingsJson: String, txs: List<Transaction>): String {
        val arr = JSONArray()
        txs.sortedBy { it.timestamp }.forEach { tx ->
            arr.put(
                JSONObject().apply {
                    put("id", tx.id)
                    put("amount", tx.amount)
                    put("type", tx.type)
                    put("category", tx.category)
                    put("merchant", tx.merchant)
                    put("source", tx.source)
                    put("timestamp", tx.timestamp)
                    put("localTimestamp", tx.localTimestamp)
                    put("tags", tx.tags)
                    put("book", tx.book)
                    put("refNo", tx.refNo)
                    put("balanceAfter", tx.balanceAfter)
                }
            )
        }
        val json = JSONObject()
            .put("settings", JSONObject(settingsJson))
            .put("txs", arr)
        return MAGIC_V2 + json.toString()
    }

    data class Backup(val settingsJson: String, val txs: List<Transaction>)

    /** 解析备份文本；非备份文件返回 null */
    fun decode(text: String): Backup? {
        val body = when {
            text.startsWith(MAGIC_V2) -> text.removePrefix(MAGIC_V2)
            text.startsWith(MAGIC_V1) -> text.removePrefix(MAGIC_V1)
            else -> return null
        }
        val json = try {
            JSONObject(body)
        } catch (_: Exception) {
            return null
        }
        val settings = json.optJSONObject("settings")?.toString() ?: return null
        val txs = if (json.has("txs")) {
            decodeTxs(json.getJSONArray("txs"))
        } else {
            // ABK1 兼容：流水在 CSV 里（无 refNo/balanceAfter，尽力恢复）
            CsvCodec.decode(json.optString("csv", ""))
        }
        return Backup(settings, txs)
    }

    private fun decodeTxs(arr: JSONArray): List<Transaction> {
        val out = ArrayList<Transaction>(arr.length())
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            val amount = o.optDouble("amount", Double.NaN)
            if (amount.isNaN() || amount <= 0 || amount > Transaction.MAX_AMOUNT) continue
            val ts = o.optLong("timestamp", 0L)
            if (ts <= 0) continue
            out.add(
                Transaction(
                    id = o.optLong("id", 0L),
                    amount = amount,
                    type = o.optInt("type", Transaction.TYPE_EXPENSE),
                    category = o.optString("category", ""),
                    merchant = o.optString("merchant", ""),
                    source = o.optString("source", ""),
                    timestamp = ts,
                    localTimestamp = o.optLong("localTimestamp", 0L),
                    tags = o.optString("tags", ""),
                    book = o.optString("book", com.autobook.app.util.AppSettings.DEFAULT_BOOK)
                        .ifBlank { com.autobook.app.util.AppSettings.DEFAULT_BOOK },
                    refNo = o.optString("refNo", ""),
                    balanceAfter = o.optDouble("balanceAfter", 0.0)
                )
            )
        }
        return out
    }
}
