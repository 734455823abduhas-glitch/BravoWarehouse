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

@Query("SELECT isActive FROM items WHERE id = :itemId LIMIT 1")
suspend fun isItemActive(itemId: Long): Boolean?

@Query("SELECT isActive FROM warehouses WHERE id = :warehouseId LIMIT 1")
suspend fun isWarehouseActive(warehouseId: Long): Boolean?

@Insert
suspend fun insertMovement(movement: StockMovementEntity): Long

@Query("""
    UPDATE stock_movements
    SET isApproved = 1
    WHERE id = :movementId AND isApproved = 0
""")
suspend fun approveMovement(movementId: Long): Int

@Query("""
    SELECT COALESCE(SUM(quantity), 0)
    FROM stock_movements
    WHERE itemId = :itemId
    AND warehouseId = :warehouseId
    AND isApproved = 1
    AND movementType IN ('IN', 'RETURN_IN', 'ADJUSTMENT_IN')
""")
suspend fun getTotalIncoming(
    itemId: Long,
    warehouseId: Long
): Double

@Query("""
    SELECT COALESCE(SUM(quantity), 0)
    FROM stock_movements
    WHERE itemId = :itemId
    AND warehouseId = :warehouseId
    AND isApproved = 1
    AND movementType IN ('OUT', 'RETURN_OUT', 'ADJUSTMENT_OUT')
""")
suspend fun getTotalOutgoing(
    itemId: Long,
    warehouseId: Long
): Double

@Transaction
suspend fun approveMovementSafely(
    movementId: Long
): Boolean {

    val movement = getMovementById(movementId)
        ?: return false

    if (movement.isApproved) return false

    if (!movement.quantity.isFinite() || movement.quantity <= 0) {
        return false
    }

    if (isItemActive(movement.itemId) != true) {
        return false
    }

    if (isWarehouseActive(movement.warehouseId) != true) {
        return false
    }

    val incomingTypes = listOf(
        "IN",
        "RETURN_IN",
        "ADJUSTMENT_IN"
    )

    val outgoingTypes = listOf(
        "OUT",
        "RETURN_OUT",
        "ADJUSTMENT_OUT"
    )

    if (
        movement.movementType !in incomingTypes &&
        movement.movementType !in outgoingTypes
    ) {
        return false
    }

    if (movement.movementType in outgoingTypes) {
        val incoming = getTotalIncoming(
            movement.itemId,
            movement.warehouseId
        )

        val outgoing = getTotalOutgoing(
            movement.itemId,
            movement.warehouseId
        )

        val balance = incoming - outgoing

        if (movement.quantity > balance) {
            return false
        }
    }

    return approveMovement(movementId) > 0
}

@Transaction
suspend fun insertAndApproveMovementSafely(
    movement: StockMovementEntity
): Long {

    require(!movement.isApproved) {
        "Movement must be created as unapproved"
    }

    require(movement.quantity.isFinite() && movement.quantity > 0) {
        "Invalid movement quantity"
    }

    val validTypes = listOf(
        "IN",
        "OUT",
        "RETURN_IN",
        "RETURN_OUT",
        "ADJUSTMENT_IN",
        "ADJUSTMENT_OUT"
    )

    require(movement.movementType in validTypes) {
        "Invalid movement type"
    }

    require(isItemActive(movement.itemId) == true) {
        "Item is inactive or does not exist"
    }

    require(isWarehouseActive(movement.warehouseId) == true) {
        "Warehouse is inactive or does not exist"
    }

    val movementId = insertMovement(movement)

    check(approveMovementSafely(movementId)) {
        "Movement approval failed"
    }

    return movementId
}

}
