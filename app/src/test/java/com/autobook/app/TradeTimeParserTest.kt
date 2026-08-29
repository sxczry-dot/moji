package com.autobook.app

import com.autobook.app.util.TradeTimeParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class TradeTimeParserTest {

    @Test
    fun `完整日期时间`() {
        val cal = Calendar.getInstance()
        val t = TradeTimeParser.parse(
            "您尾号为3174的农行借记卡于08月24日09:23发生一笔支出3.00元",
            cal.timeInMillis
        )
        val parsed = Calendar.getInstance().apply { timeInMillis = t }
        assertEquals(8, parsed.get(Calendar.MONTH) + 1)
        assertEquals(24, parsed.get(Calendar.DAY_OF_MONTH))
        assertEquals(9, parsed.get(Calendar.HOUR_OF_DAY))
        assertEquals(23, parsed.get(Calendar.MINUTE))
    }

    @Test
    fun `仅时分`() {
        val cal = Calendar.getInstance().apply {
            set(2026, Calendar.AUGUST, 24, 10, 0, 0)
        }
        val t = TradeTimeParser.parse("发生一笔支出3.00元 09:23 详情", cal.timeInMillis)
        val parsed = Calendar.getInstance().apply { timeInMillis = t }
        assertEquals(9, parsed.get(Calendar.HOUR_OF_DAY))
        assertEquals(23, parsed.get(Calendar.MINUTE))
    }

    @Test
    fun `无时间返回0`() {
        assertEquals(0L, TradeTimeParser.parse("没有时间的文本"))
    }
}
