package com.autobook.app.parser

import com.autobook.app.data.Categories

object CategoryClassifier {

    private val rules = linkedMapOf(
        Categories.TRANSPORT to listOf(
            "滴滴", "地铁", "公交", "加油", "中石化", "中石油", "停车", "高铁",
            "火车", "12306", "机票", "航空", "打车", "出租车", "共享单车", "哈啰",
            "青桔", "etc", "高速", "充电桩", "代驾", "单车", "曹操", "花小猪",
            "t3出行", "神州", "高德打车", "洗车", "船票", "汽车票"
        ),
        Categories.FOOD to listOf(
            "外卖", "美团", "饿了么", "麦当劳", "肯德基", "汉堡", "星巴克", "瑞幸",
            "咖啡", "奶茶", "茶饮", "餐厅", "食堂", "饭店", "火锅", "烧烤", "小吃",
            "面馆", "烘焙", "面包", "早餐", "午餐", "晚餐", "夜宵", "买菜", "生鲜",
            "水果", "超市", "便利店", "饮水", "饭", "零食", "甜品", "蛋糕",
            "冰淇淋", "饺子", "包子", "煎饼", "拉面", "寿司", "披萨", "炸鸡",
            "鸡排", "鸭脖", "麻辣烫", "黄焖鸡", "粥", "蜜雪冰城", "茶百道",
            "喜茶", "奈雪", "沪上阿姨", "霸王茶姬", "古茗", "一点点", "库迪",
            "tims", "必胜客", "华莱士", "德克士", "塔斯汀", "乡村基", "老乡鸡",
            "大米先生", "真功夫", "杨国福", "绝味", "周黑鸭", "良品铺子",
            "三只松鼠", "来伊份", "盒马", "山姆", "永辉", "大润发", "沃尔玛",
            "家乐福", "全家", "罗森", "711", "便利蜂", "美宜佳", "海底捞",
            "呷哺", "小龙坎", "沙县"
        ),
        Categories.MEDICAL to listOf(
            "医院", "药房", "药店", "大药房", "门诊", "诊所", "体检", "医保",
            "挂号", "疫苗", "牙科", "眼科", "住院", "救护车"
        ),
        Categories.SHOPPING to listOf(
            "淘宝", "天猫", "京东", "拼多多", "唯品会", "商场", "百货", "服装",
            "服饰", "鞋", "化妆品", "数码", "家电", "家具", "日用品", "网购",
            "旗舰店", "优衣库", "zara", "hm", "耐克", "阿迪", "李宁", "安踏",
            "鸿星尔克", "名创优品", "无印良品", "宜家", "小米", "华为", "苹果",
            "荣耀", "oppo", "vivo", "当当", "苏宁", "国美", "母婴", "童装", "玩具"
        ),
        Categories.ENTERTAINMENT to listOf(
            "电影", "ktv", "游戏", "视频会员", "爱奇艺", "腾讯视频", "优酷",
            "哔哩哔哩", "网易云", "qq音乐", "steam", "门票", "景点", "旅游",
            "酒店", "网吧", "剧本杀", "密室", "b站", "抖音", "快手", "微博",
            "演唱会", "健身房", "游泳", "switch", "ps5", "xbox"
        ),
        Categories.COMMUNICATION to listOf(
            "话费", "宽带", "流量", "月租"
        ),
        Categories.EDUCATION to listOf(
            "学费", "培训", "课程", "书店", "图书", "文具", "考试", "网课",
            "学习", "学校", "幼儿园", "辅导班", "教材"
        ),
        Categories.GIFT to listOf(
            "红包", "礼物", "请客", "份子钱", "随礼", "孝敬", "社交", "亲友",
            "礼金", "压岁钱"
        ),
        Categories.BEAUTY to listOf(
            "理发", "美发", "美容", "护肤", "美甲", "美睫", "spa", "烫发",
            "染发", "造型"
        )
    )

    fun classifyExpense(text: String): String {
        val t = text.lowercase()
        for ((category, keywords) in rules) {
            if (keywords.any { t.contains(it.lowercase()) }) return category
        }
        return Categories.OTHER
    }

    fun classifyIncome(text: String): String = Categories.INCOME
}
