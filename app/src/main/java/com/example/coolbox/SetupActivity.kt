package com.example.coolbox

import android.content.Intent
import com.example.coolbox.legacy.R
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.widget.SwitchCompat
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.coolbox.ui.MainViewModel
import androidx.appcompat.app.AlertDialog
import android.widget.LinearLayout
import android.widget.TextView
import android.view.ViewGroup
import android.content.DialogInterface
import android.view.Gravity

class SetupActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setup)

        val viewModel = ViewModelProvider(this)[MainViewModel::class.java]

        val fridgesContainer = findViewById<android.widget.LinearLayout>(R.id.fridgeListContainer)
        val btnAddFridge = findViewById<Button>(R.id.btnAddFridge)
        val categoryInput = findViewById<EditText>(R.id.categoryInput)
        val completeBtn = findViewById<Button>(R.id.completeBtn)
        val syncEnabledSwitch = findViewById<SwitchCompat>(R.id.syncEnabledSwitch)
        val syncUrlInput = findViewById<EditText>(R.id.syncUrlInput)

        // Initialize category input with current categories from ViewModel
        val currentCats = viewModel.categories.value
        if (currentCats != null && currentCats.isNotEmpty()) {
            categoryInput.setText(currentCats.joinToString(", "))
        }

        syncEnabledSwitch.setOnCheckedChangeListener { _, isChecked ->
            syncUrlInput.visibility = if (isChecked) View.VISIBLE else View.GONE
        }

        // Initialize sync settings from ViewModel
        syncEnabledSwitch.isChecked = viewModel.syncEnabled.value ?: false
        syncUrlInput.setText(viewModel.syncServerUrl)
        syncUrlInput.visibility = if (syncEnabledSwitch.isChecked) View.VISIBLE else View.GONE

        val fridgeNames = mutableListOf<String>()
        
        // Initialize with default or existing fridges (bases)
        val existingBases = viewModel.fridgeBases.value ?: listOf("我的冰箱", "小冰柜")
        existingBases.forEach { addFridgeRow(it, fridgesContainer, fridgeNames) }

        btnAddFridge.setOnClickListener {
            addFridgeRow("", fridgesContainer, fridgeNames)
        }

        completeBtn.setOnClickListener {
            val categoriesFromInput = categoryInput.text.toString().split(",").map { it.trim() }.filter { it.isNotEmpty() }
            val finalFridges = mutableListOf<String>()
            
            // Collect non-empty names from the UI rows
            for (i in 0 until fridgesContainer.childCount) {
                val row = fridgesContainer.getChildAt(i)
                val input = row.findViewById<EditText>(android.R.id.text1)
                val name = input.text.toString().trim()
                if (name.isNotEmpty()) finalFridges.add(name)
            }

            if (finalFridges.isEmpty() || categoriesFromInput.isEmpty()) {
                Toast.makeText(this, "请输入至少一个设备和一个分类", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val syncUrl = syncUrlInput.text.toString()
            val existingBases = viewModel.fridgeBases.value ?: emptyList()
            
            val syncEnabled = findViewById<SwitchCompat>(R.id.syncEnabledSwitch).isChecked
            
            // Logic: If device names are identical to previous ones, allow skipping detailed layer setup
            if (finalFridges.sorted() == existingBases.sorted()) {
                AlertDialog.Builder(this)
                    .setTitle("设备未变更")
                    .setMessage("检测到储存设备名称未变动，是否跳过层级/容量设置，直接管理食品分类？")
                    .setPositiveButton("跳过") { _, _ ->
                        val currentFridges = viewModel.fridges.value ?: emptyList()
                        val currentCaps = viewModel.fridgeCapabilities.value ?: emptyMap()
                        // Pass categoriesFromInput forward to save them properly
                        requestCategorySetup(currentFridges, finalFridges, currentCaps, viewModel, syncEnabled, syncUrl, categoriesFromInput)
                    }
                    .setNegativeButton("重新设置") { _, _ ->
                        requestCapabilities(finalFridges, 0, mutableListOf(), mutableMapOf(), categoriesFromInput, viewModel, syncEnabled, syncUrl)
                    }
                    .show()
            } else {
                // Device names changed, must go through full flow
                requestCapabilities(finalFridges, 0, mutableListOf(), mutableMapOf(), categoriesFromInput, viewModel, syncEnabled, syncUrl)
            }
        }
    }

    private fun addFridgeRow(name: String, container: android.widget.LinearLayout, names: MutableList<String>) {
        val row = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.HORIZONTAL
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT, 
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setPadding(0, 8, 0, 8)
        }

        val editText = EditText(this).apply {
            id = android.R.id.text1
            layoutParams = android.widget.LinearLayout.LayoutParams(0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            hint = "设备名称 (如: 侧边小柜)"
            setText(name)
        }

        val btnDelete = Button(this).apply {
            text = "删除"
            setTextColor(android.graphics.Color.RED)
            background = null
            setOnClickListener {
                container.removeView(row)
            }
        }

        row.addView(editText)
        row.addView(btnDelete)
        container.addView(row)
    }

    private fun getChineseOrdinal(i: Int): String {
        return when (i) {
            1 -> "第一"
            2 -> "第二"
            3 -> "第三"
            4 -> "第四"
            5 -> "第五"
            6 -> "第六"
            7 -> "第七"
            8 -> "第八"
            9 -> "第九"
            else -> i.toString()
        }
    }

    private fun requestCapabilities(
        fridgeBaseNames: List<String>,
        index: Int,
        allFinalLocations: MutableList<String>,
        allCapabilities: MutableMap<String, String>,
        categories: List<String>,
        viewModel: MainViewModel,
        syncEnabled: Boolean,
        syncUrl: String
    ) {
        if (index >= fridgeBaseNames.size) {
            requestCategorySetup(
                allFinalLocations,
                fridgeBaseNames,
                allCapabilities,
                viewModel,
                syncEnabled,
                syncUrl
            )
            return
        }

        val baseName = fridgeBaseNames[index]
        val typeOptions = arrayOf(
            "冰箱：仅冷藏室",
            "冰柜：仅冷冻仓 (支持多仓/分层)",
            "常规冰箱：冷藏 + 冷冻 (两门/对开门)",
            "三门冰箱：冷藏 + 微冻 + 冷冻"
        )

        android.app.AlertDialog.Builder(this)
            .setTitle("配置设备：$baseName")
            .setItems(typeOptions) { _, which ->
                when (which) {
                    0 -> { // Only Refrig
                        val locName = "$baseName 冷藏室"
                        allFinalLocations.add(locName)
                        allCapabilities[locName] = "冷藏"
                        requestCapabilities(fridgeBaseNames, index + 1, allFinalLocations, allCapabilities, categories, viewModel, syncEnabled, syncUrl)
                    }
                    1 -> { // Deep Freezer
                        promptForFreezerLayers(baseName, "", true) { layers ->
                            layers.forEach { l ->
                                allFinalLocations.add(l)
                                allCapabilities[l] = "冷冻"
                            }
                            requestCapabilities(fridgeBaseNames, index + 1, allFinalLocations, allCapabilities, categories, viewModel, syncEnabled, syncUrl)
                        }
                    }
                    2 -> { // Refrig + Freezer
                        val refrigLoc = "$baseName 冷藏室"
                        allFinalLocations.add(refrigLoc)
                        allCapabilities[refrigLoc] = "冷藏"
                        promptForFreezerLayers(baseName, "冷冻室", false) { layers ->
                            layers.forEach { l ->
                                allFinalLocations.add(l)
                                allCapabilities[l] = "冷冻"
                            }
                            requestCapabilities(fridgeBaseNames, index + 1, allFinalLocations, allCapabilities, categories, viewModel, syncEnabled, syncUrl)
                        }
                    }
                    3 -> { // Refrig + Micro + Freezer
                        val refrigLoc = "$baseName 冷藏室"
                        allFinalLocations.add(refrigLoc)
                        allCapabilities[refrigLoc] = "冷藏"
                        
                        val microLoc = "$baseName 微冻室"
                        allFinalLocations.add(microLoc)
                        allCapabilities[microLoc] = "冷冻" // Treat micro-freeze as freezer for logic
                        
                        promptForFreezerLayers(baseName, "冷冻室", false) { layers ->
                            layers.forEach { l ->
                                allFinalLocations.add(l)
                                allCapabilities[l] = "冷冻"
                            }
                            requestCapabilities(fridgeBaseNames, index + 1, allFinalLocations, allCapabilities, categories, viewModel, syncEnabled, syncUrl)
                        }
                    }
                }
            }
            .setCancelable(false)
            .show()
    }

    private fun promptForFreezerLayers(baseName: String, compartmentName: String, isDeepFreezer: Boolean, onComplete: (List<String>) -> Unit) {
        val layerOptions = (1..9).map { "${it}层" }.toTypedArray()
        val title = if (isDeepFreezer) "$baseName 有几个仓位/层？" else "$baseName 的 $compartmentName 有几层？"
        
        android.app.AlertDialog.Builder(this)
            .setTitle(title)
            .setItems(layerOptions) { _, which ->
                val count = which + 1
                val layers = if (count == 1) {
                    if (isDeepFreezer) listOf(baseName) else listOf("$baseName $compartmentName")
                } else {
                    (1..count).map { i -> 
                        val ordinal = getChineseOrdinal(i)
                        if (isDeepFreezer) "$baseName ${ordinal}层" else "$baseName $compartmentName ${ordinal}层"
                    }
                }
                onComplete(layers)
            }
            .setCancelable(false)
            .show()
    }

    private fun requestCategorySetup(
        allFinalLocations: List<String>,
        fridgeBaseNames: List<String>,
        allCapabilities: Map<String, String>,
        viewModel: MainViewModel,
        syncEnabled: Boolean,
        syncUrl: String,
        categoriesToStartWith: List<String>? = null
    ) {
        val oldCategories = viewModel.categories.value ?: emptyList()
        val currentCategories = (categoriesToStartWith ?: oldCategories).toMutableList()

        val showEditDialog = {
            val listLayout = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(32, 16, 32, 16)
            }

            val refreshList = {
                listLayout.removeAllViews()
                currentCategories.forEach { cat ->
                    val row = LinearLayout(this).apply {
                        orientation = LinearLayout.HORIZONTAL
                        gravity = android.view.Gravity.CENTER_VERTICAL
                    }
                    val tv = TextView(this).apply {
                        text = cat
                        layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                    }
                    val btnDel = Button(this).apply {
                        text = "删除"
                        setOnClickListener {
                            currentCategories.remove(cat)
                            // Note: refreshList() will be called indirectly by the outer logic if needed, 
                            // but for simplicity we re-invoke current logic or dialog.
                            // In a real app we'd use a RecyclerView, but for this guided setup, a re-render works.
                        }
                    }
                    row.addView(tv)
                    // row.addView(btnDel) // Removing categories might be dangerous for data loss, user asked for migration
                    // Let's stick to the user's flow: they want to be able to change/set categories.
                    listLayout.addView(row)
                }
            }

            // Simplified approach: use a multi-select or a simple list with "Add"
            AlertDialog.Builder(this)
                .setTitle("设置食品分类")
                .setMessage("根据您的习惯调整分类名称（长按项可删除/修改）")
                .setItems(currentCategories.toTypedArray()) { _, idx ->
                    val cat = currentCategories[idx]
                    val input = EditText(this).apply { setText(cat) }
                    AlertDialog.Builder(this)
                        .setTitle("修改分类")
                        .setView(input)
                        .setPositiveButton("保存") { _, _ ->
                            val newName = input.text.toString()
                            if (newName.isNotBlank() && newName != cat) {
                                currentCategories[idx] = newName
                                requestCategorySetup(allFinalLocations, fridgeBaseNames, allCapabilities, viewModel, syncEnabled, syncUrl, currentCategories) // Recursively refresh
                            }
                        }
                        .setNegativeButton("删除") { _, _ ->
                            currentCategories.removeAt(idx)
                            requestCategorySetup(allFinalLocations, fridgeBaseNames, allCapabilities, viewModel, syncEnabled, syncUrl, currentCategories)
                        }
                        .show()
                }
                .setNeutralButton("新增分类") { _, _ ->
                    val input = EditText(this).apply { hint = "例如：🍷 零度保鲜" }
                    AlertDialog.Builder(this)
                        .setTitle("新增分类")
                        .setView(input)
                        .setPositiveButton("添加") { _, _ ->
                            val newName = input.text.toString()
                            if (newName.isNotBlank()) {
                                currentCategories.add(newName)
                                requestCategorySetup(allFinalLocations, fridgeBaseNames, allCapabilities, viewModel, syncEnabled, syncUrl, currentCategories)
                            }
                        }
                        .show()
                }
                .setPositiveButton("完成设置") { _, _ ->
                    handleCategoryMigrationAndFinish(
                        oldCategories,
                        currentCategories,
                        allFinalLocations,
                        fridgeBaseNames,
                        allCapabilities,
                        viewModel,
                        syncEnabled,
                        syncUrl
                    )
                }
                .show()
        }
        showEditDialog()
    }

    private fun handleCategoryMigrationAndFinish(
        oldCategories: List<String>,
        newCategories: List<String>,
        allFinalLocations: List<String>,
        fridgeBaseNames: List<String>,
        allCapabilities: Map<String, String>,
        viewModel: MainViewModel,
        syncEnabled: Boolean,
        syncUrl: String
    ) {
        val removedCategories = oldCategories.filter { !newCategories.contains(it) }
        
        fun processMigration(index: Int) {
            if (index >= removedCategories.size) {
                viewModel.completeSetup(allFinalLocations, fridgeBaseNames, allCapabilities, newCategories, syncEnabled, syncUrl)
                startActivity(Intent(this, MainActivity::class.java))
                finish()
                return
            }

            val oldCat = removedCategories[index]
            val dialog = AlertDialog.Builder(this)
            dialog.setTitle("[$oldCat] \u5df2\u5220\u9664\uff0c\u8bf7\u9009\u62e9\u63a5\u7ba1\u5206\u7c7b")
            dialog.setItems(newCategories.toTypedArray()) { _: DialogInterface, targetIdx: Int ->
                val targetCat = newCategories[targetIdx]
                viewModel.migrateCategory(oldCat, targetCat)
                processMigration(index + 1)
            }
            dialog.setCancelable(true)
            dialog.show()
        }

        processMigration(0)
    }
}
