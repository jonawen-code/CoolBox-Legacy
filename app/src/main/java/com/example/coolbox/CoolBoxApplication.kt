package com.example.coolbox

import android.app.Application
import com.example.coolbox.data.AppDatabase
import com.example.coolbox.data.FoodRepository

class CoolBoxApplication : Application() {
    val database by lazy { AppDatabase.getDatabase(this) }
    val repository by lazy { FoodRepository(database.foodDao()) }
}
