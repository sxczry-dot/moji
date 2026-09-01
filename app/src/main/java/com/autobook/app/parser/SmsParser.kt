package com.autobook.app.parser

import com.autobook.app.data.Transaction

object SmsParser {

    private val bankNumbers = listOf(
        "95588", "95533", "95555", "95566", "95599", "95568", "95528",
        "95558", "95577", "95561", "95501", "95595", "95508", "95559",
        "95511", "95516", "95580", "95594", "96699", "95567"
    )

    private val expenseKeywords = listOf(
        "消费", "支出", "支付", "扣款", "取款", "转账支出", "划出", "代扣", "交易", "转账"
    )

    private val incomeKeywords = listOf(
        "入账", "存入", "转入", "转账收入", "收到转账", "退款", "工资", "收入", "汇入", "到账"
    )

    private val amountRegex = Regex("""(\d{1,3}(?:,\d{3})*(?:\.\d{1,2})?|\d+(?:\.\d{1,2})?)\s*元""")

    private val contextualAmountRegex = Regex(
        """(?:支出|消费|收入|付款|支付|退款|交易|扣款|入账|到账|实付|已扣)[^0-9¥￥]{0,6}[-－]?\s*(\d{1,3}(?:,\d{3})*(?:\.\d{1,2})?|\d+(?:\.\d{1,2})?)(?:元|人民币)?"""
    )

    private val prefixedAmountRegex = Regex(
        """(?:人民币|RMB|[¥￥])\s*[-－]?\s*(\d{1,3}(?:,\d{3})*(?:\.\d{1,2})?|\d+(?:\.\d{1,2})?)"""
    )

    private val merchantPatterns = listOf(
        Regex("""在(.{1,20}?)(?:支付|消费|购买|缴纳)"""),
        Regex("""向(.{1,20}?)(?:完成|支付|消费|购买)"""),
        Regex("""(?:商户|商家)[：:]\s*(.{1,20})"""),
        Regex("""于(.{1,20}?)(?:消费|支付)""")
    )

    fun parse(sender: String, body: String): ParsedResult? {
        if (body.contains("验证码")) return null
        if (listOf("零钱通", "余额宝", "理财通", "理财", "申购", "赎回", "基金", "国债").any { body.contains(it) }) return null
        if (listOf("优惠券", "立减", "满减", "抽奖", "待领取", "点击领取", "礼包", "邀您").any { body.contains(it) }) return null
        val isStrongExpense = listOf("支出", "消费", "扣款", "取款", "划出").any { body.contains(it) }
        val isExpense = expenseKeywords.any { body.contains(it) }
        val isIncome = incomeKeywords.any { body.contains(it) } && !isStrongExpense
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
        return ParsedResult(amount, type, merchant, body, tradeTime, refNo)
    }

    private fun isBankSender(sender: String): Boolean {
        val digits = sender.filter { it.isDigit() }
        if (digits.length !in 5..6) return false
        return bankNumbers.any { digits.startsWith(it) }
    }

    private val rebateAmountRegex = Regex("""返\s*[¥￥]\s*\d+(?:\.\d{1,2})?(?:\s*元)?""")

    private fun extractAmount(body: String): Double? {
        val cleanedBody = body.replace(rebateAmountRegex, "")
        val prefixed = prefixedAmountRegex.find(cleanedBody)
        if (prefixed != null) {
            return prefixed.groupValues[1].replace(",", "").toDoubleOrNull()
        }
        val match = amountRegex.find(cleanedBody) ?: return null
        return match.groupValues[1].replace(",", "").toDoubleOrNull()
    }

    private fun extractMerchant(body: String): String {
        for (p in merchantPatterns) {
            val m = p.find(body) ?: continue
            val name = m.groupValues[1].trim()
            if (name.isNotEmpty()) return name
        }
        return ""
    }
}
