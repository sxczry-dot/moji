package com.autobook.app

import com.autobook.app.util.RefNoParser
import org.junit.Assert.assertEquals
import org.junit.Test

class RefNoParserTest {

    @Test
    fun `流水号取尾段`() {
        assertEquals(
            "345678",
            RefNoParser.parse("您尾号3174账户消费人民币3.00元，交易流水号2026082412345678。")
        )
    }

    @Test
    fun `订单号`() {
        assertEquals(
            "567890",
            RefNoParser.parse("订单号 567890 已完成支付")
        )
    }

    @Test
    fun `过短数字不提取`() {
        assertEquals("", RefNoParser.parse("流水号后四位1234"))
    }

    @Test
    fun `无流水号返回空`() {
        assertEquals("", RefNoParser.parse("您尾号为3174的农行借记卡发生一笔支出3.00元"))
    }
}
