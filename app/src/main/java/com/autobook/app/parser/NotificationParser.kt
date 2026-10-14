package com.autobook.app.parser

import com.autobook.app.data.Transaction

object NotificationParser {

    const val WECHAT_PKG = "com.tencent.mm"
    const val ALIPAY_PKG = "com.eg.android.AlipayGphone"
    const val UNIONPAY_PKG = "com.unionpay"
    const val JD_PKG = "com.jingdong.app.mall"

    val BANK_PKGS = ParseRules.bankPackages

    fun bankNameOf(pkg: String): String? = BANK_PKGS[pkg]

    private val amountRegex = Regex("""[¥￥]\s*(\d+(?:\.\d{1,2})?)(?!\d)|(?<![0-9.])(\d+(?:\.\d{1,2})?)\s*元""")

    private val merchantPatterns = listOf(
        Regex("""^(.{2,20}?)于.{0,16}?向您"""),
        Regex("""(?:商家|商户|收款方)[：:]\s*([^，,。;；\s]{1,20})"""),
        Regex("""在(.{2,20}?)(?:支付|付款|消费|购买)"""),
        Regex("""向(.{1,20}?)(?:完成|支付|消费|购买)"""),
        Regex("""(.{1,20}?)(?:收款|付款)成功""")
    )

    private val paymentKeywords = ParseRules.paymentKeywords

    private val incomeToAccountRegex = Regex(
        """向您.{0,12}(?:账户|卡).{0,8}(?:完成|入账|存入|转入|汇入|到账|转账)""",
        RegexOption.DOT_MATCHES_ALL
    )

    fun parse(packageName: String, title: String?, text: String?): ParsedResult? {
        if (packageName != WECHAT_PKG && packageName != ALIPAY_PKG &&
            packageName != UNIONPAY_PKG && packageName != JD_PKG &&
            bankNameOf(packageName) == null
        ) return null
        val t = title ?: ""
        val c = text ?: ""
        val full = "$t $c"

        if (packageName == WECHAT_PKG &&
            !t.contains("支付") && !t.contains("收款") && !t.contains("转账") && !t.contains("缴费")
        ) return null

        if (bankNameOf(packageName) != null &&
            ParseRules.bankGateWords.none { full.contains(it) }
        ) return null
        if (packageName == ALIPAY_PKG && !t.contains("支付宝") && !t.contains("支付") &&
            !t.contains("花呗") && !t.contains("还款") && !t.contains("缴费")
        ) {
            val hasTradeInText = yuanAmountRegex.find(c) != null &&
                paymentKeywords.any { c.contains(it) }
            if (!hasTradeInText) return null
        }
        if (packageName == UNIONPAY_PKG && !t.contains("云闪付") && !t.contains("银联")) return null

        if (ParseRules.financialBlockWords.any { full.contains(it) }) return null

        if (ParseRules.summaryWords.any { full.contains(it) }) return null

        if (paymentKeywords.none { full.contains(it) }) return null

        val textForAmount = full.replace("支付宝", "")
        if (ParseRules.marketingStrongWords.any { full.contains(it) }) return null
        if (ParseRules.marketingWeakWords.any { full.contains(it) } &&
            !full.contains("¥") && !full.contains("￥") &&
            yuanAmountRegex.find(textForAmount) == null
        ) return null

        val amount = extractAmount(c) ?: extractAmount(t) ?: return null
        if (amount < 0.01 || amount > Transaction.MAX_AMOUNT) return null

        val isStrongExpense = ParseRules.strongExpenseKeywords.any { full.contains(it) }
        val isIncome = (ParseRules.refundIncomeKeywords.any { full.contains(it) } ||
            incomeToAccountRegex.containsMatchIn(full)) &&
            !full.contains("收款方") && !full.contains("收款人") && !isStrongExpense
        val type = if (isIncome) Transaction.TYPE_INCOME else Transaction.TYPE_EXPENSE

        val merchant = if (bankNameOf(packageName) != null) {
            extractMerchantFromText(c).ifEmpty { extractMerchantTitle(t) }
        } else {
            extractMerchant(t, c)
        }
        val tradeTime = com.autobook.app.util.TradeTimeParser.parse(full)
        val refNo = com.autobook.app.util.RefNoParser.parse(full)
        val balance = extractBalance(full)
        return ParsedResult(amount, type, merchant, full, tradeTime, refNo, balance)
    }

