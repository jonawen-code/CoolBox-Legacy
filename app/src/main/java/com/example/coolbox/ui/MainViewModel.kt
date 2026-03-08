package com.example.coolbox.ui

import android.app.Application
import androidx.lifecycle.*
import com.example.coolbox.CoolBoxApplication
import com.example.coolbox.InventoryItem
import com.example.coolbox.data.AppDatabase
import com.example.coolbox.data.FoodEntity
import com.example.coolbox.data.FoodRepository
import kotlinx.coroutines.launch
import java.util.UUID

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val _catalog = linkedMapOf(
        "肉蛋水产" to listOf("牛肉", "猪肉", "排骨", "鸡", "鱼", "虾", "蟹"),
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

    // Simplified SharedPreferences management for legacy
    private val prefs = application.getSharedPreferences("settings", Application.MODE_PRIVATE)

    private val _fridges = MutableLiveData<List<String>>()
    val fridges: LiveData<List<String>> = _fridges

    private val _fridgeBases = MutableLiveData<List<String>>()
    val fridgeBases: LiveData<List<String>> = _fridgeBases

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
    var syncServerUrl: String = "http://192.168.31.94:3000/coolbox"
    
    private val _syncEnabled = MutableLiveData<Boolean>(false)
    val syncEnabled: LiveData<Boolean> = _syncEnabled

    private val _categories = MutableLiveData<List<String>>()
    val categories: LiveData<List<String>> = _categories





    fun getIconForItem(name: String): String = _iconMap[name] ?: ""
    
    fun getRecommendedFridge(category: String, itemName: String): String {
        val currentFridges = _fridges.value ?: emptyList()
        if (currentFridges.isEmpty()) return ""

        // 1. Check Frequency-based preference (SharedPreferences)
        val prefKey = "pref_$category"
        val lastUsed = prefs.getString(prefKey, null)
        if (lastUsed != null && currentFridges.contains(lastUsed)) {
            return lastUsed
        }

        // 2. Category-based Logic
        val isFrozenPreferred = itemName.contains("肉") || itemName.contains("鱼") || itemName.contains("虾") || 
                               itemName.contains("蟹") || itemName.contains("排骨") || itemName.contains("熟食") ||
                               category.contains("肉") || category.contains("熟食")
        
        val caps = _fridgeCapabilities.value ?: emptyMap()
        
        // Find a fridge that matches our need
        val bestMatch = if (isFrozenPreferred) {
            currentFridges.find { caps[it]?.contains("冷冻") == true }
        } else {
            currentFridges.find { caps[it]?.contains("冷藏") == true }
        }

        return bestMatch ?: currentFridges[0]
    }

    private fun trackStorageUsage(category: String, fridgeName: String) {
        prefs.edit().putString("pref_$category", fridgeName).apply()
    }



    fun getCatalogCategories(): List<String> = _categories.value ?: _catalog.keys.toList()
    fun getCatalogItems(category: String): List<String> {
        // Exact match first, then fuzzy match (handles emoji prefix differences)
        return _catalog[category] ?: _catalog.entries.find { it.key.contains(category) || category.contains(it.key) }?.value ?: emptyList()
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

    private fun loadSettings() {
        val fridgeSet = prefs.getStringSet("fridges", setOf("我的冰箱", "小冰柜"))
        val fridges = fridgeSet?.toList() ?: listOf("我的冰箱", "小冰柜")
        _fridges.value = fridges

        val baseSet = prefs.getStringSet("fridge_bases", emptySet())
        _fridgeBases.value = if (baseSet.isNullOrEmpty()) listOf("我的冰箱", "小冰柜") else baseSet.toList()

        // Category order: ALWAYS use _catalog key order as the single source of truth.
        // Any user-added custom categories (from Settings) are appended at the end.
        val catalogKeys = _catalog.keys.toList()
        val savedStr = prefs.getString("categories_ordered", null)
        val userExtras = if (!savedStr.isNullOrBlank()) {
            val saved = savedStr.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            // Find categories the user added that are NOT in _catalog
            saved.filter { s -> catalogKeys.none { s.contains(it) || it.contains(s) } }
        } else emptyList()
        _categories.value = catalogKeys + userExtras
        prefs.edit().putString("categories_ordered", _categories.value!!.joinToString(",")).apply()

        val caps = mutableMapOf<String, String>()
        fridges.forEach { name ->
            caps[name] = prefs.getString("cap_$name", "冷藏+冷冻") ?: "冷藏+冷冻"
        }
        _fridgeCapabilities.value = caps

        _syncEnabled.value = prefs.getBoolean("sync_enabled", false)
        syncServerUrl = prefs.getString("sync_server_url", "http://192.168.31.94:3000/coolbox") ?: "http://192.168.31.94:3000/coolbox"
    }

    private val _currentFridge = MutableLiveData<String?>()
    private val appVersion: LiveData<String> = MutableLiveData("v1.2.16")
    val currentFridge: LiveData<String?> = _currentFridge

    fun refreshState() {
        loadSettings()
        _isSetupComplete.value = prefs.getBoolean("setup_complete", false)
        _currentFridge.value = _fridges.value?.firstOrNull() ?: ""
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
                // Use a generic key or pass category down to track usage
                trackStorageUsage(category = name, fridgeName = fridge) 
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
                    portions = entity.portions - amount,
                    quantity = entity.quantity - (entity.weightPerPortion * amount)
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
                    quantity = entity.quantity - weightToMove,
                    portions = entity.portions - p
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
    }
}
