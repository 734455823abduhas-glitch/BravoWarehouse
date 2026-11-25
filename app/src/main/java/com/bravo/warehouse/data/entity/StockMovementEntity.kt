package com.bravo.warehouse.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "stock_movements",
    indices = [
        Index(value = ["itemId"]),
        Index(value = ["warehouseId"]),
        Index(value = ["movementType"]),
        Index(value = ["documentNumber"])
    ]
)
data class StockMovementEntity(

    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val documentNumber: String,
    val movementType: String,
    val itemId: Long,
    val warehouseId: Long,
    val quantity: Double,
    val notes: String = "",
    val movementDate: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis(),
    val createdBy: String = "",
    val isApproved: Boolean = false,

    val recipientName: String = "",
    val department: String = ""
)
