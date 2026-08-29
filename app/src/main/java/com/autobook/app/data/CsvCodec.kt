package com.autobook.app.data

import com.autobook.app.parser.CategoryClassifier
import com.autobook.app.util.formatAmount
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CsvCodec {

    private val timeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA)
    private const val HEADER = "金额,类型,分类,商户,来源,时间,标签,账本"

    fun encode(transactions: List<Transaction>): String {
        val sb = StringBuilder(HEADER).append("\n")
        transactions.sortedBy { it.timestamp }.forEach { tx ->
            sb.append(escape(formatAmount(tx.amount))).append(",")
            sb.append(if (tx.type == Transaction.TYPE_EXPENSE) "支出" else "收入").append(",")
            sb.append(escape(tx.category)).append(",")
            sb.append(escape(tx.merchant)).append(",")
            sb.append(escape(tx.source)).append(",")
            sb.append(timeFormat.format(Date(tx.timestamp))).append(",")
            sb.append(escape(tx.tags)).append(",")
            sb.append(escape(tx.book)).append("\n")
        }
        return sb.toString()
    }

    fun decode(content: String): List<Transaction> {
        val lines = content.lineSequence()
            .filter { it.isNotBlank() }
            .map { parseLine(it) }
            .toList()
        if (lines.isEmpty()) return emptyList()

        val header = lines.first()
        val hasHeader = header.any { it.contains("金额") || it.contains("分类") || it.contains("备注") }
        if (!hasHeader) {
            return lines.mapNotNull { parseTransaction(it) }
        }
        val columns = buildColumnMap(header)
        if (!columns.containsKey("金额") &&
            !(columns.containsKey("收入金额") && columns.containsKey("支出金额"))
        ) {
            return emptyList()
        }
        val rows = lines.subList(1, lines.size)
        return rows.mapNotNull { parseWithColumns(it, columns) }
    }

    private fun buildColumnMap(header: List<String>): Map<String, Int> {
        val map = mutableMapOf<String, Int>()
        header.forEachIndexed { i, name ->
            val n = name.trim().lowercase()
            when {
                n.contains("金额") || n.contains("amount") || n.contains("price") -> {
                    when {
                        n.contains("收入") || n.contains("income") -> map["收入金额"] = i
                        n.contains("支出") || n.contains("expense") -> map["支出金额"] = i
                        else -> map["金额"] = i
                    }
                }
                n.contains("类型") || n.contains("收支") || n.contains("type") -> map["类型"] = i
                n.contains("分类") || n.contains("类别") || n.contains("category") -> map["分类"] = i
                n.contains("商户") || n.contains("商家") || n.contains("备注") || n.contains("merchant") ||
                    n.contains("remark") || n.contains("note") -> map["商户"] = i
                n.contains("来源") || n.contains("账户") || n.contains("支付方式") || n.contains("source") ||
                    n.contains("account") -> map["来源"] = i
                n.contains("时间") || n.contains("日期") || n.contains("time") || n.contains("date") ->
                    map["时间"] = i
                n.contains("标签") || n.contains("tag") -> map["标签"] = i
                n.contains("账本") || n.contains("book") || n.contains("ledger") -> map["账本"] = i
            }
        }
        return map
    }

    private fun parseWithColumns(fields: List<String>, columns: Map<String, Int>): Transaction? {
        fun cell(key: String): String =
            columns[key]?.let { idx -> fields.getOrNull(idx)?.trim() ?: "" } ?: ""

        var amount: Double
        var type: Int
        if (columns.containsKey("金额")) {
            amount = parseAmount(cell("金额")) ?: return null
            type = parseType(cell("类型"))
        } else {
            val income = parseAmount(cell("收入金额"))
            val expense = parseAmount(cell("支出金额"))
            when {
                income != null && income > 0 -> {
                    amount = income
                    type = Transaction.TYPE_INCOME
                }
                expense != null && expense > 0 -> {
                    amount = expense
                    type = Transaction.TYPE_EXPENSE
                }
                else -> return null
            }
        }
        if (amount <= 0 || amount > Transaction.MAX_AMOUNT) return null

        val category = normalizeCategory(cell("分类"), type)
        val merchant = cell("商户").ifEmpty { category }
        val source = cell("来源")
        val timestamp = parseTimeFlexible(cell("时间")) ?: System.currentTimeMillis()
        return Transaction(
            amount = amount,
            type = type,
            category = category,
            merchant = merchant,
            source = source,
            timestamp = timestamp,
            tags = cell("标签"),
            book = cell("账本").ifEmpty { com.autobook.app.util.AppSettings.DEFAULT_BOOK }
        )
    }

    private fun normalizeCategory(raw: String, type: Int): String {
        val name = raw.trim()
        if (type == Transaction.TYPE_INCOME) {
            val stripped = name
                .removePrefix("${Categories.INCOME}/")
                .removePrefix("${Categories.INCOME}-")
                .trim()
            if (stripped.isEmpty() || stripped == Categories.INCOME) return Categories.INCOME
            return "${Categories.INCOME}/$stripped"
        }
        if (name.isEmpty()) return Categories.OTHER
        val parts = name.split("/", "-", "－", "—", "·")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        if (parts.size >= 2) {
            return "${normalizePrimary(parts[0])}/${parts[1]}"
        }
        Categories.mapLegacy(name).takeIf { it != name }?.let { return it }
        val primary = normalizePrimary(name)
        return if (primary == name) name else "$primary/$name"
    }

    private fun normalizePrimary(name: String): String {
        Categories.mapLegacy(name).takeIf { it != name }?.let { return it }
        if (Categories.expenseCategories.contains(name)) return name
        return CategoryClassifier.classifyExpense(name)
    }

    private fun parseAmount(text: String): Double? {
        if (text.isBlank()) return null
        val cleaned = text.replace(",", "").replace("¥", "").replace("￥", "").trim()
        return cleaned.toDoubleOrNull()
    }

    private fun parseType(text: String): Int {
        return if (text.contains("收") || text == "1") {
            Transaction.TYPE_INCOME
        } else {
            Transaction.TYPE_EXPENSE
        }
    }

    private fun parseTransaction(fields: List<String>): Transaction? {
        if (fields.size < 6) return null
        val amount = fields[0].trim().replace(",", "").toDoubleOrNull() ?: return null
        if (amount <= 0 || amount > Transaction.MAX_AMOUNT) return null
        val type = if (fields[1].trim() in listOf("收入", "1")) {
            Transaction.TYPE_INCOME
        } else {
            Transaction.TYPE_EXPENSE
        }
        val category = normalizeCategory(fields[2], type)
        val merchant = fields[3].trim().ifEmpty { category }
        val source = fields[4].trim()
        val timestamp = parseTime(fields[5].trim()) ?: System.currentTimeMillis()
        return Transaction(
            amount = amount,
            type = type,
            category = category,
            merchant = merchant,
            source = source,
            timestamp = timestamp,
            tags = fields.getOrNull(6)?.trim() ?: "",
            book = fields.getOrNull(7)?.trim()?.ifEmpty { com.autobook.app.util.AppSettings.DEFAULT_BOOK } ?: com.autobook.app.util.AppSettings.DEFAULT_BOOK
        )
    }

    private fun parseLine(line: String): List<String> {
        val fields = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val ch = line[i]
            when {
                ch == '"' -> {
                    if (inQuotes && i + 1 < line.length && line[i + 1] == '"') {
                        current.append('"')
                        i++
                    } else {
                        inQuotes = !inQuotes
                    }
                }
                ch == ',' && !inQuotes -> {
                    fields.add(current.toString())
                    current.clear()
                }
                else -> current.append(ch)
            }
            i++
        }
        fields.add(current.toString())
        return fields
    }

    private fun parseTime(text: String): Long? {
        return try {
            timeFormat.parse(text)?.time
        } catch (_: Exception) {
            null
        }
    }

    private val flexibleTimeFormats = listOf(
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA),
        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA),
        SimpleDateFormat("yyyy-MM-dd", Locale.CHINA),
        SimpleDateFormat("yyyy/M/d HH:mm:ss", Locale.CHINA),
        SimpleDateFormat("yyyy/M/d HH:mm", Locale.CHINA),
        SimpleDateFormat("yyyy/M/d", Locale.CHINA),
        SimpleDateFormat("yyyy年M月d日 HH:mm", Locale.CHINA),
        SimpleDateFormat("yyyy年M月d日", Locale.CHINA)
    ).onEach { it.isLenient = false }

    private fun parseTimeFlexible(text: String): Long? {
        if (text.isBlank()) return null
        synchronized(flexibleTimeFormats) {
            for (format in flexibleTimeFormats) {
                try {
                    return format.parse(text)?.time
                } catch (_: Exception) {
                }
            }
        }
        return null
    }

    private fun escape(field: String): String {
        if (field.contains(",") || field.contains("\"") || field.contains("\n")) {
            return "\"" + field.replace("\"", "\"\"") + "\""
        }
        return field
    }
}
