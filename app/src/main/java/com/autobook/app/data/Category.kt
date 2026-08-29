package com.autobook.app.data

object Categories {
    const val FOOD = "餐饮美食"
    const val SHOPPING = "购物消费"
    const val TRANSPORT = "交通出行"
    const val EDUCATION = "教育学习"
    const val BEAUTY = "美容护理"
    const val MEDICAL = "医疗健康"
    const val ENTERTAINMENT = "娱乐休闲"
    const val GIFT = "人情社交"
    const val COMMUNICATION = "通讯网络"
    const val INCOME = "收入"
    const val OTHER = "其他"

    data class CategoryGroup(val name: String, val children: List<String>)

    val expenseGroups = listOf(
        CategoryGroup(FOOD, listOf("早餐", "午餐", "晚餐", "夜宵", "外卖", "零食", "水果", "烟酒", "食材")),
        CategoryGroup(SHOPPING, listOf("日用", "服饰", "鞋包", "美妆", "数码", "家电", "超市", "网购")),
        CategoryGroup(TRANSPORT, listOf("公交", "地铁", "打车", "加油", "停车", "火车", "飞机", "骑行")),
        CategoryGroup(EDUCATION, listOf("学费", "书籍", "培训", "文具")),
        CategoryGroup(BEAUTY, listOf("理发", "护肤", "美甲")),
        CategoryGroup(MEDICAL, listOf("药品", "门诊", "住院", "体检", "保健")),
        CategoryGroup(ENTERTAINMENT, listOf("电影", "游戏", "KTV", "旅游", "运动", "聚会")),
        CategoryGroup(GIFT, listOf("红包", "礼物", "请客", "孝敬")),
        CategoryGroup(COMMUNICATION, listOf("话费", "宽带", "流量")),
        CategoryGroup(OTHER, emptyList())
    )

    val incomeChildren = listOf("工资", "奖金", "兼职", "理财", "红包", "退款", "其他")

    val expenseCategories = expenseGroups.map { it.name }
    val all = expenseCategories + INCOME

    fun childrenOf(primary: String, custom: List<String> = emptyList()): List<String> {
        val builtin = expenseGroups.firstOrNull { it.name == primary }?.children ?: emptyList()
        return builtin + custom.filter { it !in builtin }
    }

    fun primaryOf(category: String): String {
        val idx = category.indexOf('/')
        val primary = if (idx > 0) category.substring(0, idx) else category
        return if (all.contains(primary)) primary else OTHER
    }

    // 旧分类名 → 新分类名（CSV 导入与学习数据迁移用）
    val legacyMap = mapOf(
        "餐饮" to FOOD,
        "购物" to SHOPPING,
        "交通" to TRANSPORT,
        "教育" to EDUCATION,
        "美容" to BEAUTY,
        "医疗" to MEDICAL,
        "娱乐" to ENTERTAINMENT,
        "人情" to GIFT,
        "通讯" to COMMUNICATION,
        "居住" to OTHER,
        "宠物" to OTHER
    )

    fun mapLegacy(name: String): String = legacyMap[name] ?: name
}
