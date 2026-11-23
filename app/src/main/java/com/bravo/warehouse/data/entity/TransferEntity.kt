package com.bravo.warehouse.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "stock_transfers",
    indices = [
        Index(value = ["transferNumber"], unique = true),
        Index(value = ["itemId"]),
        Index(value = ["sourceWarehouseId"]),
        Index(value = ["destinationWarehouseId"])
    ]
)
data class TransferEntity(

    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val transferNumber: String,

    val itemId: Long,

    val sourceWarehouseId: Long,

    val destinationWarehouseId: Long,

    val quantity: Double,

    val notes: String = "",

    val transferDate: Long = System.currentTimeMillis(),

    val createdAt: Long = System.currentTimeMillis(),

    val isApproved: Boolean = false
)
