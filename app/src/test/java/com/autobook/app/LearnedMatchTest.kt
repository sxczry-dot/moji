package com.autobook.app

import com.autobook.app.util.LearnedMatch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LearnedMatchTest {

    @Test
    fun `精确匹配优先于模糊`() {
        val entries = listOf(
            "蜜雪冰城" to "餐饮",
            "蜜雪冰城万达店" to "购物"
        )
        assertEquals("购物", LearnedMatch.categoryOf(entries, "蜜雪冰城万达店"))
        assertEquals("餐饮", LearnedMatch.categoryOf(entries, "蜜雪冰城"))
    }

    @Test
    fun `商户名变体模糊匹配`() {
        val entries = listOf("蜜雪冰城" to "餐饮")
        assertEquals("餐饮", LearnedMatch.categoryOf(entries, "蜜雪冰城（万达店）"))
        assertEquals("餐饮", LearnedMatch.categoryOf(entries, "蜜雪冰城XX分店"))
    }

    @Test
    fun `反向包含也能匹配`() {
        val entries = listOf("蜜雪冰城（万达店）" to "餐饮")
        assertEquals("餐饮", LearnedMatch.categoryOf(entries, "蜜雪冰城"))
    }

    @Test
    fun `单字不参与模糊`() {
        val entries = listOf("饭" to "餐饮")
        assertNull(LearnedMatch.categoryOf(entries, "饭店"))
    }

    @Test
    fun `无匹配返回空`() {
        val entries = listOf("瑞幸" to "餐饮")
        assertNull(LearnedMatch.categoryOf(entries, "星巴克"))
        assertNull(LearnedMatch.categoryOf(entries, ""))
    }
}
