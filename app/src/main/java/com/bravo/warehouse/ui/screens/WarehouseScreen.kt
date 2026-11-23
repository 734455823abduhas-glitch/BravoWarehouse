package com.bravo.warehouse.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import com.bravo.warehouse.data.entity.WarehouseEntity
import com.bravo.warehouse.data.repository.WarehouseRepository
import kotlinx.coroutines.launch

@Composable
fun WarehouseScreen(
    repository: WarehouseRepository,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()

    val warehouses by repository.getAllWarehouses()
        .collectAsState(initial = emptyList())

    var showDialog by remember { mutableStateOf(false) }
    var code by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(onClick = onBack) {
                    Text("رجوع")
                }

                Text(
                    text = "إدارة المخازن",
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
                }
            ) {
                Text("+")
            }
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {

            Text(
                text = "عدد المخازن: ${warehouses.size}",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (warehouses.isEmpty()) {
                Text("لا توجد مخازن حتى الآن")
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(warehouses) { warehouse ->

                        Card(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Text(
                                    text = warehouse.warehouseName,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleMedium
                                )

                                Text("كود المخزن: ${warehouse.warehouseCode}")

                                if (warehouse.location.isNotBlank()) {
                                    Text("الموقع: ${warehouse.location}")
                                }

                                if (warehouse.description.isNotBlank()) {
                                    Text("الوصف: ${warehouse.description}")
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
            onDismissRequest = { showDialog = false },
            title = {
                Text("إضافة مخزن جديد")
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = code,
                        onValueChange = { code = it },
                        label = { Text("كود المخزن") },
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("اسم المخزن") },
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = location,
                        onValueChange = { location = it },
                        label = { Text("موقع المخزن") },
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("ملاحظات") }
                    )

                    if (errorMessage.isNotBlank()) {
                        Text(
                            text = errorMessage,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (code.isBlank() || name.isBlank()) {
                            errorMessage = "يرجى إدخال الكود واسم المخزن"
                        } else {
                            scope.launch {
                                try {
                                    val warehouse = WarehouseEntity(
                                        warehouseCode = code.trim(),
                                        warehouseName = name.trim(),
                                        location = location.trim(),
                                        description = description.trim()
                                    )

                                    repository.insertWarehouse(warehouse)
                                    showDialog = false
                                } catch (e: Exception) {
                                    errorMessage = "تعذر الحفظ. تأكد أن كود المخزن غير مكرر."
                                }
                            }
                        }
                    }
                ) {
                    Text("حفظ")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDialog = false }
                ) {
                    Text("إلغاء")
                }
            }
        )
    }
}
