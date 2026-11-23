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

        require(transferNumber.isNotBlank()) {
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

            check(
                transferDao.countByTransferNumber(transferNumber) == 0
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

            check(quantity <= balance) {
                "Insufficient stock in source warehouse"
            }

            val now = System.currentTimeMillis()

            val transferId = transferDao.insertTransfer(
                TransferEntity(
                    transferNumber = transferNumber,
                    itemId = itemId,
                    sourceWarehouseId = sourceWarehouseId,
                    destinationWarehouseId = destinationWarehouseId,
                    quantity = quantity,
                    notes = notes,
                    transferDate = now,
                    createdAt = now,
                    isApproved = true
                )
            )

            movementDao.insertMovement(
                StockMovementEntity(
                    documentNumber = transferNumber,
                    movementType = "OUT",
                    itemId = itemId,
                    warehouseId = sourceWarehouseId,
                    quantity = quantity,
                    notes = "Transfer to warehouse $destinationWarehouseId. $notes",
                    movementDate = now,
                    createdAt = now,
                    createdBy = createdBy,
                    isApproved = true
                )
            )

            movementDao.insertMovement(
                StockMovementEntity(
                    documentNumber = transferNumber,
                    movementType = "IN",
                    itemId = itemId,
                    warehouseId = destinationWarehouseId,
                    quantity = quantity,
                    notes = "Transfer from warehouse $sourceWarehouseId. $notes",
                    movementDate = now,
                    createdAt = now,
                    createdBy = createdBy,
                    isApproved = true
                )
            )

            transferId
        }
    }
}
