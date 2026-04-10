// Version: V2.9.0-RC3 (Refactor - Final)
package com.example.coolbox.ui
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import com.example.coolbox.MainActivity
import com.example.coolbox.data.FoodEntity
import com.example.coolbox.legacy.R
import com.example.coolbox.util.EquipmentManager
import com.example.coolbox.util.formatQuantity
import java.text.SimpleDateFormat
import java.util.*

class FoodEditHelper(private val context: Context, private val viewModel: MainViewModel) {

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

    fun showAddOrEditFoodDialog(existingItem: FoodEntity? = null, categoryToSelect: String? = null) {
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_add_food, null)
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

        val units = if (viewModel.currentSpace.value == 1) {
            listOf("盒", "粒", "支", "袋", "瓶", "支", "个")
        } else {
            listOf("个", "kg", "斤", "袋", "盒", "瓶", "升")
        }

        // Setup Device & Zone Spinners (Direct calculation from fridges.value for instantaneous data)
        val allFullNames = viewModel.fridges.value ?: emptyList()
        val deviceZoneMap = allFullNames.groupBy { EquipmentManager.extractBase(it) }
            .mapValues { entry -> 
                val base = entry.key
                val zs = entry.value.map { EquipmentManager.extractZone(it, base) }
                    .filter { it.isNotEmpty() && it != base && it != "默认层" }
                    .distinct().toMutableList()
                
                // Ensure Refrigerator/Freezer requirements (Standard Templates v4.1.6)
                val standard = EquipmentManager.getStandardZones(base)
                standard.forEach { if (!zs.contains(it)) zs.add(it) }
                
                if (zs.isEmpty()) zs.add("默认层")
                zs.sortedWith { s1, s2 -> EquipmentManager.normalizeToDigit(s1).compareTo(EquipmentManager.normalizeToDigit(s2)) }
            }
            
