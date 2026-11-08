package com.autobook.app

import com.autobook.app.data.Transaction
import com.autobook.app.parser.NotificationParser
import com.autobook.app.parser.SmsParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 真实通知语料大测试：覆盖微信/支付宝/云闪付/京东推送与工建招农中浦发光中信民平安等银行短信。
 * 每条断言金额/方向/余额，部分断言商户提取质量；null 期望 = 应拦截不记账。
 */
class NotificationCorpusTest {

    private val EXP = Transaction.TYPE_EXPENSE
    private val INC = Transaction.TYPE_INCOME

    private data class NCase(
        val desc: String,
        val pkg: String,
        val title: String?,
        val text: String?,
        val expAmount: Double?,
        val expType: Int?
    )

    private val notifCases = listOf(
        // ===== 微信 =====
        NCase("微信付款成功", NotificationParser.WECHAT_PKG, "微信支付", "已支付38.00元", 38.0, EXP),
        NCase("微信向商户付款", NotificationParser.WECHAT_PKG, "微信支付", "你已向美团外卖付款成功38.00元", 38.0, EXP),
        NCase("微信收款助手", NotificationParser.WECHAT_PKG, "微信收款助手", "你收到一笔收款：50.00元", 50.0, INC),
        NCase("微信退款到账", NotificationParser.WECHAT_PKG, "微信支付", "退款到账通知：50.00元已原路退回", 50.0, INC),
        NCase("微信亲属卡扣款", NotificationParser.WECHAT_PKG, "微信支付", "亲属卡扣款凭证：为亲人支付28.00元", 28.0, EXP),
        NCase("微信零钱通拦截", NotificationParser.WECHAT_PKG, "微信支付", "零钱通转入500.00元", null, null),
        NCase("微信理财通拦截", NotificationParser.WECHAT_PKG, "微信支付", "理财通申购成功1000.00元", null, null),
        NCase("微信营销优惠券拦截", NotificationParser.WECHAT_PKG, "微信支付", "你有一张5元优惠券待使用", null, null),
        // ===== 支付宝 =====
        NCase("支付宝付款", NotificationParser.ALIPAY_PKG, "支付宝", "你已成功付款45.00元", 45.0, EXP),
        NCase("支付宝转账给朋友", NotificationParser.ALIPAY_PKG, "支付宝", "你已向李四转账100.00元", 100.0, EXP),
        NCase("支付宝收到转账", NotificationParser.ALIPAY_PKG, "支付宝", "张三已向你转账100.00元", 100.0, INC),
        NCase("支付宝花呗还款", NotificationParser.ALIPAY_PKG, "花呗", "花呗自动还款成功500.00元", 500.0, EXP),
        NCase("支付宝退款", NotificationParser.ALIPAY_PKG, "支付宝", "退款成功：50.00元已退回原支付方式", 50.0, INC),
        NCase("支付宝余额宝拦截", NotificationParser.ALIPAY_PKG, "支付宝", "你的余额宝昨日收益1.23元", null, null),
        NCase("支付宝基金拦截", NotificationParser.ALIPAY_PKG, "支付宝", "基金申购确认成功200.00元", null, null),
        NCase("支付宝优惠券拦截", NotificationParser.ALIPAY_PKG, "支付宝", "你有1张优惠券待领取，满10减5", null, null),
        NCase("支付宝积分拦截", NotificationParser.ALIPAY_PKG, "支付宝", "点击领取9个支付宝积分。", null, null),
        // ===== 云闪付 =====
        NCase("云闪付消费", NotificationParser.UNIONPAY_PKG, "云闪付", "您尾号1234卡消费支出75.00元。", 75.0, EXP),
        NCase("云闪付收到转账", NotificationParser.UNIONPAY_PKG, "云闪付", "收到转账200.00元", 200.0, INC),
        // ===== 京东 =====
        NCase("京东支付", NotificationParser.JD_PKG, "京东", "京东支付成功：订单金额299.00元", 299.0, EXP),
        NCase("京东白条还款", NotificationParser.JD_PKG, "白条", "白条还款成功500.00元", 500.0, EXP),
        // ===== 银行 App 推送 =====
        NCase("工行推送支出", "com.icbc", "中国工商银行", "您尾号1234卡支出(消费)25.00元，余额5,000.00元。", 25.0, EXP),
        NCase("建行推送消费", "com.chinamworld.main", "中国建设银行", "您尾号1234的储蓄卡消费支出人民币45.00元，余额3,200.00元。", 45.0, EXP),
        NCase("招行推送快捷支付", "com.cmbchina.ccd.pluto.cmbActivity", "招商银行", "您账户1234于11月08日19:52在(美团)发生快捷支付交易人民币29.90元。", 29.9, EXP),
        NCase("银行推送无关键词拦截", "com.icbc", "中国工商银行", "您好，欢迎使用工行手机银行。", null, null)
    )

