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

    database.openHelper.writableDatabase.query(
        "PRAGMA wal_checkpoint(FULL)"
    ).use { }

    DatabaseProvider.closeDatabase()

    try {
        val databaseFile =
            context.getDatabasePath(AppDatabase.DATABASE_NAME)

        require(databaseFile.exists()) {
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

    val temporaryFile = File(
        databaseFile.parentFile,
        "bravo_restore_temp.db"
    )

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

        if (databaseFile.exists()) {
            check(databaseFile.delete()) {
                "Cannot replace current database"
            }
        }

        check(temporaryFile.renameTo(databaseFile)) {
            "Cannot restore database"
        }

        DatabaseProvider.getDatabase(context)

    } catch (error: Exception) {
        temporaryFile.delete()
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

            check(cursor.moveToFirst()) {
                "Invalid backup database"
            }

            check(cursor.getString(0) == "ok") {
                "Backup database is damaged"
            }
        }

        database.rawQuery(
            "PRAGMA user_version",
            null
        ).use { cursor ->

            check(cursor.moveToFirst()) {
                "Cannot read database version"
            }

            check(cursor.getInt(0) == DATABASE_VERSION) {
                "Backup version is not supported"
            }
        }

    } finally {
        database.close()
    }
}

}
