package com.bravo.warehouse.data

import androidx.room.Database
import androidx.room.RoomDatabase
import com.bravo.warehouse.data.dao.ItemDao
import com.bravo.warehouse.data.dao.StockMovementDao
import com.bravo.warehouse.data.dao.TransferDao
import com.bravo.warehouse.data.dao.WarehouseDao
import com.bravo.warehouse.data.entity.ItemEntity
import com.bravo.warehouse.data.entity.StockMovementEntity
import com.bravo.warehouse.data.entity.TransferEntity
import com.bravo.warehouse.data.entity.WarehouseEntity

@Database(
    entities = [
        ItemEntity::class,
        WarehouseEntity::class,
        StockMovementEntity::class,
        TransferEntity::class
    ],
    version = AppDatabase.DATABASE_VERSION,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun itemDao(): ItemDao
    abstract fun warehouseDao(): WarehouseDao
    abstract fun stockMovementDao(): StockMovementDao
    abstract fun transferDao(): TransferDao

    companion object {
        /** Keep Room migrations and backup metadata on the same schema version. */
        const val DATABASE_VERSION = 6
        const val DATABASE_NAME = "bravo_warehouse.db"
    }
}
