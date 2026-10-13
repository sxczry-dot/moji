package com.autobook.app

import com.autobook.app.util.BillDueParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class BillDueParserTest {

    private val now = LocalDate.of(2026, 8, 10)

    @Test
    fun `完整账单短信解析`() {
        val due = BillDueParser.parse(
            "【中国农业银行】您尾号1234信用卡本期应还款人民币5,000.00元，最低还款500.00元，最后还款日8月23日。",
            now
        )
        assertEquals(5000.0, due!!.amount, 0.001)
        assertEquals(500.0, due.minPayment, 0.001)
        assertEquals(LocalDate.of(2026, 8, 23), due.dueDate)
    }

    @Test
    fun `只有还款日无金额`() {
        val due = BillDueParser.parse("本期账单已出，最后还款日为9月5日。", now)
        assertEquals(LocalDate.of(2026, 9, 5), due!!.dueDate)
        assertEquals(0.0, due.amount, 0.001)
    }

    @Test
    fun `短格式还款日下月`() {
        val due = BillDueParser.parse("最后还款日：3日", LocalDate.of(2026, 8, 10))
        assertEquals(LocalDate.of(2026, 9, 3), due!!.dueDate)
    }

    @Test
    fun `跨年账单解析到下一年`() {
        // 12月收到"还款日1月5日" → 次年1月5日
        val due = BillDueParser.parse(
            "本期账单应还款100元，最后还款日1月5日。",
            LocalDate.of(2026, 12, 20)
        )
        assertEquals(LocalDate.of(2027, 1, 5), due!!.dueDate)
    }

    @Test
    fun `无账单信息返回null`() {
        assertNull(BillDueParser.parse("您尾号1234卡8月23日消费人民币25.00元", now))
    }

    @Test
    fun `账单信息识别`() {
        assertTrue(BillDueParser.hasBillInfo("本期应还款100元"))
        assertTrue(BillDueParser.hasBillInfo("最低还款10元"))
    }
}