        val allDevices = deviceZoneMap.keys.toList().filter { it.isNotBlank() }.ifEmpty { listOf("我的冰箱") }
        val deviceAdapter = android.widget.ArrayAdapter(context, R.layout.spinner_item_dark, allDevices)
        deviceAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item_dark)
        deviceSpinner.adapter = deviceAdapter

        val zoneAdapter = android.widget.ArrayAdapter(context, R.layout.spinner_item_dark, mutableListOf<String>())
        zoneAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item_dark)
        zoneSpinner.adapter = zoneAdapter

        var isDirty = false
        var originalDirtyLocation = ""

        if (existingItem != null) {
            val loc = existingItem.fridgeName.trim()
            val base = EquipmentManager.extractBase(loc)
            val zone = EquipmentManager.extractZone(loc, base)
            
            if (allDevices.contains(base) && deviceZoneMap[base]?.contains(zone) == true) {
                val dIdx = allDevices.indexOf(base)
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
                if (position < 0 || position >= allDevices.size) return
                val selectedDevice = allDevices[position]
                val zones = deviceZoneMap[selectedDevice] ?: emptyList()
                
                android.util.Log.d("CoolBox", "Spinner selected: $selectedDevice, zones found: ${zones.size}")

                zoneAdapter.clear()
                if (zones.isNotEmpty()) {
                    zoneAdapter.addAll(zones)
                } else {
                    zoneAdapter.add("默认层")
                }
                zoneAdapter.notifyDataSetChanged()

                val pendingZone = deviceSpinner.tag as? String
                if (pendingZone != null) {
                    val zIdx = zones.indexOfFirst { it.contains(pendingZone) || pendingZone.contains(it) }
                    if (zIdx >= 0) zoneSpinner.setSelection(zIdx)
                    else zoneSpinner.setSelection(0)
                    deviceSpinner.tag = null
                } else {
                    zoneSpinner.setSelection(0)
                }
                
                if (isDirty) { isDirty = false; textDirtyLocation.visibility = View.GONE }
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }

        // 🛑 CRITICAL: Manually trigger the first update because Android Spinners are inconsistent with initial events
        val initialPos = deviceSpinner.selectedItemPosition
        if (initialPos >= 0) {
            val selectedDevice = allDevices[initialPos]
            val zones = deviceZoneMap[selectedDevice] ?: emptyList()
            zoneAdapter.clear()
            if (zones.isNotEmpty()) zoneAdapter.addAll(zones) else zoneAdapter.add("默认层")
            zoneAdapter.notifyDataSetChanged()
        }

        val layoutRemark = view.findViewById<LinearLayout>(R.id.layoutRemark)
        val editRemark = view.findViewById<EditText>(R.id.editRemark)

        // Setup Category Spinner
        val categories = viewModel.getCatalogCategories()
        val categoryAdapter = android.widget.ArrayAdapter(context, R.layout.spinner_item_dark, categories)
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
                
                val itemAdapter = android.widget.ArrayAdapter(context, R.layout.spinner_item_dark, items)
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
                            
                            // Build 4.0: Smart Sensing for existing items in medicine mode
                            if (viewModel.currentSpace.value == 1) {
                                val (recCat, _) = viewModel.getMedicineRecommendation(selectedItem)
                                val catIdx = categories.indexOf(recCat)
                                if (catIdx >= 0 && catIdx != categorySpinner.selectedItemPosition) {
                                    categorySpinner.setSelection(catIdx)
                                }
                            }
                        }
                    }
                    override fun onNothingSelected(p: android.widget.AdapterView<*>?) {}
                }

                // Initial linkage trigger for the first item in the category list
                if (items.isNotEmpty()) {
                    applySmartLinkage(selectedCategory, items[0], deviceSpinner, units, unitSpinner, allDevices)
                }
                
                layoutRemark.visibility = View.VISIBLE
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }

        // Build 4.0: TextWatcher for real-time sensing in "New Item" mode
        editNewItemName.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (viewModel.currentSpace.value == 1 && s != null && s.isNotEmpty()) {
                    val (recCat, _) = viewModel.getMedicineRecommendation(s.toString())
                    val catIdx = categories.indexOf(recCat)
                    if (catIdx >= 0 && catIdx != categorySpinner.selectedItemPosition) {
                        categorySpinner.setSelection(catIdx)
                    }
                }
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })

        // Setup Unit Spinner
        val unitAdapter = android.widget.ArrayAdapter(context, R.layout.spinner_item_dark, units)
        unitAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item_dark)
        unitSpinner.adapter = unitAdapter
        
        if (existingItem != null) {
            val uIdx = units.indexOf(existingItem.unit)
            if (uIdx >= 0) unitSpinner.setSelection(uIdx)
            editQuantity.setText(formatQuantity(existingItem.quantity))
            editPortions.setText(existingItem.portions.toString())
            editRemark.setText(existingItem.remark)
            
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

        datePicker.init(datePicker.year, datePicker.month, datePicker.dayOfMonth) { _, year, month, day ->
            updateExpiryLabel()
        }

        var selectedYear = Calendar.getInstance().apply { timeInMillis = viewModel.nowMs() }.get(Calendar.YEAR)
        var selectedMonth = Calendar.getInstance().apply { timeInMillis = viewModel.nowMs() }.get(Calendar.MONTH)

        val btnThisYear = view.findViewById<Button>(R.id.btnThisYear)
        val btnNextYear = view.findViewById<Button>(R.id.btnNextYear)
        val monthButtons = (1..12).map { i -> 
            val resId = context.resources.getIdentifier("btnMonth$i", "id", context.packageName)
            view.findViewById<Button>(resId)
        }

        val updateCalendarSelection = {
            val now = Calendar.getInstance().apply { timeInMillis = viewModel.nowMs() }
            val currentYear = now.get(Calendar.YEAR)
            
            btnThisYear.setTextColor(if (selectedYear == currentYear) android.graphics.Color.WHITE else android.graphics.Color.BLACK)
            btnThisYear.setBackgroundColor(if (selectedYear == currentYear) android.graphics.Color.parseColor("#03A9F4") else android.graphics.Color.WHITE)
            btnNextYear.setTextColor(if (selectedYear == currentYear + 1) android.graphics.Color.WHITE else android.graphics.Color.BLACK)
            btnNextYear.setBackgroundColor(if (selectedYear == currentYear + 1) android.graphics.Color.parseColor("#03A9F4") else android.graphics.Color.WHITE)

            monthButtons.forEachIndexed { index, btn ->
                if (index == selectedMonth) {
                    btn.setBackgroundColor(android.graphics.Color.parseColor("#03A9F4"))
                    btn.setTextColor(android.graphics.Color.WHITE)
                } else {
                    btn.setBackgroundColor(android.graphics.Color.parseColor("#EEEEEE"))
                    btn.setTextColor(android.graphics.Color.BLACK)
                }
            }

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
        
        updateCalendarSelection()

        androidx.appcompat.app.AlertDialog.Builder(context)
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
                val targetLocation = if (isDirty) {
                    originalDirtyLocation 
                } else {
                    EquipmentManager.formatFullName(selectedDevice, selectedZone)
                }
                
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
                            icon = viewModel.getIconForItem(fullItemName).ifEmpty { existingItem.icon },
                            category = category,
                            quantity = qty,
                            unit = unit,
                            expiryDateMs = expiryMs,
                            portions = portions,
                            weightPerPortion = if (portions > 0) {
                                (java.math.BigDecimal(qty.toString()).divide(java.math.BigDecimal(portions.toString()), 4, java.math.RoundingMode.HALF_UP)).toDouble()
                            } else qty,
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
}
// Version: V4.0.8 (Hierarchy Refine)