    private val contextualAmountRegex = Regex(
        """(?:支出|消费|收入|付款|支付|退款|交易|转账|实付|已扣|还款|缴费|缴纳)[^0-9¥￥]{0,6}[-－]?\s*(?<![0-9.])(\d{1,3}(?:,\d{3})*(?:\.\d{1,2})?|\d+(?:\.\d{1,2})?)"""
    )

    private val rebateAmountRegex = Regex("""返\s*[¥￥]\s*\d+(?:\.\d{1,2})?(?:\s*元)?""")
    private val originalPriceRegex = Regex("""(?:原价|划线价|门市价)\s*\d+(?:\.\d{1,2})?\s*元""")
    private val balanceRegex = Regex(
        """余额\s*(?:为|：|:)?\s*[¥￥]?\s*(\d{1,3}(?:,\d{3})*(?:\.\d{1,2})?|\d+(?:\.\d{1,2})?)(?=[^\d.]|$)(?:\s*元)?"""
    )

    fun extractBalance(text: String): Double =
        balanceRegex.find(text)?.groupValues?.get(1)?.replace(",", "")?.toDoubleOrNull() ?: 0.0

    private val yuanAmountRegex = Regex("""(?<![0-9.])(\d+(?:\.\d{1,2})?)\s*元""")

    private fun extractAmount(text: String): Double? {
        val cleaned = text.replace(",", "").replace("支付宝", "")
            .replace(rebateAmountRegex, "").replace(originalPriceRegex, "")
        yuanAmountRegex.find(cleaned)?.let { m ->
            m.groupValues[1].toDoubleOrNull()?.let { return it }
        }
        val contextual = contextualAmountRegex.find(cleaned)
        if (contextual != null) {
            return contextual.groupValues[1].toDoubleOrNull()
        }
        val match = amountRegex.find(cleaned) ?: return null
        return (match.groups[1]?.value ?: match.groups[2]?.value)?.toDoubleOrNull()
    }

    private fun isAccountPlaceholder(name: String): Boolean =
        name.contains("尾号") || name.contains("余额") || name.contains("账户") || name == "您"

    private val merchantStopWords = setOf("已", "您", "我", "本", "该", "成功", "一笔", "在线")

    private fun extractMerchantFromText(text: String): String {
        for (p in merchantPatterns) {
            val m = p.find(text) ?: continue
            val name = m.groupValues[1].trim()
            if (name.length >= 2 && !isAccountPlaceholder(name) && name !in merchantStopWords) return name
        }
        return ""
    }

    private fun extractMerchantTitle(title: String): String {
        val cleaned = title.replace("微信支付", "").replace("支付宝", "").replace("中国农业银行", "").replace("工商银行", "").replace("建设银行", "").replace("中国银行", "").replace("招商银行", "").replace("交通银行", "").replace("邮储银行", "").replace("中信银行", "").replace("光大银行", "").replace("民生银行", "").replace("平安银行", "").replace("广发银行", "").replace("兴业银行", "").replace("浦发银行", "").trim()
        val genericTitles = ParseRules.genericTitles
        return if (cleaned.isNotEmpty() && cleaned.length <= 20 && cleaned !in genericTitles) {
            cleaned
        } else {
            ""
        }
    }

    private fun extractMerchant(title: String, text: String): String {
        val cleaned = title
            .replace("微信支付", "")
            .replace("微信收款助手", "")
            .replace("支付助手", "")
            .replace("支付宝", "")
            .replace("微信", "")
            .replace("收款成功", "")
            .replace("付款成功", "")
            .replace("支付成功", "")
            .replace("-", " ")
            .replace("－", " ")
            .replace("：", " ")
            .replace(":", " ")
            .trim()
        if (cleaned.isNotEmpty() && cleaned.length <= 20 &&
            cleaned !in ParseRules.genericTitles
        ) return cleaned

        // 通用标题（交易提醒/动账通知等）或空标题：从正文提取商户
        for (p in textMerchantPatterns) {
            val m = p.find(text) ?: continue
            val name = m.groupValues[1].trim()
            if (name.length >= 2 && !isAccountPlaceholder(name) && name !in merchantStopWords) return name
        }
        return ""
    }

    /** 正文商户提取模式："你在起点读书有一笔…" 等支付宝句式 */
    private val textMerchantPatterns = listOf(
        Regex("""你在(.{1,16}?)(?:有|购买|消费|支付|平台|商户|商城)""")
    ) + merchantPatterns
}
