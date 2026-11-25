package com.bravo.warehouse.data

import androidx.room.Database
import androidx.room.RoomDatabase
import com.bravo.warehouse.data.dao.ItemDao
import com.bravo.warehouse.data.dao.WarehouseDao
import com.bravo.warehouse.data.dao.StockMovementDao
import com.bravo.warehouse.data.dao.TransferDao
import com.bravo.warehouse.data.entity.ItemEntity
import com.bravo.warehouse.data.entity.WarehouseEntity
import com.bravo.warehouse.data.entity.StockMovementEntity
import com.bravo.warehouse.data.entity.TransferEntity

@Database(
    entities = [
        ItemEntity::class,
        WarehouseEntity::class,
        StockMovementEntity::class,
        TransferEntity::class
    ],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun itemDao(): ItemDao

    abstract fun warehouseDao(): WarehouseDao

    abstract fun stockMovementDao(): StockMovementDao

    abstract fun transferDao(): TransferDao

    companion object {
        const val DATABASE_NAME = "bravo_warehouse.db"
    }
}
