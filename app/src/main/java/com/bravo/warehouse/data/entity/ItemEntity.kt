package com.bravo.warehouse.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "items",
    indices = [
        Index(value = ["itemCode"], unique = true),
        Index(value = ["itemName"])
    ]
)
data class ItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val itemCode: String,
    val itemName: String,
    val category: String,
    val unit: String,

    val minimumStock: Double = 0.0,
    val description: String = "",
    val isActive: Boolean = true,

    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
