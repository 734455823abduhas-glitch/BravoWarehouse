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

    // كود الصنف
    val itemCode: String,

    // اسم الصنف
    val itemName: String,

    // تصنيف الصنف
    val category: String,

    // وحدة القياس
    val unit: String,

    // الحد الأدنى للمخزون
    val minimumStock: Double = 0.0,

    // وصف الصنف
    val description: String = "",

    // حالة الصنف
    val isActive: Boolean = true,

    // تاريخ الإضافة
    val createdAt: Long = System.currentTimeMillis(),

    // تاريخ آخر تحديث
    val updatedAt: Long = System.currentTimeMillis()
)
