// Version: V3.0.0-Pre21
package com.example.coolbox.util

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.example.coolbox.data.AppDatabase
import com.example.coolbox.data.FoodEntity
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import okhttp3.*
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object CloudSyncManager {
    @Volatile
    private var isSyncing = false
    @Volatile
    private var isUploading = false
    @Volatile
    private var isRestoring = false

    data class BackupInfo(val filename: String, val time: String)

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

    private fun getLastSyncMs(context: Context): Long {
        return context.getSharedPreferences("coolbox_prefs", Context.MODE_PRIVATE).getLong("last_sync_ms", 0L)
    }

    private fun setLastSyncMs(context: Context, ms: Long) {
        context.getSharedPreferences("coolbox_prefs", Context.MODE_PRIVATE).edit().putLong("last_sync_ms", ms).apply()
    }

    private fun getRemoteBase(serverUrl: String): String {
        val base = if (serverUrl.endsWith("/")) serverUrl.substring(0, serverUrl.length - 1) else serverUrl
        return if (base.contains("/coolbox")) base else "$base/coolbox"
    }

    fun uploadConfig(context: Context, serverUrl: String, onComplete: (Boolean) -> Unit = {}) {
        val nasBase = getRemoteBase(serverUrl)
        val prefs = context.getSharedPreferences("coolbox_prefs", Context.MODE_PRIVATE)
        
        // V3.0.0-Pre17-Refined: Switch to "fridges" key per Architect instruction
        val fridgesJson: String = try {
            prefs.getString("fridges", "[]") ?: "[]"
        } catch (e: ClassCastException) {
            val set = prefs.getStringSet("fridges", emptySet()) ?: emptySet()
            gson.toJson(set)
        }

        val categoriesJson: String = try {
            prefs.getString("categories", "[]") ?: "[]"
        } catch (e: ClassCastException) {
            val set = prefs.getStringSet("categories", emptySet()) ?: emptySet()
            gson.toJson(set)
        }

        val capabilitiesJson: String = try {
            prefs.getString("capabilities", "[]") ?: "[]"
        } catch (e: ClassCastException) {
            val set = prefs.getStringSet("capabilities", emptySet()) ?: emptySet()
            gson.toJson(set)
        }
        
        // Transform to sync format
        val fridgeList = try {
            gson.fromJson<List<String>>(fridgesJson, object : TypeToken<List<String>>() {}.type) ?: emptyList()
        } catch (e: Exception) { emptyList() }
        
        // Architect Diagnostics
        Log.d("CloudSync", "Local fridge list size (key: fridges): ${fridgeList.size}")
        if (fridgeList.isEmpty()) {
            Log.w("CloudSync", "WARNING: fridgeList is EMPTY! uploadConfig may result in empty database on server.")
        }
        
        val categoryList = try {
            gson.fromJson<List<String>>(categoriesJson, object : TypeToken<List<String>>() {}.type) ?: emptyList()
        } catch (e: Exception) { emptyList() }
        
        val configData = JSONObject().apply {
            val fArray = JSONArray()
            fridgeList.forEach { name ->
                fArray.put(JSONObject().apply {
                    put("id", name)
                    put("name", name)
                    put("layers", "3") // Architect Fix: Default value "3"
                    // Architect Fix: Handle as JSONArray format
                    put("capabilities", try { JSONArray(capabilitiesJson) } catch(e: Exception) { JSONArray() })
                    put("lastModifiedMs", System.currentTimeMillis())
                })
            }
            put("fridges", fArray)
            
            val cArray = JSONArray()
            categoryList.forEach { name ->
                cArray.put(JSONObject().apply {
                    put("id", name)
                    put("name", name)
                    put("lastModifiedMs", System.currentTimeMillis())
                })
            }
            put("categories", cArray)
        }

        val body = RequestBody.create(MediaType.parse("application/json"), configData.toString())
        val request = Request.Builder().url("$nasBase/sync/config").post(body).build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: java.io.IOException) { onComplete(false) }
            override fun onResponse(call: Call, response: Response) { onComplete(response.isSuccessful) }
        })
    }

    fun downloadConfig(context: Context, serverUrl: String, onComplete: (Boolean) -> Unit) {
        val nasBase = getRemoteBase(serverUrl)
        val request = Request.Builder().url("$nasBase/sync/config").get().build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: java.io.IOException) { onComplete(false) }
            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!it.isSuccessful) { onComplete(false); return }
                    val body = it.body()?.string() ?: ""
                    try {
                        val obj = JSONObject(body)
                        val fridges = obj.getJSONArray("fridges")
                        val categories = obj.getJSONArray("categories")
                        
                        val fList = mutableListOf<String>()
                        for(i in 0 until fridges.length()) fList.add(fridges.getJSONObject(i).getString("name"))
                        
                        val cList = mutableListOf<String>()
                        for(i in 0 until categories.length()) cList.add(categories.getJSONObject(i).getString("name"))
                        
                        context.getSharedPreferences("coolbox_prefs", Context.MODE_PRIVATE).edit().apply {
                            putString("fridges", gson.toJson(fList))
                            putString("fridge_bases", gson.toJson(fList))
                            putString("categories", gson.toJson(cList))
                            apply()
                        }
                        onComplete(true)
                    } catch(e: Exception) { onComplete(false) }
                }
            }
        })
    }

    fun uploadDatabase(context: Context, serverUrl: String, onComplete: (Boolean) -> Unit = {}) {
        if (isUploading) return
        
        uploadConfig(context, serverUrl) { configSuccess ->
            isUploading = true
            val nasBase = getRemoteBase(serverUrl)

            Thread {
                try {
                    val dao = AppDatabase.getDatabase(context).foodDao()
                    val allItems = kotlinx.coroutines.runBlocking { dao.getAllIncludeDeleted() }
                    
                    val jsonContent = gson.toJson(allItems)
                    val timestamp = System.currentTimeMillis()
                    
                    // 核心修复：URL 更改为 /sync/update
                    val requestBody = RequestBody.create(MediaType.parse("application/json"), jsonContent)
                    val request = Request.Builder()
                        .url("$nasBase/sync/update") 
                        .header("X-Filename", "sync_pad_$timestamp.json")
                        .post(requestBody)
                        .build()

                    Log.d("CloudSync", "[Pre21] Attempting PUSH to: $nasBase/sync/update | Count: ${allItems.size}")

                    client.newCall(request).execute().use { response ->
                        if (response.isSuccessful) {
                            setLastSyncMs(context, timestamp)
                            onComplete(true)
                        } else {
                            Log.e("CloudSync", "NAS Upload failed | Code: ${response.code()}")
                            onComplete(false)
                        }
                    }
                } catch (e: Exception) {
                    Log.e("CloudSync", "Upload Thread Error", e)
                    onComplete(false)
                } finally {
                    isUploading = false
                }
            }.start()
        }
    }

    fun downloadDatabase(context: Context, serverUrl: String, onComplete: (Boolean) -> Unit) {
        if (isSyncing) {
            Log.w("CloudSync", "Sync in progress, skipping.")
            return
        }
        isSyncing = true
        val base = if (serverUrl.endsWith("/")) serverUrl.substring(0, serverUrl.length - 1) else serverUrl
        val nasBase = if (base.contains("/coolbox")) base else "$base/coolbox"
        
        // V3.0-Pre7: Ensure Push happens BEFORE Pull to trigger remote snapshot of current state
        Log.d("CloudSync", "[V3.0] Pre-sync upload starting to trigger NAS snapshot...")
        uploadDatabase(context, serverUrl) { _ ->
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
                    // Architect Fix: Mark Pull as done as long as request is successful (even if 0 items)
                    context.getSharedPreferences("coolbox_prefs", Context.MODE_PRIVATE).edit().putBoolean("is_pull_first_done", true).apply()
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
                        } else {
                            // V3.0.0-Pre11 Absolute Soft Delete Lock:
                            // Priority 1: If Cloud says deleted, local MUST be deleted.
                            // Priority 2: If Local says deleted, it STAYS deleted.
                            val shouldBeDeleted = local.isDeleted || remote.isDeleted
                            
                            if (shouldBeDeleted) {
                                // Force lock if not already synced or if cloud is newer deletion
                                if (!local.isDeleted || remote.isDeleted && remote.lastModifiedMs > local.lastModifiedMs) {
                                    Log.d("CloudSync", "[MERGE] Absolute Delete Lock: ${remote.name} (Forced)")
                                    kotlinx.coroutines.runBlocking { dao.insertItem(remote.copy(isDeleted = true)) }
                                    updatedCount++
                                } else {
                                    Log.v("CloudSync", "[MERGE] Delete Stay Locked: ${remote.name}")
                                    skippedCount++
                                }
                            } else if (remote.lastModifiedMs > local.lastModifiedMs) {
                                // Normal update logic for non-deleted items
                                Log.d("CloudSync", "[MERGE] Update: ${remote.name} | Remote newer")
                                kotlinx.coroutines.runBlocking { dao.insertItem(remote) }
                                updatedCount++
                             } else {
                                Log.v("CloudSync", "[MERGE] Skip: ${remote.name} | Same or older")
                                skippedCount++
                            }
                        }
                    }
                }
                
                Log.d("CloudSync", "[AUDIT] Sync Result: $importedCount imported, $updatedCount updated, $skippedCount skipped.")
                val mergedCountTotal = importedCount + updatedCount

                android.os.Handler(android.os.Looper.getMainLooper()).post { 
                    Toast.makeText(context, "同步成功: 导入 $mergedCountTotal 条 (跳过 $skippedCount)", Toast.LENGTH_SHORT).show()
                    onComplete(true)
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

    fun fetchBackups(serverUrl: String, onComplete: (List<BackupInfo>?, String?) -> Unit) {
        val base = if (serverUrl.endsWith("/")) serverUrl.substring(0, serverUrl.length - 1) else serverUrl
        val nasBase = if (base.contains("/coolbox")) base else "$base/coolbox"
        val url = "$nasBase/api/backups"

        Thread {
            try {
                val request = Request.Builder().url(url).get().cacheControl(CacheControl.FORCE_NETWORK).build()
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        onComplete(null, "Server error: ${response.code()}")
                        return@Thread
                    }
                    val jsonStr = response.body()?.string() ?: "[]"
                    val type = object : TypeToken<List<BackupInfo>>() {}.type
                    val list: List<BackupInfo> = gson.fromJson(jsonStr, type)
                    onComplete(list, null)
                }
            } catch (e: Exception) {
                onComplete(null, e.localizedMessage)
            }
        }.start()
    }

    fun restoreBackup(serverUrl: String, filename: String, onComplete: (Boolean, String?) -> Unit) {
        if (isRestoring) return
        isRestoring = true

        val base = if (serverUrl.endsWith("/")) serverUrl.substring(0, serverUrl.length - 1) else serverUrl
        val nasBase = if (base.contains("/coolbox")) base else "$base/coolbox"
        val url = "$nasBase/api/restore"

        Thread {
            try {
                val json = JSONObject().put("filename", filename)
                val body = RequestBody.create(MediaType.parse("application/json"), json.toString())
                val request = Request.Builder().url(url).post(body).cacheControl(CacheControl.FORCE_NETWORK).build()
                
                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        onComplete(true, null)
                    } else {
                        onComplete(false, "Restore failed: ${response.code()}")
                    }
                }
            } catch (e: Exception) {
                onComplete(false, e.localizedMessage)
            } finally {
                isRestoring = false
            }
        }.start()
    }
}
// Version: V3.0.0-Pre18
