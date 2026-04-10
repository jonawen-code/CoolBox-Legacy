package com.example.coolbox.util

object InventoryIntelligence {
    val FOOD_CATALOG = linkedMapOf(
        "肉蛋水产" to listOf("牛肉", "猪肉", "羊肉", "排骨", "鸡", "鸭", "鱼", "虾", "蟹", "鸡蛋", "鸭蛋"),
        "奶品饮料" to listOf("鲜奶", "黄油", "奶酪", "橙汁", "柠檬汁", "汽水", "啤酒"),
        "速冻食品" to listOf("水饺", "汤圆", "冻榴莲果肉"),
        "蔬菜水果" to listOf("苹果", "橙", "蓝莓", "草莓", "西瓜", "番茄", "辣椒", "白菜", "菜心", "绿叶菜"),
        "熟食剩菜" to listOf("剩菜")
    )

    val MEDICINE_CATALOG = linkedMapOf(
        "🌡️ 感冒发烧" to listOf("感冒灵", "布洛芬", "对乙酰氨基酚", "连花清瘟", "板蓝根"),
        "💊 消炎止痛" to listOf("阿莫西林", "头孢", "扶他林", "芬必得"),
        "🤢 肠胃用药" to listOf("蒙脱石散", "健胃消食片", "吗丁啉", "益生菌"),
        "🩹 外用药膏" to listOf("碘伏", "酒精", "创可贴", "支架", "棉签"),
        "💖 慢性病药" to listOf("降压药", "降糖药", "阿司匹林"),
        "🥦 营养补充" to listOf("维生素C", "钙片", "鱼油", "复合维生素"),
        "🚑 急救必备" to listOf("速效救心丸", "硝酸甘油", "体温计")
    )

    val UNIT_MAP = mapOf(
        "蔬菜水果" to "斤",
        "肉蛋水产" to "kg",
        "奶品饮料" to "瓶",
        "速冻食品" to "袋",
        "熟食剩菜" to "份"
    )

    val MEDICINE_UNIT_MAP = mapOf(
        "🌡️ 感冒发烧" to "盒",
        "💊 消炎止痛" to "盒",
        "🤢 肠胃用药" to "盒",
        "🩹 外用药膏" to "个",
        "💖 慢性病药" to "盒",
        "🥦 营养补充" to "瓶",
        "🚑 急救必备" to "个"
    )

    fun getMedicineRecommendation(name: String): Pair<String, String> {
        val n = name.lowercase()
        return when {
            n.contains("感") || n.contains("烧") || n.contains("咳") || n.contains("热") || n.contains("维c") -> "🌡️ 感冒发烧" to "🌡️"
            n.contains("消炎") || n.contains("布") || n.contains("头") || n.contains("孢") || n.contains("阿") -> "💊 消炎止痛" to "💊"
            n.contains("胃") || n.contains("泻") || n.contains("肠") || n.contains("消化") -> "🤢 肠胃用药" to "🤢"
            n.contains("外用") || n.contains("膏") || n.contains("贴") || n.contains("碘") || n.contains("创") -> "🩹 外用药膏" to "🩹"
            n.contains("压") || n.contains("糖") || n.contains("心") || n.contains("慢") -> "💖 慢性病药" to "💖"
            n.contains("维") || n.contains("钙") || n.contains("油") || n.contains("补") -> "🥦 营养补充" to "🥦"
            n.contains("急") || n.contains("丸") || n.contains("救") -> "🚑 急救必备" to "🚑"
            else -> ("🌡️ 感冒发烧" to "🌡️")
        }
    }

    fun getRecommendedLocation(category: String, itemName: String, allFullNames: List<String>, deviceZoneMap: Map<String, List<String>>, caps: Map<String, String>): String {
        if (allFullNames.isEmpty()) return ""
        
        // 0. High priority rules
        if (category == "奶品饮料" || category == "蔬菜水果") {
            val lockedTarget = allFullNames.find { it.contains("大冰箱") && it.contains("冷藏") }
            if (lockedTarget != null) return lockedTarget
        }

        // 1. Expert dictionary rules
        val isEgg = itemName.contains("蛋")
        val requiresFreezer = itemName.contains("冻") || itemName.contains("冰") || itemName.contains("雪糕") ||
                              category == "速冻食品" ||
                              (category == "肉蛋水产" && !isEgg)
        
        val targetCap = if (requiresFreezer) "冷冻" else "冷藏"
        val oppositeCap = if (targetCap == "冷冻") "冷藏" else "冷冻"

        // 2. Selection logic
        return allFullNames.find { fullName ->
            val isOpposite = fullName.contains(oppositeCap)
            val explicitMatch = fullName.contains(targetCap)
            val base = deviceZoneMap.keys.find { EquipmentManager.normalizeToDigit(fullName).startsWith(EquipmentManager.normalizeToDigit(it)) }
            val baseMatch = base != null && caps[base]?.contains(targetCap) == true

            if (isOpposite) return@find false
            explicitMatch || baseMatch
        } ?: allFullNames.firstOrNull() ?: ""
    }
}
