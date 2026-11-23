package com.bravo.warehouse.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.bravo.warehouse.data.entity.StockMovementEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StockMovementDao {

    @Query("SELECT * FROM stock_movements ORDER BY movementDate DESC")
    fun getAllMovements(): Flow<List<StockMovementEntity>>

    @Query("SELECT * FROM stock_movements WHERE id = :movementId LIMIT 1")
    suspend fun getMovementById(movementId: Long): StockMovementEntity?

    @Query("SELECT * FROM stock_movements WHERE itemId = :itemId ORDER BY movementDate DESC")
    fun getMovementsByItem(itemId: Long): Flow<List<StockMovementEntity>>

    @Query("SELECT * FROM stock_movements WHERE warehouseId = :warehouseId ORDER BY movementDate DESC")
    fun getMovementsByWarehouse(warehouseId: Long): Flow<List<StockMovementEntity>>

    @Insert
    suspend fun insertMovement(movement: StockMovementEntity): Long

    @Query(
        "UPDATE stock_movements SET isApproved = 1 WHERE id = :movementId AND isApproved = 0"
    )
    suspend fun approveMovement(movementId: Long): Int

    @Query(
        "SELECT COALESCE(SUM(quantity), 0) FROM stock_movements WHERE itemId = :itemId AND warehouseId = :warehouseId AND isApproved = 1 AND movementType = 'IN'"
    )
    suspend fun getTotalIncoming(itemId: Long, warehouseId: Long): Double

    @Query(
        "SELECT COALESCE(SUM(quantity), 0) FROM stock_movements WHERE itemId = :itemId AND warehouseId = :warehouseId AND isApproved = 1 AND movementType = 'OUT'"
    )
    suspend fun getTotalOutgoing(itemId: Long, warehouseId: Long): Double

    @Transaction
    suspend fun approveMovementSafely(movementId: Long): Boolean {

        val movement = getMovementById(movementId) ?: return false

        if (movement.isApproved) return false

        if (movement.quantity <= 0) return false

        if (movement.movementType == "OUT") {

            val incoming = getTotalIncoming(
                movement.itemId,
                movement.warehouseId
            )

            val outgoing = getTotalOutgoing(
                movement.itemId,
                movement.warehouseId
            )

            val balance = incoming - outgoing

            if (movement.quantity > balance) return false
        }

        return approveMovement(movementId) > 0
    }
}
