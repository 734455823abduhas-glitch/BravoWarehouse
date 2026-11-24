package com.bravo.warehouse.data.repository

import androidx.room.withTransaction
import com.bravo.warehouse.data.AppDatabase
import com.bravo.warehouse.data.entity.StockMovementEntity
import com.bravo.warehouse.data.entity.TransferEntity

class TransferRepository(
private val database: AppDatabase
) {

fun getAllTransfers() =
    database.transferDao().getAllTransfers()

suspend fun createTransfer(
    transferNumber: String,
    itemId: Long,
    sourceWarehouseId: Long,
    destinationWarehouseId: Long,
    quantity: Double,
    notes: String = "",
    createdBy: String = ""
): Long {

    val cleanNumber = transferNumber.trim()
    val cleanNotes = notes.trim()

    require(cleanNumber.isNotBlank()) {
        "Transfer number is required"
    }

    require(itemId > 0) {
        "Invalid item"
    }

    require(sourceWarehouseId > 0 && destinationWarehouseId > 0) {
        "Invalid warehouse"
    }

    require(sourceWarehouseId != destinationWarehouseId) {
        "Source and destination must be different"
    }

    require(quantity.isFinite() && quantity > 0) {
        "Quantity must be greater than zero"
    }

    return database.withTransaction {

        val transferDao = database.transferDao()
        val movementDao = database.stockMovementDao()
        val itemDao = database.itemDao()
        val warehouseDao = database.warehouseDao()

        val item = itemDao.getItemById(itemId)

        check(item != null && item.isActive) {
            "Item does not exist or is inactive"
        }

        val sourceWarehouse =
            warehouseDao.getWarehouseById(sourceWarehouseId)

        check(sourceWarehouse != null && sourceWarehouse.isActive) {
            "Source warehouse does not exist or is inactive"
        }

        val destinationWarehouse =
            warehouseDao.getWarehouseById(destinationWarehouseId)

        check(
            destinationWarehouse != null &&
            destinationWarehouse.isActive
        ) {
            "Destination warehouse does not exist or is inactive"
        }

        check(
            transferDao.countByTransferNumber(cleanNumber) == 0
        ) {
            "Transfer number already exists"
        }

        val incoming = movementDao.getTotalIncoming(
            itemId,
            sourceWarehouseId
        )

        val outgoing = movementDao.getTotalOutgoing(
            itemId,
            sourceWarehouseId
        )

        val balance = incoming - outgoing

        check(quantity <= balance + 0.000001) {
            "Insufficient stock in source warehouse"
        }

        val now = System.currentTimeMillis()

        val transferId = transferDao.insertTransfer(
            TransferEntity(
                transferNumber = cleanNumber,
                itemId = itemId,
                sourceWarehouseId = sourceWarehouseId,
                destinationWarehouseId = destinationWarehouseId,
                quantity = quantity,
                notes = cleanNotes,
                transferDate = now,
                createdAt = now,
                isApproved = true
            )
        )

        val outgoingMovement = StockMovementEntity(
            documentNumber = cleanNumber,
            movementType = "OUT",
            itemId = itemId,
            warehouseId = sourceWarehouseId,
            quantity = quantity,
            notes = "تحويل إلى المخزن رقم $destinationWarehouseId. $cleanNotes",
            movementDate = now,
            createdAt = now,
            createdBy = createdBy,
            isApproved = false
        )

        movementDao.insertAndApproveMovementSafely(
            outgoingMovement
        )

        val incomingMovement = StockMovementEntity(
            documentNumber = cleanNumber,
            movementType = "IN",
            itemId = itemId,
            warehouseId = destinationWarehouseId,
            quantity = quantity,
            notes = "تحويل من المخزن رقم $sourceWarehouseId. $cleanNotes",
            movementDate = now,
            createdAt = now,
            createdBy = createdBy,
            isApproved = false
        )

        movementDao.insertAndApproveMovementSafely(
            incomingMovement
        )

        transferId
    }
}

}
