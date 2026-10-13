package com.autobook.app

import com.autobook.app.data.DedupChecker
import com.autobook.app.data.Transaction
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DedupCheckerTest {

    private fun tx(
        amount: Double = 50.0,
        source: String,
        merchant: String = "",
        timestamp: Long,
        type: Int = Transaction.TYPE_EXPENSE,
        refNo: String = ""
    ) = Transaction(
        amount = amount,
        type = type,
        category = "购物",
        merchant = merchant,
        source = source,
        timestamp = timestamp,
        refNo = refNo
    )

    private val t0 = 1_752_000_000_000L

    @Test
    fun `真实场景-微信短信银行三渠道同额判重`() {
        val wechat = tx(source = "微信", timestamp = t0 + 30_000)
        val sms = tx(source = "短信", merchant = "杭州深度求索", timestamp = t0)
        val bank = tx(source = "农业银行", merchant = "农业银行", timestamp = t0)

        assertTrue(DedupChecker.isDuplicateOf(wechat, sms))
        assertTrue(DedupChecker.isDuplicateOf(wechat, bank))
        assertTrue(DedupChecker.isDuplicateOf(bank, sms))
    }

    @Test
    fun `渠道名兜底的商户不阻断跨渠道判重`() {
        // 入库时无商户名的通知用渠道名兜底：微信存"微信"、银行存"农业银行"
        val wechat = tx(source = "微信", merchant = "微信", timestamp = t0 + 30_000)
        val sms = tx(source = "短信", merchant = "杭州深度求索", timestamp = t0)
        val bank = tx(source = "农业银行", merchant = "农业银行", timestamp = t0)

        assertTrue(DedupChecker.isDuplicateOf(wechat, sms))
        assertTrue(DedupChecker.isDuplicateOf(wechat, bank))
        assertTrue(DedupChecker.isDuplicateOf(bank, sms))
    }

    @Test
    fun `跨渠道真实商户不同不判重`() {
        val a = tx(source = "微信", merchant = "美团", timestamp = t0)
        val b = tx(source = "短信", merchant = "星巴克", timestamp = t0 + 30_000)
        assertFalse(DedupChecker.isDuplicateOf(a, b))
    }

    @Test
    fun `两笔真实同额消费不误判`() {
        val first = tx(source = "微信", timestamp = t0)
        val second = tx(source = "微信", timestamp = t0 + 4 * 60_000)
        assertFalse(DedupChecker.isDuplicateOf(first, second))
    }

    @Test
    fun `金额不同不判重`() {
        val a = tx(amount = 50.0, source = "微信", timestamp = t0)
        val b = tx(amount = 50.5, source = "短信", timestamp = t0 + 10_000)
        assertFalse(DedupChecker.isDuplicateOf(a, b))
    }

    @Test
    fun `流水号相同判重`() {
        val a = tx(source = "微信", timestamp = t0, refNo = "345678")
        val b = tx(source = "短信", timestamp = t0 + 200_000, refNo = "345678")
        assertTrue(DedupChecker.isDuplicateOf(a, b))
    }

    @Test
    fun `银行通知延迟到达仍判重`() {
        val wechat = tx(source = "微信", timestamp = t0 + 30_000)
        val bank = tx(source = "农业银行", timestamp = t0)
        assertTrue(DedupChecker.isDuplicateOf(wechat, bank))
    }
    @Test
    fun `银行App同笔重复推送判重`() {
        val first = tx(source = "农业银行", merchant = "京东商城", timestamp = t0)
        val dup = tx(source = "农业银行", merchant = "京东商城", timestamp = t0 + 5_000)
        assertTrue(DedupChecker.isDuplicateOf(first, dup))
    }

    @Test
    fun `同渠道同商户超10秒不判重`() {
        val first = tx(source = "农业银行", merchant = "京东商城", timestamp = t0)
        val later = tx(source = "农业银行", merchant = "京东商城", timestamp = t0 + 15_000)
        assertFalse(DedupChecker.isDuplicateOf(first, later))
    }

    @Test
    fun `同渠道无商户名2秒内判重`() {
        // 渠道名兜底（merchant==source）视为未知商户：短时间内重复投递仍判重
        val first = tx(source = "微信", merchant = "微信", timestamp = t0)
        val dup = tx(source = "微信", merchant = "微信", timestamp = t0 + 2_000)
        assertTrue(DedupChecker.isDuplicateOf(first, dup))
    }

    @Test
    fun `同渠道无商户名4秒后不判重-避免吞掉同额真实交易`() {
        // 修复：未知商户同渠道判重窗口收窄到 3 秒，两笔同金额不同商户的真实支付不再被静默丢弃
        val first = tx(source = "微信", merchant = "微信", timestamp = t0)
        val later = tx(source = "微信", merchant = "微信", timestamp = t0 + 4_000)
        assertFalse(DedupChecker.isDuplicateOf(first, later))
    }

    @Test
    fun `导入记录空来源不参与判重`() {
        // 导入记录（来源兜底为"导入"）与真实通知不会因空来源直接判重
        val imported = tx(source = "导入", merchant = "美团", timestamp = t0)
        val notif = tx(source = "微信", merchant = "美团", timestamp = t0 + 30_000)
        assertTrue(DedupChecker.isDuplicateOf(imported, notif))
    }
}
