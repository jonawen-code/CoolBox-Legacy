// Version: V3.0.0-Pre25 (Network Hardened)
package com.example.coolbox.util

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import com.example.coolbox.data.AppDatabase
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import okhttp3.*
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

object CloudSyncManager {
    @Volatile private var isUploading = false
    @Volatile private var isRestoring = false
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()
    private val gson = Gson()

    data class BackupInfo(val filename: String, val time: String)

    // 清洗 URL 的逻辑：确保不重复拼接 /coolbox
    private fun getRemoteBase(serverUrl: String): String {
        var base = serverUrl.trim()
        if (base.endsWith("/")) base = base.substring(0, base.length - 1)
        return if (base.endsWith("/coolbox")) base else "$base/coolbox"
    }

    private fun runOnMain(action: () -> Unit) {
        Handler(Looper.getMainLooper()).post(action)
    }

    /**
     * 【PULL 逻辑】下载数据库
     */
    fun downloadDatabase(context: Context, serverUrl: String, onComplete: (Boolean, String) -> Unit) {
        val nasBase = getRemoteBase(serverUrl)
        Log.d("CloudSync", ">>> PULL START: $nasBase")
        
        Thread {
            try {
                val request = Request.Builder().url(nasBase).get().build()
                client.newCall(request).execute().use { response ->
                    Log.d("CloudSync", ">>> PULL RESPONSE: ${response.code()}")
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
                    Log.d("CloudSync", ">>> PULL SUCCESS: File Saved")
                    onComplete(true, "同步成功")
                }
            } catch (e: Exception) {
                Log.e("CloudSync", ">>> PULL ERROR", e)
                onComplete(false, "连接失败: ${e.localizedMessage}")
            }
        }.start()
    }

    /**
     * 【PUSH 逻辑】上传数据库
     */
    fun uploadDatabase(context: Context, serverUrl: String, onComplete: (Boolean) -> Unit = {}) {
        if (isUploading) return
        val nasBase = getRemoteBase(serverUrl)
        Log.d("CloudSync", ">>> PUSH START: $nasBase")

        forceUploadDatabaseFile(context, serverUrl) { success, msg ->
            Log.d("CloudSync", ">>> PUSH RESULT: $success | $msg")
            onComplete(success)
        }
    }

    fun forceUploadDatabaseFile(context: Context, serverUrl: String, onComplete: (Boolean, String) -> Unit) {
        val nasBase = getRemoteBase(serverUrl)
        val exportDir = android.os.Environment.getExternalStoragePublicDirectory("CoolBox")
        val dbFile = File(exportDir, "sync.db")

        if (!dbFile.exists()) {
            onComplete(false, "本地 sync.db 不存在")
            return
        }

        isUploading = true
        Thread {
            try {
                val requestBody = RequestBody.create(MediaType.parse("application/octet-stream"), dbFile)
                val request = Request.Builder().url(nasBase).post(requestBody).build()
                client.newCall(request).execute().use { 
                    onComplete(it.isSuccessful, if(it.isSuccessful) "OK" else "Error ${it.code()}")
                }
            } catch (e: Exception) {
                onComplete(false, e.localizedMessage ?: "Network Error")
            } finally {
                isUploading = false
            }
        }.start()
    }

    // --- 以下为补回 SetupActivity 需要的函数，保持逻辑极简 ---
    fun downloadConfig(context: Context, serverUrl: String, onComplete: (Boolean) -> Unit) {
        val url = "${getRemoteBase(serverUrl)}/config"
        Thread {
            try {
                val request = Request.Builder().url(url).get().build()
                client.newCall(request).execute().use { onComplete(it.isSuccessful) }
            } catch (e: Exception) { onComplete(false) }
        }.start()
    }

    fun fetchBackups(serverUrl: String, onComplete: (List<BackupInfo>?, String?) -> Unit) {
        val url = "${getRemoteBase(serverUrl)}/api/backups"
        Thread {
            try {
                val request = Request.Builder().url(url).get().build()
                client.newCall(request).execute().use { response ->
                    val list: List<BackupInfo> = gson.fromJson(response.body()?.string() ?: "[]", object : TypeToken<List<BackupInfo>>() {}.type)
                    onComplete(list, null)
                }
            } catch (e: Exception) { onComplete(null, e.localizedMessage) }
        }.start()
    }

    fun restoreBackup(serverUrl: String, filename: String, onComplete: (Boolean, String?) -> Unit) {
        val url = "${getRemoteBase(serverUrl)}/api/restore"
        Thread {
            try {
                val body = RequestBody.create(MediaType.parse("application/json"), JSONObject().put("filename", filename).toString())
                val request = Request.Builder().url(url).post(body).build()
                client.newCall(request).execute().use { onComplete(it.isSuccessful, null) }
            } catch (e: Exception) { onComplete(false, e.localizedMessage) }
        }.start()
    }
}