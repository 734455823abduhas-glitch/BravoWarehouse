package com.bravo.warehouse.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
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

val backupLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.CreateDocument("application/octet-stream")
) { uri: Uri? ->

    if (uri != null && !isLoading) {
        scope.launch {
            isLoading = true
            message = ""

            try {
                DatabaseBackupManager.exportBackup(context, uri)
                message = "تم إنشاء النسخة الاحتياطية بنجاح"
            } catch (e: Exception) {
                message = "فشل إنشاء النسخة: ${e.message}"
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

            try {
                DatabaseBackupManager.importBackup(context, uri)
                message = "تمت الاستعادة بنجاح"
                onRestoreComplete()
            } catch (e: Exception) {
                message = "فشلت الاستعادة: ${e.message}"
            } finally {
                isLoading = false
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
            text = "حافظ على بيانات المخازن بإنشاء نسخة احتياطية، ويمكنك استعادتها عند الحاجة.",
            fontSize = 16.sp,
            lineHeight = 25.sp
        )

        Spacer(modifier = Modifier.height(30.dp))

        Button(
            onClick = {
                backupLauncher.launch("BravoWarehouse_Backup.db")
            },
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "إنشاء نسخة احتياطية",
                modifier = Modifier.padding(8.dp)
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
                modifier = Modifier.padding(8.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (isLoading) {
            CircularProgressIndicator()

            Spacer(modifier = Modifier.height(12.dp))

            Text("يرجى الانتظار...")
        }

        if (message.isNotEmpty()) {
            Text(
                text = message,
                fontSize = 15.sp,
                modifier = Modifier.padding(top = 16.dp)
            )
        }
    }

    if (showRestoreDialog) {
        AlertDialog(
            onDismissRequest = {
                showRestoreDialog = false
            },
            title = {
                Text("تأكيد الاستعادة")
            },
            text = {
                Text(
                    "تنبيه: ستُستبدل بيانات المخازن الحالية " +
                        "بالبيانات الموجودة في النسخة الاحتياطية. هل تريد المتابعة؟"
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showRestoreDialog = false
                        restoreLauncher.launch(arrayOf("*/*"))
                    }
                ) {
                    Text("متابعة")
                }
            },
            dismissButton = {
                TextButton(
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
