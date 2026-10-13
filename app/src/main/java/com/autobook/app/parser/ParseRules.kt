package com.autobook.app.parser

import com.autobook.app.data.Categories

/**
 * 解析规则数据化（Phase 1）：全部关键词/词表/分类规则以数据形式集中于此，
 * 两个解析通道（通知/短信）与分类器共用单一数据源，从根源上杜绝"两通道关键词不对称"类 bug。
 * 行为与历史硬编码版本完全一致（137 个回归测试零改动护航）。
 *
 * Phase 2（后续版本）：规则可由用户查看/启停，甚至自定义关键词映射。
 */
object ParseRules {

    // ===== 短信通道 =====
    val smsIncomeKeywords = listOf(
        "入账", "存入", "转入", "转账收入", "收到转账", "退款", "工资", "收入", "汇入", "到账"
    )
    val smsExpenseKeywords = listOf(
        "消费", "支出", "支付", "扣款", "取款", "转账支出", "划出", "代扣", "交易", "转账", "还款"
    )
    val smsStrongExpenseKeywords = listOf("支出", "消费", "扣款", "取款", "划出")
    val smsFinanceBlockWords = listOf("零钱通", "余额宝", "理财通", "理财", "申购", "赎回", "基金", "国债")
    val smsMarketingWords = listOf(
        "优惠券", "立减", "满减", "抽奖", "待领取", "点击领取", "礼包", "邀您"
    )
    val smsBankNumbers = listOf(
        "95588", "95533", "95555", "95566", "95599", "95568", "95528",
        "95558", "95577", "95561", "95501", "95595", "95508", "95559",
        "95511", "95516", "95580", "95594", "96699", "95567"
    )

    // ===== 通知通道 =====
    val paymentKeywords = listOf(
        "支付", "付款", "收款", "转账", "退款", "消费", "入账", "扣款", "支出", "交易", "还款", "缴费", "缴纳"
    )
    val refundIncomeKeywords = listOf(
        "收款", "入账", "转入", "收到", "退款", "返现", "收入", "到账", "汇入"
    )
    val strongExpenseKeywords = listOf("支出", "扣款")
    val financialBlockWords = listOf(
        "零钱通", "余额宝", "理财通", "理财", "申购", "赎回", "基金", "国债", "定期存款", "大额存单"
    )
    val summaryWords = listOf(
        "共计", "总计", "汇总", "账单已出", "账单未出", "对账单", "本期账单", "月账单", "账单出炉"
    )
    val marketingStrongWords = listOf("立减金", "优惠券", "满减", "抽奖", "邀您", "礼包")
    val marketingWeakWords = listOf("积分", "待领取", "点击领取", "限时")
    val bankGateWords = listOf(
        "银行", "支出", "收入", "消费", "交易", "支付", "入账", "收款", "转入",
        "退款", "还款", "到账", "转账", "缴费", "缴纳"
    )
    val genericTitles = setOf(
        "交易提醒", "动账通知", "账户变动", "账户变动提醒", "消费通知",
        "收支通知", "入账通知", "交易通知", "动账提醒", "资金变动"
    )
    val bankPackages = mapOf(
        "com.android.bankabc" to "农业银行",
        "com.icbc" to "工商银行",
        "com.chinamworld.main" to "建设银行",
        "com.chinamworld.bocmbci" to "中国银行",
        "cmb.pb" to "招商银行",
        "com.cmbchina.ccd.pluto.cmbActivity" to "招商银行",
        "com.bankcomm.Bankcomm" to "交通银行",
        "com.yitong.mbank.psbc" to "邮储银行",
        "com.ecitic.bank.mobile" to "中信银行",
        "com.cebbank.mobile.cemb" to "光大银行",
        "cn.com.cmbc.newmbank" to "民生银行",
        "com.pingan.paces.ccms" to "平安银行",
        "com.cgbchina.xpt" to "广发银行",
        "com.cib.cibmb" to "兴业银行",
        "cn.com.spdb.mobilebank.per" to "浦发银行",
        "com.srcb.pmbank" to "上海银行",
        "com.nbbank" to "宁波银行"
    )

    // ===== 分类规则（分类 → 全文关键词，按顺序匹配，首个命中生效）=====
    val categoryRules: LinkedHashMap<String, List<String>> = linkedMapOf(
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
}
