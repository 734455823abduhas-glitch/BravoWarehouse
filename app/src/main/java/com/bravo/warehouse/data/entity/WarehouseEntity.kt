package com.bravo.warehouse.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "warehouses",
    indices = [
        Index(value = ["warehouseCode"], unique = true),
        Index(value = ["warehouseName"])
    ]
)
data class WarehouseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val warehouseCode: String,
    val warehouseName: String,
    val location: String = "",
    val description: String = "",
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
