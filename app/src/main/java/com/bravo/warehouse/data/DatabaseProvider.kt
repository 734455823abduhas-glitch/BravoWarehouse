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

    @Volatile
    private var INSTANCE: AppDatabase? = null

    fun getDatabase(context: Context): AppDatabase {
        return INSTANCE ?: synchronized(this) {
            val instance = Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                AppDatabase.DATABASE_NAME
            )
                .addMigrations(MIGRATION_1_2)
                .build()

            INSTANCE = instance
            instance
        }
    }
}
