package com.autobook.app.util

object NotifDiagnose {

    fun reasonOf(text: String): String? {
        if (listOf("零钱通", "余额宝", "理财通", "理财", "申购", "赎回", "基金", "国债").any { text.contains(it) }) return "理财"
        if (listOf(
                "积分", "待领取", "点击领取", "限时",
                "立减金", "优惠券", "满减", "抽奖", "邀您", "礼包"
            ).any { text.contains(it) }) return "营销"
        if (text.contains("验证码")) return "验证码"
        val hasAmount = Regex("""[¥￥]\s*\d|\d+(?:\.\d{1,2})?\s*元|人民币\s*\d""").find(text) != null
        if (!hasAmount) return "无金额"
        return null
    }
}
