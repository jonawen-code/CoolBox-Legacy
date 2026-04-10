// Version: V3.0.0-Pre22
package com.example.coolbox.ui

import android.app.Application
import androidx.lifecycle.*
import com.example.coolbox.CoolBoxApplication
import com.example.coolbox.InventoryItem
import com.example.coolbox.data.AppDatabase
import com.example.coolbox.data.FoodEntity
import com.example.coolbox.data.FoodRepository
import com.example.coolbox.data.FridgeSettings
import kotlinx.coroutines.launch
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.UUID
import com.example.coolbox.util.EquipmentManager
import com.example.coolbox.util.IconManager
import com.example.coolbox.util.InventoryIntelligence

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: FoodRepository = (application as CoolBoxApplication).repository
    
    val allFood: LiveData<List<FoodEntity>> = repository.allItems
    
    private val gson = Gson()

    // Simplified SharedPreferences management for legacy
    private val prefs = application.getSharedPreferences("coolbox_prefs", Application.MODE_PRIVATE)

    private val _deviceZoneMap = MutableLiveData<Map<String, List<String>>>()
    val deviceZoneMap: LiveData<Map<String, List<String>>> = _deviceZoneMap

    private val _fridgeSettings = MutableLiveData<List<FridgeSettings>>(emptyList())
    val fridgeSettings: LiveData<List<FridgeSettings>> = _fridgeSettings

    private val _fridges = MutableLiveData<List<String>>(emptyList())
    val fridges: LiveData<List<String>> = _fridges

    private val _fridgeBases = MutableLiveData<List<String>>(emptyList())
    val fridgeBases: LiveData<List<String>> = _fridgeBases

    private val _currentSpace = MutableLiveData<Int>(0) // 0: Food, 1: Medicine
    val currentSpace: LiveData<Int> = _currentSpace

    val filteredFridges: LiveData<List<String>> = MediatorLiveData<List<String>>().apply {
        val observer = Observer<Any?> {
            val space = _currentSpace.value ?: 0
            val settings = _fridgeSettings.value ?: emptyList()
            // V4.0.8 Refined: ONLY return base names registered in settings
            value = settings.filter { it.spaceAffinity == 2 || it.spaceAffinity == space }
                    .map { com.example.coolbox.util.EquipmentManager.extractBase(it.fridgeName) }
                    .distinct()
                    .sortedNaturally()
        }
        addSource(_currentSpace, observer)
        addSource(_fridgeSettings, observer)
    }

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

    private val _syncEnabled = MutableLiveData<Boolean>(false)
    val syncEnabled: LiveData<Boolean> = _syncEnabled

    private val _syncUrl = MutableLiveData<String>("http://192.168.31.94:3001/coolbox")
    val syncUrl: LiveData<String> = _syncUrl

    private val _categories = MutableLiveData<List<String>>()
    val categories: LiveData<List<String>> = _categories


    fun setSpace(space: Int) {
        _currentSpace.value = space
        loadSettings() // Reload categories for the new space
    }





    fun getIconForItem(name: String): String {
        return IconManager.getIconForItem(name, _currentSpace.value ?: 0)
    }

    fun getMedicineRecommendation(name: String): Pair<String, String> {
        return InventoryIntelligence.getMedicineRecommendation(name)
    }
    // Version: V2.8.0-RC3 (Build 35)
    fun getRecommendedFridge(category: String, itemName: String): String {
        return InventoryIntelligence.getRecommendedLocation(
            category, itemName, _fridges.value ?: emptyList(), _deviceZoneMap.value ?: emptyMap(), _fridgeCapabilities.value ?: emptyMap()
        )
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

    fun normalizeToDigit(s: String): String {
        return EquipmentManager.normalizeToDigit(s)
    }

    fun extractDeviceBase(fullName: String): String {
        return EquipmentManager.extractBase(fullName)
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



    fun getCatalogCategories(): List<String> = _categories.value ?: (if (currentSpace.value == 1) InventoryIntelligence.MEDICINE_CATALOG.keys.toList() else InventoryIntelligence.FOOD_CATALOG.keys.toList())
    fun getCatalogItems(category: String): List<String> {
        val catalog = if (currentSpace.value == 1) InventoryIntelligence.MEDICINE_CATALOG else InventoryIntelligence.FOOD_CATALOG
        val baseItems = catalog[category] ?: catalog.entries.find { it.key.contains(category) || category.contains(it.key) }?.value ?: emptyList()
        val customItems = prefs.getStringSet("custom_items_$category", emptySet())?.toList() ?: emptyList()
        return (baseItems + customItems).distinct()
    }
    fun getCatalogData(): Map<String, List<String>> {
        val currentKeys = getCatalogCategories()
        return currentKeys.associateWith { key -> getCatalogItems(key) }
    }
    
    fun getDefaultUnit(category: String, item: String): String {
        if (currentSpace.value == 1) {
            if (item.contains("贴")) return "张"
            if (item.contains("酒精") || item.contains("碘伏")) return "瓶"
            return InventoryIntelligence.MEDICINE_UNIT_MAP[category] ?: "盒"
        }
        if (item.contains("鸡蛋")) return "个"
        if (item.contains("牛奶") || item.contains("可乐") || item.contains("果汁") || item.contains("啤酒")) return "瓶"
        if (category.contains("饮料") || category.contains("奶")) return "瓶"
        if (category.contains("肉")) return "kg"
        if (category.contains("蔬菜")) return "斤"
        return InventoryIntelligence.UNIT_MAP[category] ?: "个"
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
        val url = prefs.getString("sync_server_url", "http://192.168.31.94:3001/coolbox") ?: "http://192.168.31.94:3001/coolbox"
        _syncUrl.value = url
        
        // --- 兼容性读取 Fridges 开始 ---
        val fridgesList = try {
            // 1. 尝试按 JSON 字符串读取
            val json = prefs.getString("fridges", null)
            if (json != null) {
                gson.fromJson<List<String>>(json, object : TypeToken<List<String>>() {}.type)
            } else {
                // 2. 如果没有 JSON，尝试按旧版 StringSet 读取
                prefs.getStringSet("fridges", emptySet())?.toList()
            }
        } catch (e: Exception) {
            // 3. 如果类型冲突报错，强制按 StringSet 再次尝试
            prefs.getStringSet("fridges", emptySet())?.toList()
        } ?: emptyList()
        
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
        } ?: emptyList()

        val normalizedBases = basesList.map { normalizeToDigit(it) }.sortedNaturally()
        _fridgeBases.value = normalizedBases
        // --- 兼容性读取 Fridge Bases 结束 ---

        // Write back normalized names in JSON format to standardize for Pre17
        prefs.edit().putString("fridges", gson.toJson(normalizedFridges)).apply()
        prefs.edit().putString("fridge_bases", gson.toJson(normalizedBases)).apply()

        // Category order: ALWAYS use catalogs as the single source of truth.
        val catalogKeys = if (currentSpace.value == 1) 
            InventoryIntelligence.MEDICINE_CATALOG.keys.toList() 
        else 
            InventoryIntelligence.FOOD_CATALOG.keys.toList()
            
        val savedStr = prefs.getString(if (currentSpace.value == 1) "categories_ordered_med" else "categories_ordered", null)
        val userExtras = if (!savedStr.isNullOrBlank()) {
            val saved = savedStr.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            saved.filter { s -> catalogKeys.none { s.contains(it) || it.contains(s) } }
        } else emptyList()
        _categories.value = catalogKeys + userExtras
        prefs.edit().putString(if (currentSpace.value == 1) "categories_ordered_med" else "categories_ordered", _categories.value!!.joinToString(",")).apply()

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
            // 1. Gather all possible full names (Settings, Database, and Legacy Prefs)
            val settingsNames = _fridgeSettings.value?.map { it.fridgeName } ?: emptyList()
            val allFullNames = allFood.value?.map { it.fridgeName }?.distinct()?.toMutableList() ?: mutableListOf()
            _fridges.value?.let { allFullNames.addAll(it) }
            allFullNames.addAll(settingsNames)
            
            // 2. Extract all unique bases
            val uniqueFullNames = allFullNames.distinct()
            val bases = uniqueFullNames.map { EquipmentManager.extractBase(it) }.distinct().filter { it.isNotBlank() }

            val mapping = LinkedHashMap<String, List<String>>()
            bases.forEach { base ->
                val zones = mutableListOf<String>()
                
                // A. Add existing zones from all known names
                val existingZones = uniqueFullNames.filter { EquipmentManager.extractBase(it) == base }
                    .map { EquipmentManager.extractZone(it, base) }
                    .filter { it.isNotEmpty() && it != base && it != "默认层" }
                    .distinct()
                zones.addAll(existingZones)

                // B. Force-inject required zones for specialized equipment (Standard Templates v4.1.6)
                val standard = EquipmentManager.getStandardZones(base)
                standard.forEach { if (!zones.contains(it)) zones.add(it) }
                
                // C. Fallback for others
                if (zones.isEmpty()) {
                    zones.add("默认层")
                }

                mapping[base] = zones.distinct().sortedWith { s1, s2 -> 
                    EquipmentManager.normalizeToDigit(s1).compareTo(EquipmentManager.normalizeToDigit(s2)) 
                }
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
    private val appVersion: LiveData<String> = MutableLiveData("V3.0.0-Pre22")
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

    fun updateSyncSettings(enabled: Boolean, url: String) {
        prefs.edit()
            .putBoolean("sync_enabled", enabled)
            .putString("sync_server_url", url)
            .apply()
        _syncEnabled.value = enabled
        _syncUrl.value = url
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
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val fridge = targetFridge ?: _currentFridge.value ?: ""
            val weightPerPortion = if (portions > 0) {
                (java.math.BigDecimal(quantity.toString()).divide(java.math.BigDecimal(portions.toString()), 4, java.math.RoundingMode.HALF_UP)).toDouble()
            } else quantity
            
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
                val totalQty = existing.quantity + quantity
                val totalPortions = existing.portions + portions
                val updated = existing.copy(
                    quantity = totalQty,
                    portions = totalPortions,
                    weightPerPortion = if (totalPortions > 0) {
                        (java.math.BigDecimal(totalQty.toString()).divide(java.math.BigDecimal(totalPortions.toString()), 4, java.math.RoundingMode.HALF_UP)).toDouble()
                    } else totalQty
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
                    lastModifiedMs = nowMs(),
                    itemType = currentSpace.value ?: 0
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
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
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
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
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
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val updated = entity.copy(icon = newIcon)
            repository.insertItem(updated)
            AppDatabase.exportDatabase(getApplication())
        }
    }

    fun updateItem(entity: FoodEntity) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            repository.insertItem(entity)
            saveCustomItemToCatalog(entity.category, entity.name) // Build 37: Remember user-added items
            AppDatabase.exportDatabase(getApplication())
        }
    }

    fun migrateCategory(oldCategory: String, newCategory: String) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            repository.migrateCategory(oldCategory, newCategory)
            AppDatabase.exportDatabase(getApplication())
        }
    }

    fun deleteFood(entity: FoodEntity) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val dao = AppDatabase.getDatabase(getApplication()).foodDao()
            dao.softDelete(entity.id, nowMs())
            AppDatabase.exportDatabase(getApplication())
        }
    }


    fun updateFridgeSpaceAffinity(name: String, affinity: Int) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val db = AppDatabase.getDatabase(getApplication())
            db.fridgeSettingsDao().insert(FridgeSettings(name, affinity))
        }
    }

    fun addFridgeDevice(name: String, affinity: Int = 2) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val db = AppDatabase.getDatabase(getApplication())
            val baseName = extractDeviceBase(name)
            db.fridgeSettingsDao().insert(FridgeSettings(baseName, affinity))
            // Also add to the legacy bases list to keep UI consistent in other places
            val currentBases = _fridgeBases.value?.toMutableList() ?: mutableListOf()
            if (!currentBases.contains(baseName)) {
                currentBases.add(baseName)
                _fridgeBases.postValue(currentBases)
                prefs.edit().putString("fridge_bases", gson.toJson(currentBases)).apply()
            }
        }
    }

    fun deleteFridgeDevice(name: String) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val db = AppDatabase.getDatabase(getApplication())
            db.fridgeSettingsDao().deleteByName(name)
            // Also remove from legacy bases
            val currentBases = _fridgeBases.value?.toMutableList() ?: mutableListOf()
            if (currentBases.contains(name)) {
                currentBases.remove(name)
                _fridgeBases.postValue(currentBases)
                prefs.edit().putString("fridge_bases", gson.toJson(currentBases)).apply()
            }
        }
    }

    fun renameFridgeDevice(oldName: String, newName: String) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            if (oldName == newName || newName.isBlank()) return@launch
            
            val db = AppDatabase.getDatabase(getApplication())
            val dao = db.fridgeSettingsDao()
            val foodDao = db.foodDao()
            
            // 1. Update fridge_settings (PrimaryKey update requires Delete/Insert)
            val oldSettings = dao.getByName(oldName) ?: return@launch
            dao.deleteByName(oldName)
            dao.insert(oldSettings.copy(fridgeName = newName))
            
            // 2. Translocate all food items that share this base equipment
            val allItems = allFood.value ?: emptyList()
            allItems.forEach { item ->
                if (extractDeviceBase(item.fridgeName) == oldName) {
                    val suffix = item.fridgeName.substring(oldName.length)
                    val newFullName = newName + suffix
                    repository.insertItem(item.copy(fridgeName = newFullName))
                }
            }
            
            // 3. Update legacy bases list
            val currentBases = _fridgeBases.value?.toMutableList() ?: mutableListOf()
            if (currentBases.contains(oldName)) {
                currentBases.remove(oldName)
                if (!currentBases.contains(newName)) currentBases.add(newName)
                _fridgeBases.postValue(currentBases)
                prefs.edit().putString("fridge_bases", gson.toJson(currentBases)).apply()
            }
            
            AppDatabase.exportDatabase(getApplication())
        }
    }

    private fun normalizeFridgeSettings(current: List<FridgeSettings>) {
        if (current.isEmpty()) return
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val db = AppDatabase.getDatabase(getApplication())
            val dao = db.fridgeSettingsDao()

            val alreadyBase = mutableSetOf<String>()
            val toDelete = mutableListOf<String>()
            val toInsert = mutableListOf<FridgeSettings>()

            // Pass 1: Identify all existing bases
            current.forEach { if (extractDeviceBase(it.fridgeName) == it.fridgeName) alreadyBase.add(it.fridgeName) }

            // Pass 2: Identify non-bases to delete and new bases to insert
            current.forEach { s ->
                val base = extractDeviceBase(s.fridgeName)
                if (base != s.fridgeName) {
                    toDelete.add(s.fridgeName)
                    if (!alreadyBase.contains(base)) {
                        toInsert.add(FridgeSettings(base, s.spaceAffinity))
                        alreadyBase.add(base)
                    }
                }
            }

            if (toDelete.isNotEmpty()) {
                toDelete.forEach { dao.deleteByName(it) }
            }
            if (toInsert.isNotEmpty()) {
                toInsert.forEach { dao.insert(it) }
            }
        }
    }


    init {
        loadSettings()
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            repository.migrateEmptyIcons("ic_food_default", System.currentTimeMillis())
            AppDatabase.exportDatabase(getApplication())
            
            // Load fridge settings
            val db = AppDatabase.getDatabase(getApplication())
            val dao = db.fridgeSettingsDao()
            
            dao.getAll().collect { settings ->
                _fridgeSettings.postValue(settings)
                
                if (settings.isEmpty()) {
                    // Build 4.0.8 Migration: If DB is empty, migrate from legacy SharedPreferences
                    val fridgesList = _fridges.value ?: emptyList()
                    if (fridgesList.isNotEmpty()) {
                        fridgesList.forEach { fullName ->
                            val base = extractDeviceBase(fullName)
                            dao.insert(FridgeSettings(base, 2)) // 2 = Food & Medicine
                        }
                    }
                } else {
                    // Trigger normalization once after first load
                    normalizeFridgeSettings(settings)
                    refreshDynamicData()
                }
            }
        }
    }
}
// Version: V4.0.8 (RC3 Refactor)
