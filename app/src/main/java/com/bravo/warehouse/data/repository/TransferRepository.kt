package com.bravo.warehouse.data.repository

import androidx.room.withTransaction
import com.bravo.warehouse.data.AppDatabase
import com.bravo.warehouse.data.entity.StockMovementEntity
import com.bravo.warehouse.data.entity.TransferEntity
import kotlinx.coroutines.flow.Flow

 data class TransferLine(
    val itemId: Long,
    val quantity: Double
)

class TransferRepository(
    private val database: AppDatabase
) {

    fun getAllTransfers(): Flow<List<TransferEntity>> =
        database.transferDao().getAllTransfers()

    suspend fun getTransfersByNumber(transferNumber: String): List<TransferEntity> =
        database.transferDao().getTransfersByNumber(transferNumber.trim())

    fun observeTransfersByNumber(transferNumber: String): Flow<List<TransferEntity>> =
        database.transferDao().observeTransfersByNumber(transferNumber.trim())

    suspend fun createTransfer(
        transferNumber: String,
        itemId: Long,
        sourceWarehouseId: Long,
        destinationWarehouseId: Long,
        quantity: Double,
        notes: String = "",
        createdBy: String = ""
    ): Long = createTransfers(
        transferNumber = transferNumber,
        lines = listOf(TransferLine(itemId, quantity)),
        sourceWarehouseId = sourceWarehouseId,
        destinationWarehouseId = destinationWarehouseId,
        notes = notes,
        createdBy = createdBy
    )

    suspend fun createTransfers(
        transferNumber: String,
        lines: List<TransferLine>,
        sourceWarehouseId: Long,
        destinationWarehouseId: Long,
        notes: String = "",
        createdBy: String = ""
    ): Long {
        val cleanNumber = transferNumber.trim()
        val cleanNotes = notes.trim()

        require(cleanNumber.isNotBlank()) { "Transfer number is required" }
        require(lines.isNotEmpty()) { "Transfer must contain at least one item" }
        require(sourceWarehouseId > 0 && destinationWarehouseId > 0) { "Invalid warehouse" }
        require(sourceWarehouseId != destinationWarehouseId) {
            "Source and destination must be different"
        }
        require(lines.all { it.itemId > 0 && it.quantity.isFinite() && it.quantity > 0 }) {
            "Invalid item or quantity"
        }
        require(lines.map { it.itemId }.distinct().size == lines.size) {
            "Duplicate items are not allowed"
        }

        return database.withTransaction {
            val transferDao = database.transferDao()
            val movementDao = database.stockMovementDao()
            val itemDao = database.itemDao()
            val warehouseDao = database.warehouseDao()

            check(transferDao.countByTransferNumber(cleanNumber) == 0) {
                "Transfer number already exists"
            }

            val source = warehouseDao.getWarehouseById(sourceWarehouseId)
            check(source != null && source.isActive) {
                "Source warehouse does not exist or is inactive"
            }
            val destination = warehouseDao.getWarehouseById(destinationWarehouseId)
            check(destination != null && destination.isActive) {
                "Destination warehouse does not exist or is inactive"
            }

            // Validate every line before writing any part of the transfer.
            lines.forEach { line ->
                val item = itemDao.getItemById(line.itemId)
                check(item != null && item.isActive) {
                    "Item ${line.itemId} does not exist or is inactive"
                }
                val incoming = movementDao.getTotalIncoming(line.itemId, sourceWarehouseId)
                val outgoing = movementDao.getTotalOutgoing(line.itemId, sourceWarehouseId)
                check(line.quantity <= incoming - outgoing + 0.000001) {
                    "Insufficient stock for item ${line.itemId}"
                }
            }

            val now = System.currentTimeMillis()
            val transferIds = lines.map { line ->
                transferDao.insertTransfer(
                    TransferEntity(
                        transferNumber = cleanNumber,
                        itemId = line.itemId,
                        sourceWarehouseId = sourceWarehouseId,
                        destinationWarehouseId = destinationWarehouseId,
                        quantity = line.quantity,
                        notes = cleanNotes,
                        transferDate = now,
                        createdAt = now,
                        isApproved = true
                    )
                )
            }

            val outgoingMovements = lines.map { line ->
                StockMovementEntity(
                    documentNumber = cleanNumber,
                    movementType = "OUT",
                    itemId = line.itemId,
                    warehouseId = sourceWarehouseId,
                    quantity = line.quantity,
                    notes = "تحويل إلى المخزن رقم $destinationWarehouseId. $cleanNotes",
                    movementDate = now,
                    createdAt = now,
                    createdBy = createdBy,
                    isApproved = false
                )
            }
            val incomingMovements = lines.map { line ->
                StockMovementEntity(
                    documentNumber = cleanNumber,
                    movementType = "IN",
                    itemId = line.itemId,
                    warehouseId = destinationWarehouseId,
                    quantity = line.quantity,
                    notes = "تحويل من المخزن رقم $sourceWarehouseId. $cleanNotes",
                    movementDate = now,
                    createdAt = now,
                    createdBy = createdBy,
                    isApproved = false
                )
            }

            // A transfer intentionally has the same document number on both
            // sides. Insert and approve both sides directly inside this outer
            // Room transaction; the generic document method rejects a second
            // batch with the same number and would split this logical document.
            (outgoingMovements + incomingMovements).forEach { movement ->
                val id = movementDao.insertMovement(movement)
                check(movementDao.approveMovement(id) == 1) {
                    "Transfer movement approval failed"
                }
            }

            transferIds.first()
        }
    }
}
