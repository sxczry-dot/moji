package com.autobook.app

import com.autobook.app.data.Transaction
import com.autobook.app.parser.SmsParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SmsParserTest {

    @Test
    fun `工行消费短信-无商户`() {
        val r = SmsParser.parse(
            "95588",
            "您尾号1234卡8月23日12:30消费支出人民币25.00元，余额3,456.78元【工商银行】"
        )
        assertEquals(25.0, r!!.amount, 0.001)
        assertEquals(Transaction.TYPE_EXPENSE, r.type)
    }

    @Test
    fun `招行消费短信-带商户`() {
        val r = SmsParser.parse(
            "95555",
            "您账户1234于08月23日12:30在美团支付人民币45.50元。"
        )
        assertEquals(45.5, r!!.amount, 0.001)
        assertEquals("美团", r.merchant)
    }

    @Test
    fun `入账短信-收入`() {
        val r = SmsParser.parse(
            "95588",
            "您尾号1234卡8月23日入账人民币5,000.00元（工资），余额12,345.67元。"
        )
        assertEquals(5000.0, r!!.amount, 0.001)
        assertEquals(Transaction.TYPE_INCOME, r.type)
    }

    @Test
    fun `余额短信不误记`() {
        assertNull(
            SmsParser.parse(
                "95588",
                "您尾号1234卡8月23日余额为3,456.78元。"
            )
        )
    }

    @Test
    fun `106端口金融短信-强模式通过`() {
        val r = SmsParser.parse(
            "106980009588",
            "【京东金融】您尾号1234的白条已消费人民币88.00元。"
        )
        assertEquals(88.0, r!!.amount, 0.001)
    }

    @Test
    fun `106端口普通短信不强不记`() {
        assertNull(
            SmsParser.parse(
                "106900001234",
                "您的验证码是123456，请在5分钟内输入。"
            )
        )
        val r = SmsParser.parse(
            "106900001234",
            "您本月已消费88元，点击查看详情"
        )
        assertEquals(88.0, r!!.amount, 0.001)
        assertEquals(Transaction.TYPE_EXPENSE, r.type)
    }

    @Test
    fun `整数金额无货币符号也能记`() {
        val r = SmsParser.parse(
            "95599",
            "【中国农业银行】您尾号3174账户消费100元，余额500元。"
        )
        assertEquals(100.0, r!!.amount, 0.001)
        assertEquals(Transaction.TYPE_EXPENSE, r.type)
    }

    @Test
    fun `长号码不以银行短号特殊对待`() {
        val r = SmsParser.parse(
            "106955889999",
            "【营销】消费人民币100元送大礼"
        )
        assertEquals(100.0, r!!.amount, 0.001)
    }

    @Test
    fun `朋友闲聊短信不误记`() {
        assertNull(SmsParser.parse("13800138000", "我花了25元吃饭"))
    }

    @Test
    fun `农行网银交易短信`() {
        val r = SmsParser.parse(
            "95599",
            "【中国农业银行】您尾号3174账户08月23日22:28向京东商城京东大药房完成网银在线交易人民币-63.20，余额365.02，详见掌银。"
        )
        assertEquals(63.20, r!!.amount, 0.001)
        assertEquals(Transaction.TYPE_EXPENSE, r.type)
        assertEquals("京东商城京东大药房", r.merchant)
    }

    @Test
    fun `收入关键词优先`() {
        val r = SmsParser.parse(
            "95533",
            "您尾号1234卡8月23日收到转账人民币50.00元，余额1234.56元。"
        )
        assertEquals(Transaction.TYPE_INCOME, r!!.type)
    }

    @Test
    fun `向他人转账记为支出`() {
        val r = SmsParser.parse(
            "95533",
            "您尾号1234卡8月23日向张三转账人民币50.00元，余额1234.56元。"
        )
        assertEquals(Transaction.TYPE_EXPENSE, r!!.type)
    }
}
