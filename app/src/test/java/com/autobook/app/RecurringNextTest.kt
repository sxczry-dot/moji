package com.autobook.app

import com.autobook.app.util.AppSettings.RecurringRule
import com.autobook.app.util.RecurringNext
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar

class RecurringNextTest {

    private fun cal(y: Int, m: Int, d: Int): Calendar =
        Calendar.getInstance().apply {
            clear()
            set(y, m - 1, d, 12, 0, 0)
        }

    private fun rule(period: Int, day: Int, paused: Boolean = false) = RecurringRule(
        name = "测试", amount = 100.0, type = 0, category = "居住",
        day = day, merchant = "", period = period, paused = paused
    )

    @Test
    fun `每月规则未到日则为当月`() {
        val now = cal(2026, 8, 25)
        assertEquals("下次：2026年8月28日", RecurringNext.nextRunText(rule(0, 28), now))
    }

    @Test
    fun `每月规则已过日则为下月`() {
        val now = cal(2026, 8, 25)
        assertEquals("下次：2026年9月15日", RecurringNext.nextRunText(rule(0, 15), now))
    }

    @Test
    fun `每月规则跨年`() {
        val now = cal(2026, 12, 20)
        assertEquals("下次：2027年1月5日", RecurringNext.nextRunText(rule(0, 5), now))
    }

    @Test
    fun `月末规则为本月最后一天`() {
        val now = cal(2026, 2, 10)
        assertEquals("下次：2026年2月28日", RecurringNext.nextRunText(rule(2, 0), now))
    }

    @Test
    fun `每年规则未到则为今年`() {
        val now = cal(2026, 8, 25)
        assertEquals("下次：2026年9月3日", RecurringNext.nextRunText(rule(3, 903), now))
    }

    @Test
    fun `每年规则已过则为明年`() {
        val now = cal(2026, 8, 25)
        assertEquals("下次：2027年7月1日", RecurringNext.nextRunText(rule(3, 701), now))
    }

    @Test
    fun `暂停规则显示已暂停`() {
        val now = cal(2026, 8, 25)
        assertEquals("已暂停", RecurringNext.nextRunText(rule(0, 28, paused = true), now))
    }

    @Test
    fun `月末规则当天已过则显示下月最后一天`() {
        val now = cal(2026, 8, 31)
        assertEquals("下次：2026年9月30日", RecurringNext.nextRunText(rule(2, 0), now))
    }

    @Test
    fun `每年2月29非闰年顺延到下一个闰年`() {
        val now = cal(2027, 2, 20)
        assertEquals("下次：2028年2月29日", RecurringNext.nextRunText(rule(3, 229), now))
    }

    @Test
    fun `每年规则当天已过则明年`() {
        val now = cal(2026, 9, 3)
        assertEquals("下次：2027年9月3日", RecurringNext.nextRunText(rule(3, 903), now))
    }
}
