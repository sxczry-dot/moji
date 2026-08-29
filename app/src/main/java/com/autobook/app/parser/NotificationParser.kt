package com.autobook.app.parser

import com.autobook.app.data.Transaction

object NotificationParser {

    const val WECHAT_PKG = "com.tencent.mm"
    const val ALIPAY_PKG = "com.eg.android.AlipayGphone"
    const val UNIONPAY_PKG = "com.unionpay"
    const val JD_PKG = "com.jingdong.app.mall"

    val BANK_PKGS = mapOf(
        "com.android.bankabc" to "农业银行",
        "com.icbc" to "工商银行",
        "com.chinamworld.main" to "建设银行",
        "com.chinamworld.bocmbci" to "中国银行",
        "cmb.pb" to "招商银行",
        "com.cmbchina.ccd.pluto.cmbActivity" to "招商银行",
        "com.bankcomm.Bankcomm" to "交通银行",
        "com.yitong.mbank.psbc" to "邮储银行",
        "com.ecitic.bank.mobile" to "中信银行",
        "com.cebbank.mobile.cemb" to "光大银行",
        "cn.com.cmbc.newmbank" to "民生银行",
        "com.pingan.paces.ccms" to "平安银行",
        "com.cgbchina.xpt" to "广发银行",
        "com.cib.cibmb" to "兴业银行",
        "cn.com.spdb.mobilebank.per" to "浦发银行",
        "com.srcb.pmbank" to "上海银行",
        "com.nbbank" to "宁波银行"
    )

    fun bankNameOf(pkg: String): String? = BANK_PKGS[pkg]

    private val amountRegex = Regex("""[¥￥]\s*(\d+(?:\.\d{1,2})?)|(\d+(?:\.\d{1,2})?)\s*元""")

    private val merchantPatterns = listOf(
        Regex("""(?:商家|商户|收款方)[：:]\s*(.{1,20})"""),
        Regex("""在(.{1,20}?)(?:支付|付款|消费|购买)"""),
        Regex("""向(.{1,20}?)(?:完成|支付|消费|购买)"""),
        Regex("""(.{1,20}?)(?:收款|付款)成功""")
    )

    private val paymentKeywords = listOf(
        "支付", "付款", "收款", "转账", "退款", "消费", "入账", "扣款", "支出", "交易", "还款"
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
            !t.contains("支付") && !t.contains("收款") && !t.contains("转账")
        ) return null

        if (bankNameOf(packageName) != null &&
            !full.contains("银行") && !full.contains("支出") && !full.contains("收入") &&
            !full.contains("消费") && !full.contains("交易") && !full.contains("支付")
        ) return null
        if (packageName == ALIPAY_PKG && !t.contains("支付宝") && !t.contains("支付") &&
            !t.contains("花呗") && !t.contains("还款")
        ) return null
        if (packageName == UNIONPAY_PKG && !t.contains("云闪付") && !t.contains("银联")) return null

        val financialWords = listOf("零钱通", "余额宝", "理财通", "理财", "申购", "赎回", "基金", "国债", "定期存款", "大额存单")
        if (financialWords.any { full.contains(it) }) return null

        if (paymentKeywords.none { full.contains(it) }) return null

        val textForAmount = full.replace("支付宝", "")
        val strongMarketingWords = listOf("立减金", "优惠券", "满减", "抽奖", "邀您", "礼包")
        if (strongMarketingWords.any { full.contains(it) }) return null
        val weakMarketingWords = listOf("积分", "待领取", "点击领取", "限时")
        if (weakMarketingWords.any { full.contains(it) } &&
            !full.contains("¥") && !full.contains("￥") &&
            contextualAmountRegex.find(textForAmount) == null
        ) return null

        val amount = extractAmount(c) ?: extractAmount(t) ?: return null
        if (amount < 0.01 || amount > Transaction.MAX_AMOUNT) return null

        val isStrongExpense = listOf("支出", "扣款").any { full.contains(it) }
        val isIncome = listOf("收款", "入账", "转入", "收到", "退款", "返现")
            .any { full.contains(it) } &&
            !full.contains("收款方") && !full.contains("收款人") && !isStrongExpense
        val type = if (isIncome) Transaction.TYPE_INCOME else Transaction.TYPE_EXPENSE

        val merchant = if (bankNameOf(packageName) != null) {
            extractMerchantFromText(c).ifEmpty { extractMerchantTitle(t) }
        } else {
            extractMerchant(t, c)
        }
        val tradeTime = com.autobook.app.util.TradeTimeParser.parse(full)
        val refNo = com.autobook.app.util.RefNoParser.parse(full)
        return ParsedResult(amount, type, merchant, full, tradeTime, refNo)
    }

    private val contextualAmountRegex = Regex(
        """(?:支出|消费|收入|付款|支付|退款|交易|转账|实付|已扣|还款)[^0-9¥￥]{0,6}[-－]?\s*(\d{1,3}(?:,\d{3})*(?:\.\d{1,2})?|\d+(?:\.\d{1,2})?)"""
    )

    private fun extractAmount(text: String): Double? {
        val cleaned = text.replace(",", "").replace("支付宝", "")
        val contextual = contextualAmountRegex.find(cleaned)
        if (contextual != null) {
            return contextual.groupValues[1].toDoubleOrNull()
        }
        val match = amountRegex.find(cleaned) ?: return null
        return match.groups[1]?.value?.toDoubleOrNull()
    }

    private fun extractMerchantFromText(text: String): String {
        for (p in merchantPatterns) {
            val m = p.find(text) ?: continue
            val name = m.groupValues[1].trim()
            if (name.isNotEmpty()) return name
        }
        return ""
    }

    private fun extractMerchantTitle(title: String): String {
        val cleaned = title.replace("微信支付", "").replace("支付宝", "").replace("中国农业银行", "").replace("工商银行", "").replace("建设银行", "").replace("中国银行", "").replace("招商银行", "").replace("交通银行", "").replace("邮储银行", "").replace("中信银行", "").replace("光大银行", "").replace("民生银行", "").replace("平安银行", "").replace("广发银行", "").replace("兴业银行", "").replace("浦发银行", "").trim()
        val genericTitles = setOf(
            "交易提醒", "动账通知", "账户变动", "账户变动提醒", "消费通知",
            "收支通知", "入账通知", "交易通知", "动账提醒", "资金变动"
        )
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
        if (cleaned.isNotEmpty() && cleaned.length <= 20) return cleaned

        for (p in merchantPatterns) {
            val m = p.find(text) ?: continue
            val name = m.groupValues[1].trim()
            if (name.isNotEmpty()) return name
        }
        return ""
    }
}
