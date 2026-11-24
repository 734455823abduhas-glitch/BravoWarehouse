package com.bravo.warehouse.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import androidx.room.Room
import com.bravo.warehouse.data.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

object DatabaseBackupManager {

private const val DB_VERSION = 4

suspend fun exportBackup(
    context: Context,
    destination: Uri
) = withContext(Dispatchers.IO) {

    val dbFile = context.getDatabasePath(AppDatabase.DATABASE_NAME)
    require(dbFile.exists()) { "قاعدة البيانات غير موجودة" }

    try {
        val database = DatabaseProvider.getDatabase(context)

        database.openHelper.writableDatabase
            .query("PRAGMA wal_checkpoint(TRUNCATE)")
            .use { cursor ->
                if (cursor.moveToFirst() && cursor.getInt(0) != 0) {
                    throw Exception("تعذر إتمام حفظ بيانات قاعدة البيانات")
                }
            }

        DatabaseProvider.closeDatabase()

        context.contentResolver.openOutputStream(destination, "wt").use { output ->
            requireNotNull(output) { "تعذر فتح ملف النسخة الاحتياطية" }
            FileInputStream(dbFile).use { input ->
                input.copyTo(output)
                output.flush()
            }
        }

    } finally {
        DatabaseProvider.getDatabase(context)
    }
}

suspend fun importBackup(
    context: Context,
    source: Uri
) = withContext(Dispatchers.IO) {

    val dbFile = context.getDatabasePath(AppDatabase.DATABASE_NAME)
    val dbDirectory = dbFile.parentFile
        ?: throw Exception("تعذر تحديد مجلد قاعدة البيانات")

    if (!dbDirectory.exists()) dbDirectory.mkdirs()

    val tempFile = File(dbDirectory, "bravo_restore_temp.db")
    val rollbackFile = File(dbDirectory, "bravo_restore_rollback.db")

    tempFile.delete()
    rollbackFile.delete()

    try {
        context.contentResolver.openInputStream(source).use { input ->
            requireNotNull(input) { "تعذر قراءة ملف النسخة الاحتياطية" }
            FileOutputStream(tempFile).use { output ->
                input.copyTo(output)
                output.flush()
                output.fd.sync()
            }
        }

        validateBackup(context, tempFile)

        DatabaseProvider.closeDatabase()

        val walFile = File(dbFile.path + "-wal")
        val shmFile = File(dbFile.path + "-shm")

        if (dbFile.exists()) {
            if (!dbFile.renameTo(rollbackFile)) {
                throw Exception("تعذر حفظ قاعدة البيانات الحالية")
            }
        }

        walFile.delete()
        shmFile.delete()

        if (!tempFile.renameTo(dbFile)) {
            if (rollbackFile.exists()) {
                rollbackFile.renameTo(dbFile)
            }
            throw Exception("تعذر تثبيت النسخة الاحتياطية")
        }

        try {
            DatabaseProvider.getDatabase(context)
        } catch (e: Exception) {
            DatabaseProvider.closeDatabase()
            dbFile.delete()

            if (rollbackFile.exists()) {
                rollbackFile.renameTo(dbFile)
            }

            DatabaseProvider.getDatabase(context)
            throw Exception("النسخة غير متوافقة، تمت محاولة استعادة البيانات السابقة")
        }

        rollbackFile.delete()

    } catch (e: Exception) {
        tempFile.delete()

        if (!dbFile.exists() && rollbackFile.exists()) {
            rollbackFile.renameTo(dbFile)
        }

        throw e
    }
}

private fun validateBackup(
    context: Context,
    file: File
) {
    require(file.exists() && file.length() > 0) {
        "ملف النسخة فارغ أو غير صالح"
    }

    val sqlite = SQLiteDatabase.openDatabase(
        file.absolutePath,
        null,
        SQLiteDatabase.OPEN_READONLY
    )

    try {
        sqlite.rawQuery("PRAGMA integrity_check", null).use { cursor ->
            require(cursor.moveToFirst() && cursor.getString(0) == "ok") {
                "قاعدة البيانات تالفة"
            }
        }

        require(sqlite.version == DB_VERSION) {
            "إصدار قاعدة البيانات غير متوافق"
        }

        val requiredTables = setOf(
            "items",
            "warehouses",
            "stock_movements",
            "stock_transfers",
            "room_master_table"
        )

        val existingTables = mutableSetOf<String>()

        sqlite.rawQuery(
            "SELECT name FROM sqlite_master WHERE type='table'",
            null
        ).use { cursor ->
            while (cursor.moveToNext()) {
                existingTables.add(cursor.getString(0))
            }
        }

        require(existingTables.containsAll(requiredTables)) {
            "النسخة لا تحتوي على جميع جداول التطبيق"
        }

    } finally {
        sqlite.close()
    }

    // اختبار توافق النسخة مع مخطط Room قبل استبدال قاعدة البيانات الحالية.
    val testDatabase = Room.databaseBuilder(
        context.applicationContext,
        AppDatabase::class.java,
        file.absolutePath
    ).build()

    try {
        testDatabase.openHelper.writableDatabase
    } finally {
        testDatabase.close()
    }
}

}
