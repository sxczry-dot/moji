package com.autobook.app.parser

import com.autobook.app.data.Transaction

object SmsParser {

    private val bankNumbers = ParseRules.smsBankNumbers

    private val expenseKeywords = ParseRules.smsExpenseKeywords

    private val incomeKeywords = ParseRules.smsIncomeKeywords

    private val incomeToAccountRegex = Regex(
        """向[你您].{0,12}(?:账户|卡).{0,8}(?:完成|入账|存入|转入|汇入|到账|转账)""",
        RegexOption.DOT_MATCHES_ALL
    )

    /** "张三向你转账500元" 类点对点转入 */
    private val p2pTransferRegex = Regex("""向[你您][^，,。]{0,10}?转账""")

    /** "收到跨行转账2000元" 类转入（"收到转账"关键词覆盖不了跨行变体） */
    private val receivedTransferRegex = Regex("""收到[^，,。]{0,8}?转账""")

    private val amountRegex = Regex("""(?<![0-9.])(\d{1,3}(?:,\d{3})*(?:\.\d{1,2})?|\d+(?:\.\d{1,2})?)\s*元""")

    private val contextualAmountRegex = Regex(
        """(?:支出|消费|收入|付款|支付|退款|交易|扣款|入账|到账|实付|已扣)[^0-9¥￥]{0,6}[-－]?\s*(\d{1,3}(?:,\d{3})*(?:\.\d{1,2})?|\d+(?:\.\d{1,2})?)(?:元|人民币)?"""
    )

    private val prefixedAmountRegex = Regex(
        """(?:人民币|RMB|[¥￥])\s*[-－]?\s*(\d{1,3}(?:,\d{3})*(?:\.\d{1,2})?|\d+(?:\.\d{1,2})?)(?!\d)"""
    )

    private val merchantPatterns = listOf(
        Regex("""^(.{2,20}?)于.{0,16}?向您"""),
        Regex("""在[（(]?([^）)]{1,20})[）)]?发生"""),
        Regex("""在(.{1,20}?)(?:支付|消费|购买|缴纳)"""),
        Regex("""向(.{1,20}?)(?:完成|支付|消费|购买|还款)"""),
        Regex("""(?:商户|商家)[：:]\s*(.{1,20})"""),
        Regex("""于(.{1,20}?)(?:消费|支付)"""),
        // 兜底：正文括号内容（如"（京东商城）"）
        Regex("""[（(]([^）)]{1,20})[）)]""")
    )

    fun parse(sender: String, body: String): ParsedResult? {
        if (body.contains("验证码")) return null
        if (ParseRules.smsFinanceBlockWords.any { body.contains(it) }) return null
        if (ParseRules.smsMarketingWords.any { body.contains(it) }) return null
        if (ParseRules.summaryWords.any { body.contains(it) }) return null
        val isStrongExpense = ParseRules.smsStrongExpenseKeywords.any { body.contains(it) }
        val isExpense = expenseKeywords.any { body.contains(it) }
        // "消费退款/退货退款" 等含"退款"的短信是收入；只有"退款支出"类才按支出处理
        val hasRefundIncome = body.contains("退款") && !body.contains("退款支出")
        val isIncome = ((incomeKeywords.any { body.contains(it) } ||
            incomeToAccountRegex.containsMatchIn(body) ||
            p2pTransferRegex.containsMatchIn(body) ||
            receivedTransferRegex.containsMatchIn(body)) && !isStrongExpense) ||
            (hasRefundIncome && !body.contains("支出") && !body.contains("扣款"))
        if (!isExpense && !isIncome) return null

        val isBank = isBankSender(sender)
        if (!isBank) {
            val contextual = contextualAmountRegex.find(body) ?: return null
        }

        val amount = extractAmount(body) ?: return null
        if (amount < 0.01 || amount > Transaction.MAX_AMOUNT) return null

        val type = if (isIncome) Transaction.TYPE_INCOME else Transaction.TYPE_EXPENSE
        val merchant = extractMerchant(body)
        val tradeTime = com.autobook.app.util.TradeTimeParser.parse(body)
        val refNo = com.autobook.app.util.RefNoParser.parse(body)
        val balance = extractBalance(body)
        return ParsedResult(amount, type, merchant, body, tradeTime, refNo, balance)
    }

    private fun isBankSender(sender: String): Boolean {
        val digits = sender.filter { it.isDigit() }
        if (digits.length !in 5..6) return false
        return bankNumbers.any { digits.startsWith(it) }
    }

    private val rebateAmountRegex = Regex("""返\s*[¥￥]\s*\d+(?:\.\d{1,2})?(?:\s*元)?""")
    private val originalPriceRegex = Regex("""(?:原价|划线价|门市价)\s*\d+(?:\.\d{1,2})?\s*元""")
    private val balanceRegex = Regex(
        """余额\s*(?:为|：|:)?\s*(?:人民币)?\s*[¥￥]?\s*(\d{1,3}(?:,\d{3})*(?:\.\d{1,2})?|\d+(?:\.\d{1,2})?)(?=[^\d.]|$)(?:\s*元)?"""
    )
    /** 余额整段子句（供金额提取前剥离，防止"余额人民币500.00元，支出45.00元"把余额当交易金额） */
    private val balanceClauseRegex = Regex(
        """余额\s*(?:为|：|:)?\s*(?:人民币)?\s*[¥￥]?\s*\d[\d,]*(?:\.\d{1,2})?\s*元?"""
    )

    fun extractBalance(text: String): Double =
        balanceRegex.find(text)?.groupValues?.get(1)?.replace(",", "")?.toDoubleOrNull() ?: 0.0

    private fun extractAmount(body: String): Double? {
        val cleanedBody = body
            .replace(balanceClauseRegex, "")
            .replace(rebateAmountRegex, "")
            .replace(originalPriceRegex, "")
        val prefixed = prefixedAmountRegex.find(cleanedBody)
        if (prefixed != null) {
            return prefixed.groupValues[1].replace(",", "").toDoubleOrNull()
        }
        val match = amountRegex.find(cleanedBody) ?: return null
        return match.groupValues[1].replace(",", "").toDoubleOrNull()
    }

    private val bracketRegex = Regex("""【[^】]*】""")

    private fun isAccountPlaceholder(name: String): Boolean =
        name.contains("尾号") || name.contains("余额") || name == "您"

    /** "于11月08日19:52消费" 这类日期串不能当商户名 */
    private fun isDateLike(name: String): Boolean =
        name.count { it.isDigit() } >= 3

    /** 商户名清洗：优先取括号内容（"（京东商城）"→京东商城），再剥引导词与杂质 */
    private fun cleanMerchantName(raw: String): String {
        val bracketed = Regex("""[（(]([^）)]{1,20})[）)]""").find(raw)
        if (bracketed != null) {
            val inner = bracketed.groupValues[1].trim()
            if (inner.length >= 2 && !isAccountPlaceholder(inner) && !isDateLike(inner)) {
                return inner
            }
        }
        var name = raw.replace(bracketRegex, "").trim()
        name = name.replace(Regex("""^[你我已向在之于的为]+"""), "")
        name = name.replace(Regex("""(发生|快捷|交易|付款|收款|支付)$"""), "")
            .trim()
        return if (name.length >= 2 && !isAccountPlaceholder(name) && !isDateLike(name)) {
            name
        } else {
            ""
        }
    }

    private fun extractMerchant(body: String): String {
        for (p in merchantPatterns) {
            val m = p.find(body) ?: continue
            val cleaned = cleanMerchantName(m.groupValues[1].trim())
            if (cleaned.isNotEmpty()) return cleaned
        }
        return ""
    }
}
