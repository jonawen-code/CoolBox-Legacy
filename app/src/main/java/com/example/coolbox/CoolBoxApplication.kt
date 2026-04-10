package com.example.coolbox

import android.app.Application
import com.example.coolbox.data.AppDatabase
import com.example.coolbox.data.FoodRepository

class CoolBoxApplication : Application() {
    // 改为动态获取，避免数据库重制后单例持有已被关闭的旧连接池
    val database: AppDatabase
        get() = AppDatabase.getDatabase(this)
        
    val repository: FoodRepository
        get() = FoodRepository(database.foodDao())
}
