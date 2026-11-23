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

    @Query("SELECT * FROM warehouses WHERE isActive = 1 ORDER BY warehouseName ASC")
    fun getAllWarehouses(): Flow<List<WarehouseEntity>>

    @Query("SELECT * FROM warehouses WHERE id = :warehouseId LIMIT 1")
    suspend fun getWarehouseById(warehouseId: Long): WarehouseEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertWarehouse(warehouse: WarehouseEntity): Long

    @Update
    suspend fun updateWarehouse(warehouse: WarehouseEntity)

    @Query("UPDATE warehouses SET isActive = 0, updatedAt = :time WHERE id = :warehouseId")
    suspend fun deactivateWarehouse(
        warehouseId: Long,
        time: Long = System.currentTimeMillis()
    )

    @Query("SELECT COUNT(*) FROM warehouses WHERE warehouseCode = :code")
    suspend fun countByCode(code: String): Int
}
