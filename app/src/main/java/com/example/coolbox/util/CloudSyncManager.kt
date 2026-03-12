// Version: V2.0
package com.example.coolbox.util

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.example.coolbox.data.AppDatabase
import com.example.coolbox.data.FoodEntity
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import okhttp3.*
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object CloudSyncManager {
    @Volatile
    private var isSyncing = false
    @Volatile
    private var isUploading = false

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

    private fun getLastSyncMs(context: Context): Long {
        return context.getSharedPreferences("settings", Context.MODE_PRIVATE).getLong("last_sync_ms", 0L)
    }

    private fun setLastSyncMs(context: Context, ms: Long) {
        context.getSharedPreferences("settings", Context.MODE_PRIVATE).edit().putLong("last_sync_ms", ms).apply()
    }

    fun uploadDatabase(context: Context, serverUrl: String, onComplete: (Boolean) -> Unit = {}) {
        if (isUploading) return
        isUploading = true
        
        val base = if (serverUrl.endsWith("/")) serverUrl.substring(0, serverUrl.length - 1) else serverUrl
        val nasBase = if (base.contains("/coolbox")) base else "$base/coolbox"

        Thread {
            try {
                Log.d("CloudSync", "NAS Push starting: $nasBase")
                
                val lastSyncMs = getLastSyncMs(context)
                val dao = AppDatabase.getDatabase(context).foodDao()
                
                val allItems = kotlinx.coroutines.runBlocking { dao.getAllIncludeDeleted() }
                val increments = allItems.filter { it.lastModifiedMs > lastSyncMs }
                
                if (increments.isEmpty()) {
                    Log.d("CloudSync", "No local increments to push.")
                    onComplete(true)
                    return@Thread
                }

                val jsonContent = gson.toJson(increments)
                val timestamp = System.currentTimeMillis()
                val filename = "sync_pad_$timestamp.json"
                
                val requestBody = RequestBody.create(MediaType.parse("application/json"), jsonContent)
                val request = Request.Builder()
                    .url("$nasBase/sync/upload/$filename")
                    .post(requestBody)
                    .cacheControl(CacheControl.FORCE_NETWORK)
                    .build()

                client.newCall(request).execute().use { response ->
                    val bodyStr = response.body()?.string() ?: ""
                    val isJsonSuccess = try {
                        JSONObject(bodyStr).optString("status") == "success"
                    } catch(e: Exception) { false }

                    if (response.isSuccessful() && isJsonSuccess) {
                        setLastSyncMs(context, timestamp)
                        Log.d("CloudSync", "NAS Push success: $filename")
                        onComplete(true)
                    } else {
                        val err = "NAS Push failed | Code: ${response.code()} | URL: ${request.url()} | Resp: $bodyStr"
                        Log.e("CloudSync", err)
                        onComplete(false)
                    }
                }
            } catch (e: Exception) {
                Log.e("CloudSync", "NAS Push Exception | URL: $nasBase", e)
                onComplete(false)
            } finally {
                isUploading = false
            }
        }.start()
    }

    fun downloadDatabase(context: Context, serverUrl: String, onComplete: (Boolean) -> Unit) {
        if (isSyncing) {
            Log.w("CloudSync", "Sync in progress, skipping.")
            return
        }
        isSyncing = true
        val base = if (serverUrl.endsWith("/")) serverUrl.substring(0, serverUrl.length - 1) else serverUrl
        val nasBase = if (base.contains("/coolbox")) base else "$base/coolbox"
        
        Thread {
            try {
                Log.d("CloudSync", "NAS Pull starting: $nasBase")
                val lastSyncMs = getLastSyncMs(context)
                
                // 1. Get All Remote Items from /sync/list (Spec v1.2)
                val url = "$nasBase/sync/list"
                val listRequest = Request.Builder()
                    .url(url)
                    .get()
                    .cacheControl(CacheControl.FORCE_NETWORK)
                    .build()
                    
                val remoteItems = mutableListOf<FoodEntity>()
                client.newCall(listRequest).execute().use { response ->
                    if (!response.isSuccessful()) {
                        Log.e("CloudSync", "NAS List failed | Code: ${response.code()} | URL: $url")
                        onComplete(false); return@Thread
                    }
                    val jsonStr = response.body()?.string() ?: ""
                    if (jsonStr.isNotBlank()) {
                        val type = object : TypeToken<List<FoodEntity>>() {}.type
                        try {
                            val items: List<FoodEntity> = gson.fromJson(jsonStr, type) ?: emptyList()
                            remoteItems.addAll(items.filterNotNull())
                        } catch (e: Exception) {
                            Log.e("CloudSync", "NAS Parse failed | URL: $url", e)
                        }
                    }
                    Unit
                }
                
                // 2. BACKUP & MERGE in Transaction
                val db = AppDatabase.getDatabase(context)
                val dao = db.foodDao()
                
                // CRITICAL: Memory Backup before any mutations
                val localSnapshot = kotlinx.coroutines.runBlocking { dao.getAllIncludeDeleted() }
                Log.d("CloudSync", "[AUDIT] Local Snapshot size: ${localSnapshot.size} items")
                
                var importedCount = 0
                var skippedCount = 0
                var updatedCount = 0
                
                db.runInTransaction {
                    remoteItems.forEach { remote ->
                        val local = localSnapshot.find { it.id == remote.id }
                        val now = System.currentTimeMillis()
                        
                        // Safety: ignore items with impossible future dates (> 2 years from now)
                        if (remote.lastModifiedMs > now + (365L * 24 * 3600 * 1000 * 2)) {
                            Log.e("CloudSync", "[SECURITY] Rejection: Item ${remote.name} has suspicious timestamp: ${remote.lastModifiedMs}")
                            return@forEach
                        }

                        if (local == null) {
                            Log.d("CloudSync", "[MERGE] New: ${remote.name} (ID: ${remote.id})")
                            kotlinx.coroutines.runBlocking { dao.insertItem(remote) }
                            importedCount++
                        } else if (remote.lastModifiedMs > local.lastModifiedMs) {
                            Log.d("CloudSync", "[MERGE] Update: ${remote.name} | Remote TS ${remote.lastModifiedMs} > Local TS ${local.lastModifiedMs}")
                            kotlinx.coroutines.runBlocking { dao.insertItem(remote) }
                            updatedCount++
                        } else {
                            Log.v("CloudSync", "[MERGE] Skip: ${remote.name} | Local is newer or same")
                            skippedCount++
                        }
                    }
                }
                
                Log.d("CloudSync", "[AUDIT] Sync Result: $importedCount imported, $updatedCount updated, $skippedCount skipped.")
                val mergedCountTotal = importedCount + updatedCount

                // 3. Now UPLOAD our own increments (Push)
                uploadDatabase(context, serverUrl) { uploadSuccess ->
                    android.os.Handler(android.os.Looper.getMainLooper()).post { 
                        if (uploadSuccess) {
                            Toast.makeText(context, "同步成功: 导入 $mergedCountTotal 条 (跳过 $skippedCount)", Toast.LENGTH_SHORT).show()
                            onComplete(true)
                        } else {
                            // If upload fails, we still effectively merged locally
                            Toast.makeText(context, "合并完成，但推送到 NAS 失败", Toast.LENGTH_SHORT).show()
                            onComplete(true) // Still return true for UI refresh purposes since download succeeded
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("CloudSync", "V2.0 Pull blocked by error", e)
                val errorMsg = when {
                    e is java.net.SocketTimeoutException -> "网络超时 (熔断)，请检查连接"
                    e is java.net.ConnectException -> "服务器连接失败，请检查设置"
                    else -> "同步异常: ${e.localizedMessage}"
                }
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    Toast.makeText(context, errorMsg, Toast.LENGTH_SHORT).show()
                }
                onComplete(false)
            } finally {
                isSyncing = false
            }
        }.start()
    }
}
// Version: V2.0
