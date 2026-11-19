package com.autobook.app

import com.autobook.app.data.BackupCodec
import com.autobook.app.data.Transaction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** 完整备份编解码：ABK2 全字段往返、ABK1 旧格式兼容、坏文件拒识 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BackupCodecTest {

    private fun sampleTxs() = listOf(
        Transaction(
            id = 7, amount = 45.5, type = Transaction.TYPE_EXPENSE,
            category = "餐饮", merchant = "美团", source = Transaction.SOURCE_WECHAT,
            timestamp = 1700000000000L, localTimestamp = 1700000000000L,
            tags = "退款 关联退款", book = "默认账本",
            refNo = "REF123456", balanceAfter = 1234.56
        ),
        Transaction(
            id = 9, amount = 5000.0, type = Transaction.TYPE_INCOME,
            category = "工资", merchant = "公司", source = Transaction.SOURCE_SMS,
            timestamp = 1700000100000L, localTimestamp = 1700000100000L,
            book = "家庭账本"
        )
    )

    @Test
    fun `ABK2 全字段往返一致`() {
        val settings = """{"books":"家庭账本","font_scale":1.1}"""
        val encoded = BackupCodec.encode(settings, sampleTxs())
        assertTrue(encoded.startsWith(BackupCodec.MAGIC_V2))

        val decoded = BackupCodec.decode(encoded)
        assertNotNull(decoded)
        assertEquals(settings, decoded!!.settingsJson)
        assertEquals(2, decoded.txs.size)

        val a = decoded.txs[0]
        assertEquals(7, a.id)
        assertEquals(45.5, a.amount, 0.0001)
        assertEquals("退款 关联退款", a.tags)
        assertEquals("REF123456", a.refNo)
        assertEquals(1234.56, a.balanceAfter, 0.0001)
        val b = decoded.txs[1]
        assertEquals("家庭账本", b.book)
        assertEquals("", b.refNo)
        assertEquals(0.0, b.balanceAfter, 0.0001)
    }

    @Test
    fun `ABK1 旧格式备份仍可恢复`() {
        // 模拟 v5.3 及更早版本生成的备份：settings + csv
        val csv = "id,金额,类型,分类,商户,来源,时间,标签,账本\n" +
            "3,25.00,支出,餐饮,美团,微信,2023-11-14 22:13:20.000,,默认账本\n"
        val legacy = BackupCodec.MAGIC_V1 + """{"settings":{"books":""},"csv":${org.json.JSONObject.quote(csv)}}"""

        val decoded = BackupCodec.decode(legacy)
        assertNotNull(decoded)
        assertEquals(1, decoded!!.txs.size)
        assertEquals(25.0, decoded.txs[0].amount, 0.0001)
        assertEquals("美团", decoded.txs[0].merchant)
    }

    @Test
    fun `非备份文件与坏 JSON 返回 null`() {
        assertNull(BackupCodec.decode("随便一个文本文件"))
        assertNull(BackupCodec.decode(BackupCodec.MAGIC_V2 + "{这不是合法JSON"))
        assertNull(BackupCodec.decode(BackupCodec.MAGIC_V1 + """{"没有settings":1}"""))
    }

    @Test
    fun `非法金额与缺时间戳的记录被跳过`() {
        val encoded = BackupCodec.MAGIC_V2 + org.json.JSONObject()
            .put("settings", org.json.JSONObject())
            .put(
                "txs",
                org.json.JSONArray()
                    .put(org.json.JSONObject().put("amount", -5.0).put("timestamp", 1700000000000L))
                    .put(org.json.JSONObject().put("amount", 10.0).put("timestamp", 0L))
                    .put(org.json.JSONObject().put("amount", 10.0).put("timestamp", 1700000000000L))
            ).toString()
        val decoded = BackupCodec.decode(encoded)
        assertNotNull(decoded)
        assertEquals(1, decoded!!.txs.size)
        assertEquals(10.0, decoded.txs[0].amount, 0.0001)
    }
}
