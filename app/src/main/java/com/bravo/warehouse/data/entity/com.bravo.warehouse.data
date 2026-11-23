package com.bravo.warehouse.data

import androidx.room.Database
import androidx.room.RoomDatabase
import com.bravo.warehouse.data.dao.ItemDao
import com.bravo.warehouse.data.entity.ItemEntity

@Database(
    entities = [ItemEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun itemDao(): ItemDao

    companion object {
        const val DATABASE_NAME = "bravo_warehouse.db"
    }
}
