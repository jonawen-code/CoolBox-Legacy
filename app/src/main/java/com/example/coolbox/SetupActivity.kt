// Version: V3.0.0-Pre21
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
        val txtSyncHint = findViewById<View>(R.id.txtSyncHint)
        val sectionStorage = findViewById<View>(R.id.sectionStorageSettings)
        val sectionCategory = findViewById<View>(R.id.sectionCategorySettings)
        val btnStartNew = findViewById<Button>(R.id.btnStartNew)
        val btnTakeover = findViewById<Button>(R.id.btnTakeover)
        val setupContent = findViewById<View>(R.id.setupContent)
        val txtTakeoverHint = findViewById<View>(R.id.txtTakeoverHint)

        var setupMode = "" // "NEW" or "TAKEOVER"

        btnStartNew.setOnClickListener {
            setupMode = "NEW"
            setupContent.visibility = View.VISIBLE
            txtTakeoverHint.visibility = View.GONE
            txtSyncHint.visibility = View.VISIBLE
            sectionStorage.visibility = View.VISIBLE
            sectionCategory.visibility = View.VISIBLE
            btnStartNew.setBackgroundColor(android.graphics.Color.parseColor("#BBDEFB"))
            btnTakeover.setBackgroundColor(android.graphics.Color.parseColor("#FFF3E0"))
        }

        btnTakeover.setOnClickListener {
            setupMode = "TAKEOVER"
            setupContent.visibility = View.VISIBLE
            txtTakeoverHint.visibility = View.VISIBLE
            txtSyncHint.visibility = View.GONE
            syncEnabledSwitch.isChecked = true
            // Hide local setup for takeover to focus on NAS
            sectionStorage.visibility = View.GONE
            sectionCategory.visibility = View.GONE
            btnTakeover.setBackgroundColor(android.graphics.Color.parseColor("#FFE0B2"))
            btnStartNew.setBackgroundColor(android.graphics.Color.parseColor("#E3F2FD"))
        }

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

        // Time Machine Restore removed in Pre15 UI Purification

        val fridgeNames = mutableListOf<String>()
        
        // Initialize with default or existing fridges (bases)
        val existingBases = viewModel.fridgeBases.value ?: listOf("我的冰箱", "小冰柜")
        existingBases.forEach { addFridgeRow(it, fridgesContainer, fridgeNames) }

        btnAddFridge.setOnClickListener {
            addFridgeRow("", fridgesContainer, fridgeNames)
        }

        completeBtn.setOnClickListener {
            if (setupMode == "NEW") {
                AlertDialog.Builder(this)
                    .setTitle("⚠️ 安全提示")
                    .setMessage("注意：此操作将创建全新的本地数据。如果后续连接服务器，可能会覆盖掉云端的旧数据，请确认是否继续？")
                    .setPositiveButton("确认继续") { _, _ ->
                        performFinalSetup(viewModel, fridgesContainer, categoryInput, syncUrlInput, setupMode)
                    }
                    .setNegativeButton("取消", null)
                    .show()
            } else if (setupMode == "TAKEOVER") {
                performFinalSetup(viewModel, fridgesContainer, categoryInput, syncUrlInput, setupMode)
            } else {
                Toast.makeText(this, "请先选择入驻方式", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun performFinalSetup(viewModel: MainViewModel, fridgesContainer: LinearLayout, categoryInput: EditText, syncUrlInput: EditText, setupMode: String) {
        val categoriesFromInput = categoryInput.text.toString().split(",").map { it.trim() }.filter { it.isNotEmpty() }
        val finalFridges = mutableListOf<String>()
        val syncUrl = syncUrlInput.text.toString()
        val syncEnabled = findViewById<SwitchCompat>(R.id.syncEnabledSwitch).isChecked

        if (setupMode == "TAKEOVER") {
            if (syncUrl.isBlank()) {
                Toast.makeText(this, "接管模式必须输入服务器地址", Toast.LENGTH_SHORT).show()
                return
            }
            // 模式二：接管已有仓库
            val progress = android.app.ProgressDialog(this).apply {
                setMessage("正在连接服务器并接管仓库...")
                setCancelable(false)
                show()
            }
            com.example.coolbox.util.CloudSyncManager.downloadConfig(this, syncUrl) { configSuccess ->
                if (!configSuccess) {
                    runOnUiThread {
                        progress.dismiss()
                        Toast.makeText(this, "获取云端配置失败", Toast.LENGTH_SHORT).show()
                    }
                    return@downloadConfig
                }
                
                com.example.coolbox.util.CloudSyncManager.downloadDatabase(this, syncUrl) { success ->
                    runOnUiThread {
                        progress.dismiss()
                        if (success) {
                            // Mark pull as done to enable future pushes
                            getSharedPreferences("coolbox_prefs", MODE_PRIVATE).edit().putBoolean("is_pull_first_done", true).apply()
                            // Reload VM state from SharedPreferences (which were updated by downloadConfig)
                            viewModel.refreshSettings() 
                            viewModel.completeSetup(emptyList(), emptyList(), emptyMap(), emptyList(), true, syncUrl)
                            Toast.makeText(this, "接管成功！您的设备与配置已就绪。", Toast.LENGTH_LONG).show()
                            startActivity(Intent(this, MainActivity::class.java))
                            finish()
                        } else {
                            AlertDialog.Builder(this)
                                .setTitle("接管失败")
                                .setMessage("无法从云端下载食品数据。")
                                .setPositiveButton("重试", null)
                                .show()
                        }
                    }
                }
            }
            return
        }

        // 默认模式 / NEW 模式逻辑
        for (i in 0 until fridgesContainer.childCount) {
            val row = fridgesContainer.getChildAt(i)
            val input = row.findViewById<EditText>(android.R.id.text1)
            val name = input.text.toString().trim()
            if (name.isNotEmpty()) finalFridges.add(name)
        }

        if (finalFridges.isEmpty() || categoriesFromInput.isEmpty()) {
            Toast.makeText(this, "请输入至少一个设备和一个分类", Toast.LENGTH_SHORT).show()
            return
        }

        // NEW 模式默认 Pull 完毕（因为它是新起点）
        getSharedPreferences("coolbox_prefs", MODE_PRIVATE).edit().putBoolean("is_pull_first_done", true).apply()

        val existingBases = viewModel.fridgeBases.value ?: emptyList()
        if (finalFridges.sorted() == existingBases.sorted()) {
            AlertDialog.Builder(this)
                .setTitle("设备未变更")
                .setMessage("检测到储存设备名称未变动，是否跳过层级/容量设置，直接管理食品分类？")
                .setPositiveButton("跳过") { _, _ ->
                    val currentFridges = viewModel.fridges.value ?: emptyList()
                    val currentCaps = viewModel.fridgeCapabilities.value ?: emptyMap()
                    requestCategorySetup(currentFridges, finalFridges, currentCaps, viewModel, syncEnabled, syncUrl, categoriesFromInput)
                }
                .setNegativeButton("重新设置") { _, _ ->
                    requestCapabilities(finalFridges, 0, mutableListOf(), mutableMapOf(), categoriesFromInput, viewModel, syncEnabled, syncUrl)
                }
                .show()
        } else {
            requestCapabilities(finalFridges, 0, mutableListOf(), mutableMapOf(), categoriesFromInput, viewModel, syncEnabled, syncUrl)
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

    private fun showRestoreDialog(serverUrl: String, viewModel: MainViewModel) {
        val progress = android.app.ProgressDialog(this).apply {
            setMessage("正在从云端获取快照列表...")
            setCancelable(false)
            show()
        }

        com.example.coolbox.util.CloudSyncManager.fetchBackups(serverUrl) { list, error ->
            runOnUiThread {
                progress.dismiss()
                if (error != null) {
                    Toast.makeText(this, "获取失败: $error", Toast.LENGTH_LONG).show()
                    return@runOnUiThread
                }
                if (list.isNullOrEmpty()) {
                    Toast.makeText(this, "暂无可用快照", Toast.LENGTH_SHORT).show()
                    return@runOnUiThread
                }

                // Use the backend-provided formatted time
                val displayList = list.map { it.time }.toTypedArray()

                AlertDialog.Builder(this)
                    .setTitle("🕒 选择云端快照 (Time Machine)")
                    .setItems(displayList) { _, which ->
                        val selected = list[which]
                        AlertDialog.Builder(this)
                            .setTitle("确认恢复？")
                            .setMessage("确定要将数据回滚至 [${displayList[which]}] 吗？\n\n⚠️ 注意：恢复后将覆盖当前全库数据。")
                            .setPositiveButton("确定回滚") { _, _ ->
                                executeRestore(serverUrl, selected.filename, viewModel)
                            }
                            .setNegativeButton("取消", null)
                            .show()
                    }
                    .setNegativeButton("关闭", null)
                    .show()
            }
        }
    }

    private fun executeRestore(serverUrl: String, filename: String, viewModel: MainViewModel) {
        val progress = android.app.ProgressDialog(this).apply {
            setMessage("正在启动时光机回滚...")
            setCancelable(false)
            show()
        }

        com.example.coolbox.util.CloudSyncManager.restoreBackup(serverUrl, filename) { success, error ->
            runOnUiThread {
                progress.dismiss()
                if (success) {
                    Toast.makeText(this, "回滚成功！正在同步最新快照...", Toast.LENGTH_LONG).show()
                    // Force a full download/sync to refresh local DB
                    com.example.coolbox.util.CloudSyncManager.downloadDatabase(this, serverUrl) { _ ->
                        // No extra action needed, Toast handles feedback
                    }
                } else {
                    Toast.makeText(this, "回滚失败: $error", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
// Version: V3.0.0-Pre21
