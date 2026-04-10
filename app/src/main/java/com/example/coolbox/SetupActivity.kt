package com.example.coolbox

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.coolbox.ui.MainViewModel
import com.example.coolbox.util.BackupManager
import com.example.coolbox.util.CloudSyncManager
import com.example.coolbox.data.AppDatabase
import com.example.coolbox.legacy.BuildConfig
import com.example.coolbox.legacy.R

class SetupActivity : AppCompatActivity() {

    private lateinit var viewModel: MainViewModel
    private val PICK_CBK_RESTORE = 1001
    private val PICK_CBK_EXPORT = 1002

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setup)

        viewModel = ViewModelProvider(this).get(MainViewModel::class.java)

        val syncSwitch = findViewById<androidx.appcompat.widget.SwitchCompat>(R.id.syncEnabledSwitch)
        val syncUrlInput = findViewById<EditText>(R.id.syncUrlInput)
        val btnTest = findViewById<Button>(R.id.btnTestConnection)
        val btnComplete = findViewById<Button>(R.id.completeBtn)
        val txtVersion = findViewById<TextView>(R.id.txtAppVersion)

        // Load current settings
        syncSwitch.isChecked = viewModel.syncEnabled.value ?: false
        syncUrlInput.setText(viewModel.syncUrl.value ?: "")
        txtVersion.text = "V${BuildConfig.VERSION_NAME}\n4.0 全能物资空间版"

        // 1. Factory Reset
        findViewById<Button>(R.id.btnFactoryReset).setOnClickListener {
            showFactoryResetDialog()
        }

        // 2. Backup & Restore
        findViewById<Button>(R.id.btnBackup).setOnClickListener {
            showBackupBranchDialog()
        }

        findViewById<Button>(R.id.btnRestore).setOnClickListener {
            showRestoreBranchDialog()
        }

        // 3. Sync Settings
        btnTest.setOnClickListener {
            val url = syncUrlInput.text.toString().trim()
            if (url.isEmpty()) {
                Toast.makeText(this, "请输入服务器地址", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            CloudSyncManager.testConnection(url) { success, error ->
                runOnUiThread {
                    if (success) {
                        Toast.makeText(this, "✅ 连接成功！", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "❌ 连接失败: $error", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }

        btnComplete.setOnClickListener {
            val enabled = syncSwitch.isChecked
            val url = syncUrlInput.text.toString().trim()
            
            // Save settings and mark as complete
            getSharedPreferences("coolbox_prefs", MODE_PRIVATE).edit()
                .putBoolean("setup_complete", true)
                .apply()

            viewModel.updateSyncSettings(enabled, url)
            Toast.makeText(this, "设置已保存", Toast.LENGTH_SHORT).show()
            
            // Restart MainActivity since it might have been finished
            val intent = Intent(this, MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            startActivity(intent)
            finish()
        }
    }

    private fun showFactoryResetDialog() {
        val input = EditText(this).apply {
            hint = "我确认需要重制"
        }
        AlertDialog.Builder(this)
            .setTitle("⚠️ 危险操作：恢复出厂设置")
            .setMessage("此操作将抹除所有本地食品数据、存储设备设置及分类配置。此操作无法撤销！\n\n请输入“我确认需要重制”以继续：")
            .setView(input)
            .setPositiveButton("立即重制") { _, _ ->
                if (input.text.toString() == "我确认需要重制") {
                    executeFactoryReset()
                } else {
                    Toast.makeText(this, "输入错误，取消重制", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun executeFactoryReset() {
        val progress = android.app.ProgressDialog(this).apply {
            setMessage("正在重制系统...")
            show()
        }
        
        Thread {
            try {
                // 1. Clear SharedPreferences
                getSharedPreferences("coolbox_prefs", MODE_PRIVATE).edit().clear().commit()
                
                // 2. Delete Database
                AppDatabase.closeDatabase()
                deleteDatabase("coolbox_database")
                
                runOnUiThread {
                    progress.dismiss()
                    Toast.makeText(this, "系统已重制，正在退出...", Toast.LENGTH_LONG).show()
                    val intent = Intent(this, MainActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    startActivity(intent)
                    finish()
                }
            } catch (e: Exception) {
                runOnUiThread {
                    progress.dismiss()
                    Toast.makeText(this, "重制失败: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    private fun showBackupBranchDialog() {
        val options = arrayOf("本地导出 (CBK 压缩包)", "本地导出 (CSV 列表)", "云端备份到 NAS")
        AlertDialog.Builder(this)
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
            val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "application/zip"
                putExtra(android.content.Intent.EXTRA_TITLE, BackupManager.generateBackupFileName("CoolBox_Backup", ".cbk"))
            }
            startActivityForResult(intent, PICK_CBK_EXPORT)
        } catch (e: Exception) {
            executeLocalFallbackBackup(".cbk")
        }
    }

    private fun startCsvExport() {
        try {
            val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "text/csv"
                putExtra(android.content.Intent.EXTRA_TITLE, BackupManager.generateBackupFileName("CoolBox_Data", ".csv"))
            }
            startActivityForResult(intent, PICK_CBK_EXPORT) // Reuse code, distinguish by Uri later
        } catch (e: Exception) {
            executeLocalFallbackBackup(".csv")
        }
    }

    private fun executeLocalFallbackBackup(extension: String) {
        val dir = android.os.Environment.getExternalStoragePublicDirectory("CoolBox")
        if (!dir.exists()) dir.mkdirs()
        val fileName = BackupManager.generateBackupFileName("CoolBox_Export", extension)
        val file = java.io.File(dir, fileName)
        val uri = Uri.fromFile(file)
        
        Thread {
            val success = if (extension == ".csv") BackupManager.exportToCsv(this, uri) else BackupManager.exportData(this, uri)
            runOnUiThread {
                if (success) Toast.makeText(this, "文件已保存至: ${file.absolutePath}", Toast.LENGTH_LONG).show()
                else Toast.makeText(this, "保存失败", Toast.LENGTH_SHORT).show()
            }
        }.start()
    }

    private fun chooseBackupFormatForNas() {
        val formats = arrayOf("CBK 压缩包", "CSV 列表")
        AlertDialog.Builder(this)
            .setTitle("选择同步格式")
            .setItems(formats) { _, which ->
                executeNasBackup(if (which == 0) ".cbk" else ".csv")
            }
            .show()
    }

    private fun showRestoreBranchDialog() {
        val options = arrayOf("本地还原 (选择文件)", "云端还原 (从 NAS 下载)")
        AlertDialog.Builder(this)
            .setTitle("选择还原来源")
            .setItems(options) { _, which ->
                if (which == 0) {
                    try {
                        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                            addCategory(Intent.CATEGORY_OPENABLE)
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
        AlertDialog.Builder(this)
            .setTitle("选择本地备份")
            .setItems(files.toTypedArray()) { _, which ->
                val file = java.io.File(dir, files[which])
                handleRestoreFile(Uri.fromFile(file))
            }
            .show()
    }

    private fun showRestoreModeDialog(uri: Uri) {
        val options = arrayOf("完全覆盖 (删除当前数据)", "增量合并 (保留当前数据)")
        AlertDialog.Builder(this)
            .setTitle("选择还原模式")
            .setItems(options) { _, which ->
                if (which == 0) {
                    if (BackupManager.importData(this, uri)) restartApp("数据已覆盖")
                    else Toast.makeText(this, "还原失败", Toast.LENGTH_SHORT).show()
                } else {
                    if (BackupManager.importDataIncremental(this, uri)) Toast.makeText(this, "合并成功", Toast.LENGTH_SHORT).show()
                    else Toast.makeText(this, "合并失败", Toast.LENGTH_SHORT).show()
                }
            }
            .show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != RESULT_OK || data == null) return
        val uri = data.data ?: return
        when (requestCode) {
            PICK_CBK_EXPORT -> {
                Thread {
                    val isCsv = contentResolver.getType(uri)?.contains("csv") == true || uri.path?.endsWith(".csv") == true
                    val success = if (isCsv) BackupManager.exportToCsv(this, uri) else BackupManager.exportData(this, uri)
                    runOnUiThread { Toast.makeText(this, if (success) "导出成功" else "导出失败", Toast.LENGTH_SHORT).show() }
                }.start()
            }
            PICK_CBK_RESTORE -> {
                handleRestoreFile(uri)
            }
        }
    }

    private fun handleRestoreFile(uri: Uri) {
        val type = contentResolver.getType(uri) ?: ""
        val path = uri.path ?: ""
        val isCsv = type.contains("csv") || path.endsWith(".csv")
        
        if (isCsv) {
            val progress = android.app.ProgressDialog(this).apply {
                setMessage("正在导入 CSV 数据...")
                show()
            }
            Thread {
                val success = BackupManager.importDataFromCsv(this, uri)
                runOnUiThread {
                    progress.dismiss()
                    if (success) Toast.makeText(this, "CSV 导入成功（增量合并模式）", Toast.LENGTH_LONG).show()
                    else Toast.makeText(this, "CSV 导入失败", Toast.LENGTH_SHORT).show()
                }
            }.start()
        } else {
            showRestoreModeDialog(uri)
        }
    }

    private fun executeNasBackup(extension: String) {
        val url = viewModel.syncUrl.value ?: ""
        if (url.isEmpty()) {
            Toast.makeText(this, "请先设置并测试同步地址", Toast.LENGTH_SHORT).show()
            return
        }
        
        // 确保路径中包含 /backup，但不重复添加
        var baseUrl = url.trim()
        if (baseUrl.endsWith("/")) baseUrl = baseUrl.substring(0, baseUrl.length - 1)
        val targetPath = if (baseUrl.endsWith("/backup")) baseUrl else "$baseUrl/backup"

        AlertDialog.Builder(this)
            .setTitle("确认备份至云端")
            .setMessage("文件将上传至：\n$targetPath\n\n注意：App 将尝试使用与同步数据库相同的协议进行传输。")
            .setPositiveButton("开始上传") { _, _ ->
                val progress = android.app.ProgressDialog(this).apply {
                    setMessage("正在打包并上传至 NAS...")
                    show()
                }
                val fileName = BackupManager.generateBackupFileName("CoolBox_Backup", extension)
                val tempFile = java.io.File(cacheDir, fileName)
                val uri = Uri.fromFile(tempFile)
                
                Thread {
                    val exportSuccess = if (extension == ".csv") BackupManager.exportToCsv(this, uri) else BackupManager.exportData(this, uri)
                    if (exportSuccess) {
                        CloudSyncManager.uploadBackupFile(this, url, tempFile) { success, msg ->
                            runOnUiThread {
                                progress.dismiss()
                                if (success) Toast.makeText(this, "云端备份成功", Toast.LENGTH_SHORT).show()
                                else Toast.makeText(this, "上传失败: $msg", Toast.LENGTH_LONG).show()
                                tempFile.delete()
                            }
                        }
                    } else {
                        runOnUiThread { 
                            progress.dismiss()
                            Toast.makeText(this, "本地生成失败", Toast.LENGTH_SHORT).show()
                        }
                    }
                }.start()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showNasRestoreList() {
        val url = viewModel.syncUrl.value ?: ""
        if (url.isEmpty()) return
        
        val progress = android.app.ProgressDialog(this).apply {
            setMessage("正在获取云端列表...")
            show()
        }
        
        CloudSyncManager.fetchBackupFiles(url) { list, error ->
            runOnUiThread {
                progress.dismiss()
                if (list.isNullOrEmpty()) {
                    Toast.makeText(this, "暂无备份文件: $error", Toast.LENGTH_SHORT).show()
                } else {
                    AlertDialog.Builder(this)
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
        CloudSyncManager.downloadBackupFile(serverUrl, filename, tempFile) { success, error ->
            runOnUiThread {
                progress.dismiss()
                if (success) {
                    val uri = androidx.core.content.FileProvider.getUriForFile(this@SetupActivity, "${packageName}.fileprovider", tempFile)
                    showRestoreModeDialog(uri)
                } else {
                    Toast.makeText(this, "下载失败: $error", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun restartApp(msg: String) {
        Toast.makeText(this, "$msg，正在重启...", Toast.LENGTH_LONG).show()
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(intent)
        finish()
    }
}
