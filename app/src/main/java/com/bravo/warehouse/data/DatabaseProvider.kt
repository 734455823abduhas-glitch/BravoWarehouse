package com.bravo.warehouse.data

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object DatabaseProvider {

private val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS warehouses (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                warehouseCode TEXT NOT NULL,
                warehouseName TEXT NOT NULL,
                location TEXT NOT NULL,
                description TEXT NOT NULL,
                isActive INTEGER NOT NULL,
                createdAt INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS index_warehouses_warehouseCode ON warehouses(warehouseCode)"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS index_warehouses_warehouseName ON warehouses(warehouseName)"
        )
    }
}

private val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS stock_movements (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                documentNumber TEXT NOT NULL,
                movementType TEXT NOT NULL,
                itemId INTEGER NOT NULL,
                warehouseId INTEGER NOT NULL,
                quantity REAL NOT NULL,
                notes TEXT NOT NULL,
                movementDate INTEGER NOT NULL,
                createdAt INTEGER NOT NULL,
                createdBy TEXT NOT NULL,
                isApproved INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS index_stock_movements_itemId ON stock_movements(itemId)"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS index_stock_movements_warehouseId ON stock_movements(warehouseId)"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS index_stock_movements_movementType ON stock_movements(movementType)"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS index_stock_movements_documentNumber ON stock_movements(documentNumber)"
        )
    }
}

private val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS stock_transfers (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                transferNumber TEXT NOT NULL,
                itemId INTEGER NOT NULL,
                sourceWarehouseId INTEGER NOT NULL,
                destinationWarehouseId INTEGER NOT NULL,
                quantity REAL NOT NULL,
                notes TEXT NOT NULL,
                transferDate INTEGER NOT NULL,
                createdAt INTEGER NOT NULL,
                isApproved INTEGER NOT NULL
            )
            """.trimIndent()
        )

        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS index_stock_transfers_transferNumber ON stock_transfers(transferNumber)"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS index_stock_transfers_itemId ON stock_transfers(itemId)"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS index_stock_transfers_sourceWarehouseId ON stock_transfers(sourceWarehouseId)"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS index_stock_transfers_destinationWarehouseId ON stock_transfers(destinationWarehouseId)"
        )
    }
}

@Volatile
private var INSTANCE: AppDatabase? = null

fun getDatabase(context: Context): AppDatabase {
    return INSTANCE ?: synchronized(this) {
        INSTANCE ?: Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            AppDatabase.DATABASE_NAME
        )
            .addMigrations(
                MIGRATION_1_2,
                MIGRATION_2_3,
                MIGRATION_3_4
            )
            .build()
            .also { INSTANCE = it }
    }
}

fun closeDatabase() {
    synchronized(this) {
        INSTANCE?.close()
        INSTANCE = null
    }
}

}
