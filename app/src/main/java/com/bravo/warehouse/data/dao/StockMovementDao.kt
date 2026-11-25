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

    @Query("SELECT COUNT(*) FROM stock_movements WHERE documentNumber = :documentNumber")
    suspend fun countByDocumentNumber(documentNumber: String): Int

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
    suspend fun getTotalIncoming(itemId: Long, warehouseId: Long): Double

    @Query("""
        SELECT COALESCE(SUM(quantity), 0)
        FROM stock_movements
        WHERE itemId = :itemId
        AND warehouseId = :warehouseId
        AND isApproved = 1
        AND movementType IN ('OUT', 'RETURN_OUT', 'ADJUSTMENT_OUT')
    """)
    suspend fun getTotalOutgoing(itemId: Long, warehouseId: Long): Double

    @Transaction
    suspend fun approveMovementSafely(movementId: Long): Boolean {
        val movement = getMovementById(movementId) ?: return false

        if (movement.isApproved) return false
        if (!movement.quantity.isFinite() || movement.quantity <= 0) return false
        if (isItemActive(movement.itemId) != true) return false
        if (isWarehouseActive(movement.warehouseId) != true) return false

        val incomingTypes = listOf("IN", "RETURN_IN", "ADJUSTMENT_IN")
        val outgoingTypes = listOf("OUT", "RETURN_OUT", "ADJUSTMENT_OUT")

        if (
            movement.movementType !in incomingTypes &&
            movement.movementType !in outgoingTypes
        ) return false

        if (movement.movementType in outgoingTypes) {
            val balance = getTotalIncoming(
                movement.itemId,
                movement.warehouseId
            ) - getTotalOutgoing(
                movement.itemId,
                movement.warehouseId
            )

            if (movement.quantity > balance + 0.000001) return false
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

        require(movement.documentNumber.isNotBlank()) {
            "Document number is required"
        }

        require(countByDocumentNumber(movement.documentNumber) == 0) {
            "Document number already exists"
        }

        require(movement.quantity.isFinite() && movement.quantity > 0) {
            "Invalid movement quantity"
        }

        val validTypes = listOf(
            "IN", "OUT", "RETURN_IN",
            "RETURN_OUT", "ADJUSTMENT_IN", "ADJUSTMENT_OUT"
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

        val outgoingTypes = listOf("OUT", "RETURN_OUT", "ADJUSTMENT_OUT")

        if (movement.movementType in outgoingTypes) {
            val balance = getTotalIncoming(
                movement.itemId,
                movement.warehouseId
            ) - getTotalOutgoing(
                movement.itemId,
                movement.warehouseId
            )

            require(movement.quantity <= balance + 0.000001) {
                "Insufficient stock"
            }
        }

        val id = insertMovement(movement)

        check(approveMovement(id) == 1) {
            "Movement approval failed"
        }

        return id
    }

    @Transaction
    suspend fun insertAndApproveMovementsSafely(
        movements: List<StockMovementEntity>
    ): List<Long> {

        require(movements.isNotEmpty()) {
            "Document cannot be empty"
        }

        require(movements.all { !it.isApproved }) {
            "All movements must be unapproved"
        }

        require(movements.all { it.documentNumber.isNotBlank() }) {
            "Document number is required"
        }

        require(movements.map { it.documentNumber }.distinct().size == 1) {
            "All movements must belong to the same document"
        }

        val documentNumber = movements.first().documentNumber.trim()

        require(
            movements.all { it.documentNumber.trim() == documentNumber }
        ) {
            "Document numbers must match"
        }

        require(countByDocumentNumber(documentNumber) == 0) {
            "Document number already exists"
        }

        require(movements.map { it.movementType }.distinct().size == 1) {
            "Movement types must be identical"
        }

        require(
            movements.map { it.itemId }.distinct().size == movements.size
        ) {
            "Duplicate items are not allowed"
        }

        require(
            movements.map { it.warehouseId }.distinct().size == 1
        ) {
            "All movements must belong to the same warehouse"
        }

        require(movements.all {
            it.quantity.isFinite() && it.quantity > 0
        }) {
            "Invalid movement quantity"
        }

        val validTypes = listOf(
            "IN", "OUT", "RETURN_IN",
            "RETURN_OUT", "ADJUSTMENT_IN", "ADJUSTMENT_OUT"
        )

        val movementType = movements.first().movementType

        require(movementType in validTypes) {
            "Invalid movement type"
        }

        for (movement in movements) {
            require(isItemActive(movement.itemId) == true) {
                "Item ${movement.itemId} is inactive or does not exist"
            }

            require(isWarehouseActive(movement.warehouseId) == true) {
                "Warehouse ${movement.warehouseId} is inactive or does not exist"
            }
        }

        val outgoingTypes = listOf(
            "OUT", "RETURN_OUT", "ADJUSTMENT_OUT"
        )

        if (movementType in outgoingTypes) {
            for (movement in movements) {
                val balance = getTotalIncoming(
                    movement.itemId,
                    movement.warehouseId
                ) - getTotalOutgoing(
                    movement.itemId,
                    movement.warehouseId
                )

                require(movement.quantity <= balance + 0.000001) {
                    "Insufficient stock for item ${movement.itemId}"
                }
            }
        }

        val ids = mutableListOf<Long>()

        for (movement in movements) {
            val id = insertMovement(movement)

            check(approveMovement(id) == 1) {
                "Movement approval failed"
            }

            ids.add(id)
        }

        return ids
    }
}