    private data class SCase(
        val desc: String,
        val sender: String,
        val body: String,
        val expAmount: Double?,
        val expType: Int?,
        val expBalance: Double = 0.0
    )

    private val smsCases = listOf(
        // ===== 银行短信 =====
        SCase("工行支出", "95588", "您尾号1234的储蓄卡11月08日19:52支出(消费)人民币25.00元,余额5,000.00元。【工商银行】", 25.0, EXP, 5000.0),
        SCase("建行消费支出", "95533", "您尾号1234的储蓄卡11月08日19:52消费支出人民币45.00元,活期余额3,200.00元。【建设银行】", 45.0, EXP, 3200.0),
        SCase("招行快捷支付", "95555", "您账户1234于11月08日19:52在(美团)发生快捷支付交易人民币29.90元。", 29.9, EXP),
        SCase("农行财付通交易", "95599", "您尾号3174账户11月08日19:52完成财付通交易人民币-25.00，余额1,189.03。", 25.0, EXP, 1189.03),
        SCase("中行支出", "95566", "您尾号1234账户支出人民币99.00元，余额2,345.00元。", 99.0, EXP, 2345.0),
        SCase("浦发消费", "95528", "您尾号1234卡11月08日19:52消费支出人民币60.00元，余额1,500.00元。", 60.0, EXP, 1500.0),
        SCase("光大向商户支付", "95595", "您尾号1234账户11月08日19:52向（京东商城）支付人民币88.00元。", 88.0, EXP),
        SCase("中信微信支付", "95558", "您尾号1234卡11月08日19:52通过微信支付消费50.00元。", 50.0, EXP),
        SCase("民生存入", "95568", "您尾号1234账户于11月08日19:52存入人民币1,000.00元，余额8,000.00元。【民生银行】", 1000.0, INC, 8000.0),
        SCase("平安快捷支出", "95511", "您尾号1234银行卡11月08日19:52快捷支付支出66.00元。", 66.0, EXP),
        SCase("工行退款", "95588", "您尾号1234卡11月08日19:52收到退款人民币50.00元，余额5,050.00元。", 50.0, INC, 5050.0),
        // ===== 应拦截 =====
        SCase("验证码拦截", "95588", "【工商银行】您的验证码为123456，请勿泄露。", null, null),
        SCase("营销优惠券拦截", "95533", "【建设银行】满100减20优惠券已到账，点击领取。", null, null),
        SCase("理财拦截", "95588", "【工商银行】您购买的基金产品已确认，份额1,000.00。", null, null),
        SCase("账单汇总拦截", "95533", "【建设银行】您尾号1234信用卡本期账单已出，应还款500.00元。", null, null),
        SCase("无银行字样普通短信拦截", "1069012345", "您的快递已到菜鸟驿站，请及时取件。", null, null)
    )

    @Test
    fun `通知语料全量验证`() {
        for (c in notifCases) {
            val r = NotificationParser.parse(c.pkg, c.title, c.text)
            if (c.expAmount == null) {
                assertNull("${c.desc} 应拦截但解析了", r)
            } else {
                assertNotNull("${c.desc} 应解析但未识别", r)
                assertEquals("${c.desc} 金额", c.expAmount, r!!.amount, 0.001)
                assertEquals("${c.desc} 方向", c.expType, r.type)
            }
        }
    }

    @Test
    fun `短信语料全量验证`() {
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
            }
        }
    }

    @Test
    fun `商户提取质量-微信付款`() {
        val r = NotificationParser.parse(
            NotificationParser.WECHAT_PKG, "微信支付", "你已向美团外卖付款成功38.00元"
        )
        assertEquals("美团外卖", r!!.merchant)
    }

    @Test
    fun `商户提取质量-招行快捷支付`() {
        val r = NotificationParser.parse(
            "com.cmbchina.ccd.pluto.cmbActivity", "招商银行",
            "您账户1234于11月08日19:52在(美团)发生快捷支付交易人民币29.90元。"
        )
        assertNotNull(r)
        assertTrue("商户应含美团，实际=${r!!.merchant}", r.merchant.contains("美团"))
        assertFalse("商户不应含发生，实际=${r.merchant}", r.merchant.contains("发生"))
    }

    @Test
    fun `商户提取质量-光大向商户支付`() {
        val r = SmsParser.parse(
            "95595",
            "您尾号1234账户11月08日19:52向（京东商城）支付人民币88.00元。"
        )
        assertTrue("商户应含京东商城，实际=${r!!.merchant}", r.merchant.contains("京东商城"))
    }
}
