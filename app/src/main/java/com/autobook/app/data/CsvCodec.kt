package com.autobook.app.data

import com.autobook.app.parser.CategoryClassifier
import com.autobook.app.util.formatAmount
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CsvCodec {

    private const val HEADER = "金额,类型,分类,商户,来源,时间,标签,账本"

    fun encode(transactions: List<Transaction>): String {
        // 时间含毫秒：导入去重按精确时间戳比对，丢毫秒会导致重新导入必重复
        val timeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.CHINA)
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

    fun decode(
        content: String,
        defaultBook: String = com.autobook.app.util.AppSettings.DEFAULT_BOOK
    ): List<Transaction> {
        // 引号内换行按字段内容处理，而非记录分隔（先统一行尾再按引号状态合并物理行）
        val lines = splitCsvLines(
            content.replace("\r\n", "\n").replace('\r', '\n')
        )
            .map { parseLine(it) }
            .toList()
        if (lines.isEmpty()) return emptyList()

        val header = lines.first()
        val hasHeader = header.any {
            it.contains("金额") || it.contains("分类") || it.contains("备注") ||
                it.contains("amount", true) || it.contains("category", true) ||
                it.contains("date", true)
        }
        if (!hasHeader) {
            return lines.mapNotNull { parseTransaction(it, defaultBook) }
        }
        val columns = buildColumnMap(header)
        if (!columns.containsKey("金额") &&
            !(columns.containsKey("收入金额") && columns.containsKey("支出金额"))
        ) {
            return emptyList()
        }
        val rows = lines.subList(1, lines.size)
        return rows.mapNotNull { parseWithColumns(it, columns, defaultBook) }
    }

    private fun splitCsvLines(content: String): List<String> {
        val result = mutableListOf<String>()
        val sb = StringBuilder()
        var inQuotes = false
        content.forEach { ch ->
            when {
                ch == '"' -> {
                    sb.append(ch)
                    inQuotes = !inQuotes
                }
                ch == '\n' -> {
                    if (inQuotes) {
                        sb.append('\n')
                    } else {
                        result.add(sb.toString())
                        sb.clear()
                    }
                }
                else -> sb.append(ch)
            }
        }
        if (sb.isNotEmpty()) result.add(sb.toString())
        return result
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

    private fun parseWithColumns(
        fields: List<String>,
        columns: Map<String, Int>,
        defaultBook: String
    ): Transaction? {
        fun cell(key: String): String =
            columns[key]?.let { idx -> fields.getOrNull(idx)?.trim() ?: "" } ?: ""

        var amount: Double
        var type: Int
        if (columns.containsKey("金额")) {
            val rawAmount = parseAmount(cell("金额")) ?: return null
            // 负数金额（银行流水常见：负=支出）：取绝对值；有类型列时以类型列为准
            type = if (columns.containsKey("类型") && cell("类型").isNotBlank()) {
                parseType(cell("类型"))
            } else {
                Transaction.TYPE_EXPENSE
            }
            amount = kotlin.math.abs(rawAmount)
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
        // 时间列存在但解析失败的行跳过（避免"当前时间"兜底导致重复导入翻倍）；时间列缺失才用导入时刻
        val timeCell = cell("时间")
        val timestamp = if (timeCell.isBlank()) {
            System.currentTimeMillis()
        } else {
            parseTimeFlexible(timeCell) ?: return null
        }
        return Transaction(
            amount = amount,
            type = type,
            category = category,
            merchant = merchant,
            source = source,
            timestamp = timestamp,
            tags = cell("标签"),
            book = cell("账本").ifEmpty { defaultBook }
        )
    }

    private fun normalizeCategory(raw: String, type: Int): String {
        val name = raw.trim()
        if (type == Transaction.TYPE_INCOME) return Categories.INCOME
        if (name.isEmpty()) return Categories.OTHER
        val parts = name.split("/", "-", "－", "—", "·")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        if (parts.size >= 2) return normalizePrimary(parts[0])
        return normalizePrimary(name)
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

    private fun parseTransaction(fields: List<String>, defaultBook: String): Transaction? {
        if (fields.size < 6) return null
        val rawAmount = fields[0].trim().replace(",", "").toDoubleOrNull() ?: return null
        val amount = kotlin.math.abs(rawAmount)
        if (amount <= 0 || amount > Transaction.MAX_AMOUNT) return null
        val type = if (fields[1].trim() in listOf("收入", "1")) {
            Transaction.TYPE_INCOME
        } else {
            Transaction.TYPE_EXPENSE
        }
        val category = normalizeCategory(fields[2], type)
        val merchant = fields[3].trim().ifEmpty { category }
        val source = fields[4].trim()
        val timestamp = parseTime(fields[5].trim()) ?: return null
        return Transaction(
            amount = amount,
            type = type,
            category = category,
            merchant = merchant,
            source = source,
            timestamp = timestamp,
            tags = fields.getOrNull(6)?.trim() ?: "",
            book = fields.getOrNull(7)?.trim()?.ifEmpty { defaultBook } ?: defaultBook
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
        val timeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA)
        return try {
            timeFormat.parse(text)?.time
        } catch (_: Exception) {
            null
        }
    }

    private val flexibleTimeFormats = listOf(
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.CHINA),
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA),
        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA),
        SimpleDateFormat("yyyy-MM-dd", Locale.CHINA),
        SimpleDateFormat("yyyy-M-d HH:mm:ss", Locale.CHINA),
        SimpleDateFormat("yyyy-M-d HH:mm", Locale.CHINA),
        SimpleDateFormat("yyyy-M-d", Locale.CHINA),
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
