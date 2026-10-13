package com.autobook.app

import com.autobook.app.data.Categories
import com.autobook.app.data.Transaction
import com.autobook.app.data.refundSum
import com.autobook.app.parser.CategoryClassifier
import com.autobook.app.parser.NotificationParser
import com.autobook.app.parser.SmsParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * v4.85–v4.89 线上问题修复的回归用例（真实报文原文）。
 * 新增解析规则时务必保证这里全部通过。
 */
class ParserRegressionTest {

    // ===== v4.85 京东返现误记 =====

    @Test
    fun `v4_85_京东返现记实际支付金额`() {
        val r = NotificationParser.parse(
            NotificationParser.JD_PKG,
            "京东支付",
            "您通过农行卡*3174支付23.10元。返￥30.77，可提现至银行卡（12小时内） 3.cn/v/vjZhn5 拒收请回复R"
        )
        assertEquals(23.10, r!!.amount, 0.001)
        assertEquals(Transaction.TYPE_EXPENSE, r.type)
        assertEquals("京东支付", r.merchant)
    }

    @Test
    fun `v4_85_纯返现通知不记账`() {
        assertNull(
            NotificationParser.parse(
                NotificationParser.JD_PKG,
                "京东支付",
                "返￥30.77，可提现至银行卡（12小时内）"
            )
        )
    }

    @Test
    fun `v4_85_短信返现片段被剔除`() {
        val r = SmsParser.parse(
            "95599",
            "您通过农行卡*3174支付23.10元。返￥30.77，可提现至银行卡（12小时内）"
        )
        assertEquals(23.10, r!!.amount, 0.001)
        assertEquals(Transaction.TYPE_EXPENSE, r.type)
    }

    // ===== v4.86 拼多多免密扣款漏记 =====

    @Test
    fun `v4_86_拼多多免密扣款带积分文案仍记账`() {
        val r = NotificationParser.parse(
            NotificationParser.ALIPAY_PKG,
            "支付宝",
            "你在拼多多平台商户有一笔21.51元的免密/自动扣款支付，点击领取9个支付宝积分。"
        )
        assertEquals(21.51, r!!.amount, 0.001)
        assertEquals(Transaction.TYPE_EXPENSE, r.type)
    }

    // ===== v4.87 支付宝标题无关键词按正文放行 =====

    @Test
    fun `v4_87_花呗生活缴费提醒标题无关键词仍记账`() {
        val r = NotificationParser.parse(
            NotificationParser.ALIPAY_PKG,
            "生活缴费提醒",
            "你有一笔12.44元的支出，领1元生活缴费红包。使用花呗支付，请及时还款。"
        )
        assertEquals(12.44, r!!.amount, 0.001)
        assertEquals(Transaction.TYPE_EXPENSE, r.type)
    }

    // ===== v4.88 农行入账句式 / 大额截断 / 千分位 =====

    @Test
    fun `v4_88_农行向您账户完成交易记收入`() {
        val r = SmsParser.parse(
            "95599",
            "【中国农业银行】山东省立医院门诊于09月12日15:20向您尾号3174账户完成财付通交易人民币385.01，余额1,189.03。"
        )
        assertEquals(385.01, r!!.amount, 0.001)
        assertEquals(Transaction.TYPE_INCOME, r.type)
        assertEquals("山东省立医院门诊", r.merchant)
    }

    @Test
    fun `v4_88_农行支出式对照仍记支出`() {
        val r = SmsParser.parse(
            "95599",
            "【中国农业银行】山东省立医院门诊于09月12日15:20向您尾号3174账户完成消费交易人民币385.01，余额1,189.03。"
        )
        assertEquals(385.01, r!!.amount, 0.001)
        assertEquals(Transaction.TYPE_EXPENSE, r.type)
    }

    @Test
    fun `v4_88_人民币大额金额不被截断`() {
        val r = SmsParser.parse(
            "95588",
            "您尾号1234卡8月23日入账人民币5000元（工资），余额12345.67元。"
        )
        assertEquals(5000.0, r!!.amount, 0.001)
        assertEquals(Transaction.TYPE_INCOME, r.type)
    }

    @Test
    fun `v4_88_千分位金额解析正确`() {
        val r = SmsParser.parse(
            "95588",
            "您尾号1234卡8月23日消费支出人民币1,234.56元，余额9,000.00元。"
        )
        assertEquals(1234.56, r!!.amount, 0.001)
        assertEquals(Transaction.TYPE_EXPENSE, r.type)
    }

    // ===== v4.89 账单汇总不记账 + 全文分类 =====

