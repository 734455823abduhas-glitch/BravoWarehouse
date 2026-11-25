package com.bravo.warehouse.data.repository

import com.bravo.warehouse.data.dao.StockMovementDao
import com.bravo.warehouse.data.entity.StockMovementEntity

class StockMovementRepository(
private val stockMovementDao: StockMovementDao
) {

private val validMovementTypes = setOf(
    "IN",
    "OUT",
    "RETURN_IN",
    "RETURN_OUT",
    "ADJUSTMENT_IN",
    "ADJUSTMENT_OUT"
)

fun getAllMovements() =
    stockMovementDao.getAllMovements()

fun getMovementsByItem(itemId: Long) =
    stockMovementDao.getMovementsByItem(itemId)

fun getMovementsByWarehouse(warehouseId: Long) =
    stockMovementDao.getMovementsByWarehouse(warehouseId)

suspend fun getMovementById(id: Long) =
    stockMovementDao.getMovementById(id)

suspend fun insertMovement(
    movement: StockMovementEntity
): Long {
    require(!movement.isApproved) {
        "Approved movements must use the safe approval method"
    }

    require(movement.documentNumber.isNotBlank()) {
        "Document number is required"
    }

    require(movement.quantity.isFinite() && movement.quantity > 0) {
        "Quantity must be greater than zero"
    }

    require(movement.movementType in validMovementTypes) {
        "Invalid movement type"
    }

    require(
        movement.movementType !in setOf("OUT", "RETURN_OUT") ||
            movement.recipientName.isNotBlank()
    ) {
        "Recipient name is required"
    }

    return stockMovementDao.insertMovement(
        movement.copy(
            documentNumber = movement.documentNumber.trim(),
            recipientName = movement.recipientName.trim(),
            department = movement.department.trim()
        )
    )
}

suspend fun insertAndApproveMovement(
    movement: StockMovementEntity
): Long {
    return stockMovementDao.insertAndApproveMovementSafely(
        movement.copy(
            documentNumber = movement.documentNumber.trim(),
            recipientName = movement.recipientName.trim(),
            department = movement.department.trim()
        )
    )
}

/**
 * اعتماد جميع أصناف مستند الصرف أو التوريد كوحدة واحدة.
 */
suspend fun insertAndApproveMovements(
    movements: List<StockMovementEntity>
) {
    require(movements.isNotEmpty()) {
        "Movement document cannot be empty"
    }

    val documentNumber = movements.first().documentNumber.trim()
    val movementType = movements.first().movementType
    val warehouseId = movements.first().warehouseId

    require(documentNumber.isNotBlank()) {
        "Document number is required"
    }

    require(movements.all { !it.isApproved }) {
        "Movements must be unapproved"
    }

    require(movements.all {
        it.documentNumber.trim() == documentNumber
    }) {
        "Document numbers must match"
    }

    require(movements.all {
        it.movementType == movementType &&
            it.warehouseId == warehouseId
    }) {
        "Movement type and warehouse must match"
    }

    require(movementType in validMovementTypes) {
        "Invalid movement type"
    }

    require(movements.all {
        it.quantity.isFinite() && it.quantity > 0
    }) {
        "Invalid movement quantity"
    }

    require(movements.map { it.itemId }.distinct().size == movements.size) {
        "Duplicate items are not allowed"
    }

    require(
        movementType !in setOf("OUT", "RETURN_OUT") ||
            movements.all { it.recipientName.isNotBlank() }
    ) {
        "Recipient name is required"
    }

    val normalizedMovements = movements.map { movement ->
        movement.copy(
            documentNumber = documentNumber,
            recipientName = movement.recipientName.trim(),
            department = movement.department.trim(),
            notes = movement.notes.trim()
        )
    }

    stockMovementDao.insertAndApproveMovementsSafely(
        normalizedMovements
    )
}

/**
 * اعتماد تسويات الجرد متعددة الأصناف.
 * يسمح بوجود زيادة وعجز في المستند نفسه.
 * ترجع أرقام الحركات التي تم اعتمادها.
 */
suspend fun insertAndApproveStocktaking(
    movements: List<StockMovementEntity>
): List<Long> {
    require(movements.isNotEmpty()) {
        "Stocktaking document cannot be empty"
    }

    val documentNumber = movements.first().documentNumber.trim()
    val warehouseId = movements.first().warehouseId

    require(documentNumber.isNotBlank()) {
        "Document number is required"
    }

    require(movements.all { !it.isApproved }) {
        "Stocktaking movements must be unapproved"
    }

    require(movements.all {
        it.documentNumber.trim() == documentNumber
    }) {
        "Document numbers must match"
    }

    require(movements.all {
        it.warehouseId == warehouseId
    }) {
        "All items must belong to the same warehouse"
    }

    require(movements.all {
        it.movementType in setOf("ADJUSTMENT_IN", "ADJUSTMENT_OUT")
    }) {
        "Only stock adjustment movements are allowed"
    }

    require(movements.all {
        it.quantity.isFinite() && it.quantity > 0
    }) {
        "Invalid stocktaking quantity"
    }

    require(movements.map { it.itemId }.distinct().size == movements.size) {
        "Duplicate items are not allowed"
    }

    val normalizedMovements = movements.map { movement ->
        movement.copy(
            documentNumber = documentNumber,
            notes = movement.notes.trim()
        )
    }

    return stockMovementDao.insertAndApproveStocktakingSafely(
        normalizedMovements
    )
}

suspend fun approveMovement(id: Long): Boolean {
    return stockMovementDao.approveMovementSafely(id)
}

suspend fun getCurrentBalance(
    itemId: Long,
    warehouseId: Long
): Double {
    val incoming = stockMovementDao.getTotalIncoming(
        itemId,
        warehouseId
    )

    val outgoing = stockMovementDao.getTotalOutgoing(
        itemId,
        warehouseId
    )

    return incoming - outgoing
}

}
