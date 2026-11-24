package com.bravo.warehouse.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import androidx.room.OnConflictStrategy
import com.bravo.warehouse.data.entity.WarehouseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WarehouseDao {

    // المخازن النشطة لشاشات الإدخال
    @Query("SELECT * FROM warehouses WHERE isActive = 1 ORDER BY warehouseName ASC")
    fun getAllWarehouses(): Flow<List<WarehouseEntity>>

    // جميع المخازن، النشطة والمتوقفة، للتقارير والسجل التاريخي
    @Query("SELECT * FROM warehouses ORDER BY warehouseName ASC")
    fun getAllWarehousesIncludingInactive(): Flow<List<WarehouseEntity>>

    @Query("SELECT * FROM warehouses WHERE id = :warehouseId LIMIT 1")
    suspend fun getWarehouseById(warehouseId: Long): WarehouseEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertWarehouse(warehouse: WarehouseEntity): Long

    @Update
    suspend fun updateWarehouse(warehouse: WarehouseEntity)

    @Query("""
        UPDATE warehouses
        SET isActive = 0, updatedAt = :time
        WHERE id = :warehouseId
        AND NOT EXISTS (
            SELECT itemId
            FROM stock_movements
            WHERE warehouseId = :warehouseId
            AND isApproved = 1
            GROUP BY itemId
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
    suspend fun deactivateWarehouse(
        warehouseId: Long,
        time: Long = System.currentTimeMillis()
    ): Int

    @Query("SELECT COUNT(*) FROM warehouses WHERE warehouseCode = :code")
    suspend fun countByCode(code: String): Int
}
