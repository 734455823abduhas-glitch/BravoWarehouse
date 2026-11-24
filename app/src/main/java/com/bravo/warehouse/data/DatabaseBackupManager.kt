package com.bravo.warehouse.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import androidx.room.Room
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object DatabaseBackupManager {

    private const val DB_VERSION = 4
    private const val BACKUP_FORMAT = "BRAVO_WAREHOUSE_BACKUP"
    private const val DB_ENTRY = "bravo_warehouse.db"
    private const val SETTINGS_ENTRY = "settings.json"
    private const val MANIFEST_ENTRY = "manifest.json"

    private const val MAX_DATABASE_SIZE = 512L * 1024L * 1024L
    private const val MAX_SETTINGS_SIZE = 1024L * 1024L

    private fun preferences(context: Context) =
        context.getSharedPreferences(
            "bravo_settings",
            Context.MODE_PRIVATE
        )

    private fun createSettingsJson(context: Context): JSONObject {
        val prefs = preferences(context)

        return JSONObject().apply {
            put("factory_name", prefs.getString(
                "factory_name",
                "براڤو برو للصناعات البلاستيكية"
            ))

            put(
                "default_warehouse_id",
                prefs.getLong("default_warehouse_id", 0L)
            )

            put(
                "date_format",
                prefs.getString("date_format", "dd/MM/yyyy")
            )

            put(
                "time_format",
                prefs.getString("time_format", "24")
            )
        }
    }

    private fun restoreSettings(
        context: Context,
        settings: JSONObject
    ) {
        require(settings.has("factory_name")) {
            "النسخة لا تحتوي على اسم المصنع"
        }

        require(settings.has("default_warehouse_id")) {
            "النسخة لا تحتوي على المخزن الافتراضي"
        }

        require(settings.has("date_format")) {
            "النسخة لا تحتوي على تنسيق التاريخ"
        }

        require(settings.has("time_format")) {
            "النسخة لا تحتوي على تنسيق الوقت"
        }

        val factoryName = settings.getString("factory_name")
        val warehouseId = settings.getLong("default_warehouse_id")
        val dateFormat = settings.getString("date_format")
        val timeFormat = settings.getString("time_format")

        require(factoryName.isNotBlank()) {
            "اسم المصنع في النسخة غير صالح"
        }

        require(
            dateFormat in listOf(
                "dd/MM/yyyy",
                "yyyy-MM-dd",
                "dd-MM-yyyy"
            )
        ) {
            "تنسيق التاريخ غير صالح"
        }

        require(timeFormat == "12" || timeFormat == "24") {
            "تنسيق الوقت غير صالح"
        }

        val success = preferences(context).edit()
            .putString("factory_name", factoryName)
            .putLong("default_warehouse_id", warehouseId)
            .putString("date_format", dateFormat)
            .putString("time_format", timeFormat)
            .commit()

        check(success) {
            "تعذر حفظ إعدادات النسخة الاحتياطية"
        }
    }

    private fun restoreOldSettings(
        context: Context,
        oldSettings: Map<String, *>
    ) {
        val editor = preferences(context).edit().clear()

        oldSettings.forEach { (key, value) ->
            when (value) {
                is String -> editor.putString(key, value)
                is Boolean -> editor.putBoolean(key, value)
                is Int -> editor.putInt(key, value)
                is Long -> editor.putLong(key, value)
                is Float -> editor.putFloat(key, value)
                is Set<*> -> {
                    val strings = value.filterIsInstance<String>().toSet()
                    editor.putStringSet(key, strings)
                }
            }
        }

        check(editor.commit()) {
            "تعذرت استعادة الإعدادات السابقة"
        }
    }

    suspend fun exportBackup(
        context: Context,
        destination: Uri
    ) = withContext(Dispatchers.IO) {

        val dbFile = context.getDatabasePath(
            AppDatabase.DATABASE_NAME
        )

        require(dbFile.exists()) {
            "قاعدة البيانات غير موجودة"
        }

        val settingsJson = createSettingsJson(context)

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
                        "تعذر إنشاء ملف النسخة الاحتياطية"
                    }

                    ZipOutputStream(
                        BufferedOutputStream(output)
                    ).use { zip ->

                        zip.putNextEntry(
                            ZipEntry(MANIFEST_ENTRY)
                        )

                        val manifest = JSONObject().apply {
                            put("format", BACKUP_FORMAT)
                            put("database_version", DB_VERSION)
                            put("created_at", System.currentTimeMillis())
                        }

                        zip.write(
                            manifest.toString().toByteArray(Charsets.UTF_8)
                        )
                        zip.closeEntry()

                        zip.putNextEntry(
                            ZipEntry(DB_ENTRY)
                        )

                        FileInputStream(dbFile).use { input ->
                            input.copyTo(zip)
                        }

                        zip.closeEntry()

                        zip.putNextEntry(
                            ZipEntry(SETTINGS_ENTRY)
                        )

                        zip.write(
                            settingsJson.toString()
                                .toByteArray(Charsets.UTF_8)
                        )

                        zip.closeEntry()
                        zip.finish()
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

        val dbFile = context.getDatabasePath(
            AppDatabase.DATABASE_NAME
        )

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

        val oldSettings = preferences(context).all.toMap()

        var databaseClosed = false
        var swapStarted = false

        var oldDbMoved = false
        var oldWalMoved = false
        var oldShmMoved = false
        var settingsChanged = false

        var backupSettings: JSONObject? = null

        try {

            // استخراج النسخة، أو قراءة نسخة DB قديمة.
            context.contentResolver.openInputStream(source).use { raw ->

                requireNotNull(raw) {
                    "تعذر قراءة ملف النسخة"
                }

                BufferedInputStream(raw).use { input ->

                    input.mark(8)

                    val firstBytes = ByteArray(4)
                    val count = input.read(firstBytes)

                    input.reset()

                    val isZip =
                        count == 4 &&
                            firstBytes[0] == 0x50.toByte() &&
                            firstBytes[1] == 0x4B.toByte()

                    if (isZip) {

                        var foundDatabase = false
                        var foundManifest = false
                        var foundSettings = false

                        ZipInputStream(input).use { zip ->

                            var entry = zip.nextEntry

                            while (entry != null) {

                                when (entry.name) {

                                    DB_ENTRY -> {
                                        FileOutputStream(tempFile).use { output ->
                                            copyLimited(
                                                zip,
                                                output,
                                                MAX_DATABASE_SIZE
                                            )
                                            output.fd.sync()
                                        }
                                        foundDatabase = true
                                    }

                                    MANIFEST_ENTRY -> {
                                        val bytes = readLimited(
                                            zip,
                                            16 * 1024L
                                        )

                                        val manifest = JSONObject(
                                            String(
                                                bytes,
                                                Charsets.UTF_8
                                            )
                                        )

                                        require(
                                            manifest.optString("format") ==
                                                BACKUP_FORMAT
                                        ) {
                                            "نوع ملف النسخة غير معروف"
                                        }

                                        require(
                                            manifest.optInt(
                                                "database_version",
                                                -1
                                            ) == DB_VERSION
                                        ) {
                                            "إصدار قاعدة البيانات غير متوافق"
                                        }

                                        foundManifest = true
                                    }

                                    SETTINGS_ENTRY -> {
                                        val bytes = readLimited(
                                            zip,
                                            MAX_SETTINGS_SIZE
                                        )

                                        backupSettings = JSONObject(
                                            String(
                                                bytes,
                                                Charsets.UTF_8
                                            )
                                        )

                                        foundSettings = true
                                    }
                                }

                                zip.closeEntry()
                                entry = zip.nextEntry
                            }
                        }

                        require(foundDatabase) {
                            "النسخة لا تحتوي على قاعدة البيانات"
                        }

                        require(foundManifest) {
                            "ملف تعريف النسخة غير موجود"
                        }

                        require(foundSettings) {
                            "النسخة لا تحتوي على الإعدادات"
                        }

                    } else {

                        // دعم النسخ القديمة بصيغة SQLite DB.
                        FileOutputStream(tempFile).use { output ->
                            copyLimited(
                                input,
                                output,
                                MAX_DATABASE_SIZE
                            )
                            output.fd.sync()
                        }
                    }
                }
            }

            // التحقق من قاعدة البيانات قبل لمس البيانات الحالية.
            validateBackup(context, tempFile)

            // التحقق من الإعدادات قبل بدء الاستعادة.
            backupSettings?.let {
                validateSettings(it)
            }

            // إغلاق قاعدة البيانات الحالية.
            DatabaseProvider.closeDatabase()
            databaseClosed = true

            swapStarted = true

            // حفظ قاعدة البيانات القديمة.
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

            // فتح قاعدة البيانات المستعادة.
            val restoredDatabase =
                DatabaseProvider.getDatabase(context)

            restoredDatabase.openHelper.writableDatabase

            databaseClosed = false

            // استعادة الإعدادات بعد نجاح قاعدة البيانات.
            backupSettings?.let {
                restoreSettings(context, it)
                settingsChanged = true
            }

            // حذف النسخة القديمة بعد نجاح كل الخطوات.
            rollbackFile.delete()
            rollbackWal.delete()
            rollbackShm.delete()

        } catch (e: Exception) {

            // إعادة الإعدادات السابقة عند حدوث خطأ.
            if (settingsChanged) {
                try {
                    restoreOldSettings(context, oldSettings)
                } catch (settingsError: Exception) {
                    e.addSuppressed(settingsError)
                }
            }

            // استعادة قاعدة البيانات القديمة.
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

    private fun validateSettings(settings: JSONObject) {
        require(settings.has("factory_name")) {
            "اسم المصنع غير موجود في النسخة"
        }

        require(settings.has("default_warehouse_id")) {
            "المخزن الافتراضي غير موجود في النسخة"
        }

        require(settings.has("date_format")) {
            "تنسيق التاريخ غير موجود في النسخة"
        }

        require(settings.has("time_format")) {
            "تنسيق الوقت غير موجود في النسخة"
        }

        require(settings.getString("factory_name").isNotBlank()) {
            "اسم المصنع غير صالح"
        }

        require(
            settings.getString("date_format") in listOf(
                "dd/MM/yyyy",
                "yyyy-MM-dd",
                "dd-MM-yyyy"
            )
        ) {
            "تنسيق التاريخ غير صالح"
        }

        require(
            settings.getString("time_format") in listOf("12", "24")
        ) {
            "تنسيق الوقت غير صالح"
        }
    }

    private fun copyLimited(
        input: java.io.InputStream,
        output: java.io.OutputStream,
        maximum: Long
    ) {
        val buffer = ByteArray(8192)
        var total = 0L

        while (true) {
            val read = input.read(buffer)

            if (read == -1) break

            total += read

            require(total <= maximum) {
                "حجم ملف النسخة أكبر من الحد المسموح"
            }

            output.write(buffer, 0, read)
        }
    }

    private fun readLimited(
        input: java.io.InputStream,
        maximum: Long
    ): ByteArray {
        val output = java.io.ByteArrayOutputStream()
        copyLimited(input, output, maximum)
        return output.toByteArray()
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

        // اختبار النسخة باستخدام Room قبل اعتمادها.
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
