package com.bravo.warehouse.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bravo.warehouse.data.entity.ItemEntity
import com.bravo.warehouse.data.entity.StockMovementEntity
import com.bravo.warehouse.data.entity.WarehouseEntity
import com.bravo.warehouse.data.repository.TransferLine
import com.bravo.warehouse.data.repository.TransferRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class InventoryCriticalPathTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var database: AppDatabase

    @Before
    fun setUp() {
        DatabaseProvider.closeDatabase()
        context.deleteDatabase(AppDatabase.DATABASE_NAME)
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        database.close()
        DatabaseProvider.closeDatabase()
        context.deleteDatabase(AppDatabase.DATABASE_NAME)
    }

    @Test
    fun roomVersionSixMigrationPreservesLegacyMovementsAndBalances() = runBlocking {
        database.close()
        createLegacyVersionFiveDatabase()

        val migrated = DatabaseProvider.getDatabase(context)
        assertEquals(AppDatabase.DATABASE_VERSION, migrated.openHelper.writableDatabase.version)
        val item = migrated.itemDao().getItemById(41L)
        assertEquals("LEGACY-ITEM", item?.itemCode)
        assertEquals(1, migrated.stockMovementDao().getMovementsByItem(41L).first().size)
        assertEquals(12.5, migrated.stockMovementDao().getTotalIncoming(41L, 7L), 0.000001)
        assertEquals(0.0, migrated.stockMovementDao().getTotalOutgoing(41L, 7L), 0.000001)
    }

    @Test
    fun transferUsesOneNumberAndMovesBalancesAtomically() = runBlocking {
        val source = database.warehouseDao().insertWarehouse(
            WarehouseEntity(warehouseCode = "SRC", warehouseName = "Source")
        )
        val destination = database.warehouseDao().insertWarehouse(
            WarehouseEntity(warehouseCode = "DST", warehouseName = "Destination")
        )
        val item = database.itemDao().insertItem(
            ItemEntity(itemCode = "ITEM-1", itemName = "Resin", category = "Raw", unit = "kg")
        )
        database.stockMovementDao().insertMovement(
            StockMovementEntity(
                documentNumber = "OPENING-1",
                movementType = "IN",
                itemId = item,
                warehouseId = source,
                quantity = 20.0,
                isApproved = true
            )
        )

        TransferRepository(database).createTransfer(
            transferNumber = "TR-ATOMIC-1",
            itemId = item,
            sourceWarehouseId = source,
            destinationWarehouseId = destination,
            quantity = 7.5
        )

        val transferLines = database.transferDao().getTransfersByNumber("TR-ATOMIC-1")
        val movementLines = database.stockMovementDao().getMovementsByItem(item).first()
            .filter { it.documentNumber == "TR-ATOMIC-1" }
        assertEquals(1, transferLines.size)
        assertEquals(2, movementLines.size)
        assertTrue(movementLines.all { it.isApproved })
        assertTrue(movementLines.map { it.documentNumber }.distinct() == listOf("TR-ATOMIC-1"))
        assertEquals(12.5, balance(item, source), 0.000001)
        assertEquals(7.5, balance(item, destination), 0.000001)
    }

    @Test
    fun failedMultiItemTransferLeavesNoTransferOrPartialMovements() = runBlocking {
        val source = database.warehouseDao().insertWarehouse(
            WarehouseEntity(warehouseCode = "SRC", warehouseName = "Source")
        )
        val destination = database.warehouseDao().insertWarehouse(
            WarehouseEntity(warehouseCode = "DST", warehouseName = "Destination")
        )
        val enough = database.itemDao().insertItem(
            ItemEntity(itemCode = "ITEM-1", itemName = "Item 1", category = "Raw", unit = "kg")
        )
        val short = database.itemDao().insertItem(
            ItemEntity(itemCode = "ITEM-2", itemName = "Item 2", category = "Raw", unit = "kg")
        )
        database.stockMovementDao().insertMovement(
            StockMovementEntity("OPEN-1", "IN", enough, source, 10.0, isApproved = true)
        )
        database.stockMovementDao().insertMovement(
            StockMovementEntity("OPEN-2", "IN", short, source, 2.0, isApproved = true)
        )

        val result = runCatching {
            TransferRepository(database).createTransfers(
                transferNumber = "TR-ROLLBACK-1",
                lines = listOf(TransferLine(enough, 5.0), TransferLine(short, 3.0)),
                sourceWarehouseId = source,
                destinationWarehouseId = destination
            )
        }

        assertTrue(result.isFailure)
        assertTrue(database.transferDao().getTransfersByNumber("TR-ROLLBACK-1").isEmpty())
        assertTrue(database.stockMovementDao().getAllMovements().first()
            .none { it.documentNumber == "TR-ROLLBACK-1" })
        assertEquals(10.0, balance(enough, source), 0.000001)
        assertEquals(2.0, balance(short, source), 0.000001)
        assertEquals(0.0, balance(enough, destination), 0.000001)
    }

    @Test
    fun exportedBackupCanBeRestoredWithoutLosingHistoricalRows() = runBlocking {
        val warehouse = database.warehouseDao().insertWarehouse(
            WarehouseEntity(warehouseCode = "BACKUP", warehouseName = "Backup warehouse")
        )
        val item = database.itemDao().insertItem(
            ItemEntity(itemCode = "KEEP-ME", itemName = "Historical item", category = "Raw", unit = "kg")
        )
        database.stockMovementDao().insertMovement(
            StockMovementEntity("BACKUP-IN", "IN", item, warehouse, 14.0, isApproved = true)
        )

        // The backup manager operates on the app's named database.
        database.close()
        val activeDatabase = DatabaseProvider.getDatabase(context)
        activeDatabase.itemDao().insertItem(
            ItemEntity(itemCode = "TEMPORARY", itemName = "After backup", category = "Raw", unit = "kg")
        )
        val uri = android.net.Uri.parse("content://com.bravo.warehouse.test.backup/roundtrip.zip")
        DatabaseBackupManager.exportBackup(context, uri)

        // Change the live database after exporting, then restore the archive.
        DatabaseProvider.getDatabase(context).itemDao().insertItem(
            ItemEntity(itemCode = "REMOVE-ME", itemName = "Not in archive", category = "Raw", unit = "kg")
        )
        DatabaseBackupManager.importBackup(context, uri)

        val restored = DatabaseProvider.getDatabase(context)
        assertEquals(1, restored.itemDao().getAllItemsIncludingInactive().first()
            .count { it.itemCode == "KEEP-ME" })
        assertEquals(0, restored.itemDao().getAllItemsIncludingInactive().first()
            .count { it.itemCode == "REMOVE-ME" })
        assertEquals(14.0, restored.stockMovementDao().getTotalIncoming(item, warehouse), 0.000001)
    }

    private suspend fun balance(itemId: Long, warehouseId: Long): Double {
        val dao = database.stockMovementDao()
        return dao.getTotalIncoming(itemId, warehouseId) - dao.getTotalOutgoing(itemId, warehouseId)
    }

    private fun createLegacyVersionFiveDatabase() {
        val file = context.getDatabasePath(AppDatabase.DATABASE_NAME)
        file.parentFile?.mkdirs()
        val sqlite = SQLiteDatabase.openOrCreateDatabase(file, null)
        try {
            sqlite.execSQL("CREATE TABLE items (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, itemCode TEXT NOT NULL, itemName TEXT NOT NULL, category TEXT NOT NULL, unit TEXT NOT NULL, minimumStock REAL NOT NULL, description TEXT NOT NULL, isActive INTEGER NOT NULL, createdAt INTEGER NOT NULL, updatedAt INTEGER NOT NULL)")
            sqlite.execSQL("CREATE UNIQUE INDEX index_items_itemCode ON items(itemCode)")
            sqlite.execSQL("CREATE INDEX index_items_itemName ON items(itemName)")
            sqlite.execSQL("CREATE TABLE warehouses (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, warehouseCode TEXT NOT NULL, warehouseName TEXT NOT NULL, location TEXT NOT NULL, description TEXT NOT NULL, isActive INTEGER NOT NULL, createdAt INTEGER NOT NULL, updatedAt INTEGER NOT NULL)")
            sqlite.execSQL("CREATE UNIQUE INDEX index_warehouses_warehouseCode ON warehouses(warehouseCode)")
            sqlite.execSQL("CREATE INDEX index_warehouses_warehouseName ON warehouses(warehouseName)")
            sqlite.execSQL("CREATE TABLE stock_movements (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, documentNumber TEXT NOT NULL, movementType TEXT NOT NULL, itemId INTEGER NOT NULL, warehouseId INTEGER NOT NULL, quantity REAL NOT NULL, notes TEXT NOT NULL, movementDate INTEGER NOT NULL, createdAt INTEGER NOT NULL, createdBy TEXT NOT NULL, isApproved INTEGER NOT NULL)")
            sqlite.execSQL("CREATE INDEX index_stock_movements_itemId ON stock_movements(itemId)")
            sqlite.execSQL("CREATE INDEX index_stock_movements_warehouseId ON stock_movements(warehouseId)")
            sqlite.execSQL("CREATE INDEX index_stock_movements_movementType ON stock_movements(movementType)")
            sqlite.execSQL("CREATE INDEX index_stock_movements_documentNumber ON stock_movements(documentNumber)")
            sqlite.execSQL("CREATE TABLE stock_transfers (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, transferNumber TEXT NOT NULL, itemId INTEGER NOT NULL, sourceWarehouseId INTEGER NOT NULL, destinationWarehouseId INTEGER NOT NULL, quantity REAL NOT NULL, notes TEXT NOT NULL, transferDate INTEGER NOT NULL, createdAt INTEGER NOT NULL, isApproved INTEGER NOT NULL)")
            sqlite.execSQL("CREATE INDEX index_stock_transfers_transferNumber ON stock_transfers(transferNumber)")
            sqlite.execSQL("CREATE INDEX index_stock_transfers_itemId ON stock_transfers(itemId)")
            sqlite.execSQL("CREATE INDEX index_stock_transfers_sourceWarehouseId ON stock_transfers(sourceWarehouseId)")
            sqlite.execSQL("CREATE INDEX index_stock_transfers_destinationWarehouseId ON stock_transfers(destinationWarehouseId)")
            sqlite.execSQL("CREATE TABLE room_master_table (id INTEGER PRIMARY KEY, identity_hash TEXT)")
            sqlite.execSQL("INSERT INTO room_master_table (id, identity_hash) VALUES (42, 'legacy-v5')")
            sqlite.execSQL("INSERT INTO items VALUES (41, 'LEGACY-ITEM', 'Legacy', 'Raw', 'kg', 0.0, '', 1, 1, 1)")
            sqlite.execSQL("INSERT INTO warehouses VALUES (7, 'LEGACY-WH', 'Legacy warehouse', '', '', 1, 1, 1)")
            sqlite.execSQL("INSERT INTO stock_movements VALUES (9, 'LEGACY-IN', 'IN', 41, 7, 12.5, '', 1, 1, '', 1)")
            sqlite.version = 5
        } finally {
            sqlite.close()
        }
    }
}
