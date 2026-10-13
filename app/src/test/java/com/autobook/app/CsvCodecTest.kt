package com.autobook.app

import com.autobook.app.data.Categories
import com.autobook.app.data.CsvCodec
import com.autobook.app.data.Transaction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CsvCodecTest {

    private val sharkHeader = "\"日期\",\"收支类型\",\"类别\",\"金额\",\"备注\""

    @Test
    fun `鲨鱼记账真实格式-带引号表头和行`() {
        val csv = "$sharkHeader\n" +
            "\"2026年08月23日\",\"支出\",\"购物\",\"46\",\"熊果\"\n" +
            "\"2026年08月23日\",\"支出\",\"餐饮\",\"25\",\"午饭\"\n" +
            "\"2026年08月21日\",\"收入\",\"股票\",\"1000\",\"分红\"\n"
        val txs = CsvCodec.decode(csv)
        assertEquals(3, txs.size)

        val first = txs[0]
        assertEquals(46.0, first.amount, 0.001)
        assertEquals(Transaction.TYPE_EXPENSE, first.type)
        assertEquals(Categories.SHOPPING, first.category)
        assertEquals("熊果", first.merchant)
        assertEquals("", first.source)

        val income = txs[2]
        assertEquals(Transaction.TYPE_INCOME, income.type)
        assertEquals(Categories.INCOME, income.category)
    }

    @Test
    fun `鲨鱼全部类别映射正确`() {
        val cases = mapOf(
            "餐饮" to "餐饮美食",
            "购物" to "购物消费",
            "美容" to "美容护理",
            "学习" to "教育学习",
            "交通" to "交通出行",
            "医疗" to "医疗健康",
            "通讯" to "通讯网络",
            "服饰" to "购物消费",
            "饮水" to "餐饮美食",
            "水果" to "餐饮美食",
            "社交" to "人情社交",
            "亲友" to "人情社交",
            "数码" to "购物消费",
            "娱乐" to "娱乐休闲"
        )
        for ((raw, expected) in cases) {
            val csv = "$sharkHeader\n\"2026年08月01日\",\"支出\",\"$raw\",\"10\",\"x\"\n"
            val txs = CsvCodec.decode(csv)
            assertEquals("类别[$raw]映射错误", expected, txs[0].category)
        }
    }

    @Test
    fun `收入类别往返导入不叠加前缀`() {
        val csv = "$sharkHeader\n\"2026年08月01日\",\"收入\",\"收入/工资\",\"5000\",\"\"\n"
        val txs = CsvCodec.decode(csv)
        assertEquals(Categories.INCOME, txs[0].category)

        val csv2 = "$sharkHeader\n\"2026年08月01日\",\"收入\",\"工资\",\"5000\",\"\"\n"
        val txs2 = CsvCodec.decode(csv2)
        assertEquals(Categories.INCOME, txs2[0].category)
    }

    @Test
    fun `无表头旧格式兼容`() {
        val csv = "25.50,支出,餐饮,美团,微信,2026-08-23 12:30:00\n"
        val txs = CsvCodec.decode(csv)
        assertEquals(1, txs.size)
        assertEquals(25.5, txs[0].amount, 0.001)
        assertEquals("美团", txs[0].merchant)
        assertEquals("微信", txs[0].source)
    }

    @Test
    fun `导出再导入往返一致`() {
        // 旧格式带子分类，导入后按 v4.89 规则归一为大分类
        val original = listOf(
            Transaction(amount = 25.5, type = Transaction.TYPE_EXPENSE, category = "餐饮/外卖", merchant = "美团,外卖", source = "微信", timestamp = 1755916200000L),
            Transaction(amount = 5000.0, type = Transaction.TYPE_INCOME, category = "收入/工资", merchant = "工资", source = "短信", timestamp = 1755916300000L)
        )
        val encoded = CsvCodec.encode(original)
        val decoded = CsvCodec.decode(encoded)
        assertEquals(2, decoded.size)
        assertEquals("餐饮美食", decoded[0].category)
        assertEquals("美团,外卖", decoded[0].merchant)
        assertEquals(Categories.INCOME, decoded[1].category)
        assertEquals(5000.0, decoded[1].amount, 0.001)
        assertEquals(Transaction.TYPE_INCOME, decoded[1].type)
    }

    @Test
    fun `表头列顺序打乱也能识别`() {
        val csv = "\"备注\",\"金额\",\"类别\",\"日期\",\"收支类型\"\n" +
            "\"奶茶\",\"15.5\",\"餐饮\",\"2026/8/23\",\"支出\"\n"
        val txs = CsvCodec.decode(csv)
        assertEquals(1, txs.size)
        assertEquals(15.5, txs[0].amount, 0.001)
        assertEquals("奶茶", txs[0].merchant)
        assertEquals(Categories.FOOD, txs[0].category)
    }

    @Test
    fun `异常行被跳过不崩溃`() {
        val csv = "$sharkHeader\n" +
            "\"2026年08月01日\",\"支出\",\"购物\",\"abc\",\"x\"\n" +
            "\"2026年08月01日\",\"支出\",\"购物\",\"-5\",\"x\"\n" +
            "\"2026年08月01日\",\"支出\",\"购物\",\"99999999\",\"x\"\n" +
            "\"2026年08月01日\",\"支出\",\"购物\",\"20\",\"ok\"\n" +
            "\"残缺行\"\n"
        val txs = CsvCodec.decode(csv)
        assertEquals(1, txs.size)
        assertEquals(20.0, txs[0].amount, 0.001)
    }

    @Test
    fun `空文件返回空列表`() {
        assertTrue(CsvCodec.decode("").isEmpty())
        assertTrue(CsvCodec.decode("\n\n\n").isEmpty())
    }
}
