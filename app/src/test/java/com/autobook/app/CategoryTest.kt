package com.autobook.app

import com.autobook.app.data.Categories
import com.autobook.app.parser.CategoryClassifier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CategoryTest {

    @Test
    fun `primaryOf 正常情况`() {
        assertEquals(Categories.FOOD, Categories.primaryOf("餐饮美食"))
        assertEquals(Categories.FOOD, Categories.primaryOf("餐饮美食/早餐"))
        assertEquals(Categories.INCOME, Categories.primaryOf("收入/工资"))
    }

    @Test
    fun `primaryOf 异常输入兜底`() {
        assertEquals(Categories.OTHER, Categories.primaryOf(""))
        assertEquals(Categories.OTHER, Categories.primaryOf("不存在的分类"))
        assertEquals(Categories.OTHER, Categories.primaryOf("/餐饮"))
    }

    @Test
    fun `childrenOf 正常和异常`() {
        assertEquals(9, Categories.childrenOf(Categories.FOOD).size)
        assertTrue(Categories.childrenOf("不存在").isEmpty())
    }

    @Test
    fun `话费套餐不误分餐饮`() {
        assertEquals(Categories.COMMUNICATION, CategoryClassifier.classifyExpense("话费套餐充值"))
    }

    @Test
    fun `常见商户分类`() {
        assertEquals(Categories.FOOD, CategoryClassifier.classifyExpense("美团外卖"))
        assertEquals(Categories.TRANSPORT, CategoryClassifier.classifyExpense("滴滴出行"))
        assertEquals(Categories.SHOPPING, CategoryClassifier.classifyExpense("淘宝"))
        assertEquals(Categories.ENTERTAINMENT, CategoryClassifier.classifyExpense("爱奇艺会员"))
        assertEquals(Categories.OTHER, CategoryClassifier.classifyExpense("物业费"))
        assertEquals(Categories.MEDICAL, CategoryClassifier.classifyExpense("药店"))
        assertEquals(Categories.EDUCATION, CategoryClassifier.classifyExpense("书店"))
        assertEquals(Categories.GIFT, CategoryClassifier.classifyExpense("微信红包"))
        assertEquals(Categories.OTHER, CategoryClassifier.classifyExpense("猫粮"))
        assertEquals(Categories.BEAUTY, CategoryClassifier.classifyExpense("理发店"))
    }

    @Test
    fun `未匹配归其他`() {
        assertEquals(Categories.OTHER, CategoryClassifier.classifyExpense("未知商户"))
    }

    @Test
    fun `品牌词分类`() {
        assertEquals(Categories.FOOD, CategoryClassifier.classifyExpense("蜜雪冰城"))
        assertEquals(Categories.FOOD, CategoryClassifier.classifyExpense("永辉超市"))
        assertEquals(Categories.SHOPPING, CategoryClassifier.classifyExpense("优衣库"))
        assertEquals(Categories.TRANSPORT, CategoryClassifier.classifyExpense("美团单车"))
        assertEquals(Categories.MEDICAL, CategoryClassifier.classifyExpense("京东大药房"))
        assertEquals(Categories.ENTERTAINMENT, CategoryClassifier.classifyExpense("抖音"))
    }

    @Test
    fun `人名转账不误判餐饮`() {
        assertEquals(Categories.OTHER, CategoryClassifier.classifyExpense("张亮"))
    }

    @Test
    fun `收入恒为收入分类`() {
        assertEquals(Categories.INCOME, CategoryClassifier.classifyIncome("任何文本"))
    }
}
