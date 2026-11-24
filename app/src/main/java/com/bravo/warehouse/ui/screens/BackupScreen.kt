package com.bravo.warehouse.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bravo.warehouse.data.DatabaseBackupManager
import kotlinx.coroutines.launch

@Composable
fun BackupScreen(
    onBack: () -> Unit,
    onRestoreComplete: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var message by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var showRestoreDialog by remember { mutableStateOf(false) }

    var operationSucceeded by remember { mutableStateOf(false) }

    val backupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri: Uri? ->

        if (uri != null && !isLoading) {
            scope.launch {
                isLoading = true
                message = ""
                operationSucceeded = false

                try {
                    DatabaseBackupManager.exportBackup(context, uri)

                    message =
                        "تم إنشاء النسخة الاحتياطية الشاملة بنجاح"

                    operationSucceeded = true

                } catch (e: Exception) {
                    message =
                        "فشل إنشاء النسخة الاحتياطية: ${e.message}"

                } finally {
                    isLoading = false
                }
            }
        }
    }

    val restoreLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->

        if (uri != null && !isLoading) {
            scope.launch {
                isLoading = true
                message = ""
                operationSucceeded = false

                var restoreSucceeded = false

                try {
                    DatabaseBackupManager.importBackup(context, uri)

                    message =
                        "تمت استعادة البيانات والإعدادات بنجاح"

                    operationSucceeded = true
                    restoreSucceeded = true

                } catch (e: Exception) {
                    message =
                        "فشلت الاستعادة: ${e.message}"

                } finally {
                    isLoading = false
                }

                if (restoreSucceeded) {
                    onRestoreComplete()
                }
            }
        }
    }

    CompositionLocalProvider(
        LocalLayoutDirection provides LayoutDirection.Rtl
    ) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {

                TextButton(
                    onClick = onBack,
                    enabled = !isLoading
                ) {
                    Text("رجوع")
                }

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = "النسخ الاحتياطي",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 16.dp)
            )

            Text(
                text = "حافظ على بيانات مصنع براڤو برو بإنشاء نسخة احتياطية يمكنك استعادتها عند الحاجة.",
                fontSize = 16.sp,
                lineHeight = 26.sp
            )

            Spacer(modifier = Modifier.height(20.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {

                Column(
                    modifier = Modifier.padding(16.dp)
                ) {

                    Text(
                        "محتويات النسخة الاحتياطية",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text("✓ الأصناف والبيانات الأساسية")
                    Text("✓ المخازن")
                    Text("✓ حركات الوارد والمنصرف")
                    Text("✓ التحويلات بين المخازن")
                    Text("✓ إعدادات المصنع")
                    Text("✓ المخزن الافتراضي")
                    Text("✓ تنسيق التاريخ والوقت")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    backupLauncher.launch(
                        "BravoWarehouse_Backup.zip"
                    )
                },
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth()
            ) {

                Text(
                    text = "إنشاء نسخة احتياطية",
                    modifier = Modifier.padding(8.dp),
                    fontSize = 16.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = {
                    showRestoreDialog = true
                },
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth()
            ) {

                Text(
                    text = "استعادة نسخة احتياطية",
                    modifier = Modifier.padding(8.dp),
                    fontSize = 16.sp
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (isLoading) {

                CircularProgressIndicator()

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    "جارٍ تنفيذ العملية، يرجى الانتظار..."
                )
            }

            if (message.isNotEmpty()) {

                Spacer(modifier = Modifier.height(12.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (operationSucceeded) {
                            Color(0xFFE2F4E8)
                        } else {
                            MaterialTheme.colorScheme.errorContainer
                        }
                    )
                ) {

                    Text(
                        text = message,
                        modifier = Modifier.padding(16.dp),
                        fontSize = 15.sp,
                        color = if (operationSucceeded) {
                            Color(0xFF176B3A)
                        } else {
                            MaterialTheme.colorScheme.onErrorContainer
                        }
                    )
                }
            }
        }

        if (showRestoreDialog) {

            AlertDialog(
                onDismissRequest = {
                    if (!isLoading) {
                        showRestoreDialog = false
                    }
                },

                title = {
                    Text("تأكيد استعادة النسخة")
                },

                text = {
                    Text(
                        "سيتم استبدال بيانات المخازن الحالية " +
                            "وإعدادات التطبيق بالبيانات الموجودة في النسخة المختارة. " +
                            "تأكد من الاحتفاظ بنسخة من بياناتك الحالية قبل المتابعة."
                    )
                },

                confirmButton = {
                    TextButton(
                        enabled = !isLoading,
                        onClick = {
                            showRestoreDialog = false

                            restoreLauncher.launch(
                                arrayOf(
                                    "application/zip",
                                    "application/octet-stream",
                                    "application/x-sqlite3",
                                    "application/vnd.sqlite3",
                                    "*/*"
                                )
                            )
                        }
                    ) {
                        Text("متابعة")
                    }
                },

                dismissButton = {
                    TextButton(
                        enabled = !isLoading,
                        onClick = {
                            showRestoreDialog = false
                        }
                    ) {
                        Text("إلغاء")
                    }
                }
            )
        }
    }
}
