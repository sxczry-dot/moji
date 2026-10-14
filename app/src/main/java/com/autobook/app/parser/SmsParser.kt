package com.autobook.app.parser

import com.autobook.app.data.Transaction

object SmsParser {

    private val bankNumbers = ParseRules.smsBankNumbers

    private val expenseKeywords = ParseRules.smsExpenseKeywords

    private val incomeKeywords = ParseRules.smsIncomeKeywords

    private val incomeToAccountRegex = Regex(
        """向您.{0,12}(?:账户|卡).{0,8}(?:完成|入账|存入|转入|汇入|到账|转账)""",
        RegexOption.DOT_MATCHES_ALL
    )

    private val amountRegex = Regex("""(?<![0-9.])(\d{1,3}(?:,\d{3})*(?:\.\d{1,2})?|\d+(?:\.\d{1,2})?)\s*元""")

    private val contextualAmountRegex = Regex(
        """(?:支出|消费|收入|付款|支付|退款|交易|扣款|入账|到账|实付|已扣)[^0-9¥￥]{0,6}[-－]?\s*(\d{1,3}(?:,\d{3})*(?:\.\d{1,2})?|\d+(?:\.\d{1,2})?)(?:元|人民币)?"""
    )

    private val prefixedAmountRegex = Regex(
        """(?:人民币|RMB|[¥￥])\s*[-－]?\s*(\d{1,3}(?:,\d{3})*(?:\.\d{1,2})?|\d+(?:\.\d{1,2})?)(?!\d)"""
    )

    private val merchantPatterns = listOf(
        Regex("""^(.{2,20}?)于.{0,16}?向您"""),
        Regex("""在(.{1,20}?)(?:支付|消费|购买|缴纳)"""),
        Regex("""向(.{1,20}?)(?:完成|支付|消费|购买)"""),
        Regex("""(?:商户|商家)[：:]\s*(.{1,20})"""),
        Regex("""于(.{1,20}?)(?:消费|支付)""")
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
            incomeToAccountRegex.containsMatchIn(body)) && !isStrongExpense) ||
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
        """余额\s*(?:为|：|:)?\s*[¥￥]?\s*(\d{1,3}(?:,\d{3})*(?:\.\d{1,2})?|\d+(?:\.\d{1,2})?)(?=[^\d.]|$)(?:\s*元)?"""
    )

    fun extractBalance(text: String): Double =
        balanceRegex.find(text)?.groupValues?.get(1)?.replace(",", "")?.toDoubleOrNull() ?: 0.0

    private fun extractAmount(body: String): Double? {
        val cleanedBody = body.replace(rebateAmountRegex, "").replace(originalPriceRegex, "")
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

    private fun extractMerchant(body: String): String {
        for (p in merchantPatterns) {
            val m = p.find(body) ?: continue
            val name = m.groupValues[1].replace(bracketRegex, "").trim()
            if (name.isNotEmpty() && !isAccountPlaceholder(name)) return name
        }
        return ""
    }
}
