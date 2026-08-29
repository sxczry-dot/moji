package com.autobook.app

import com.autobook.app.data.Transaction
import com.autobook.app.parser.NotificationParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NotificationParserTest {

    @Test
    fun `微信支付通知-支出`() {
        val r = NotificationParser.parse(
            NotificationParser.WECHAT_PKG,
            "微信支付",
            "已支付¥25.00"
        )
        assertEquals(25.0, r!!.amount, 0.001)
        assertEquals(Transaction.TYPE_EXPENSE, r.type)
    }

    @Test
    fun `微信支付通知-收款`() {
        val r = NotificationParser.parse(
            NotificationParser.WECHAT_PKG,
            "微信支付",
            "微信支付收款0.01元"
        )
        assertEquals(0.01, r!!.amount, 0.001)
        assertEquals(Transaction.TYPE_INCOME, r.type)
    }

    @Test
    fun `微信转账标题不被拒绝`() {
        val r = NotificationParser.parse(
            NotificationParser.WECHAT_PKG,
            "微信转账",
            "你收到一笔转账¥100.00"
        )
        assertEquals(100.0, r!!.amount, 0.001)
        assertEquals(Transaction.TYPE_INCOME, r.type)
    }

    @Test
    fun `微信聊天消息不误记`() {
        val r = NotificationParser.parse(
            NotificationParser.WECHAT_PKG,
            "张三",
            "我微信支付25元买的"
        )
        assertNull(r)
    }

    @Test
    fun `转账的收款方字样不判收入`() {
        val r = NotificationParser.parse(
            NotificationParser.WECHAT_PKG,
            "微信支付",
            "已转账¥200.00（收款方：李四）"
        )
        assertEquals(200.0, r!!.amount, 0.001)
        assertEquals(Transaction.TYPE_EXPENSE, r.type)
    }

    @Test
    fun `千分位金额解析正确`() {
        val r = NotificationParser.parse(
            NotificationParser.ALIPAY_PKG,
            "支付宝",
            "成功付款¥1,000.00元"
        )
        assertEquals(1000.0, r!!.amount, 0.001)
    }

    @Test
    fun `支付宝付款`() {
        val r = NotificationParser.parse(
            NotificationParser.ALIPAY_PKG,
            "支付宝",
            "成功付款25.50元"
        )
        assertEquals(25.5, r!!.amount, 0.001)
        assertEquals(Transaction.TYPE_EXPENSE, r.type)
    }

    @Test
    fun `云闪付付款`() {
        val r = NotificationParser.parse(
            NotificationParser.UNIONPAY_PKG,
            "云闪付",
            "你已成功付款38.00元"
        )
        assertEquals(38.0, r!!.amount, 0.001)
        assertEquals(Transaction.TYPE_EXPENSE, r.type)
    }

    @Test
    fun `非支付通知被拒绝`() {
        assertNull(NotificationParser.parse("com.other.app", "标题", "支付¥10.00"))
        assertNull(NotificationParser.parse(NotificationParser.ALIPAY_PKG, "蚂蚁森林", "收取能量"))
        assertNull(NotificationParser.parse(NotificationParser.WECHAT_PKG, "微信支付", "无金额的通知"))
    }

    @Test
    fun `超大金额被拒绝`() {
        assertNull(
            NotificationParser.parse(
                NotificationParser.WECHAT_PKG,
                "微信支付",
                "已支付¥99999999.00"
            )
        )
    }
    @Test
    fun `农行App推送通知`() {
        val r = NotificationParser.parse(
            "com.android.bankabc",
            "中国农业银行",
            "您尾号为3174的农行借记卡于08月23日23:28发生一笔支出1.00元，详情请点击"
        )
        assertEquals(1.0, r!!.amount, 0.001)
        assertEquals(Transaction.TYPE_EXPENSE, r.type)
    }
    @Test
    fun `支付宝积分营销不误记`() {
        assertNull(
            NotificationParser.parse(
                "com.eg.android.AlipayGphone",
                "支付宝",
                "你有137积分待领取，积分可以当钱花，别浪费"
            )
        )
    }
}
