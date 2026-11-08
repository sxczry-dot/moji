package com.autobook.app

import com.autobook.app.data.Transaction
import com.autobook.app.parser.NotificationParser
import com.autobook.app.parser.SmsParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * 真实通知语料大测试·第二期：更多银行/更多微信支付宝变体/缴费还款/城商行长号码。
 */
class NotificationCorpus2Test {

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
        // ===== 微信变体 =====
        NCase("微信扫码付款", NotificationParser.WECHAT_PKG, "微信支付", "扫码付款成功20.00元", 20.0, EXP),
        NCase("微信自动续费", NotificationParser.WECHAT_PKG, "微信支付", "自动续费扣款成功：15.00元（爱奇艺会员）", 15.0, EXP, "爱奇艺会员"),
        NCase("微信群收款", NotificationParser.WECHAT_PKG, "群收款", "群收款成功：你已收款100.00元", 100.0, INC),
        NCase("微信还信用卡", NotificationParser.WECHAT_PKG, "微信支付", "已向招商银行信用卡还款500.00元", 500.0, EXP, "招商银行信用卡"),
        // ===== 支付宝变体 =====
        NCase("支付宝生活缴费", NotificationParser.ALIPAY_PKG, "支付宝", "生活缴费成功：电费68.50元", 68.5, EXP),
        NCase("支付宝收款到账", NotificationParser.ALIPAY_PKG, "支付宝", "到账通知：收款100.00元已存入余额", 100.0, INC),
        NCase("支付宝还信用卡", NotificationParser.ALIPAY_PKG, "支付宝", "信用卡还款成功500.00元", 500.0, EXP),
        NCase("淘宝发货无支付词拦截", NotificationParser.ALIPAY_PKG, "淘宝", "您购买的宝贝已发货，请注意查收", null, null),
        // ===== 京东变体 =====
        NCase("京东支付变体", NotificationParser.JD_PKG, "京东支付", "京东支付成功99.00元", 99.0, EXP),
        NCase("京东白条出账拦截", NotificationParser.JD_PKG, "京东金融", "白条已出账：本期应还500.00元", null, null),
        // ===== 更多银行推送 =====
        NCase("中行推送支出", "com.chinamworld.bocmbci", "中国银行", "您尾号1234账户支出人民币99.00元，余额2,345.00元。", 99.0, EXP),
        NCase("邮储推送支出", "com.yitong.mbank.psbc", "邮储银行", "您尾号1234账户支出人民币45.00元，余额5,678.90元。", 45.0, EXP),
        NCase("平安推送消费", "com.pingan.paces.ccms", "平安银行", "您尾号1234卡快捷支付支出66.00元。", 66.0, EXP),
        NCase("广发推送消费", "com.cgbchina.xpt", "广发银行", "您尾号1234信用卡消费支出人民币123.00元。", 123.0, EXP)
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
        SCase("交通银行快捷支付", "95559", "您尾号1234卡11月08日19:52快捷支付人民币88.00元，余额1,234.56元。【交通银行】", 88.0, EXP, 1234.56),
        SCase("兴业银行消费", "95561", "您尾号1234账户11月08日19:52消费人民币66.00元，余额888.00元。【兴业银行】", 66.0, EXP, 888.0),
        SCase("广发银行消费", "95508", "您尾号1234信用卡11月08日19:52消费支出人民币123.00元。【广发银行】", 123.0, EXP),
        SCase("华夏银行存入", "95577", "您尾号1234卡11月08日19:52存入人民币2,000.00元，余额20,000.00元。【华夏银行】", 2000.0, INC, 20000.0),
        SCase("邮储银行支出", "95580", "您尾号1234账户11月08日19:52支出人民币45.00元，余额5,678.90元。【邮储银行】", 45.0, EXP, 5678.9),
        SCase("上海银行消费", "95594", "您尾号1234卡11月08日19:52消费人民币30.00元，余额999.00元。", 30.0, EXP, 999.0),
        SCase("招行退款收入", "95555", "您账户1234于11月08日19:52收到退款人民币50.00元。", 50.0, INC),
        SCase("建行长号码通道", "1069095533", "您尾号1234储蓄卡11月08日19:52消费支出人民币30.00元。【建设银行】", 30.0, EXP),
        SCase("农行长号码通道", "1069095599", "您尾号3174账户11月08日19:52完成财付通交易人民币-25.00，余额1,189.03。", 25.0, EXP, 1189.03),
        SCase("交行退款支出", "95559", "您尾号1234卡11月08日19:52退款支出人民币50.00元。", 50.0, EXP),
        SCase("光大还款", "95595", "您尾号1234信用卡11月08日19:52还款人民币500.00元成功。", 500.0, EXP),
        SCase("华夏取款", "95577", "您尾号1234卡11月08日19:52取款人民币300.00元，余额9,700.00元。", 300.0, EXP, 9700.0),
        SCase("短信验证码拦截", "1069012345", "【某某平台】验证码123456，5分钟内有效。", null, null),
        SCase("短信积分营销拦截", "95533", "【建设银行】您有500积分即将到期，点击兑换好礼。", null, null)
    )

    @Test
    fun `通知语料第二期全量验证`() {
        for (c in notifCases) {
            val r = NotificationParser.parse(c.pkg, c.title, c.text)
            if (c.expAmount == null) {
                assertNull("${c.desc} 应拦截但解析了", r)
            } else {
                assertNotNull("${c.desc} 应解析但未识别", r)
                assertEquals("${c.desc} 金额", c.expAmount, r!!.amount, 0.001)
                assertEquals("${c.desc} 方向", c.expType, r.type)
                c.expMerchant?.let { m ->
                    assertEquals("${c.desc} 商户", m, r.merchant)
                }
            }
        }
    }

    @Test
    fun `短信语料第二期全量验证`() {
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
                c.expMerchant?.let { m ->
                    assertEquals("${c.desc} 商户", m, r.merchant)
                }
            }
        }
    }
}
