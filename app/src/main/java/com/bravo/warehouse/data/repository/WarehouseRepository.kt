package com.bravo.warehouse.data.repository

import com.bravo.warehouse.data.dao.WarehouseDao
import com.bravo.warehouse.data.entity.WarehouseEntity

class WarehouseRepository(
    private val warehouseDao: WarehouseDao
) {

    // المخازن النشطة لشاشات الإدخال
    fun getAllWarehouses() =
        warehouseDao.getAllWarehouses()

    // جميع المخازن للتقارير والسجل التاريخي
    fun getAllWarehousesIncludingInactive() =
        warehouseDao.getAllWarehousesIncludingInactive()

    suspend fun getWarehouseById(id: Long) =
        warehouseDao.getWarehouseById(id)

    suspend fun insertWarehouse(
        warehouse: WarehouseEntity
    ): Long {
        return warehouseDao.insertWarehouse(warehouse)
    }

    suspend fun updateWarehouse(
        warehouse: WarehouseEntity
    ) {
        warehouseDao.updateWarehouse(warehouse)
    }

    suspend fun deactivateWarehouse(id: Long): Boolean {
        val affectedRows = warehouseDao.deactivateWarehouse(id)
        return affectedRows > 0
    }
}