    @Test
    fun `v4_89_账单汇总通知不记账`() {
        assertNull(
            NotificationParser.parse(
                NotificationParser.JD_PKG,
                "京东",
                "昨日卡账单已出，共计消费33.06元，点击查看详情。"
            )
        )
        assertNull(
            NotificationParser.parse(
                NotificationParser.ALIPAY_PKG,
                "支付宝",
                "您上月账单已出，共计消费33.06元。"
            )
        )
    }

    @Test
    fun `v4_89_收款汇总通知不记账`() {
        assertNull(
            NotificationParser.parse(
                NotificationParser.WECHAT_PKG,
                "微信收款助手",
                "今日收款汇总：已收15笔，共计320.00元。"
            )
        )
    }

    @Test
    fun `v4_89_账单汇总短信不记账`() {
        assertNull(
            SmsParser.parse(
                "95588",
                "【工商银行】昨日卡账单已出，您上月共计消费33.06元，点击查看详情。"
            )
        )
    }

    // ===== 基础场景补充：取款 / 退款 =====

    @Test
    fun `取款短信记支出`() {
        val r = SmsParser.parse(
            "95588",
            "您尾号1234卡8月23日取款人民币500.00元，余额3,456.78元。"
        )
        assertEquals(500.0, r!!.amount, 0.001)
        assertEquals(Transaction.TYPE_EXPENSE, r.type)
    }

    @Test
    fun `退款短信记收入`() {
        val r = SmsParser.parse(
            "95588",
            "您尾号1234卡8月23日退款人民币50.00元已到账，余额1,234.56元。"
        )
        assertEquals(50.0, r!!.amount, 0.001)
        assertEquals(Transaction.TYPE_INCOME, r.type)
    }

    // ===== v4.89 全文分类（Python 镜像用例 1:1 固化） =====

    @Test
    fun `v4_89_全文含地铁归交通出行`() {
        assertEquals(
            Categories.TRANSPORT,
            CategoryClassifier.classifyExpense("3号线地铁站 出站扣费")
        )
    }

    @Test
    fun `v4_89_医院门诊归医疗健康`() {
        assertEquals(
            Categories.MEDICAL,
            CategoryClassifier.classifyExpense("山东省立医院门诊")
        )
    }

    @Test
    fun `v4_89_中石化加油归交通出行`() {
        assertEquals(
            Categories.TRANSPORT,
            CategoryClassifier.classifyExpense("中石化加油站")
        )
    }

    @Test
    fun `v4_89_视频会员归娱乐休闲`() {
        assertEquals(
            Categories.ENTERTAINMENT,
            CategoryClassifier.classifyExpense("视频会员自动续费")
        )
    }

    @Test
    fun `v4_89_无关键词兜底归其他`() {
        assertEquals(
            Categories.OTHER,
            CategoryClassifier.classifyExpense("物业费代收")
        )
    }

    // ===== 2026-10 深度审查修复的回归 =====

    @Test
    fun `消费退款短信记收入`() {
        val r = SmsParser.parse(
            "95555",
            "您尾号1234信用卡8月23日消费退款人民币50.00元，余额1,234.56元。"
        )
        assertEquals(50.0, r!!.amount, 0.001)
        assertEquals(Transaction.TYPE_INCOME, r.type)
    }

    @Test
    fun `退款支出短信仍记支出`() {
        val r = SmsParser.parse(
            "95555",
            "您尾号1234信用卡8月23日退款支出人民币10.00元，余额1,234.56元。"
        )
        assertEquals(10.0, r!!.amount, 0.001)
        assertEquals(Transaction.TYPE_EXPENSE, r.type)
    }

    @Test
    fun `银行App推送向您账户完成交易记收入`() {
        val r = NotificationParser.parse(
            "com.android.bankabc",
            "中国农业银行",
            "山东省立医院门诊于09月12日15:20向您尾号3174账户完成财付通交易人民币385.01，余额1,189.03。"
        )
        assertEquals(385.01, r!!.amount, 0.001)
        assertEquals(Transaction.TYPE_INCOME, r.type)
        assertEquals("山东省立医院门诊", r.merchant)
    }

    @Test
    fun `三位小数金额不膨胀不误记`() {
        // 修复前 "消费12.345元" 会回溯匹配 "345元" 记成 345.00；现在应拒记（不取错误金额）
        assertNull(
            SmsParser.parse(
                "95588",
                "您尾号1234卡8月23日消费12.345元。"
            )
        )
    }

