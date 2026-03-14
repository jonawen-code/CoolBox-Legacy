// Version: V3.0.0-Pre21
package com.example.coolbox.ui

import android.app.Application
import androidx.lifecycle.*
import com.example.coolbox.CoolBoxApplication
import com.example.coolbox.InventoryItem
import com.example.coolbox.data.AppDatabase
import com.example.coolbox.data.FoodEntity
import com.example.coolbox.data.FoodRepository
import kotlinx.coroutines.launch
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.UUID

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val _catalog = linkedMapOf(
        "肉蛋水产" to listOf("牛肉", "猪肉", "排骨", "鸡", "鸭", "鱼", "虾", "蟹", "鸡蛋", "鸭蛋"),
        "奶品饮料" to listOf("鲜奶", "黄油", "奶酪", "橙汁", "柠檬汁", "汽水", "啤酒"),
        "速冻食品" to listOf("水饺", "汤圆", "冻榴莲果肉"),
        "蔬菜水果" to listOf("苹果", "橙", "蓝莓", "草莓", "西瓜", "番茄", "辣椒", "白菜", "菜心", "绿叶菜"),
        "熟食剩菜" to listOf("剩菜")
    )

    private val _iconMap = mapOf(
        "牛肉" to "ic_food_beef",
        "猪肉" to "ic_food_pork",
        "排骨" to "ic_food_ribs",
        "鸡" to "ic_food_chicken",
        "鱼" to "ic_food_fish",
        "虾" to "ic_food_shrimp",
        "蟹" to "ic_food_crab",
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
        "蓝莓" to "ic_food_blueberries",
        "草莓" to "ic_food_strawberry",
        "西瓜" to "ic_food_watermelon",
        "番茄" to "ic_food_tomato",
        "辣椒" to "ic_food_pepper",
        "白菜" to "ic_food_lettuce",
        "菜心" to "ic_food_broccoli",
        "绿叶菜" to "ic_food_lettuce",
        "剩菜" to "ic_food_cooked"
    )

    private val _unitMap = mapOf(
        "蔬菜水果" to "斤",
        "肉蛋水产" to "kg",
        "奶品饮料" to "瓶",
        "速冻食品" to "袋",
        "熟食剩菜" to "份"
    )

    private val repository: FoodRepository = (application as CoolBoxApplication).repository
    
    val allFood: LiveData<List<FoodEntity>> = repository.allItems
    
    private val gson = Gson()

    // Simplified SharedPreferences management for legacy
    private val prefs = application.getSharedPreferences("coolbox_prefs", Application.MODE_PRIVATE)

    private val _fridges = MutableLiveData<List<String>>()
    val fridges: LiveData<List<String>> = _fridges

    private val _fridgeBases = MutableLiveData<List<String>>()
    val fridgeBases: LiveData<List<String>> = _fridgeBases

    private val _deviceZoneMap = MutableLiveData<Map<String, List<String>>>()
    val deviceZoneMap: LiveData<Map<String, List<String>>> = _deviceZoneMap

    private val _isSetupComplete = MutableLiveData<Boolean>(prefs.getBoolean("setup_complete", false))
    val isSetupComplete: LiveData<Boolean> = _isSetupComplete

    private val _fridgeCapabilities = MutableLiveData<Map<String, String>>()
    val fridgeCapabilities: LiveData<Map<String, String>> = _fridgeCapabilities
    
    // UI Scaling
    private val _fontScale = MutableLiveData<Float>(prefs.getFloat("font_scale", 1.0f))
    val fontScale: LiveData<Float> = _fontScale
    
    // True Time Machine
    private val _timeOffsetMs = MutableLiveData<Long>(0L)
    val timeOffsetMs: LiveData<Long> = _timeOffsetMs

    fun nowMs(): Long = System.currentTimeMillis() + (_timeOffsetMs.value ?: 0L)

    // Default sync URL for development/demo
    var syncServerUrl: String = "http://192.168.31.94:3001/coolbox"
    
    private val _syncEnabled = MutableLiveData<Boolean>(false)
    val syncEnabled: LiveData<Boolean> = _syncEnabled

    private val _categories = MutableLiveData<List<String>>()
    val categories: LiveData<List<String>> = _categories





    fun getIconForItem(name: String): String {
        // Version: V3.0.0-Pre3 (Global Icon Brain)
        if (name.contains("蛋")) {
            return "ic_food_egg"
        }
        val found = _iconMap.entries.find { name.contains(it.key) }?.value
        return if (found.isNullOrBlank()) com.example.coolbox.util.Constants.ICON_DEFAULT else found
    }
    // Version: V2.8.0-RC3 (Build 35)
    fun getRecommendedFridge(category: String, itemName: String): String {
        val currentFridges = _fridges.value ?: emptyList()
        if (currentFridges.isEmpty()) return ""
        val caps = _fridgeCapabilities.value ?: emptyMap()
        val deviceZoneMap = _deviceZoneMap.value ?: emptyMap()

        // 0. 最高指令拦截：指定分类强行绑定大冰箱冷藏室
        if (category == "奶品饮料" || category == "蔬菜水果") {
            val lockedTarget = currentFridges.find { it.contains("大冰箱") && it.contains("冷藏") }
            if (lockedTarget != null) return lockedTarget
        }

        // 1. 专家级绝对规则字典 (Expert First)
        val isEgg = itemName.contains("鸡蛋") || itemName.contains("鸭蛋") || itemName.contains("皮蛋") || itemName.contains("咸蛋")
        val requiresFreezer = itemName.contains("冻") || itemName.contains("冰") || itemName.contains("雪糕") ||
                              category == "速冻食品" ||
                              (category == "肉蛋水产" && !isEgg)
        val targetCap = if (requiresFreezer) "冷冻" else "冷藏"
        val oppositeCap = if (targetCap == "冷冻") "冷藏" else "冷冻"

        // 2. 读取用户历史习惯
        val prefKey = "pref_$itemName"
        val lastUsed = prefs.getString(prefKey, null)

        // 3. 习惯校验与防呆拦截 (Fool-proofing)
        if (!lastUsed.isNullOrEmpty()) {
            val baseDevice = deviceZoneMap.keys.find { normalizeToDigit(lastUsed).startsWith(normalizeToDigit(it)) }
            if (baseDevice != null) {
                val deviceCap = caps[baseDevice] ?: "冷藏"
                // 致命修复：不仅要求主设备支持该温区，而且历史记录的具体层级名称【绝对不能】包含相反温区字眼！
                if (deviceCap.contains(targetCap) && !lastUsed.contains(oppositeCap)) {
                    return lastUsed
                }
                // 如果走到这里，说明历史记忆违背了常识（比如牛奶放了冷冻），立刻废弃该记忆，进入下方的系统强制推荐！
            }
        }

        // 4. 习惯失效或无历史，系统强制执行专家推荐
        // Build 38: Zone-conflict-aware exclusion
        val bestFullName = currentFridges.find { fullName ->
            val isOpposite = fullName.contains(oppositeCap)
            val explicitMatch = fullName.contains(targetCap)
            val base = deviceZoneMap.keys.find { normalizeToDigit(fullName).startsWith(normalizeToDigit(it)) }
            val baseMatch = base != null && caps[base]?.contains(targetCap) == true

            // 防呆核心：如果层级名称明确写了相反的温区，绝对排除！
            if (isOpposite) return@find false

            explicitMatch || baseMatch
        } ?: currentFridges.firstOrNull() ?: ""
        if (bestFullName.isEmpty()) return ""

        val bestBase = deviceZoneMap.keys.find { normalizeToDigit(bestFullName).startsWith(normalizeToDigit(it)) } ?: return bestFullName
        val bestZone = normalizeToDigit(bestFullName).removePrefix(normalizeToDigit(bestBase)).replace("-", "").trim()

        return if (bestZone.isNotEmpty()) "$bestBase - $bestZone" else bestBase
    }
    // Version: V2.8.0-RC3 (Build 38)

    fun getDisplayName(raw: String): String {
        // Build 30: Strip prefixes, brackets, normalize Chinese digits, capitalize
        val cleaned = raw.replace("ic_food_", "")
                         .replace("cat_", "")
                         .replace("_", " ")
                         .replace(Regex("\\[.*?\\]"), "") // Strip any [icon] bracket tags
                         .trim()
        return normalizeToDigit(cleaned)
            .split(" ")
            .joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }
    }

    /**
     * Build 30: Convert all Chinese ordinal numbers to Arabic digits.
     * "第一层" -> "1层", "冷藏三层" -> "冷藏3层", "第十二层" -> "12层"
     */
    fun normalizeToDigit(s: String): String {
        var result = s.replace("第", "") // Strip "第" prefix unconditionally
                      .replace("室", "") // Strip "室" unconditionally

        // Full mapping: composite Chinese numbers -> Arabic
        val composites = listOf(
            "十二" to "12", "十三" to "13", "十四" to "14", "十五" to "15",
            "十六" to "16", "十七" to "17", "十八" to "18", "十九" to "19",
            "二十" to "20", "十一" to "11", "十" to "10"
        )
        for ((cn, digit) in composites) {
            result = result.replace(cn, digit)
        }

        // Single character mapping
        val singles = listOf(
            "一" to "1", "二" to "2", "三" to "3", "四" to "4", "五" to "5",
            "六" to "6", "七" to "7", "八" to "8", "九" to "9"
        )
        for ((cn, digit) in singles) {
            result = result.replace(cn, digit)
        }
        return result
    }

    private fun trackStorageUsage(category: String, fridgeName: String) {
        prefs.edit().putString("pref_$category", fridgeName).apply()
    }

    // Build 37: Persist user-added food items to custom dictionary
    private fun saveCustomItemToCatalog(category: String, itemName: String) {
        val key = "custom_items_$category"
        val customSet = prefs.getStringSet(key, emptySet()) ?: emptySet()
        if (!customSet.contains(itemName)) {
            prefs.edit().putStringSet(key, customSet + itemName).apply()
        }
    }



    fun getCatalogCategories(): List<String> = _categories.value ?: _catalog.keys.toList()
    fun getCatalogItems(category: String): List<String> {
        // Build 37: Merge default catalog + user custom dictionary
        val baseItems = _catalog[category] ?: _catalog.entries.find { it.key.contains(category) || category.contains(it.key) }?.value ?: emptyList()
        val customItems = prefs.getStringSet("custom_items_$category", emptySet())?.toList() ?: emptyList()
        return (baseItems + customItems).distinct()
    }
    fun getCatalogData(): Map<String, List<String>> {
        val currentKeys = _categories.value ?: return _catalog
        return currentKeys.associateWith { key -> getCatalogItems(key) }
    }
    
    fun getDefaultUnit(category: String, item: String): String {
        if (item.contains("鸡蛋")) return "个"
        if (item.contains("牛奶") || item.contains("可乐") || item.contains("果汁") || item.contains("啤酒")) return "瓶"
        if (category.contains("饮料") || category.contains("奶")) return "瓶"
        if (category.contains("肉")) return "kg"
        if (category.contains("蔬菜")) return "斤"
        return _unitMap[category] ?: "个"
    }

    private fun List<String>.sortedNaturally(): List<String> {
        // Build 30: Normalize Chinese digits BEFORE sorting
        val regex = Regex("(\\D*)(\\d+)")
        return this.sortedWith { s1, s2 ->
            val n1 = normalizeToDigit(s1)
            val n2 = normalizeToDigit(s2)
            val m1 = regex.find(n1)
            val m2 = regex.find(n2)
            if (m1 != null && m2 != null) {
                val prefixCmp = m1.groupValues[1].compareTo(m2.groupValues[1])
                if (prefixCmp != 0) prefixCmp
                else (m1.groupValues[2].toIntOrNull() ?: 0).compareTo(m2.groupValues[2].toIntOrNull() ?: 0)
            } else n1.compareTo(n2)
        }
    }

    private fun loadSettings() {
        _syncEnabled.value = prefs.getBoolean("sync_enabled", false)
        syncServerUrl = prefs.getString("sync_server_url", "http://192.168.31.94:3001/coolbox") ?: "http://192.168.31.94:3001/coolbox"
        
        // --- 兼容性读取 Fridges 开始 ---
        val fridgesList = try {
            // 1. 尝试按 JSON 字符串读取
            val json = prefs.getString("fridges", null)
            if (json != null) {
                gson.fromJson<List<String>>(json, object : TypeToken<List<String>>() {}.type)
            } else {
                // 2. 如果没有 JSON，尝试按旧版 StringSet 读取
                prefs.getStringSet("fridges", setOf("我的冰箱", "小冰柜"))?.toList()
            }
        } catch (e: Exception) {
            // 3. 如果类型冲突报错，强制按 StringSet 再次尝试
            prefs.getStringSet("fridges", setOf("我的冰箱", "小冰柜"))?.toList()
        } ?: listOf("我的冰箱", "小冰柜")
        
        val normalizedFridges = fridgesList.map { normalizeToDigit(it) }.sortedNaturally()
        _fridges.value = normalizedFridges
        // --- 兼容性读取 Fridges 结束 ---

        // --- 兼容性读取 Fridge Bases 开始 ---
        val basesList = try {
            val json = prefs.getString("fridge_bases", null)
            if (json != null) {
                gson.fromJson<List<String>>(json, object : TypeToken<List<String>>() {}.type)
            } else {
                prefs.getStringSet("fridge_bases", emptySet())?.toList()
            }
        } catch (e: Exception) {
            prefs.getStringSet("fridge_bases", emptySet())?.toList()
        } ?: listOf("我的冰箱", "小冰柜")

        val normalizedBases = basesList.map { normalizeToDigit(it) }.sortedNaturally()
        _fridgeBases.value = normalizedBases
        // --- 兼容性读取 Fridge Bases 结束 ---

        // Write back normalized names in JSON format to standardize for Pre17
        prefs.edit().putString("fridges", gson.toJson(normalizedFridges)).apply()
        prefs.edit().putString("fridge_bases", gson.toJson(normalizedBases)).apply()

        // Category order: ALWAYS use _catalog key order as the single source of truth.
        val catalogKeys = _catalog.keys.toList()
        val savedStr = prefs.getString("categories_ordered", null)
        val userExtras = if (!savedStr.isNullOrBlank()) {
            val saved = savedStr.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            saved.filter { s -> catalogKeys.none { s.contains(it) || it.contains(s) } }
        } else emptyList()
        _categories.value = catalogKeys + userExtras
        prefs.edit().putString("categories_ordered", _categories.value!!.joinToString(",")).apply()

        val caps = mutableMapOf<String, String>()
        fridgesList.forEach { name ->
            // Build 35: 冰柜也默认识别为冷冻区
            val cap = prefs.getString("cap_$name", if (name.contains("冷冻") || name.contains("冰柜")) "冷冻" else "冷藏")
            caps[name] = cap ?: "冷藏"
        }
        _fridgeCapabilities.value = caps
        refreshDynamicData()
    }

    fun refreshDynamicData() {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val bases = _fridgeBases.value ?: emptyList()
            val allFullNames = _fridges.value ?: emptyList()
            
            val mapping = LinkedHashMap<String, List<String>>()
            bases.forEach { base ->
                val zones = allFullNames.filter { it.startsWith(base) }
                    .map { normalizeToDigit(it.removePrefix(base).trim()) }
                    .filter { it.isNotEmpty() }
                    .sortedNaturally()
                mapping[base] = zones
            }
            _deviceZoneMap.postValue(mapping)
        }
    }

    private fun extractNumber(s: String): Int? {
        // Build 28: Simplified to Arabic digits only per architect instruction
        val digitMatch = Regex("(\\d+)").find(s)
        return digitMatch?.groupValues?.get(1)?.toIntOrNull()
    }
    private val _currentFridge = MutableLiveData<String?>()
    private val appVersion: LiveData<String> = MutableLiveData("V3.0.0-Pre21")
    val currentFridge: LiveData<String?> = _currentFridge

    fun refreshState(keepSummary: Boolean = false) {
        loadSettings()
        _isSetupComplete.value = prefs.getBoolean("setup_complete", false)
        if (keepSummary && _currentFridge.value == null) {
            // Stay on Summary
        } else {
            _currentFridge.value = _fridges.value?.firstOrNull() ?: ""
        }
    }

    fun refreshSettings() {
        loadSettings()
    }

    fun onSyncComplete() {
        // Force reset to Summary tab
        _currentFridge.value = null
        refreshState(keepSummary = true)
    }

    fun completeSetup(fridges: List<String>, fridgeBases: List<String>, capabilities: Map<String, String>, categories: List<String>, syncEnabled: Boolean, syncUrl: String) {
        prefs.edit()
            .putStringSet("fridges", fridges.toSet())
            .putStringSet("fridge_bases", fridgeBases.toSet())
            .putString("categories_ordered", categories.joinToString(","))
            .putBoolean("setup_complete", true)
            .putBoolean("sync_enabled", syncEnabled)
            .putString("sync_server_url", syncUrl)
            .apply()

        // Store capabilities as "cap_[FridgeName]"
        val editor = prefs.edit()
        capabilities.forEach { (name, cap) ->
            editor.putString("cap_$name", cap)
        }
        editor.apply()
        
        refreshDynamicData()
        refreshState()
    }

    fun setFridge(name: String?) {
        _currentFridge.value = name
    }

    fun setFontScale(scale: Float) {
        _fontScale.value = scale
        prefs.edit().putFloat("font_scale", scale).apply()
    }

    fun setTimeOffset(ms: Long) {
        _timeOffsetMs.value = ms
    }

    fun addTimeOffset(ms: Long) {
        val current = _timeOffsetMs.value ?: 0L
        _timeOffsetMs.value = current + ms
    }

    fun resetTimeOffset() {
        _timeOffsetMs.value = 0L
    }

    fun addFood(name: String, icon: String, category: String, quantity: Double, unit: String, expiryMs: Long, portions: Int = 1, targetFridge: String? = null, remark: String = "") {
        viewModelScope.launch {
            val fridge = targetFridge ?: _currentFridge.value ?: ""
            val weightPerPortion = if (portions > 0) quantity / portions else quantity
            
            // Search for an existing identical item to merge (same name, icon, fridge, category, unit, expiry, and REMARK)
            val existing = allFood.value?.find {
                it.name == name && 
                it.icon == icon && 
                it.fridgeName == fridge && 
                it.category == category &&
                it.unit == unit &&
                it.expiryDateMs == expiryMs &&
                it.remark == remark &&
                Math.abs(it.weightPerPortion - weightPerPortion) < 0.01 // same portion size
            }

            if (existing != null) {
                // Merge into existing item
                val updated = existing.copy(
                    quantity = existing.quantity + quantity,
                    portions = existing.portions + portions
                )
                repository.insertItem(updated)
            } else {
                // Create new item
                val entity = FoodEntity(
                    id = UUID.randomUUID().toString(),
                    icon = icon,
                    name = name,
                    fridgeName = fridge,
                    inputDateMs = nowMs(),
                    expiryDateMs = expiryMs,
                    quantity = quantity,
                    weightPerPortion = weightPerPortion,
                    portions = portions,
                    category = category,
                    unit = unit,
                    remark = remark,
                    lastModifiedMs = nowMs()
                )
                repository.insertItem(entity)
                trackStorageUsage(category = name, fridgeName = fridge)
                saveCustomItemToCatalog(category, name) // Build 37: Remember user-added items
            }
            // Always export after ANY change (new or merged)
            AppDatabase.exportDatabase(getApplication())
        }
    }

    fun takePortion(entity: FoodEntity) {
        takePortions(entity, 1)
    }

    fun takePortions(entity: FoodEntity, amount: Int) {
        viewModelScope.launch {
            if (entity.portions > amount) {
                val updated = entity.copy(
                    portions = Math.max(0, entity.portions - amount),
                    quantity = Math.max(0.0, entity.quantity - (entity.weightPerPortion * amount))
                )
                repository.insertItem(updated)
            } else {
                val dao = AppDatabase.getDatabase(getApplication()).foodDao()
                dao.softDelete(entity.id, nowMs())
            }
            AppDatabase.exportDatabase(getApplication())
        }
    }

    fun transferItem(entity: FoodEntity, targetFridge: String, portionsToTransfer: Int = -1) {
        viewModelScope.launch {
            val p = if (portionsToTransfer <= 0) entity.portions else portionsToTransfer
            
            if (p >= entity.portions) {
                // Transfer ALL
                val updated = entity.copy(fridgeName = targetFridge)
                repository.insertItem(updated)
            } else {
                // Transfer PARTIAL
                val weightToMove = entity.weightPerPortion * p
                
                // 1. Reduce original
                val originalReduced = entity.copy(
                    quantity = Math.max(0.0, entity.quantity - weightToMove),
                    portions = Math.max(0, entity.portions - p)
                )
                repository.insertItem(originalReduced)
                
                // 2. Create new in target
                val newEntity = entity.copy(
                    id = UUID.randomUUID().toString(),
                    fridgeName = targetFridge,
                    quantity = weightToMove,
                    portions = p
                )
                repository.insertItem(newEntity)
            }
            AppDatabase.exportDatabase(getApplication())
        }
    }

    fun updateFoodIcon(entity: FoodEntity, newIcon: String) {
        viewModelScope.launch {
            val updated = entity.copy(icon = newIcon)
            repository.insertItem(updated)
            AppDatabase.exportDatabase(getApplication())
        }
    }

    fun updateItem(entity: FoodEntity) {
        viewModelScope.launch {
            repository.insertItem(entity)
            saveCustomItemToCatalog(entity.category, entity.name) // Build 37: Remember user-added items
            AppDatabase.exportDatabase(getApplication())
        }
    }

    fun migrateCategory(oldCategory: String, newCategory: String) {
        viewModelScope.launch {
            repository.migrateCategory(oldCategory, newCategory)
            AppDatabase.exportDatabase(getApplication())
        }
    }

    fun deleteFood(entity: FoodEntity) {
        viewModelScope.launch {
            val dao = AppDatabase.getDatabase(getApplication()).foodDao()
            dao.softDelete(entity.id, nowMs())
            AppDatabase.exportDatabase(getApplication())
        }
    }

    init {
        loadSettings()
        viewModelScope.launch {
            repository.migrateEmptyIcons("ic_food_default", System.currentTimeMillis())
            AppDatabase.exportDatabase(getApplication())
        }
    }
}
// Version: V3.0.0-Pre21
