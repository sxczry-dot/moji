package com.autobook.app

import com.autobook.app.data.Transaction
import com.autobook.app.parser.NotificationParser
import com.autobook.app.parser.SmsParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * 真实通知语料大测试·第三期：转账/还款/红包/缴费/营销/长号码/金额格式变体。
 */
class NotificationCorpus3Test {

    private val EXP = Transaction.TYPE_EXPENSE
    private val INC = Transaction.TYPE_INCOME

    private data class NCase(
        val desc: String,
        val pkg: String,
        val title: String?,
        val text: String?,
        val expAmount: Double?,
        val expType: Int?,
        val expMerchant: String? = null
    )

    private val notifCases = listOf(
        NCase("微信商家退款", NotificationParser.WECHAT_PKG, "微信支付", "商家已退款50.00元，原路退回", 50.0, INC),
        NCase("微信收到红包无金额拦截", NotificationParser.WECHAT_PKG, "微信支付", "你收到一个红包，请在24小时内查收", null, null),
        NCase("微信金额千分位", NotificationParser.WECHAT_PKG, "微信支付", "已支付1,234.50元", 1234.5, EXP),
        NCase("微信支付带剩余红包", NotificationParser.WECHAT_PKG, "微信支付", "支付45.00元，剩余红包3.00元待使用", 45.0, EXP),
        NCase("支付宝金额带币符", NotificationParser.ALIPAY_PKG, "支付宝", "你已成功付款¥45.00", 45.0, EXP),
        NCase("支付宝工资到账", NotificationParser.ALIPAY_PKG, "支付宝", "工资到账5,000.00元", 5000.0, INC),
        NCase("支付宝收款方转账记支出", NotificationParser.ALIPAY_PKG, "支付宝", "已向收款方李四转账100.00元", 100.0, EXP),
        NCase("支付宝定期存款拦截", NotificationParser.ALIPAY_PKG, "支付宝", "定期存款存入5,000.00元", null, null),
        NCase("支付宝国债拦截", NotificationParser.ALIPAY_PKG, "支付宝", "国债购买成功100.00元", null, null),
        NCase("工行推送存入", "com.icbc", "中国工商银行", "您尾号1234卡存入人民币2,000.00元，余额12,000.00元。", 2000.0, INC),
        NCase("非目标应用拦截", "com.example.foo", "转账成功", "转账成功100.00元", null, null),
        NCase("云闪付缴费", NotificationParser.UNIONPAY_PKG, "云闪付", "缴费成功：水费45.60元", 45.6, EXP)
    )

    private data class SCase(
        val desc: String,
        val sender: String,
        val body: String,
        val expAmount: Double?,
        val expType: Int?,
        val expBalance: Double = 0.0,
        val expMerchant: String? = null
    )

    private val smsCases = listOf(
        SCase("跨行转账收入", "95566", "您尾号1234账户11月08日19:52收到跨行转账人民币2,000.00元，余额10,000.00元。", 2000.0, INC, 10000.0),
        SCase("工资入账", "95588", "您尾号1234账户11月08日19:52工资入账人民币8,000.00元，余额28,000.00元。", 8000.0, INC, 28000.0),
        SCase("银联入账", "95595", "您尾号1234账户11月08日19:52银联入账人民币1,000.00元，余额9,000.00元。", 1000.0, INC, 9000.0),
        SCase("POS消费", "95559", "您尾号1234卡11月08日19:52POS消费人民币20.00元，余额1,214.56元。", 20.0, EXP, 1214.56),
        SCase("中信信用卡还款", "95558", "您尾号1234信用卡11月08日还款人民币500.00元，已恢复额度。", 500.0, EXP),
        SCase("金额带正号", "95528", "您尾号1234卡11月08日19:52消费人民币+25.00元。", 25.0, EXP),
        SCase("金额元整后缀", "95599", "您尾号3174账户11月08日19:52消费人民币100元整。", 100.0, EXP),
        SCase("下载APP领红包拦截", "1069066666", "下载我行APP领取88元红包，还有话费券。", null, null),
        SCase("节假日理财营销拦截", "95533", "【建设银行】新春理财季，业绩比较基准3.2%，详情点击。", null, null),
        SCase("余额不足提醒拦截", "95588", "您尾号1234账户余额不足，自动扣款失败。", null, null)
    )

    @Test
    fun `通知语料第三期全量验证`() {
        for (c in notifCases) {
            val r = NotificationParser.parse(c.pkg, c.title, c.text)
            if (c.expAmount == null) {
                assertNull("${c.desc} 应拦截但解析了", r)
            } else {
                assertNotNull("${c.desc} 应解析但未识别", r)
                assertEquals("${c.desc} 金额", c.expAmount, r!!.amount, 0.001)
                assertEquals("${c.desc} 方向", c.expType, r.type)
                c.expMerchant?.let { m -> assertEquals("${c.desc} 商户", m, r.merchant) }
            }
        }
    }

    @Test
    fun `短信语料第三期全量验证`() {
        for (c in smsCases) {
            val r = SmsParser.parse(c.sender, c.body)
            if (c.expAmount == null) {
                assertNull("${c.desc} 应拦截但解析了", r)
            } else {
                assertNotNull("${c.desc} 应解析但未识别", r)
                assertEquals("${c.desc} 金额", c.expAmount, r!!.amount, 0.001)
                assertEquals("${c.desc} 方向", c.expType, r.type)
                if (c.expBalance > 0) {
                    assertEquals("${c.desc} 余额", c.expBalance, r.balance, 0.001)
                }
                c.expMerchant?.let { m -> assertEquals("${c.desc} 商户", m, r.merchant) }
            }
        }
    }
}
