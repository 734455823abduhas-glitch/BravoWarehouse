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

    require(dbFile.exists()) {
        "قاعدة البيانات غير موجودة"
    }

    try {
        val database = DatabaseProvider.getDatabase(context)

        database.openHelper.writableDatabase
            .query("PRAGMA wal_checkpoint(TRUNCATE)")
            .use { cursor ->
                check(cursor.moveToFirst()) {
                    "تعذر فحص قاعدة البيانات"
                }

                check(cursor.getInt(0) == 0) {
                    "تعذر حفظ بيانات قاعدة البيانات"
                }
            }

        DatabaseProvider.closeDatabase()

        context.contentResolver
            .openOutputStream(destination, "wt").use { output ->

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

    val directory = dbFile.parentFile
        ?: throw Exception("تعذر تحديد مجلد قاعدة البيانات")

    if (!directory.exists()) {
        check(directory.mkdirs()) {
            "تعذر إنشاء مجلد قاعدة البيانات"
        }
    }

    val id = UUID.randomUUID().toString()

    val tempFile = File(directory, "bravo_temp_$id.db")
    val rollbackFile = File(directory, "bravo_old_$id.db")

    val walFile = File(dbFile.path + "-wal")
    val shmFile = File(dbFile.path + "-shm")

    val rollbackWal = File(directory, "bravo_old_$id-wal")
    val rollbackShm = File(directory, "bravo_old_$id-shm")

    var databaseClosed = false
    var swapStarted = false

    var oldDbMoved = false
    var oldWalMoved = false
    var oldShmMoved = false

    try {

        // قراءة النسخة إلى ملف مؤقت.
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

        // التحقق من سلامة النسخة ومخطط Room.
        validateBackup(context, tempFile)

        // إغلاق قاعدة البيانات الحالية.
        DatabaseProvider.closeDatabase()
        databaseClosed = true

        swapStarted = true

        // حفظ قاعدة البيانات القديمة وملفاتها الجانبية.
        if (dbFile.exists()) {
            check(dbFile.renameTo(rollbackFile)) {
                "تعذر حفظ قاعدة البيانات الحالية"
            }
            oldDbMoved = true
        }

        if (walFile.exists()) {
            check(walFile.renameTo(rollbackWal)) {
                "تعذر حفظ ملف WAL القديم"
            }
            oldWalMoved = true
        }

        if (shmFile.exists()) {
            check(shmFile.renameTo(rollbackShm)) {
                "تعذر حفظ ملف SHM القديم"
            }
            oldShmMoved = true
        }

        // تثبيت النسخة الجديدة.
        check(tempFile.renameTo(dbFile)) {
            "تعذر تثبيت النسخة الاحتياطية"
        }

        // فتح قاعدة البيانات الجديدة واختبارها.
        val restoredDatabase = DatabaseProvider.getDatabase(context)

        restoredDatabase.openHelper.writableDatabase

        databaseClosed = false

        // لا نحذف القديمة إلا بعد نجاح فتح الجديدة.
        rollbackFile.delete()
        rollbackWal.delete()
        rollbackShm.delete()

    } catch (e: Exception) {

        // محاولة استعادة قاعدة البيانات القديمة.
        if (databaseClosed || swapStarted) {

            try {
                DatabaseProvider.closeDatabase()

                dbFile.delete()
                walFile.delete()
                shmFile.delete()

                if (oldDbMoved) {
                    check(rollbackFile.renameTo(dbFile)) {
                        "تعذرت استعادة قاعدة البيانات القديمة"
                    }
                }

                if (oldWalMoved) {
                    check(rollbackWal.renameTo(walFile)) {
                        "تعذرت استعادة ملف WAL القديم"
                    }
                }

                if (oldShmMoved) {
                    check(rollbackShm.renameTo(shmFile)) {
                        "تعذرت استعادة ملف SHM القديم"
                    }
                }

                if (dbFile.exists()) {
                    DatabaseProvider.getDatabase(context)
                        .openHelper.writableDatabase
                }

                databaseClosed = false

            } catch (restoreError: Exception) {
                e.addSuppressed(restoreError)
            }
        }

        throw e

    } finally {

        tempFile.delete()
        File(tempFile.path + "-wal").delete()
        File(tempFile.path + "-shm").delete()

        if (databaseClosed && dbFile.exists()) {
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

        sqlite.rawQuery(
            "PRAGMA integrity_check",
            null
        ).use { cursor ->

            require(
                cursor.moveToFirst() &&
                cursor.getString(0) == "ok"
            ) {
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

    // اختبار ملف النسخة نفسه باستخدام Room.
    // الملف موجود في مجلد قواعد بيانات التطبيق.
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
