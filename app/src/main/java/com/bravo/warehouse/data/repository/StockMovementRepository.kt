package com.bravo.warehouse.data.repository

import com.bravo.warehouse.data.dao.StockMovementDao
import com.bravo.warehouse.data.entity.StockMovementEntity

class StockMovementRepository(
    private val stockMovementDao: StockMovementDao
) {

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
        return stockMovementDao.insertMovement(movement)
    }

    suspend fun approveMovement(id: Long): Int {
        return stockMovementDao.approveMovement(id)
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
