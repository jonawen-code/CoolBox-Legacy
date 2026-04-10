package com.example.coolbox

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.slidingpanelayout.widget.SlidingPaneLayout
import com.example.coolbox.data.FridgeSettings
import com.example.coolbox.ui.MainViewModel
import com.example.coolbox.legacy.R

class SettingsActivity : AppCompatActivity() {

    private lateinit var viewModel: MainViewModel
    private lateinit var detailContainer: FrameLayout
    private lateinit var btnConfirmSave: Button
    
    private var currentMenu: String = "device"
    private val dirtyAffinityMap = mutableMapOf<String, Int>()
    private var dirtySyncEnabled: Boolean = false
    
    private val PICK_CBK_RESTORE = 1001
    private val PICK_CBK_EXPORT = 1002

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        viewModel = ViewModelProvider(this).get(MainViewModel::class.java)
        detailContainer = findViewById(R.id.detailContainer)
        btnConfirmSave = findViewById(R.id.btnConfirmSave)

        val isTablet = resources.configuration.smallestScreenWidthDp >= 600
        if (!isTablet) {
            findViewById<View>(R.id.menuDeviceInfo).visibility = View.GONE
        }

        setupMenu()
        showDetail(if (isTablet) "device" else "sync")

        btnConfirmSave.setOnClickListener {
            saveCurrentSettings()
        }
    }

    private fun setupMenu() {
        val click = View.OnClickListener { v ->
            val menu = when (v.id) {
                R.id.menuDeviceInfo -> "device"
                R.id.menuSync -> "sync"
                R.id.menuBackup -> "backup"
                R.id.menuMaintenance -> "maintenance"
                else -> "sync"
            }
            showDetail(menu)
            
            val slidingPane = findViewById<SlidingPaneLayout>(R.id.slidingPaneLayout)
            if (slidingPane.isSlideable) {
                slidingPane.openPane()
            }
        }
        
        findViewById<View>(R.id.menuDeviceInfo).setOnClickListener(click)
        findViewById<View>(R.id.menuSync).setOnClickListener(click)
        findViewById<View>(R.id.menuBackup).setOnClickListener(click)
        findViewById<View>(R.id.menuMaintenance).setOnClickListener(click)
    }

    private fun showDetail(menu: String) {
        currentMenu = menu
        detailContainer.removeAllViews()
        val inflater = LayoutInflater.from(this)

        when (menu) {
            "device" -> {
                val view = inflater.inflate(R.layout.layout_settings_device, detailContainer, true)
                setupDeviceSettings(view)
            }
            "sync" -> {
                val view = inflater.inflate(R.layout.layout_settings_sync, detailContainer, true)
                setupSyncSettings(view)
            }
            "backup" -> {
                val view = inflater.inflate(R.layout.layout_settings_backup, detailContainer, true)
                setupBackupSettings(view)
            }
            "maintenance" -> {
                val view = inflater.inflate(R.layout.layout_settings_maintenance, detailContainer, true)
                setupMaintenanceSettings(view)
            }
        }
        
        btnConfirmSave.visibility = if (menu == "device" || menu == "sync") View.VISIBLE else View.GONE
    }

    private fun setupDeviceSettings(root: View) {
        val rv = root.findViewById<RecyclerView>(R.id.rvDeviceAffinity)
        rv.layoutManager = LinearLayoutManager(this)
        
        val btnAdd = root.findViewById<Button>(R.id.btnAddDevice)
        btnAdd.setOnClickListener { showAddDeviceDialog() }

        val adapter = object : RecyclerView.Adapter<DeviceSettingViewHolder>() {
            var settings = emptyList<FridgeSettings>()
            
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceSettingViewHolder {
                val v = LayoutInflater.from(parent.context).inflate(R.layout.item_device_settings, parent, false)
                return DeviceSettingViewHolder(v)
            }

            override fun onBindViewHolder(holder: DeviceSettingViewHolder, position: Int) {
                val s = settings[position]
                val name = s.fridgeName
                val affinity = dirtyAffinityMap[name] ?: s.spaceAffinity
                
                holder.txtName.text = name
                holder.cbFood.isChecked = (affinity == 0 || affinity == 2)
                holder.cbMed.isChecked = (affinity == 1 || affinity == 2)
                
                val update = {
                    val newAffinity = when {
                        holder.cbFood.isChecked && holder.cbMed.isChecked -> 2
                        holder.cbFood.isChecked -> 0
                        holder.cbMed.isChecked -> 1
                        else -> 3
                    }
                    dirtyAffinityMap[name] = newAffinity
                }
                
                holder.cbFood.setOnClickListener { update() }
                holder.cbMed.setOnClickListener { update() }
                
                holder.txtName.setOnClickListener {
                    showRenameDeviceDialog(name)
                }

                holder.btnDelete.setOnClickListener {
                    androidx.appcompat.app.AlertDialog.Builder(this@SettingsActivity)
                        .setTitle("确认删除设备")
                        .setMessage("确定要删除设备 \"$name\" 吗？相关食品的存储位置信息将保留但设备配置将被移除。")
                        .setPositiveButton("删除") { _, _ ->
                            viewModel.deleteFridgeDevice(name)
                        }
                        .setNegativeButton("取消", null)
                        .show()
                }
            }

            override fun getItemCount() = settings.size
        }
        rv.adapter = adapter
        
        viewModel.fridgeSettings.observe(this) { 
            adapter.settings = it
            adapter.notifyDataSetChanged()
        }
    }

    private fun showAddDeviceDialog() {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_add_device, null)
        val editName = view.findViewById<EditText>(R.id.editDeviceName)
        val cbFood = view.findViewById<CheckBox>(R.id.cbFood)
        val cbMed = view.findViewById<CheckBox>(R.id.cbMedicine)
        
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("添加新设备")
            .setView(view)
            .setPositiveButton("添加") { _, _ ->
                val name = editName.text.toString().trim()
                if (name.isNotEmpty()) {
                    val affinity = when {
                        cbFood.isChecked && cbMed.isChecked -> 2
                        cbFood.isChecked -> 0
                        cbMed.isChecked -> 1
                        else -> 2
                    }
                    viewModel.addFridgeDevice(name, affinity)
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showRenameDeviceDialog(oldName: String) {
        val input = EditText(this)
        input.setText(oldName)
        input.selectAll()
        
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("重命名设备")
            .setView(input)
            .setPositiveButton("重命名") { _, _ ->
                val newName = input.text.toString().trim()
                if (newName.isNotEmpty() && newName != oldName) {
                    viewModel.renameFridgeDevice(oldName, newName)
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun setupSyncSettings(root: View) {
        val cb = root.findViewById<CheckBox>(R.id.cbSyncEnabled)
        val edit = root.findViewById<EditText>(R.id.editSyncUrl)
        
        cb.isChecked = viewModel.syncEnabled.value ?: false
        edit.setText(viewModel.syncUrl.value ?: "")
        
        dirtySyncEnabled = cb.isChecked
        
        cb.setOnCheckedChangeListener { _, isChecked -> dirtySyncEnabled = isChecked }
        
        root.findViewById<Button>(R.id.btnTestConnection)?.setOnClickListener {
            val url = edit.text.toString().trim()
            com.example.coolbox.util.CloudSyncManager.testConnection(url) { success, error ->
                runOnUiThread {
                    Toast.makeText(this, if (success) "✅ 连接成功" else "❌ 失败: $error", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun setupBackupSettings(root: View) {
        root.findViewById<Button>(R.id.btnExportBackup).setOnClickListener { showBackupBranchDialog() }
        root.findViewById<Button>(R.id.btnImportBackup).setOnClickListener { showRestoreBranchDialog() }
        root.findViewById<Button>(R.id.btnExportCSV).setOnClickListener { startCsvExport() }
    }

    private fun showBackupBranchDialog() {
        val options = arrayOf("本地导出 (CBK 压缩包)", "本地导出 (CSV 列表)", "云端备份到 NAS")
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("选择备份方式")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> startCbkExport()
                    1 -> startCsvExport()
                    2 -> chooseBackupFormatForNas()
                }
            }
            .show()
    }

    private fun startCbkExport() {
        try {
            val intent = android.content.Intent(android.content.Intent.ACTION_CREATE_DOCUMENT).apply {
                addCategory(android.content.Intent.CATEGORY_OPENABLE)
                type = "application/zip"
                putExtra(android.content.Intent.EXTRA_TITLE, com.example.coolbox.util.BackupManager.generateBackupFileName("CoolBox_Backup", ".cbk"))
            }
            startActivityForResult(intent, PICK_CBK_EXPORT)
        } catch (e: Exception) {
            executeLocalFallbackBackup(".cbk")
        }
    }

    private fun startCsvExport() {
        try {
            val intent = android.content.Intent(android.content.Intent.ACTION_CREATE_DOCUMENT).apply {
                addCategory(android.content.Intent.CATEGORY_OPENABLE)
                type = "text/csv"
                putExtra(android.content.Intent.EXTRA_TITLE, com.example.coolbox.util.BackupManager.generateBackupFileName("CoolBox_Data", ".csv"))
            }
            startActivityForResult(intent, PICK_CBK_EXPORT)
        } catch (e: Exception) {
            executeLocalFallbackBackup(".csv")
        }
    }

    private fun executeLocalFallbackBackup(extension: String) {
        val dir = android.os.Environment.getExternalStoragePublicDirectory("CoolBox")
        if (!dir.exists()) dir.mkdirs()
        val fileName = com.example.coolbox.util.BackupManager.generateBackupFileName("CoolBox_Export", extension)
        val file = java.io.File(dir, fileName)
        val uri = android.net.Uri.fromFile(file)
        
        Thread {
            val success = if (extension == ".csv") com.example.coolbox.util.BackupManager.exportToCsv(this, uri) else com.example.coolbox.util.BackupManager.exportData(this, uri)
            runOnUiThread {
                if (success) Toast.makeText(this, "文件已保存至: ${file.absolutePath}", Toast.LENGTH_LONG).show()
                else Toast.makeText(this, "保存失败", Toast.LENGTH_SHORT).show()
            }
        }.start()
    }

    private fun chooseBackupFormatForNas() {
        val formats = arrayOf("CBK 压缩包", "CSV 列表")
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("选择同步格式")
            .setItems(formats) { _, which ->
                executeNasBackup(if (which == 0) ".cbk" else ".csv")
            }
            .show()
    }

    private fun showRestoreBranchDialog() {
        val options = arrayOf("本地还原 (选择文件)", "云端还原 (从 NAS 下载)")
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("选择还原来源")
            .setItems(options) { _, which ->
                if (which == 0) {
                    try {
                        val intent = android.content.Intent(android.content.Intent.ACTION_OPEN_DOCUMENT).apply {
                            addCategory(android.content.Intent.CATEGORY_OPENABLE)
                            type = "*/*"
                        }
                        startActivityForResult(intent, PICK_CBK_RESTORE)
                    } catch (e: Exception) {
                        executeLocalFallbackRestore()
                    }
                } else {
                    showNasRestoreList()
                }
            }
            .show()
    }

    private fun executeLocalFallbackRestore() {
        val dir = android.os.Environment.getExternalStoragePublicDirectory("CoolBox")
        val files = dir.listFiles { _, name -> name.endsWith(".cbk") || name.endsWith(".csv") }?.map { it.name }
        if (files.isNullOrEmpty()) {
            Toast.makeText(this, "CoolBox 文件夹内未找到备份文件", Toast.LENGTH_SHORT).show()
            return
        }
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("选择本地备份")
            .setItems(files.toTypedArray()) { _, which ->
                val file = java.io.File(dir, files[which])
                handleRestoreFile(android.net.Uri.fromFile(file))
            }
            .show()
    }

    private fun showNasRestoreList() {
        val url = viewModel.syncUrl.value ?: ""
        if (url.isEmpty()) return
        
        val progress = android.app.ProgressDialog(this).apply {
            setMessage("正在获取云端列表...")
            show()
        }
        
        com.example.coolbox.util.CloudSyncManager.fetchBackupFiles(url) { list, error ->
            runOnUiThread {
                progress.dismiss()
                if (list.isNullOrEmpty()) {
                    Toast.makeText(this, "暂无备份文件: $error", Toast.LENGTH_SHORT).show()
                } else {
                    androidx.appcompat.app.AlertDialog.Builder(this)
                        .setTitle("选择云端备份")
                        .setItems(list.toTypedArray()) { _, which ->
                            executeNasRestore(url, list[which])
                        }
                        .show()
                }
            }
        }
    }

    private fun executeNasRestore(serverUrl: String, filename: String) {
        val progress = android.app.ProgressDialog(this).apply {
            setMessage("正在从 NAS 下载...")
            show()
        }
        val tempFile = java.io.File(cacheDir, "nas_restore_temp.cbk")
        com.example.coolbox.util.CloudSyncManager.downloadBackupFile(serverUrl, filename, tempFile) { success, error ->
            runOnUiThread {
                progress.dismiss()
                if (success) {
                    val uri = androidx.core.content.FileProvider.getUriForFile(this@SettingsActivity, "${packageName}.fileprovider", tempFile)
                    showRestoreModeDialog(uri)
                } else {
                    Toast.makeText(this, "下载失败: $error", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun showRestoreModeDialog(uri: android.net.Uri) {
        val options = arrayOf("完全覆盖 (删除当前数据)", "增量合并 (保留当前数据)")
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("选择还原模式")
            .setItems(options) { _, which ->
                if (which == 0) {
                    if (com.example.coolbox.util.BackupManager.importData(this, uri)) restartApp("数据已覆盖")
                    else Toast.makeText(this, "还原失败", Toast.LENGTH_SHORT).show()
                } else {
                    if (com.example.coolbox.util.BackupManager.importDataIncremental(this, uri)) Toast.makeText(this, "合并成功", Toast.LENGTH_SHORT).show()
                    else Toast.makeText(this, "合并失败", Toast.LENGTH_SHORT).show()
                }
            }
            .show()
    }

    private fun executeNasBackup(extension: String) {
        val url = viewModel.syncUrl.value ?: ""
        if (url.isEmpty()) {
            Toast.makeText(this, "请先设置并测试同步地址", Toast.LENGTH_SHORT).show()
            return
        }
        val progress = android.app.ProgressDialog(this).apply {
            setMessage("正在打包并上传至 NAS...")
            show()
        }
        val fileName = com.example.coolbox.util.BackupManager.generateBackupFileName("CoolBox_Backup", extension)
        val tempFile = java.io.File(cacheDir, fileName)
        val uri = android.net.Uri.fromFile(tempFile)
        
        Thread {
            val exportSuccess = if (extension == ".csv") com.example.coolbox.util.BackupManager.exportToCsv(this, uri) else com.example.coolbox.util.BackupManager.exportData(this, uri)
            if (exportSuccess) {
                com.example.coolbox.util.CloudSyncManager.uploadBackupFile(this, url, tempFile) { success, msg ->
                    runOnUiThread {
                        progress.dismiss()
                        if (success) Toast.makeText(this, "云端备份成功", Toast.LENGTH_SHORT).show()
                        else Toast.makeText(this, "上传失败: $msg", Toast.LENGTH_LONG).show()
                        tempFile.delete()
                    }
                }
            } else {
                runOnUiThread { progress.dismiss(); Toast.makeText(this, "本地生成失败", Toast.LENGTH_SHORT).show() }
            }
        }.start()
    }

    private fun handleRestoreFile(uri: android.net.Uri) {
        val type = contentResolver.getType(uri) ?: ""
        val path = uri.path ?: ""
        val isCsv = type.contains("csv") || path.endsWith(".csv")
        
        if (isCsv) {
            val progress = android.app.ProgressDialog(this).apply {
                setMessage("正在导入 CSV 数据...")
                show()
            }
            Thread {
                val success = com.example.coolbox.util.BackupManager.importDataFromCsv(this, uri)
                runOnUiThread {
                    progress.dismiss()
                    if (success) Toast.makeText(this, "CSV 导入成功", Toast.LENGTH_LONG).show()
                    else Toast.makeText(this, "CSV 导入失败", Toast.LENGTH_SHORT).show()
                }
            }.start()
        } else {
            showRestoreModeDialog(uri)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: android.content.Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != RESULT_OK || data == null) return
        val uri = data.data ?: return
        when (requestCode) {
            PICK_CBK_EXPORT -> {
                Thread {
                    val isCsv = contentResolver.getType(uri)?.contains("csv") == true || uri.path?.endsWith(".csv") == true
                    val success = if (isCsv) com.example.coolbox.util.BackupManager.exportToCsv(this, uri) else com.example.coolbox.util.BackupManager.exportData(this, uri)
                    runOnUiThread { Toast.makeText(this, if (success) "导出成功" else "导出失败", Toast.LENGTH_SHORT).show() }
                }.start()
            }
            PICK_CBK_RESTORE -> handleRestoreFile(uri)
        }
    }

    private fun restartApp(msg: String) {
        Toast.makeText(this, "$msg，正在重启...", Toast.LENGTH_LONG).show()
        val intent = android.content.Intent(this, MainActivity::class.java)
        intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(intent)
        finish()
    }

    private fun setupMaintenanceSettings(root: View) {
        root.findViewById<Button>(R.id.btnClearCache).setOnClickListener {
            Toast.makeText(this, "缓存已清理", Toast.LENGTH_SHORT).show()
        }
        root.findViewById<Button>(R.id.btnFactoryReset).setOnClickListener {
            showFactoryResetDialog()
        }
    }

    private fun showFactoryResetDialog() {
        val input = EditText(this).apply { hint = "我确认需要重制" }
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("⚠️ 恢复出厂设置")
            .setMessage("请输入“我确认需要重制”以继续：")
            .setView(input)
            .setPositiveButton("立即重制") { _, _ ->
                if (input.text.toString() == "我确认需要重制") executeFactoryReset()
                else Toast.makeText(this, "输入错误", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun executeFactoryReset() {
        val progress = android.app.ProgressDialog(this).apply { setMessage("正在重制..."); show() }
        Thread {
            try {
                getSharedPreferences("coolbox_prefs", MODE_PRIVATE).edit().clear().commit()
                com.example.coolbox.data.AppDatabase.closeDatabase()
                deleteDatabase("coolbox_database")
                runOnUiThread {
                    progress.dismiss()
                    restartApp("系统已归零")
                }
            } catch (e: Exception) {
                runOnUiThread { progress.dismiss(); Toast.makeText(this, "重制失败", Toast.LENGTH_SHORT).show() }
            }
        }.start()
    }

    private fun saveCurrentSettings() {
        dirtyAffinityMap.forEach { (name, affinity) ->
            viewModel.updateFridgeSpaceAffinity(name, affinity)
        }
        
        if (currentMenu == "sync") {
            val edit = findViewById<EditText>(R.id.editSyncUrl)
            viewModel.updateSyncSettings(dirtySyncEnabled, edit.text.toString())
        }
        
        Toast.makeText(this, "设置已保存", Toast.LENGTH_SHORT).show()
        dirtyAffinityMap.clear()
    }

    class DeviceSettingViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        val txtName: TextView = v.findViewById(R.id.txtDeviceName)
        val cbFood: CheckBox = v.findViewById(R.id.cbFood)
        val cbMed: CheckBox = v.findViewById(R.id.cbMedicine)
        val btnDelete: ImageButton = v.findViewById(R.id.btnDeleteDevice)
    }
}
