package com.bravo.warehouse.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import androidx.room.Room
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.UUID

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
                    throw Exception("تعذر حفظ بيانات قاعدة البيانات")
                }
            }

        DatabaseProvider.closeDatabase()

        context.contentResolver.openOutputStream(destination, "wt").use { output ->
            requireNotNull(output) {
                "تعذر فتح ملف النسخة الاحتياطية"
            }

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

    if (!dbDirectory.exists()) {
        check(dbDirectory.mkdirs()) {
            "تعذر إنشاء مجلد قاعدة البيانات"
        }
    }

    val suffix = UUID.randomUUID().toString()

    val tempFile = File(dbDirectory, "bravo_restore_$suffix.db")
    val rollbackFile = File(dbDirectory, "bravo_rollback_$suffix.db")

    var databaseClosed = false
    var oldDatabaseMoved = false

    try {
        context.contentResolver.openInputStream(source).use { input ->
            requireNotNull(input) {
                "تعذر قراءة ملف النسخة الاحتياطية"
            }

            FileOutputStream(tempFile).use { output ->
                input.copyTo(output)
                output.flush()
                output.fd.sync()
            }
        }

        validateBackup(context, tempFile)

        DatabaseProvider.closeDatabase()
        databaseClosed = true

        val walFile = File(dbFile.path + "-wal")
        val shmFile = File(dbFile.path + "-shm")

        walFile.delete()
        shmFile.delete()

        if (dbFile.exists()) {
            check(dbFile.renameTo(rollbackFile)) {
                "تعذر حفظ قاعدة البيانات الحالية"
            }
            oldDatabaseMoved = true
        }

        check(tempFile.renameTo(dbFile)) {
            "تعذر تثبيت النسخة الاحتياطية"
        }

        // فتح قاعدة البيانات الجديدة للتأكد من سلامتها.
        DatabaseProvider.getDatabase(context)
        databaseClosed = false

        rollbackFile.delete()

    } catch (e: Exception) {

        try {
            if (databaseClosed || oldDatabaseMoved) {
                DatabaseProvider.closeDatabase()
            }

            if (oldDatabaseMoved && rollbackFile.exists()) {
                dbFile.delete()
                File(dbFile.path + "-wal").delete()
                File(dbFile.path + "-shm").delete()

                check(rollbackFile.renameTo(dbFile)) {
                    "تعذرت استعادة قاعدة البيانات السابقة"
                }
            }

            DatabaseProvider.getDatabase(context)
            databaseClosed = false

        } catch (restoreError: Exception) {
            e.addSuppressed(restoreError)
        }

        throw e

    } finally {
        tempFile.delete()

        File(tempFile.path + "-wal").delete()
        File(tempFile.path + "-shm").delete()

        if (databaseClosed) {
            DatabaseProvider.getDatabase(context)
        }
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

    // اختبار مخطط Room باستخدام اسم الملف داخل مجلد قواعد بيانات التطبيق.
    val testDatabase = Room.databaseBuilder(
        context.applicationContext,
        AppDatabase::class.java,
        file.name
    ).build()

    try {
        testDatabase.openHelper.writableDatabase
    } finally {
        testDatabase.close()
    }

    File(file.path + "-wal").delete()
    File(file.path + "-shm").delete()
}

}
