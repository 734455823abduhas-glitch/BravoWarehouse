package com.bravo.warehouse.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bravo.warehouse.data.entity.WarehouseEntity
import com.bravo.warehouse.data.repository.WarehouseRepository
import kotlinx.coroutines.launch

private val WarehouseNavy = Color(0xFF082F68)
private val WarehouseRed = Color(0xFFE5242A)
private val WarehouseBackground = Color(0xFFF3F6FC)
private val WarehouseText = Color(0xFF102B50)
private val WarehouseMuted = Color(0xFF526783)
private val WarehouseBorder = Color(0xFFB7C8DF)

@Composable
fun WarehouseScreen(
    repository: WarehouseRepository,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val warehouses by repository.getAllWarehouses()
        .collectAsState(initial = emptyList())

    var showDialog by remember { mutableStateOf(false) }
    var warehouseToDeactivate by remember {
        mutableStateOf<WarehouseEntity?>(null)
    }

    var code by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }
    var isSaving by remember { mutableStateOf(false) }
    var isDeactivating by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = WarehouseBackground,
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) {
                Snackbar(
                    snackbarData = it,
                    containerColor = WarehouseNavy,
                    contentColor = Color.White
                )
            }
        },
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onBack,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = WarehouseNavy
                    )
                ) {
                    Text("رجوع")
                }

                Text(
                    text = "إدارة المخازن",
                    color = WarehouseNavy,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    code = ""
                    name = ""
                    location = ""
                    description = ""
                    errorMessage = ""
                    showDialog = true
                },
                containerColor = WarehouseRed,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("+", style = MaterialTheme.typography.headlineMedium)
            }
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = WarehouseNavy
                )
            ) {
                Column(
                    modifier = Modifier.padding(18.dp)
                ) {
                    Text(
                        text = "إجمالي المخازن",
                        color = Color.White.copy(alpha = 0.85f),
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = warehouses.size.toString(),
                        color = Color.White,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "قائمة المخازن",
                color = WarehouseNavy,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(10.dp))

            if (warehouses.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "لا توجد مخازن حتى الآن",
                        color = WarehouseMuted
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(warehouses, key = { it.id }) { warehouse ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color.White
                            ),
                            elevation = CardDefaults.cardElevation(
                                defaultElevation = 2.dp
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Text(
                                    text = warehouse.warehouseName,
                                    color = WarehouseNavy,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleMedium
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = "كود المخزن: ${warehouse.warehouseCode}",
                                    color = WarehouseText
                                )

                                if (warehouse.location.isNotBlank()) {
                                    Text(
                                        text = "الموقع: ${warehouse.location}",
                                        color = WarehouseMuted
                                    )
                                }

                                if (warehouse.description.isNotBlank()) {
                                    Text(
                                        text = "الوصف: ${warehouse.description}",
                                        color = WarehouseMuted
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                OutlinedButton(
                                    onClick = {
                                        warehouseToDeactivate = warehouse
                                    },
                                    enabled = !isDeactivating,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(
                                        1.dp,
                                        WarehouseRed
                                    ),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = WarehouseRed
                                    )
                                ) {
                                    Text("تعطيل المخزن")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = {
                if (!isSaving) showDialog = false
            },
            containerColor = Color.White,
            titleContentColor = WarehouseNavy,
            textContentColor = WarehouseText,
            title = {
                Text(
                    "إضافة مخزن جديد",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier.heightIn(max = 450.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = code,
                        onValueChange = { code = it },
                        label = { Text("كود المخزن") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = warehouseFieldColors()
                    )

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("اسم المخزن") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = warehouseFieldColors()
                    )

                    OutlinedTextField(
                        value = location,
                        onValueChange = { location = it },
                        label = { Text("موقع المخزن") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = warehouseFieldColors()
                    )

                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("ملاحظات") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = warehouseFieldColors()
                    )

                    if (errorMessage.isNotBlank()) {
                        Text(
                            text = errorMessage,
                            color = WarehouseRed,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    enabled = !isSaving,
                    onClick = {
                        if (code.isBlank() || name.isBlank()) {
                            errorMessage = "يرجى إدخال الكود واسم المخزن"
                        } else {
                            scope.launch {
                                isSaving = true
                                try {
                                    val warehouse = WarehouseEntity(
                                        warehouseCode = code.trim(),
                                        warehouseName = name.trim(),
                                        location = location.trim(),
                                        description = description.trim()
                                    )

                                    repository.insertWarehouse(warehouse)
                                    showDialog = false
                                    snackbarHostState.showSnackbar(
                                        "تم إضافة المخزن بنجاح"
                                    )
                                } catch (e: Exception) {
                                    errorMessage =
                                        "تعذر الحفظ. تأكد أن كود المخزن غير مكرر."
                                } finally {
                                    isSaving = false
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = WarehouseNavy,
                        contentColor = Color.White
                    )
                ) {
                    Text(if (isSaving) "جارٍ الحفظ..." else "حفظ")
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !isSaving,
                    onClick = { showDialog = false },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = WarehouseRed
                    )
                ) {
                    Text("إلغاء")
                }
            }
        )
    }

    warehouseToDeactivate?.let { warehouse ->
        AlertDialog(
            onDismissRequest = {
                if (!isDeactivating) warehouseToDeactivate = null
            },
            containerColor = Color.White,
            titleContentColor = WarehouseNavy,
            textContentColor = WarehouseText,
            title = {
                Text("تأكيد تعطيل المخزن")
            },
            text = {
                Text(
                    "هل أنت متأكد من تعطيل مخزن «${warehouse.warehouseName}»؟ " +
                        "لن يظهر ضمن المخازن النشطة بعد التعطيل."
                )
            },
            confirmButton = {
                TextButton(
                    enabled = !isDeactivating,
                    onClick = {
                        val warehouseId = warehouse.id

                        scope.launch {
                            isDeactivating = true
                            try {
                                val deactivated =
                                    repository.deactivateWarehouse(warehouseId)

                                snackbarHostState.showSnackbar(
                                    if (deactivated) {
                                        "تم تعطيل المخزن بنجاح"
                                    } else {
                                        "تعذر التعطيل: المخزن يحتوي على أرصدة أو أنه غير نشط"
                                    }
                                )
                            } catch (e: Exception) {
                                snackbarHostState.showSnackbar(
                                    "حدث خطأ أثناء تعطيل المخزن"
                                )
                            } finally {
                                isDeactivating = false
                                warehouseToDeactivate = null
                            }
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = WarehouseRed
                    )
                ) {
                    Text("تأكيد التعطيل")
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !isDeactivating,
                    onClick = { warehouseToDeactivate = null },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = WarehouseNavy
                    )
                ) {
                    Text("إلغاء")
                }
            }
        )
    }
}

@Composable
private fun warehouseFieldColors() =
    OutlinedTextFieldDefaults.colors(
        focusedBorderColor = WarehouseNavy,
        unfocusedBorderColor = WarehouseBorder,
        focusedLabelColor = WarehouseNavy,
        unfocusedLabelColor = WarehouseMuted,
        focusedTextColor = WarehouseText,
        unfocusedTextColor = WarehouseText,
        cursorColor = WarehouseRed
    )
