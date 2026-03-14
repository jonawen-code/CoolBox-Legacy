// Version: V3.0.0-Pre21
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
import com.example.coolbox.ui.FoodActionHelper
import com.example.coolbox.ui.FoodEditHelper
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

    lateinit var viewModel: MainViewModel
    lateinit var actionHelper: FoodActionHelper
    lateinit var editHelper: FoodEditHelper
    private lateinit var adapter: FoodAdapter
    private var searchQuery: String = ""
    private var sortType: String = "EXPIRY" // NAME, EXPIRY, CATEGORY
    private var isAscending: Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        viewModel = ViewModelProvider(this)[MainViewModel::class.java]
        actionHelper = FoodActionHelper(this, viewModel)
        editHelper = FoodEditHelper(this, viewModel)

        // Check Setup
        if (viewModel.isSetupComplete.value ?: false == false) {
            startActivity(Intent(this, SetupActivity::class.java))
            finish()
            return
        }
        
        findViewById<TextView>(R.id.txtAppVersion).text = "V3.0.0-Pre21"
        // Version: V3.0.0-Pre9
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
                    setOnClickListener { editHelper.showAddOrEditFoodDialog(categoryToSelect = categoryName) }
                }
                quickEntryBar.addView(btn)
            }
        }
        buildQuickButtons()
        viewModel.categories.observe(this) { buildQuickButtons() }

        adapter = FoodAdapter(
            currentFridge = { viewModel.currentFridge.value },
            getCatalogItems = { viewModel.getCatalogData() },
            onAction = { item -> actionHelper.showItemActionDialog(item) },
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
            editHelper.showAddOrEditFoodDialog()
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
}
// Version: V3.0.0-Pre9
