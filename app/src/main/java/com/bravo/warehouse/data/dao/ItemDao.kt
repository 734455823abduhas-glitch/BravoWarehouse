package com.bravo.warehouse.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.bravo.warehouse.data.entity.ItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ItemDao {

    // الأصناف النشطة لشاشات الإدخال
    @Query("SELECT * FROM items WHERE isActive = 1 ORDER BY itemName ASC")
    fun getAllItems(): Flow<List<ItemEntity>>

    // جميع الأصناف، بما فيها المتوقفة، للتقارير والسجل التاريخي
    @Query("SELECT * FROM items ORDER BY itemName ASC")
    fun getAllItemsIncludingInactive(): Flow<List<ItemEntity>>

    // البحث في الأصناف النشطة فقط
    @Query("""
        SELECT * FROM items
        WHERE isActive = 1
        AND (itemName LIKE '%' || :search || '%'
        OR itemCode LIKE '%' || :search || '%')
        ORDER BY itemName ASC
    """)
    fun searchItems(search: String): Flow<List<ItemEntity>>

    @Query("SELECT * FROM items WHERE id = :itemId LIMIT 1")
    suspend fun getItemById(itemId: Long): ItemEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertItem(item: ItemEntity): Long

    @Update
    suspend fun updateItem(item: ItemEntity)

    @Query("""
        UPDATE items
        SET isActive = 0, updatedAt = :time
        WHERE id = :itemId
        AND NOT EXISTS (
            SELECT warehouseId
            FROM stock_movements
            WHERE itemId = :itemId
            AND isApproved = 1
            GROUP BY warehouseId
            HAVING ABS(
                SUM(
                    CASE
                        WHEN movementType IN (
                            'IN',
                            'RETURN_IN',
                            'ADJUSTMENT_IN'
                        ) THEN quantity
                        WHEN movementType IN (
                            'OUT',
                            'RETURN_OUT',
                            'ADJUSTMENT_OUT'
                        ) THEN -quantity
                        ELSE 0
                    END
                )
            ) > 0.000001
        )
    """)
    suspend fun deactivateItem(
        itemId: Long,
        time: Long = System.currentTimeMillis()
    ): Int

    @Query("SELECT COUNT(*) FROM items WHERE itemCode = :code")
    suspend fun countByCode(code: String): Int
}
