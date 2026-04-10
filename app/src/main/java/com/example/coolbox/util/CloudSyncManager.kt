// Version: V3.0.0-Pre28 (Network Restored)
package com.example.coolbox.util

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.example.coolbox.data.AppDatabase
import com.example.coolbox.data.FoodEntity
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import okhttp3.*
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

object CloudSyncManager {
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()
    private val gson = Gson()

    data class BackupInfo(val filename: String, val time: String)

    private fun getRemoteBase(serverUrl: String): String {
        var base = serverUrl.trim()
        if (base.endsWith("/")) base = base.substring(0, base.length - 1)
        return if (base.endsWith("/coolbox")) base else "$base/coolbox"
    }

    /**
     * 【PULL 逻辑】下载主数据库 sync.db
     */
    fun downloadDatabase(context: Context, serverUrl: String, onComplete: (Boolean, String) -> Unit) {
        val nasBase = getRemoteBase(serverUrl)
        Thread {
            try {
                val request = Request.Builder().url(nasBase).get().build()
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        onComplete(false, "NAS访问失败: ${response.code()}")
                        return@Thread
                    }
                    val exportDir = android.os.Environment.getExternalStoragePublicDirectory("CoolBox")
                    if (!exportDir.exists()) exportDir.mkdirs()
                    val targetFile = File(exportDir, "sync.db")
                    response.body()?.byteStream()?.use { input ->
                        targetFile.outputStream().use { output -> input.copyTo(output) }
                    }
                    onComplete(true, "同步成功")
                }
            } catch (e: Exception) {
                onComplete(false, "网络错误: ${e.localizedMessage}")
            }
        }.start()
    }

    /**
     * 【PUSH 逻辑】上传主数据库 sync.db (包装器)
     */
    fun uploadDatabase(context: Context, serverUrl: String, onComplete: (Boolean) -> Unit = {}) {
        forceUploadDatabaseFile(context, serverUrl) { success, _ -> onComplete(success) }
    }

    /**
     * 【PUSH 逻辑】上传主数据库 sync.db
     */
    fun forceUploadDatabaseFile(context: Context, serverUrl: String, onComplete: (Boolean, String) -> Unit) {
        val nasBase = getRemoteBase(serverUrl)
        val exportDir = android.os.Environment.getExternalStoragePublicDirectory("CoolBox")
        val dbFile = File(exportDir, "sync.db")

        if (!dbFile.exists()) {
            onComplete(false, "本地 sync.db 不存在")
            return
        }

        Thread {
            try {
                val requestBody = RequestBody.create(MediaType.parse("application/octet-stream"), dbFile)
                val request = Request.Builder().url(nasBase).post(requestBody).build()
                client.newCall(request).execute().use { 
                    onComplete(it.isSuccessful, if(it.isSuccessful) "OK" else "Error ${it.code()}")
                }
            } catch (e: Exception) {
                onComplete(false, e.localizedMessage ?: "Network Error")
            }
        }.start()
    }

    /**
     * 【NAS 远程备份】上传备份文件 (支持 .cbk 和 .csv)
     */
    fun uploadBackupFile(context: Context, serverUrl: String, file: File, onComplete: (Boolean, String?) -> Unit) {
        val fileBytes = file.readBytes()
        // 既然 sync.db 能上传，我们就直接沿用最简单的逻辑，不对 URL 做二次 getRemoteBase 处理
        // 这里的 serverUrl 已经在 SetupActivity 中拼好了具体的目的地
        val nasBase = getRemoteBase(serverUrl)
        // 既然 sync.db 通过 nasBase 上传，备份文件则通过 /api/backups 上传
        val finalUrl = if (nasBase.contains("?")) "$nasBase/api/backups&filename=${file.name}" else "$nasBase/api/backups?filename=${file.name}"
        
        Thread {
            try {
                val requestBody = RequestBody.create(MediaType.parse("application/octet-stream"), fileBytes)
                val request = Request.Builder()
                    .url(finalUrl)
                    .post(requestBody)
                    .addHeader("X-Filename", file.name)
                    .build()
                
                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        onComplete(true, "成功: ${file.name}")
                    } else {
                        onComplete(false, "HTTP ${response.code()} @ ${finalUrl}")
                    }
                }
            } catch (e: Exception) {
                onComplete(false, "网络错误: ${e.localizedMessage}")
            }
        }.start()
    }

    /**
     * 【NAS 远程备份】获取备份列表 (支持 .cbk 和 .csv)
     */
    fun fetchBackupFiles(serverUrl: String, onComplete: (List<String>?, String?) -> Unit) {
        val url = "${getRemoteBase(serverUrl)}/api/backups"
        Thread {
            try {
                val request = Request.Builder().url(url).get().build()
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        onComplete(null, "HTTP ${response.code()}")
                        return@Thread
                    }
                    val json = response.body()?.string() ?: "[]"
                    val result = try {
                        val list: List<String> = gson.fromJson(json, object : TypeToken<List<String>>() {}.type)
                        list.filter { it.endsWith(".cbk") || it.endsWith(".csv") }
                    } catch (e: Exception) {
                        val list: List<BackupInfo> = gson.fromJson(json, object : TypeToken<List<BackupInfo>>() {}.type)
                        list.map { it.filename }.filter { it.endsWith(".cbk") || it.endsWith(".csv") }
                    }
                    onComplete(result, null)
                }
            } catch (e: Exception) { onComplete(null, e.localizedMessage) }
        }.start()
    }

    /**
     * 【NAS 远程备份】下载 .cbk 文件
     */
    fun downloadBackupFile(serverUrl: String, filename: String, targetFile: File, onComplete: (Boolean, String?) -> Unit) {
        val url = "${getRemoteBase(serverUrl)}/api/backups/download?filename=$filename"
        Thread {
            try {
                val request = Request.Builder().url(url).get().build()
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        onComplete(false, "Download error: ${response.code()}")
                        return@Thread
                    }
                    response.body()?.byteStream()?.use { input ->
                        targetFile.outputStream().use { output -> input.copyTo(output) }
                    }
                    onComplete(true, null)
                }
            } catch (e: Exception) { onComplete(false, e.localizedMessage) }
        }.start()
    }

    /**
     * 测试服务器连接
     */
    fun testConnection(serverUrl: String, onComplete: (Boolean, String?) -> Unit) {
        val url = "${getRemoteBase(serverUrl)}/api/backups"
        Thread {
            try {
                val request = Request.Builder().url(url).get().build()
                client.newCall(request).execute().use { response ->
                    onComplete(response.isSuccessful, if (response.isSuccessful) null else "HTTP ${response.code()}")
                }
            } catch (e: Exception) {
                onComplete(false, e.localizedMessage)
            }
        }.start()
    }
}