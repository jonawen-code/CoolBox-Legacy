package com.example.coolbox.util

object IconManager {
    private val iconMap = mapOf(
        "牛肉" to "ic_food_beef",
        "猪肉" to "ic_food_pork",
        "排骨" to "ic_food_ribs",
        "羊肉" to "ic_food_lamb",
        "鸡" to "ic_food_chicken",
        "鱼" to "ic_food_fish",
        "虾" to "ic_food_shrimp",
        "蟹" to "ic_food_crab",
        "腊肠" to "ic_food_sausages",
        "腊肉" to "ic_food_sausages",
        "水饺" to "ic_food_dumpling",
        "汤圆" to "ic_food_dumpling",
        "冻榴莲果肉" to "ic_food_durian",
        "鲜奶" to "ic_food_milk",
        "黄油" to "ic_food_butter",
        "奶酪" to "ic_food_cheese",
        "橙汁" to "ic_food_juice",
        "柠檬汁" to "ic_food_lemon",
        "汽水" to "ic_food_cola",
        "啤酒" to "ic_food_beer",
        "苹果" to "ic_food_apple",
        "橙" to "ic_food_tangerine",
        "冰淇凌" to "ic_food_icecream",
        "蓝莓" to "ic_food_blueberries",
        "草莓" to "ic_food_strawberry",
        "西瓜" to "ic_food_watermelon",
        "番茄" to "ic_food_tomato",
        "辣椒" to "ic_food_pepper",
        "白菜" to "ic_food_lettuce",
        "菜心" to "ic_food_broccoli",
        "绿叶菜" to "ic_food_lettuce",
        "剩菜" to "ic_food_cooked",
        "酱料" to "ic_food_jam",
        "果酱" to "ic_food_jam",
        "调味品" to "ic_food_jam",
        "贝类" to "ic_food_shellfish"
    )

    fun getIconForItem(name: String, currentSpace: Int = 0): String {
        if (currentSpace == 1) {
            val rec = InventoryIntelligence.getMedicineRecommendation(name)
            return rec.second
        }
        if (name.contains("蛋")) {
            return "ic_food_egg"
        }
        val found = iconMap.entries.find { name.contains(it.key) }?.value
        return if (found.isNullOrBlank()) Constants.ICON_DEFAULT else found
    }
}