    @Test
    fun `生活缴费通知记账`() {
        val r = NotificationParser.parse(
            NotificationParser.WECHAT_PKG,
            "生活缴费",
            "您的水费已缴纳成功，缴费金额38.00元。"
        )
        assertEquals(38.0, r!!.amount, 0.001)
        assertEquals(Transaction.TYPE_EXPENSE, r.type)
    }

    @Test
    fun `原价金额不干扰实付金额`() {
        val r = NotificationParser.parse(
            NotificationParser.ALIPAY_PKG,
            "支付宝",
            "已支付¥38.00（原价58.00元）"
        )
        assertEquals(38.0, r!!.amount, 0.001)
        assertEquals(Transaction.TYPE_EXPENSE, r.type)
    }

    @Test
    fun `信用卡还款短信记账`() {
        val r = SmsParser.parse(
            "95555",
            "您尾号1234信用卡8月23日还款人民币500.00元，当前余额1,234.56元。"
        )
        assertEquals(500.0, r!!.amount, 0.001)
        assertEquals(Transaction.TYPE_EXPENSE, r.type)
    }

    @Test
    fun `超16位单号取真实尾6位`() {
        assertEquals(
            "567890",
            com.autobook.app.util.RefNoParser.parse("交易号202608241234567890")
        )
    }

    @Test
    fun `时间带分后缀仍能解析`() {
        val reference = 1_752_000_000_000L // 2026-07-09 附近
        val t = com.autobook.app.util.TradeTimeParser.parse(" 15:20分，", reference)
        val cal = java.util.Calendar.getInstance().apply { timeInMillis = t }
        assertEquals(15, cal.get(java.util.Calendar.HOUR_OF_DAY))
        assertEquals(20, cal.get(java.util.Calendar.MINUTE))
    }

    @Test
    fun `抵扣红包文案从分类文本中剔除`() {
        val stripped = com.autobook.app.service.TransactionSaver.redPacketDeductionRegex
            .replace("使用5元红包，实付38.00元", "")
        assertEquals(
            Categories.SHOPPING,
            CategoryClassifier.classifyExpense("京东 $stripped")
        )
    }

    // ===== v4.91 退款净口径 =====

    @Test
    fun `银行短信余额快照解析`() {
        val r = SmsParser.parse(
            "95599",
            "【中国农业银行】山东省立医院门诊于09月12日15:20向您尾号3174账户完成财付通交易人民币385.01，余额1,189.03。"
        )
        assertEquals(1189.03, r!!.balance, 0.001)
    }

    @Test
    fun `通知余额快照解析`() {
        val r = NotificationParser.parse(
            "com.android.bankabc",
            "中国农业银行",
            "您尾号3174账户发生一笔支出10.00元，余额1,179.03元。"
        )
        assertEquals(1179.03, r!!.balance, 0.001)
    }

    @Test
    fun `支付宝通用标题从正文提取商户`() {
        val r = NotificationParser.parse(
            NotificationParser.ALIPAY_PKG,
            "交易提醒",
            "你在起点读书有一笔49.81元的免密/自动扣款支付，点击领取9个支付宝积分。"
        )
        assertEquals(49.81, r!!.amount, 0.001)
        assertEquals("起点读书", r.merchant)
        assertEquals(Transaction.TYPE_EXPENSE, r.type)
    }

    @Test
    fun `退款识别纯函数`() {
        val saver = com.autobook.app.service.TransactionSaver
        assertTrue(saver.isRefundTransaction(Transaction.TYPE_INCOME, "退款人民币50.00元已到账", ""))
        assertTrue(saver.isRefundTransaction(Transaction.TYPE_INCOME, "订单已退回", "拼多多"))
        assertTrue(saver.isRefundTransaction(Transaction.TYPE_INCOME, "收到转账500元", "退款"))
        assertFalse(saver.isRefundTransaction(Transaction.TYPE_EXPENSE, "退款支出10元", ""))
        assertFalse(saver.isRefundTransaction(Transaction.TYPE_INCOME, "收到转账500.00元", ""))
    }

    @Test
    fun `退款合计只统计退款收入`() {
        val txs = listOf(
            Transaction(
                amount = 50.0, type = Transaction.TYPE_INCOME, category = "收入",
                merchant = "", source = "短信", timestamp = 0L, tags = Transaction.TAG_REFUND
            ),
            Transaction(
                amount = 30.0, type = Transaction.TYPE_INCOME, category = "收入",
                merchant = "", source = "短信", timestamp = 0L
            ),
            Transaction(
                amount = 80.0, type = Transaction.TYPE_EXPENSE, category = "购物消费",
                merchant = "", source = "短信", timestamp = 0L
            )
        )
        assertEquals(50.0, txs.refundSum(), 0.001)
    }
}
