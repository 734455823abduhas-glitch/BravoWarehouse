package com.bravo.warehouse.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object DatabaseBackupManager {

private const val DATABASE_VERSION = 4

suspend fun exportBackup(
    context: Context,
    destination: Uri
) = withContext(Dispatchers.IO) {

    val database = DatabaseProvider.getDatabase(context)

    database.openHelper.writableDatabase
        .query("PRAGMA wal_checkpoint(FULL)")
        .use { }

    DatabaseProvider.closeDatabase()

    try {
        val databaseFile =
            context.getDatabasePath(AppDatabase.DATABASE_NAME)

        check(databaseFile.exists()) {
            "Database file not found"
        }

        context.contentResolver.openOutputStream(destination)?.use {
            output ->
            databaseFile.inputStream().use { input ->
                input.copyTo(output)
            }
        } ?: error("Cannot create backup file")

    } finally {
        DatabaseProvider.getDatabase(context)
    }
}

suspend fun importBackup(
    context: Context,
    source: Uri
) = withContext(Dispatchers.IO) {

    val databaseFile =
        context.getDatabasePath(AppDatabase.DATABASE_NAME)

    val directory = databaseFile.parentFile
        ?: error("Database directory not found")

    val temporaryFile = File(directory, "bravo_restore_temp.db")
    val rollbackFile = File(directory, "bravo_restore_previous.db")

    try {
        context.contentResolver.openInputStream(source)?.use {
            input ->
            temporaryFile.outputStream().use { output ->
                input.copyTo(output)
            }
        } ?: error("Cannot read backup file")

        validateBackup(temporaryFile)

        DatabaseProvider.closeDatabase()

        File(databaseFile.absolutePath + "-wal").delete()
        File(databaseFile.absolutePath + "-shm").delete()

        rollbackFile.delete()

        if (databaseFile.exists()) {
            check(databaseFile.renameTo(rollbackFile)) {
                "Cannot protect current database"
            }
        }

        try {
            check(temporaryFile.renameTo(databaseFile)) {
                "Cannot install backup database"
            }

            DatabaseProvider.getDatabase(context)

            rollbackFile.delete()

        } catch (error: Exception) {
            DatabaseProvider.closeDatabase()
            databaseFile.delete()

            if (rollbackFile.exists()) {
                rollbackFile.renameTo(databaseFile)
            }

            throw error
        }

    } catch (error: Exception) {
        temporaryFile.delete()

        if (!databaseFile.exists() && rollbackFile.exists()) {
            rollbackFile.renameTo(databaseFile)
        }

        throw error
    }
}

private fun validateBackup(file: File) {

    val database = SQLiteDatabase.openDatabase(
        file.absolutePath,
        null,
        SQLiteDatabase.OPEN_READONLY
    )

    try {
        database.rawQuery(
            "PRAGMA integrity_check",
            null
        ).use { cursor ->
            check(
                cursor.moveToFirst() &&
                cursor.getString(0) == "ok"
            ) {
                "Backup database is damaged"
            }
        }

        database.rawQuery(
            "PRAGMA user_version",
            null
        ).use { cursor ->
            check(
                cursor.moveToFirst() &&
                cursor.getInt(0) == DATABASE_VERSION
            ) {
                "Backup version is not supported"
            }
        }

        val requiredTables = listOf(
            "items",
            "warehouses",
            "stock_movements",
            "stock_transfers"
        )

        for (table in requiredTables) {
            database.rawQuery(
                "SELECT name FROM sqlite_master WHERE type='table' AND name=?",
                arrayOf(table)
            ).use { cursor ->
                check(cursor.moveToFirst()) {
                    "Missing table: $table"
                }
            }
        }

    } finally {
        database.close()
    }
}

}
