package com.example.coolbox

import android.os.Bundle
import android.content.Intent
import com.example.coolbox.legacy.R
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
        
        findViewById<View>(R.id.btnSettings).setOnClickListener {
            startActivity(Intent(this, SetupActivity::class.java))
        }
        
        findViewById<View>(R.id.btnRefresh)?.setOnClickListener {
            android.widget.Toast.makeText(this, "正在刷新...", android.widget.Toast.LENGTH_SHORT).show()
            com.example.coolbox.util.CloudSyncManager.downloadDatabase(this, viewModel.syncServerUrl) { success ->
                if (success) {
                    val restartIntent = packageManager.getLaunchIntentForPackage(packageName)
                    restartIntent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    startActivity(restartIntent)
                    Runtime.getRuntime().exit(0)
                }
            }
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
                    setOnClickListener { showAddFoodDialog(categoryName) }
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
            showAddFoodDialog()
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

    private fun showAddFoodDialog(categoryToSelect: String? = null) {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_add_food, null)
        val fridgeSpinner = view.findViewById<Spinner>(R.id.fridgeSpinner)
        val categorySpinner = view.findViewById<Spinner>(R.id.categorySpinner)
        val itemSpinner = view.findViewById<Spinner>(R.id.itemSpinner)
        val editNewItemName = view.findViewById<EditText>(R.id.editNewItemName)
        val layoutNewItemName = view.findViewById<LinearLayout>(R.id.layoutNewItemName)
        val editQuantity = view.findViewById<EditText>(R.id.editQuantity)
        val editPortions = view.findViewById<EditText>(R.id.editPortions)
        val unitSpinner = view.findViewById<Spinner>(R.id.unitSpinner)
        val datePicker = view.findViewById<DatePicker>(R.id.datePicker)

        val units = listOf("个", "kg", "斤", "袋", "盒", "瓶", "升")

        // Setup Fridge Spinner (Use current fridge as default)
        val fridges = viewModel.fridges.value ?: listOf("我的冰箱", "小冰柜")
        val fridgeAdapter = android.widget.ArrayAdapter(this, android.R.layout.simple_spinner_item, fridges)
        fridgeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        fridgeSpinner.adapter = fridgeAdapter
        val currentFridgeIdx = fridges.indexOf(viewModel.currentFridge.value)
        if (currentFridgeIdx >= 0) fridgeSpinner.setSelection(currentFridgeIdx)

        val layoutRemark = view.findViewById<LinearLayout>(R.id.layoutRemark)
        val editRemark = view.findViewById<EditText>(R.id.editRemark)

        // Setup Category Spinner
        val categories = viewModel.getCatalogCategories()
        val categoryAdapter = android.widget.ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        categorySpinner.adapter = categoryAdapter
        
        categoryToSelect?.let { cat ->
            val idx = categories.indexOf(cat).let { i -> if (i >= 0) i else categories.indexOfFirst { c -> c.contains(cat) || cat.contains(c) } }
            if (idx >= 0) categorySpinner.setSelection(idx)
        }

        // Setup Item Spinner (Dynamic)
        categorySpinner.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedCategory = categories[position]
                val items = viewModel.getCatalogItems(selectedCategory).toMutableList()
                items.add("新增...")
                
                val itemAdapter = android.widget.ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_item, items)
                itemAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                itemSpinner.adapter = itemAdapter
                
                itemSpinner.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(p: android.widget.AdapterView<*>?, v: View?, pos: Int, i: Long) {
                        val selectedItem = items[pos]
                        if (selectedItem == "新增...") {
                            layoutNewItemName.visibility = View.VISIBLE
                            editNewItemName.requestFocus()
                        } else {
                            layoutNewItemName.visibility = View.GONE
                            
                            // --- Smart Fridge Recommendation (Refined) ---
                            val recommendedFridge = viewModel.getRecommendedFridge(selectedCategory, selectedItem)
                            val fridgeIdx = fridges.indexOf(recommendedFridge)
                            if (fridgeIdx >= 0) fridgeSpinner.setSelection(fridgeIdx)

                            val defaultUnit = viewModel.getDefaultUnit(selectedCategory, selectedItem)
                            val unitIdx = units.indexOf(defaultUnit)
                            if (unitIdx >= 0) unitSpinner.setSelection(unitIdx)
                        }
                    }
                    override fun onNothingSelected(p: android.widget.AdapterView<*>?) {}
                }

                // Always show remark field for all categories
                layoutRemark.visibility = View.VISIBLE

                // --- Smart Fridge Recommendation ---
                val recommendedFridge = viewModel.getRecommendedFridge(selectedCategory, if (items.isNotEmpty()) items[0] else "")
                val fridgeIdx = fridges.indexOf(recommendedFridge)
                if (fridgeIdx >= 0) fridgeSpinner.setSelection(fridgeIdx)

                // Smart Units: Auto-select based on category
                val defaultUnit = viewModel.getDefaultUnit(selectedCategory, "")
                val unitIdx = units.indexOf(defaultUnit)
                if (unitIdx >= 0) unitSpinner.setSelection(unitIdx)
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }

        // Setup Unit Spinner
        val unitAdapter = android.widget.ArrayAdapter(this, android.R.layout.simple_spinner_item, units)
        unitAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        unitSpinner.adapter = unitAdapter

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
            .setView(view)
            .setPositiveButton("添加") { _, _ ->
                val selectedInSpinner = itemSpinner.selectedItem?.toString() ?: ""
                val fullItemName = if (selectedInSpinner == "新增...") {
                    editNewItemName.text.toString()
                } else {
                    selectedInSpinner
                }
                
                val targetFridge = fridgeSpinner.selectedItem?.toString() ?: viewModel.currentFridge.value ?: ""
                val qtyStr = editQuantity.text.toString()
                val qty = if (qtyStr.isEmpty()) 1.0 else (qtyStr.toDoubleOrNull() ?: 1.0)
                
                // RESTORE: Extract portions from input
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
                    val name = fullItemName
                    val category = categorySpinner.selectedItem.toString()
                    val icon = viewModel.getIconForItem(name)
                    val remark = editRemark.text.toString()
                    viewModel.addFood(name, icon, category, qty, unit, expiryMs, portions, targetFridge, remark)
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
            columnCount = 4
            rowCount = (iconList.size + 3) / 4
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
                    width = (80 * resources.displayMetrics.density).toInt()
                    height = (80 * resources.displayMetrics.density).toInt()
                    setMargins(8, 8, 8, 8)
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
        val options = arrayOf("仅拿走 1 份", "拿走多份...", "转移到其他冰箱")
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(entity.name)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> viewModel.takePortion(entity)
                    1 -> showTakePortionDialog(entity)
                    2 -> showTransferDialog(entity)
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
            expired.take(3).forEach { msg.append("- ${it.icon}${it.name}\n") }
            if (expired.size > 3) msg.append("...等\n")
        }
        if (expiring.isNotEmpty()) {
            if (msg.isNotEmpty()) msg.append("\n")
            msg.append("🕒 即将过期 (${expiring.size}件):\n")
            expiring.take(3).forEach { msg.append("- ${it.icon}${it.name}\n") }
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
            val filtered = if (currentFridge == null) {
                allFood 
            } else {
                // Filter by prefix or exact match to support "DeviceName" tab showing "DeviceName Level 1"
                allFood.filter { it.fridgeName == currentFridge || it.fridgeName.startsWith("$currentFridge ") }
            }
            adapter.submitList(filtered)
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

            holder.name.text = item.name
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
            // Show full device location, shortened for readability
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
