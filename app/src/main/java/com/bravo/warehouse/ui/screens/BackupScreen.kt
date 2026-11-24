package com.bravo.warehouse.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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

private val BackupNavy = Color(0xFF082F68)
private val BackupRed = Color(0xFFE5242A)
private val BackupBackground = Color(0xFFF3F6FC)
private val BackupText = Color(0xFF102B50)
private val BackupMuted = Color(0xFF526783)
private val BackupBorder = Color(0xFFB7C8DF)
private val BackupSuccessBackground = Color(0xFFE5F4EB)
private val BackupSuccessText = Color(0xFF176B3A)

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

                    message = "تم إنشاء النسخة الاحتياطية الشاملة بنجاح"
                    operationSucceeded = true

                } catch (e: Exception) {
                    message = "فشل إنشاء النسخة الاحتياطية: ${e.message}"

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

                    message = "تمت استعادة البيانات والإعدادات بنجاح"
                    operationSucceeded = true
                    restoreSucceeded = true

                } catch (e: Exception) {
                    message = "فشلت الاستعادة: ${e.message}"

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
                .background(BackupBackground)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
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
                    Text(
                        "رجوع",
                        color = BackupRed,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = "النسخ الاحتياطي والاستعادة",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = BackupNavy
                )
            }

            HorizontalDivider(
                color = BackupBorder,
                modifier = Modifier.padding(vertical = 16.dp)
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = BackupNavy
                )
            ) {
                Column(
                    modifier = Modifier.padding(18.dp)
                ) {
                    Text(
                        text = "حماية بيانات مصنع براڤو برو",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "أنشئ نسخة احتياطية من بيانات المخازن والإعدادات، واستعدها عند الحاجة.",
                        fontSize = 15.sp,
                        lineHeight = 25.sp,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                ),
                border = BorderStroke(1.dp, BackupBorder),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {

                Column(
                    modifier = Modifier.padding(16.dp)
                ) {

                    Text(
                        "محتويات النسخة الاحتياطية",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = BackupNavy
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    listOf(
                        "الأصناف والبيانات الأساسية",
                        "المخازن",
                        "حركات الوارد والمنصرف",
                        "التحويلات بين المخازن",
                        "إعدادات المصنع",
                        "المخزن الافتراضي",
                        "تنسيق التاريخ والوقت"
                    ).forEach { item ->

                        Row(
                            modifier = Modifier.padding(vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "✓",
                                color = BackupRed,
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp
                            )

                            Spacer(modifier = Modifier.width(10.dp))

                            Text(
                                item,
                                color = BackupText,
                                fontSize = 15.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    backupLauncher.launch("BravoWarehouse_Backup.zip")
                },
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = BackupNavy,
                    contentColor = Color.White,
                    disabledContainerColor = BackupNavy.copy(alpha = 0.5f)
                )
            ) {
                Text(
                    text = "إنشاء نسخة احتياطية",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedButton(
                onClick = {
                    showRestoreDialog = true
                },
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.5.dp, BackupRed),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = BackupRed
                )
            ) {
                Text(
                    text = "استعادة نسخة احتياطية",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            if (isLoading) {

                Spacer(modifier = Modifier.height(24.dp))

                CircularProgressIndicator(
                    color = BackupNavy
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    "جارٍ تنفيذ العملية، يرجى الانتظار...",
                    color = BackupText,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            if (message.isNotEmpty()) {

                Spacer(modifier = Modifier.height(20.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(
                        1.dp,
                        if (operationSucceeded) {
                            BackupSuccessText.copy(alpha = 0.3f)
                        } else {
                            MaterialTheme.colorScheme.error.copy(alpha = 0.3f)
                        }
                    ),
                    colors = CardDefaults.cardColors(
                        containerColor = if (operationSucceeded) {
                            BackupSuccessBackground
                        } else {
                            MaterialTheme.colorScheme.errorContainer
                        }
                    )
                ) {

                    Text(
                        text = message,
                        modifier = Modifier.padding(16.dp),
                        fontSize = 15.sp,
                        lineHeight = 24.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (operationSucceeded) {
                            BackupSuccessText
                        } else {
                            MaterialTheme.colorScheme.onErrorContainer
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "تنبيه: احتفظ بنسخة احتياطية في مكان آمن خارج الهاتف.",
                color = BackupMuted,
                fontSize = 13.sp,
                lineHeight = 21.sp
            )
        }

        if (showRestoreDialog) {

            AlertDialog(
                onDismissRequest = {
                    if (!isLoading) {
                        showRestoreDialog = false
                    }
                },

                containerColor = Color.White,

                title = {
                    Text(
                        "تأكيد استعادة النسخة",
                        color = BackupNavy,
                        fontWeight = FontWeight.Bold
                    )
                },

                text = {
                    Text(
                        "سيتم استبدال بيانات المخازن الحالية وإعدادات التطبيق بالبيانات الموجودة في النسخة المختارة. تأكد من الاحتفاظ بنسخة من بياناتك الحالية قبل المتابعة.",
                        color = BackupText,
                        lineHeight = 24.sp
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
                        Text(
                            "متابعة",
                            color = BackupRed,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },

                dismissButton = {
                    TextButton(
                        enabled = !isLoading,
                        onClick = {
                            showRestoreDialog = false
                        }
                    ) {
                        Text(
                            "إلغاء",
                            color = BackupMuted
                        )
                    }
                }
            )
        }
    }
}
