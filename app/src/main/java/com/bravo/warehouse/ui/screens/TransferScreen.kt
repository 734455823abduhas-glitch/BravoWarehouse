package com.bravo.warehouse.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bravo.warehouse.data.entity.ItemEntity
import com.bravo.warehouse.data.entity.WarehouseEntity
import com.bravo.warehouse.data.repository.TransferRepository
import kotlinx.coroutines.launch

@Composable
fun TransferScreen(
    items: List<ItemEntity>,
    warehouses: List<WarehouseEntity>,
    transferRepository: TransferRepository
) {
    var selectedItem by remember { mutableStateOf<ItemEntity?>(null) }
    var source by remember { mutableStateOf<WarehouseEntity?>(null) }
    var destination by remember { mutableStateOf<WarehouseEntity?>(null) }

    var quantity by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }

    var itemMenu by remember { mutableStateOf(false) }
    var sourceMenu by remember { mutableStateOf(false) }
    var destinationMenu by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        Text(
            text = "التحويل بين المخازن",
            style = MaterialTheme.typography.headlineSmall
        )

        // اختيار الصنف
        Box {
            OutlinedTextField(
                value = selectedItem?.let {
                    "${it.itemCode} - ${it.itemName}"
                } ?: "",
                onValueChange = {},
                readOnly = true,
                label = { Text("اختر الصنف") },
                modifier = Modifier.fillMaxWidth()
            )

            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clickable { itemMenu = true }
            )

            DropdownMenu(
                expanded = itemMenu,
                onDismissRequest = { itemMenu = false },
                modifier = Modifier.heightIn(max = 300.dp)
            ) {
                items.filter { it.isActive }.forEach { item ->
                    DropdownMenuItem(
                        text = {
                            Text("${item.itemCode} - ${item.itemName}")
                        },
                        onClick = {
                            selectedItem = item
                            itemMenu = false
                        }
                    )
                }
            }
        }

        // المخزن المصدر
        Box {
            OutlinedTextField(
                value = source?.warehouseName ?: "",
                onValueChange = {},
                readOnly = true,
                label = { Text("المخزن المصدر") },
                modifier = Modifier.fillMaxWidth()
            )

            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clickable { sourceMenu = true }
            )

            DropdownMenu(
                expanded = sourceMenu,
                onDismissRequest = { sourceMenu = false },
                modifier = Modifier.heightIn(max = 300.dp)
            ) {
                warehouses.forEach { warehouse ->
                    DropdownMenuItem(
                        text = { Text(warehouse.warehouseName) },
                        onClick = {
                            source = warehouse

                            if (destination?.id == warehouse.id) {
                                destination = null
                            }

                            sourceMenu = false
                        }
                    )
                }
            }
        }

        // المخزن المستلم
        Box {
            OutlinedTextField(
                value = destination?.warehouseName ?: "",
                onValueChange = {},
                readOnly = true,
                label = { Text("المخزن المستلم") },
                modifier = Modifier.fillMaxWidth()
            )

            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clickable { destinationMenu = true }
            )

            DropdownMenu(
                expanded = destinationMenu,
                onDismissRequest = { destinationMenu = false },
                modifier = Modifier.heightIn(max = 300.dp)
            ) {
                warehouses
                    .filter { it.id != source?.id }
                    .forEach { warehouse ->
                        DropdownMenuItem(
                            text = { Text(warehouse.warehouseName) },
                            onClick = {
                                destination = warehouse
                                destinationMenu = false
                            }
                        )
                    }
            }
        }

        // الكمية
        OutlinedTextField(
            value = quantity,
            onValueChange = { quantity = it },
            label = { Text("الكمية") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        // الملاحظات
        OutlinedTextField(
            value = notes,
            onValueChange = { notes = it },
            label = { Text("ملاحظات") },
            modifier = Modifier.fillMaxWidth()
        )

        // تنفيذ التحويل
        Button(
            onClick = {
                val item = selectedItem
                val from = source
                val to = destination
                val amount = quantity.toDoubleOrNull()

                if (item == null) {
                    message = "يرجى اختيار الصنف"
                    return@Button
                }

                if (from == null || to == null) {
                    message = "يرجى اختيار المخزن المصدر والمستلم"
                    return@Button
                }

                if (from.id == to.id) {
                    message = "لا يمكن التحويل إلى نفس المخزن"
                    return@Button
                }

                if (amount == null || !amount.isFinite() || amount <= 0) {
                    message = "يرجى إدخال كمية صحيحة"
                    return@Button
                }

                scope.launch {
                    loading = true
                    message = ""

                    try {
                        transferRepository.createTransfer(
                            transferNumber = "TR-${System.currentTimeMillis()}",
                            itemId = item.id,
                            sourceWarehouseId = from.id,
                            destinationWarehouseId = to.id,
                            quantity = amount,
                            notes = notes.trim()
                        )

                        message = "تم التحويل بنجاح"

                        selectedItem = null
                        source = null
                        destination = null
                        quantity = ""
                        notes = ""

                    } catch (e: Exception) {
                        message = e.message ?: "فشل تنفيذ التحويل"
                    } finally {
                        loading = false
                    }
                }
            },
            enabled = !loading,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text("تنفيذ التحويل")
            }
        }

        if (message.isNotEmpty()) {
            Text(
                text = message,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
