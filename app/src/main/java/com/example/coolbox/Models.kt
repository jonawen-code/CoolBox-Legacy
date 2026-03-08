package com.example.coolbox

import java.io.Serializable

data class InventoryItem(
    val id: String,
    val icon: String,
    val name: String,
    val fridgeName: String,
    val inputDateMs: Long,
    val expiryDateMs: Long,
    val quantity: Double,
    val weightPerPortion: Double,
    val unit: String
) : Serializable

data class CatalogItem(
    val category: String,
    val name: String
) : Serializable
