// Version: V2.5
package com.example.coolbox

import android.os.Bundle
import android.content.Intent
import com.example.coolbox.legacy.R
import com.example.coolbox.legacy.BuildConfig
import com.example.coolbox.util.formatQuantity
import android.widget.Button
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.coolbox.ui.MainViewModel
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.example.coolbox.data.FoodEntity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.EditText
import android.widget.Spinner
import android.widget.DatePicker
import android.widget.ImageView
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var viewModel: MainViewModel
    private lateinit var adapter: FoodAdapter
    private var searchQuery: String = ""
    private var sortType: String = "EXPIRY" // NAME, EXPIRY, CATEGORY
    private var isAscending: Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        viewModel = ViewModelProvider(this)[MainViewModel::class.java]

        // Check Setup
        if (viewModel.isSetupComplete.value ?: false == false) {
            startActivity(Intent(this, SetupActivity::class.java))
            finish()
            return
        }
        
        findViewById<TextView>(R.id.txtAppVersion).text = "V2.8.0-RC3 (Build 46)"
        findViewById<View>(R.id.btnSettings).setOnClickListener {
            startActivity(Intent(this, SetupActivity::class.java))
        }
        
        findViewById<View>(R.id.btnRefresh)?.setOnClickListener {
            android.widget.Toast.makeText(this, "正在刷新...", android.widget.Toast.LENGTH_SHORT).show()
            com.example.coolbox.util.CloudSyncManager.downloadDatabase(this, viewModel.syncServerUrl) { success ->
                if (success) {
                    // Centralized VM-driven state reset for V1.0 compliance
                    viewModel.onSyncComplete()
                    android.widget.Toast.makeText(this@MainActivity, "同步完成 (已跳转至汇总)", android.widget.Toast.LENGTH_SHORT).show()
                } else {
                    android.widget.Toast.makeText(this@MainActivity, "网络故障，请检查 NAS 设置", android.widget.Toast.LENGTH_LONG).show()
                }
            }
        }
        
        // Search & Sort Initialization
        val searchEdit = findViewById<EditText>(R.id.searchEditText)
        searchEdit.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                searchQuery = s?.toString() ?: ""
                updateList(viewModel.allFood.value, viewModel.currentFridge.value)
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })

        findViewById<View>(R.id.btnSort).setOnClickListener { view ->
            val popup = androidx.appcompat.widget.PopupMenu(this, view)
            popup.menu.add("按保质期排序 ${if (sortType == "EXPIRY") "✅" else ""}").setOnMenuItemClickListener { sortType = "EXPIRY"; updateList(viewModel.allFood.value, viewModel.currentFridge.value); true }
            popup.menu.add("按名称排序 ${if (sortType == "NAME") "✅" else ""}").setOnMenuItemClickListener { sortType = "NAME"; updateList(viewModel.allFood.value, viewModel.currentFridge.value); true }
            popup.menu.add("按分类排序 ${if (sortType == "CATEGORY") "✅" else ""}").setOnMenuItemClickListener { sortType = "CATEGORY"; updateList(viewModel.allFood.value, viewModel.currentFridge.value); true }
            popup.menu.add("----------------")
            popup.menu.add("切换升序/降序 (${if (isAscending) "当前: 升序" else "当前: 降序"})").setOnMenuItemClickListener { isAscending = !isAscending; updateList(viewModel.allFood.value, viewModel.currentFridge.value); true }
            popup.show()
        }

        val fridgeTabsContainer = findViewById<LinearLayout>(R.id.fridgeTabs)
        val btnSummary = findViewById<Button>(R.id.btnSummary)
        val recyclerView = findViewById<RecyclerView>(R.id.foodList)
        val fab = findViewById<FloatingActionButton>(R.id.addFoodFab)

        btnSummary.setOnClickListener { viewModel.setFridge(null) } // null means Summary

        viewModel.isSetupComplete.observe(this) { completed ->
            if (!completed) {
                startActivity(Intent(this, SetupActivity::class.java))
                finish()
            }
        }

        findViewById<View>(R.id.btnScaleDown).setOnClickListener {
            viewModel.setFontScale((viewModel.fontScale.value ?: 1.0f) - 0.1f)
        }
        findViewById<View>(R.id.btnScaleUp).setOnClickListener {
            viewModel.setFontScale((viewModel.fontScale.value ?: 1.0f) + 0.1f)
        }        // Quick Entry Buttons - Dynamically generated from settings categories
        val quickEntryBar = findViewById<LinearLayout>(R.id.quickEntryBar)
        fun buildQuickButtons() {
            quickEntryBar.removeAllViews()
            val categories = viewModel.getCatalogCategories()
            val blueLight = resources.getColor(R.color.blue_light)
            val blueDark = resources.getColor(R.color.blue_dark)
            categories.forEach { categoryName ->
                val btn = Button(this).apply {
                    text = "+$categoryName"
                    textSize = 15f
                    typeface = android.graphics.Typeface.DEFAULT_BOLD
                    setBackgroundColor(blueLight)
                    setTextColor(blueDark)
                    minHeight = 0
                    minWidth = 0
                    layoutParams = LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        (56 * resources.displayMetrics.density).toInt()
                    )
                    setOnClickListener { showAddOrEditFoodDialog(categoryToSelect = categoryName) }
                }
                quickEntryBar.addView(btn)
            }
        }
        buildQuickButtons()
        viewModel.categories.observe(this) { buildQuickButtons() }

        adapter = FoodAdapter(
            currentFridge = { viewModel.currentFridge.value },
            getCatalogItems = { viewModel.getCatalogData() },
            onAction = { item -> showItemActionDialog(item) },
            onDelete = { item -> viewModel.deleteFood(item) },
            onTakeOne = { item -> viewModel.takePortion(item) },
            onTakeAll = { item -> 
                androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("确认取走全部？")
                    .setMessage("确定要取走 ${item.name} 及其所有份数吗？")
                    .setPositiveButton("确定") { _, _ -> viewModel.deleteFood(item) }
                    .setNegativeButton("取消", null)
                    .show()
            },
            getFontScale = { viewModel.fontScale.value ?: 1.0f },
            getNowMs = { viewModel.nowMs() },
            getFridgeBases = { viewModel.fridgeBases.value ?: emptyList() }
        )
        
        viewModel.fontScale.observe(this) { scale ->
            adapter.notifyDataSetChanged()
            updateTabHighlights(fridgeTabsContainer, viewModel.currentFridge.value)
            findViewById<TextView>(R.id.txtScaleInfo).text = "显示比例：${Math.round(scale * 100)}%"
        }
        
        // Embedded Time Machine Observers
        viewModel.timeOffsetMs.observe(this) { offset ->
            adapter.notifyDataSetChanged()
            val df = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            findViewById<TextView>(R.id.txtCurrentSimTime).text = "🕒 模拟时间: ${df.format(Date(viewModel.nowMs()))}"
        }

        // Dashboard Time Controls
        findViewById<View>(R.id.btnTimePlus1D).setOnClickListener { 
            viewModel.addTimeOffset(24 * 60 * 60 * 1000L)
        }
        findViewById<View>(R.id.btnTimePlus1W).setOnClickListener { 
            viewModel.addTimeOffset(7 * 24 * 60 * 60 * 1000L)
        }
        findViewById<View>(R.id.btnTimePlus1M).setOnClickListener { 
            viewModel.addTimeOffset(30 * 24 * 60 * 60 * 1000L)
        }
        findViewById<View>(R.id.btnTimeReset).setOnClickListener { 
            viewModel.resetTimeOffset()
        }
        
        // Adaptive Grid Layout: 3 columns in portrait, 5 columns in landscape/tablet
        val isLandscape = resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
        val spanCount = if (isLandscape) 5 else 3
        recyclerView.layoutManager = androidx.recyclerview.widget.GridLayoutManager(this, spanCount)
        recyclerView.adapter = adapter

        // Observer for current fridge selection to update tab highlights
        viewModel.currentFridge.observe(this) { currentFridge ->
            updateTabHighlights(fridgeTabsContainer, currentFridge)
            updateList(viewModel.allFood.value, currentFridge)
        }

        btnSummary.setOnClickListener { viewModel.setFridge(null) } // null means Summary

        // Observe Fridge Bases to create tabs (Unique devices)
        viewModel.fridgeBases.observe(this) { bases ->
            // Safely remove all dynamic tabs (all children after btnSummary)
            val summaryIndex = fridgeTabsContainer.indexOfChild(btnSummary)
            if (summaryIndex >= 0) {
                while (fridgeTabsContainer.childCount > summaryIndex + 1) {
                    fridgeTabsContainer.removeViewAt(summaryIndex + 1)
                }
            } else {
                // If btnSummary is lost (unlikely), clear all and re-add
                fridgeTabsContainer.removeAllViews()
                fridgeTabsContainer.addView(btnSummary)
            }
            
            bases.forEach { fridgeName ->
                val btn = Button(this).apply {
                    text = fridgeName
                    textSize = 18f
                    typeface = android.graphics.Typeface.DEFAULT_BOLD
                    minHeight = 0
                    minWidth = (110 * resources.displayMetrics.density).toInt()
                    layoutParams = LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        (62 * resources.displayMetrics.density).toInt()
                    )
                    setOnClickListener { viewModel.setFridge(fridgeName) }
                }
                fridgeTabsContainer.addView(btn)
            }
            updateTabHighlights(fridgeTabsContainer, viewModel.currentFridge.value)
        }

        viewModel.allFood.observe(this) { food ->
            updateList(food, viewModel.currentFridge.value)
        }

        fab.setOnClickListener {
            showAddOrEditFoodDialog()
        }

        // Show Expiry Status Report on launch (simple way for legacy)
        viewModel.allFood.observe(this, object : Observer<List<FoodEntity>> {
            private var shown = false
            override fun onChanged(food: List<FoodEntity>?) {
                if (!shown && food != null) {
                    showExpiryStatusReport(food)
                    shown = true
                }
            }
        })
    }

    // Version: V2.8.0-RC3 (Build 33)
    private fun applySmartLinkage(category: String, item: String, deviceSpinner: Spinner, units: List<String>, unitSpinner: Spinner, allDevices: List<String>) {
        if (allDevices.isEmpty()) return

        val rawRec = viewModel.getRecommendedFridge(category, item)
        if (rawRec.isNotEmpty()) {
            val recDigit = viewModel.normalizeToDigit(rawRec)
            // Build 33: Brute-force prefix match against device list
            val matchedDeviceIdx = allDevices.indexOfFirst { device ->
                recDigit.startsWith(viewModel.normalizeToDigit(device))
            }

            if (matchedDeviceIdx >= 0) {
                val matchedDeviceName = allDevices[matchedDeviceIdx]
                // Strip device name to get zone portion
                val zoneStr = recDigit.removePrefix(viewModel.normalizeToDigit(matchedDeviceName)).replace("-", "").trim()

                // Build 40: Move tag assignment inside post block to prevent race condition coverage
                deviceSpinner.post {
                    deviceSpinner.tag = if (zoneStr.isNotEmpty()) zoneStr else null
                    if (deviceSpinner.selectedItemPosition == matchedDeviceIdx) {
                        // Force zone refresh even when device hasn't changed
                        deviceSpinner.onItemSelectedListener?.onItemSelected(deviceSpinner, null, matchedDeviceIdx, 0)
                    } else {
                        deviceSpinner.setSelection(matchedDeviceIdx)
                    }
                }
            }
        }

        val defaultUnit = viewModel.getDefaultUnit(category, item)
        val unitIdx = units.indexOf(defaultUnit)
        if (unitIdx in units.indices) {
            unitSpinner.post { unitSpinner.setSelection(unitIdx) }
        }
    }
    // Version: V2.8.0-RC3 (Build 33)

    private fun showAddOrEditFoodDialog(existingItem: FoodEntity? = null, categoryToSelect: String? = null) {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_add_food, null)
        val deviceSpinner = view.findViewById<Spinner>(R.id.deviceSpinner)
        val zoneSpinner = view.findViewById<Spinner>(R.id.zoneSpinner)
        val textDirtyLocation = view.findViewById<TextView>(R.id.textDirtyLocation)
        val categorySpinner = view.findViewById<Spinner>(R.id.categorySpinner)
        val itemSpinner = view.findViewById<Spinner>(R.id.itemSpinner)
        val editNewItemName = view.findViewById<EditText>(R.id.editNewItemName)
        val layoutNewItemName = view.findViewById<LinearLayout>(R.id.layoutNewItemName)
        val editQuantity = view.findViewById<EditText>(R.id.editQuantity)
        val editPortions = view.findViewById<EditText>(R.id.editPortions)
        val unitSpinner = view.findViewById<Spinner>(R.id.unitSpinner)
        val datePicker = view.findViewById<DatePicker>(R.id.datePicker)

        val units = listOf("个", "kg", "斤", "袋", "盒", "瓶", "升")

        // Setup Device & Zone Spinners (V2.2 Dynamic Mapping)
        val deviceZoneMap = viewModel.deviceZoneMap.value ?: emptyMap()
        val allDevices = deviceZoneMap.keys.toList().ifEmpty { listOf("我的冰箱", "小冰柜") }
        val deviceAdapter = android.widget.ArrayAdapter(this, R.layout.spinner_item_dark, allDevices)
        deviceAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item_dark)
        deviceSpinner.adapter = deviceAdapter

        val zoneAdapter = android.widget.ArrayAdapter(this, R.layout.spinner_item_dark, mutableListOf<String>())
        zoneAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item_dark)
        zoneSpinner.adapter = zoneAdapter

        var isDirty = false
        var originalDirtyLocation = ""

        if (existingItem != null) {
            val loc = existingItem.fridgeName.trim()
            val parts = loc.split(" - ")
            if (parts.size == 2 && allDevices.contains(parts[0]) && deviceZoneMap[parts[0]]?.contains(parts[1]) == true) {
                val dIdx = allDevices.indexOf(parts[0])
                if (dIdx >= 0) deviceSpinner.setSelection(dIdx)
                // Zone selection will happen in onItemSelected of device
            } else {
                isDirty = true
                originalDirtyLocation = loc
                textDirtyLocation.visibility = View.VISIBLE
                textDirtyLocation.text = "⚠️ 历史位置: $loc (旧配置已更迭，请确认或重选)"
                // Set device to index -1 or 0 and wait for user
            }
        } else {
            val currentFridgeIdx = allDevices.indexOf(viewModel.currentFridge.value)
            if (currentFridgeIdx >= 0) deviceSpinner.setSelection(currentFridgeIdx)
        }
        deviceSpinner.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
    override fun onItemSelected(parent: android.widget.AdapterView<*>?, v: View?, position: Int, id: Long) {
        val selectedDevice = allDevices[position]
        val zones = deviceZoneMap[selectedDevice] ?: emptyList()

        // 核心防御：判断是不是 Android 系统的“幽灵空跑初始化”
        val isGhostEvent = zoneAdapter.count == zones.size && (0 until zones.size).all { zoneAdapter.getItem(it) == zones[it] }

        if (!isGhostEvent) {
            zoneAdapter.clear()
            zoneAdapter.addAll(zones)
            zoneAdapter.notifyDataSetChanged()
        }

        val pendingZone = deviceSpinner.tag as? String

        if (pendingZone != null) {
            val zIdx = zones.indexOfFirst { it.contains(pendingZone) || pendingZone.contains(it) }
            if (zIdx >= 0) {
                zoneSpinner.setSelection(zIdx)
            } else if (!isGhostEvent) {
                zoneSpinner.setSelection(0)
            }
            deviceSpinner.tag = null
        } else {
            // 🛑 如果是幽灵事件且没有 Tag，绝对不准重置为 0！保持刚算出的冷藏！
            if (!isGhostEvent) {
                zoneSpinner.setSelection(0)
            }
        }
        
        if (isDirty) { isDirty = false; textDirtyLocation.visibility = View.GONE }
    }
    override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
}

        val layoutRemark = view.findViewById<LinearLayout>(R.id.layoutRemark)
        val editRemark = view.findViewById<EditText>(R.id.editRemark)

        // Setup Category Spinner
        val categories = viewModel.getCatalogCategories()
        val categoryAdapter = android.widget.ArrayAdapter(this, R.layout.spinner_item_dark, categories)
        categoryAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item_dark)
        categorySpinner.adapter = categoryAdapter
        
        if (existingItem != null) {
            val catIdx = categories.indexOf(existingItem.category)
            if (catIdx >= 0) categorySpinner.setSelection(catIdx)
        } else {
            categoryToSelect?.let { cat ->
                val idx = categories.indexOf(cat).let { i -> if (i >= 0) i else categories.indexOfFirst { c -> c.contains(cat) || cat.contains(c) } }
                if (idx >= 0) categorySpinner.setSelection(idx)
            }
        }

        // Setup Item Spinner (Dynamic)
        categorySpinner.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedCategory = categories[position]
                val items = viewModel.getCatalogItems(selectedCategory).toMutableList()
                items.add("新增...")
                
                val itemAdapter = android.widget.ArrayAdapter(this@MainActivity, R.layout.spinner_item_dark, items)
                itemAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item_dark)
                itemSpinner.adapter = itemAdapter

                if (existingItem != null) {
                    val itemIdx = items.indexOf(existingItem.name)
                    if (itemIdx >= 0) itemSpinner.setSelection(itemIdx)
                }
                
                itemSpinner.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(p: android.widget.AdapterView<*>?, v: View?, pos: Int, i: Long) {
                        val selectedItem = items[pos]
                        if (selectedItem == "新增...") {
                            layoutNewItemName.visibility = View.VISIBLE
                            editNewItemName.requestFocus()
                        } else {
                            layoutNewItemName.visibility = View.GONE
                            applySmartLinkage(selectedCategory, selectedItem, deviceSpinner, units, unitSpinner, allDevices)
                        }
                    }
                    override fun onNothingSelected(p: android.widget.AdapterView<*>?) {}
                }

                // Initial linkage trigger for the first item in the category list
                // Build 28: HARDENED LINKAGE - Categorical selection immediately fires device/unit recommendation
                if (items.isNotEmpty()) {
                    applySmartLinkage(selectedCategory, items[0], deviceSpinner, units, unitSpinner, allDevices)
                }
                
                // Always show remark field for all categories
                layoutRemark.visibility = View.VISIBLE

                // Smart recommendation and units moved to item selection to avoid redundant/overwriting calls
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }

        // Setup Unit Spinner
        val unitAdapter = android.widget.ArrayAdapter(this, R.layout.spinner_item_dark, units)
        unitAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item_dark)
        unitSpinner.adapter = unitAdapter
        
        if (existingItem != null) {
            val uIdx = units.indexOf(existingItem.unit)
            if (uIdx >= 0) unitSpinner.setSelection(uIdx)
            editQuantity.setText(formatQuantity(existingItem.quantity))
            editPortions.setText(existingItem.portions.toString())
            editRemark.setText(existingItem.remark)
            
            // Set DatePicker
            val cal = Calendar.getInstance()
            cal.timeInMillis = existingItem.expiryDateMs
            datePicker.updateDate(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH))
        }

        val textExpiryValue = view.findViewById<TextView>(R.id.textExpiryValue)
        
        val updateExpiryLabel = {
            val calendar = Calendar.getInstance()
            calendar.set(datePicker.year, datePicker.month, datePicker.dayOfMonth)
            val df = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            textExpiryValue.text = df.format(calendar.time)
        }

        // Initialize DatePicker with listener for manual changes
        datePicker.init(datePicker.year, datePicker.month, datePicker.dayOfMonth) { _, year, month, day ->
            updateExpiryLabel()
        }

        // --- Year & Month Selection Grid ---
        var selectedYear = Calendar.getInstance().apply { timeInMillis = viewModel.nowMs() }.get(Calendar.YEAR)
        var selectedMonth = Calendar.getInstance().apply { timeInMillis = viewModel.nowMs() }.get(Calendar.MONTH)

        val btnThisYear = view.findViewById<Button>(R.id.btnThisYear)
        val btnNextYear = view.findViewById<Button>(R.id.btnNextYear)
        val monthButtons = (1..12).map { i -> 
            val id = resources.getIdentifier("btnMonth$i", "id", packageName)
            view.findViewById<Button>(id)
        }

        val updateCalendarSelection = {
            val now = Calendar.getInstance().apply { timeInMillis = viewModel.nowMs() }
            val currentYear = now.get(Calendar.YEAR)
            
            // Year highlight
            btnThisYear.setTextColor(if (selectedYear == currentYear) android.graphics.Color.WHITE else android.graphics.Color.BLACK)
            btnThisYear.setBackgroundColor(if (selectedYear == currentYear) android.graphics.Color.parseColor("#03A9F4") else android.graphics.Color.WHITE)
            btnNextYear.setTextColor(if (selectedYear == currentYear + 1) android.graphics.Color.WHITE else android.graphics.Color.BLACK)
            btnNextYear.setBackgroundColor(if (selectedYear == currentYear + 1) android.graphics.Color.parseColor("#03A9F4") else android.graphics.Color.WHITE)

            // Month highlight
            monthButtons.forEachIndexed { index, btn ->
                if (index == selectedMonth) {
                    btn.setBackgroundColor(android.graphics.Color.parseColor("#03A9F4"))
                    btn.setTextColor(android.graphics.Color.WHITE)
                } else {
                    btn.setBackgroundColor(android.graphics.Color.parseColor("#EEEEEE"))
                    btn.setTextColor(android.graphics.Color.BLACK)
                }
            }

            // Update DatePicker (internal state)
            val cal = Calendar.getInstance()
            cal.set(selectedYear, selectedMonth, 1)
            val lastDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
            datePicker.updateDate(selectedYear, selectedMonth, Math.min(datePicker.dayOfMonth, lastDay))
            updateExpiryLabel()
        }

        btnThisYear.setOnClickListener { 
            selectedYear = Calendar.getInstance().apply { timeInMillis = viewModel.nowMs() }.get(Calendar.YEAR)
            updateCalendarSelection()
        }
        btnNextYear.setOnClickListener { 
            selectedYear = Calendar.getInstance().apply { timeInMillis = viewModel.nowMs() }.get(Calendar.YEAR) + 1
            updateCalendarSelection()
        }
        monthButtons.forEachIndexed { index, btn ->
            btn.setOnClickListener {
                selectedMonth = index
                updateCalendarSelection()
            }
        }

        view.findViewById<Button>(R.id.btnCustomDate).setOnClickListener {
            datePicker.visibility = if (datePicker.visibility == View.VISIBLE) View.GONE else View.VISIBLE
        }

        // Setup Expiry Preset Buttons (Presets like 3d, 1w)
        val setDatePreset = { days: Int ->
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = viewModel.nowMs()
            calendar.add(Calendar.DAY_OF_YEAR, days)
            selectedYear = calendar.get(Calendar.YEAR)
            selectedMonth = calendar.get(Calendar.MONTH)
            datePicker.updateDate(selectedYear, selectedMonth, calendar.get(Calendar.DAY_OF_MONTH))
            updateCalendarSelection()
            datePicker.visibility = View.GONE
        }
        
        view.findViewById<Button>(R.id.btn3d).setOnClickListener { setDatePreset(3) }
        view.findViewById<Button>(R.id.btn1w).setOnClickListener { setDatePreset(7) }
        view.findViewById<Button>(R.id.btn2w).setOnClickListener { setDatePreset(14) }
        view.findViewById<Button>(R.id.btn1m).setOnClickListener { setDatePreset(30) }
        view.findViewById<Button>(R.id.btn3m).setOnClickListener { setDatePreset(90) }
        view.findViewById<Button>(R.id.btn6m).setOnClickListener { setDatePreset(180) }
        
        // Initial selection sync
        updateCalendarSelection()

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(if (existingItem != null) "编辑食品" else "添加食品")
            .setView(view)
            .setPositiveButton(if (existingItem != null) "确定" else "添加") { _, _ ->
                val selectedInSpinner = itemSpinner.selectedItem?.toString() ?: ""
                val fullItemName = if (selectedInSpinner == "新增...") {
                    editNewItemName.text.toString()
                } else {
                    selectedInSpinner
                }
                
                val selectedDevice = deviceSpinner.selectedItem?.toString() ?: ""
                val selectedZone = zoneSpinner.selectedItem?.toString() ?: ""
                val targetLocation = if (isDirty) originalDirtyLocation else "$selectedDevice - $selectedZone"
                
                val qtyStr = editQuantity.text.toString()
                val qty = if (qtyStr.isEmpty()) 1.0 else (qtyStr.toDoubleOrNull() ?: 1.0)
                val portionsStr = editPortions.text.toString()
                val portions = if (portionsStr.isEmpty()) 1 else (portionsStr.toIntOrNull() ?: 1)
                val unit = unitSpinner.selectedItem.toString()
                
                val calendar = Calendar.getInstance()
                calendar.set(datePicker.year, datePicker.month, datePicker.dayOfMonth)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val expiryMs = calendar.timeInMillis

                if (fullItemName.isNotEmpty()) {
                    val category = categorySpinner.selectedItem.toString()
                    val remark = editRemark.text.toString()
                    
                    if (existingItem != null) {
                        val updated = existingItem.copy(
                            name = fullItemName,
                            icon = if (fullItemName == existingItem.name) existingItem.icon else viewModel.getIconForItem(fullItemName),
                            category = category,
                            quantity = qty,
                            unit = unit,
                            expiryDateMs = expiryMs,
                            portions = portions,
                            fridgeName = targetLocation,
                            remark = remark,
                            lastModifiedMs = viewModel.nowMs()
                        )
                        viewModel.updateItem(updated)
                    } else {
                        val icon = viewModel.getIconForItem(fullItemName)
                        viewModel.addFood(fullItemName, icon, category, qty, unit, expiryMs, portions, targetLocation, remark)
                    }
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    fun showIconPickerDialog(entity: FoodEntity) {
        val iconList = listOf(
            "ic_food_beef", "ic_food_pork", "ic_food_chicken", "ic_food_egg",
            "ic_food_fish", "ic_food_shrimp", "ic_food_crab", "ic_food_ribs",
            "ic_food_apple", "ic_food_tomato", "ic_food_watermelon", "ic_food_grapes",
            "ic_food_strawberry", "ic_food_onion", "ic_food_lettuce", "ic_food_milk",
            "ic_food_yogurt", "ic_food_juice", "ic_food_cola", "ic_food_beer",
            "ic_food_icecream", "ic_food_durian", "ic_food_blueberries", "ic_food_broccoli", "ic_food_butter",
            "ic_food_cheese", "ic_food_dumpling", "ic_food_green_apple", "ic_food_pepper",
            "ic_food_lemon", "ic_food_mango", "ic_food_pear", "ic_food_tangerine",
            "ic_food_shellfish", "ic_food_mussel",
            "cat_cooked", "cat_meat", "cat_veg", "cat_drink", "cat_snack"
        )
        
        val grid = android.widget.GridLayout(this).apply {
            columnCount = 5
            rowCount = (iconList.size + 4) / 5
            setPadding(16, 16, 16, 16)
        }
        
        val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("更换图标")
            .setView(grid)
            .create()

        iconList.forEach { iconName ->
            val img = ImageView(this).apply {
                val resId = resources.getIdentifier(iconName, "drawable", packageName)
                if (resId != 0) setImageResource(resId)
                layoutParams = android.widget.GridLayout.LayoutParams().apply {
                    width = (64 * resources.displayMetrics.density).toInt()
                    height = (64 * resources.displayMetrics.density).toInt()
                    setMargins(4, 4, 4, 4)
                }
                scaleType = ImageView.ScaleType.CENTER_INSIDE
                setOnClickListener {
                    viewModel.updateFoodIcon(entity, iconName)
                    dialog.dismiss()
                }
            }
            grid.addView(img)
        }
        dialog.show()
    }

    private fun showItemActionDialog(entity: FoodEntity) {
        val options = arrayOf("编辑信息", "仅拿走 1 份", "拿走多份...", "转移到其他冰箱")
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(entity.name)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showAddOrEditFoodDialog(entity)
                    1 -> viewModel.takePortion(entity)
                    2 -> showTakePortionDialog(entity)
                    3 -> showTransferDialog(entity)
                }
            }
            .show()
    }

    private fun showTakePortionDialog(entity: FoodEntity) {
        val input = EditText(this)
        input.inputType = android.text.InputType.TYPE_CLASS_NUMBER
        input.hint = "最多可拿走 ${entity.portions} 份"
        
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("取走多少份？")
            .setView(input)
            .setPositiveButton("确定") { _, _ ->
                val p = input.text.toString().toIntOrNull() ?: 0
                if (p >= entity.portions) {
                    viewModel.deleteFood(entity)
                } else if (p > 0) {
                    viewModel.takePortions(entity, p)
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showTransferDialog(entity: FoodEntity) {
        val fridges = (viewModel.fridges.value ?: emptyList()).filter { it != entity.fridgeName }
        if (fridges.isEmpty()) {
            android.widget.Toast.makeText(this, "没有其他冰箱可供转移", android.widget.Toast.LENGTH_SHORT).show()
            return
        }
        
        val fridgArray = fridges.toTypedArray()
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("转移到...")
            .setItems(fridgArray) { _, fridgeIdx ->
                val targetFridge = fridgArray[fridgeIdx]
                
                // If and only if portions > 1, ask how many to transfer
                if (entity.portions > 1) {
                    val transferOptions = arrayOf("全取 (${entity.portions}份)", "转移 1 份", "自定义份数")
                    androidx.appcompat.app.AlertDialog.Builder(this)
                        .setTitle("转移份数")
                        .setItems(transferOptions) { _, optionIdx ->
                            when (optionIdx) {
                                0 -> viewModel.transferItem(entity, targetFridge, entity.portions)
                                1 -> viewModel.transferItem(entity, targetFridge, 1)
                                2 -> {
                                    val input = EditText(this)
                                    input.inputType = android.text.InputType.TYPE_CLASS_NUMBER
                                    input.hint = "最大 ${entity.portions}"
                                    androidx.appcompat.app.AlertDialog.Builder(this)
                                        .setTitle("输入转移份数")
                                        .setView(input)
                                        .setPositiveButton("转移") { _, _ ->
                                            val p = input.text.toString().toIntOrNull() ?: 0
                                            if (p > 0 && p <= entity.portions) {
                                                viewModel.transferItem(entity, targetFridge, p)
                                            }
                                        }
                                        .setNegativeButton("取消", null)
                                        .show()
                                }
                            }
                        }
                        .show()
                } else {
                    viewModel.transferItem(entity, targetFridge, 1)
                }
            }
            .show()
    }

    private fun showExpiryStatusReport(allFood: List<FoodEntity>) {
        val now = System.currentTimeMillis()
        val expired = allFood.filter { it.expiryDateMs < now }
        val expiring = allFood.filter { !expired.contains(it) && it.expiryDateMs < now + 24 * 60 * 60 * 1000L * 3 }

        if (expired.isEmpty() && expiring.isEmpty()) return

        val msg = StringBuilder()
        if (expired.isNotEmpty()) {
            msg.append("⚠️ 已过期 (${expired.size}件):\n")
            expired.take(3).forEach { 
                val cleanIcon = viewModel.getDisplayName(it.icon)
                msg.append("• ${it.name} (${cleanIcon})\n") 
            }
            if (expired.size > 3) msg.append("...等\n")
        }
        if (expiring.isNotEmpty()) {
            if (msg.isNotEmpty()) msg.append("\n")
            msg.append("🕒 即将过期 (${expiring.size}件):\n")
            expiring.take(3).forEach { 
                val cleanIcon = viewModel.getDisplayName(it.icon)
                msg.append("• ${it.name} (${cleanIcon})\n") 
            }
            if (expiring.size > 3) msg.append("...等\n")
        }

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("冰箱状态汇总")
            .setMessage(msg.toString())
            .setPositiveButton("知道了", null)
            .show()
    }

    private fun updateList(allFood: List<FoodEntity>?, currentFridge: String?) {
        if (allFood != null) {
            var filtered = if (currentFridge == null) {
                allFood 
            } else {
                // Robust filter: match exact, prefix, or normalized (ignore spaces)
                val normalizedCurrent = currentFridge.replace(" ", "")
                allFood.filter { 
                    it.fridgeName == currentFridge || 
                    it.fridgeName.startsWith("$currentFridge ") ||
                    it.fridgeName.replace(" ", "") == normalizedCurrent
                }
            }

            // Apply Search Filter
            if (searchQuery.isNotBlank()) {
                filtered = filtered.filter { 
                    it.name.contains(searchQuery, ignoreCase = true) || 
                    it.remark.contains(searchQuery, ignoreCase = true) 
                }
            }

            // Apply Sorting
            val sorted = when (sortType) {
                "NAME" -> if (isAscending) filtered.sortedBy { it.name } else filtered.sortedByDescending { it.name }
                "CATEGORY" -> if (isAscending) filtered.sortedBy { it.category } else filtered.sortedByDescending { it.category }
                else -> if (isAscending) filtered.sortedBy { it.expiryDateMs } else filtered.sortedByDescending { it.expiryDateMs }
            }

            adapter.submitList(sorted)
        }
    }

    private fun showSettingsDialog() {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_settings, null)
        val textScale = view.findViewById<TextView>(R.id.textScaleValue)
        val btnUp = view.findViewById<Button>(R.id.btnScaleUp)
        val btnDown = view.findViewById<Button>(R.id.btnScaleDown)

        val updateText = {
            val scale = viewModel.fontScale.value ?: 1.0f
            textScale.text = "${(scale * 100).toInt()}%"
        }
        updateText()

        btnUp.setOnClickListener {
            val current = viewModel.fontScale.value ?: 1.0f
            if (current < 2.0f) {
                viewModel.setFontScale(current + 0.1f)
                updateText()
            }
        }
        btnDown.setOnClickListener {
            val current = viewModel.fontScale.value ?: 1.0f
            if (current > 0.8f) {
                viewModel.setFontScale(current - 0.1f)
                updateText()
            }
        }

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setView(view)
            .setPositiveButton("完成", null)
            .show()
    }

    private fun showTimeMachineDialog() {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_time_machine, null)
        val textTime = view.findViewById<TextView>(R.id.currentVirtualTime)
        
        val updateTimeText = {
            val df = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            textTime.text = "当前虚拟时间: ${df.format(Date(viewModel.nowMs()))}"
        }
        updateTimeText()

        view.findViewById<Button>(R.id.btnPlus1d).setOnClickListener { viewModel.addTimeOffset(24*60*60*1000L); updateTimeText() }
        view.findViewById<Button>(R.id.btnPlus1w).setOnClickListener { viewModel.addTimeOffset(7*24*60*60*1000L); updateTimeText() }
        view.findViewById<Button>(R.id.btnPlus1m).setOnClickListener { viewModel.addTimeOffset(30); updateTimeText() }
        view.findViewById<Button>(R.id.btnResetTime).setOnClickListener { viewModel.resetTimeOffset(); updateTimeText() }

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setView(view)
            .setPositiveButton("进入未来", null)
            .show()
    }

    private fun updateTabHighlights(container: LinearLayout, activeFridge: String?) {
        val blue = android.graphics.Color.parseColor("#03A9F4")
        val white = android.graphics.Color.WHITE
        val lightGray = android.graphics.Color.parseColor("#FAFAFA")
        val darkGray = android.graphics.Color.parseColor("#333333")

        for (i in 0 until container.childCount) {
            val view = container.getChildAt(i)
            if (view is Button) {
                view.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 16f)

                val isActive = when (view.id) {
                    R.id.btnSummary -> activeFridge == null
                    else -> view.text == activeFridge
                }
                if (isActive) {
                    view.setBackgroundColor(blue)
                    view.setTextColor(white)
                } else {
                    view.setBackgroundColor(lightGray)
                    view.setTextColor(darkGray)
                }
            }
        }
    }

    class FoodAdapter(
        private val currentFridge: () -> String?,
        private val getCatalogItems: () -> Map<String, List<String>>,
        private val onAction: (FoodEntity) -> Unit,
        private val onDelete: (FoodEntity) -> Unit,
        private val onTakeOne: (FoodEntity) -> Unit,
        private val onTakeAll: (FoodEntity) -> Unit,
        private val getFontScale: () -> Float,
        private val getNowMs: () -> Long,
        private val getFridgeBases: () -> List<String>
    ) : RecyclerView.Adapter<FoodAdapter.ViewHolder>() {
        private var items = emptyList<FoodEntity>()

        fun submitList(newItems: List<FoodEntity>) {
            items = newItems
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_food, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            val scale = getFontScale()
            val nowMs = getNowMs()
            
            holder.name.textSize = 20f * scale
            holder.detail.textSize = 14f * scale
            holder.quantity.textSize = 16f * scale
            holder.portions.textSize = 14f * scale
            holder.remark.textSize = 14f * scale
            holder.location.textSize = 14f * scale

            // Build 29: Render clean display name, strip ic_food_ / cat_ prefixes
            holder.name.text = (holder.itemView.context as MainActivity).viewModel.getDisplayName(item.name)
            val df = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val dateStr = "至 ：" + df.format(Date(item.expiryDateMs))
            holder.detail.text = dateStr

            holder.quantity.text = "${formatQuantity(item.quantity)} ${item.unit}"
            holder.portions.text = "${item.portions} 份，${formatQuantity(item.weightPerPortion)}${item.unit}/份"
            
            holder.remark.visibility = View.VISIBLE
            if (item.remark.isNotBlank()) {
                holder.remark.text = "注：${item.remark}"
            } else {
                holder.remark.text = " " // Stable empty line for height consistency
            }
            // Build 44: 保持与底层数据及其他对话框完全一致的显示格式，不遮蔽横杠
            val shortLoc = item.fridgeName.replace("室", "").replace("第", "")
            holder.location.text = "在：$shortLoc"

            applySafeIcon(holder.icon, item)

            holder.icon.setOnClickListener {
                (holder.itemView.context as? MainActivity)?.showIconPickerDialog(item)
            }
            
            val container = holder.itemView.findViewById<View>(R.id.itemContainer)
            
            // Near Expiry Logic
            val totalDuration = item.expiryDateMs - item.inputDateMs
            val currentNowMs = getNowMs()
            val nearExpiryThreshold = if (totalDuration > 0) item.expiryDateMs - (totalDuration / 4) else currentNowMs + 3 * 24 * 60 * 60 * 1000L

            if (currentNowMs >= item.expiryDateMs) {
                container.setBackgroundResource(R.drawable.bg_item_expired)
                holder.status.visibility = View.VISIBLE
                holder.status.text = "已过期"
                holder.status.setTextColor(android.graphics.Color.RED)
            } else if (currentNowMs >= nearExpiryThreshold) {
                container.setBackgroundResource(R.drawable.bg_item_near)
                holder.status.visibility = View.VISIBLE
                val daysRemaining = Math.max(0, Math.ceil((item.expiryDateMs - currentNowMs).toDouble() / (24 * 60 * 60 * 1000)).toInt())
                holder.status.text = "临期提醒: 剩余 $daysRemaining 天"
                holder.status.setTextColor(android.graphics.Color.parseColor("#FF9800"))
            } else {
                container.setBackgroundResource(R.drawable.bg_item_normal)
                holder.status.visibility = View.GONE
            }

            // Surface Actions
            if (item.portions > 1) {
                holder.btnTakeOne.visibility = View.VISIBLE
                holder.btnTakeOne.setOnClickListener { onTakeOne(item) }
            } else {
                holder.btnTakeOne.visibility = View.GONE
            }
            
            holder.btnTakeAll.setOnClickListener { onTakeAll(item) }


            holder.itemView.setOnClickListener { onAction(item) }
        }

        private fun applySafeIcon(imageView: ImageView, item: FoodEntity) {
            val name = item.name
            val iconName = item.icon
            val context = imageView.context
            
            // 1. Try to load by specific icon name (ic_food_*)
            if (iconName.isNotEmpty()) {
                val resId = context.resources.getIdentifier(iconName, "drawable", context.packageName)
                if (resId != 0) {
                    imageView.setImageResource(resId)
                    return
                }
            }

            // 2. Fallback to category-based matching
            val category = getCatalogItems().entries.find { it.value.contains(name) }?.key ?: ""
            val resId = when {
                name.contains("熟食") || category.contains("熟食") -> R.drawable.cat_cooked
                name.contains("肉") || name.contains("鱼") || name.contains("虾") || name.contains("螃蟹") || category.contains("肉") -> R.drawable.cat_meat
                name.contains("菜") || name.contains("果") || name.contains("苹果") || category.contains("蔬菜") -> R.drawable.cat_veg
                name.contains("奶") || name.contains("汁") || category.contains("饮料") -> R.drawable.cat_drink
                else -> R.drawable.cat_snack
            }
            imageView.setImageResource(resId)
        }

        override fun getItemCount() = items.size

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val icon: ImageView = view.findViewById(R.id.itemIcon)
            val name: TextView = view.findViewById(R.id.itemName)
            val quantity: TextView = view.findViewById(R.id.itemQuantity)
            val remark: TextView = view.findViewById(R.id.itemRemark)
            val portions: TextView = view.findViewById(R.id.itemPortions)
            val detail: TextView = view.findViewById(R.id.itemDetail)
            val location: TextView = view.findViewById(R.id.itemLocation)
            val status: TextView = view.findViewById(R.id.itemStatus)
            val btnTakeOne: Button = view.findViewById(R.id.btnTakeOne)
            val btnTakeAll: Button = view.findViewById(R.id.btnTakeAll)
        }
    }
}
