package com.bravo.warehouse.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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

        Box {
            OutlinedTextField(
                value = selectedItem?.itemName ?: "",
                onValueChange = {},
                readOnly = true,
                label = { Text("اختر الصنف") },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { itemMenu = true }
            )

            DropdownMenu(
                expanded = itemMenu,
                onDismissRequest = { itemMenu = false }
            ) {
                items.forEach { item ->
                    DropdownMenuItem(
                        text = { Text(item.itemName) },
                        onClick = {
                            selectedItem = item
                            itemMenu = false
                        }
                    )
                }
            }
        }

        Box {
            OutlinedTextField(
                value = source?.warehouseName ?: "",
                onValueChange = {},
                readOnly = true,
                label = { Text("المخزن المصدر") },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { sourceMenu = true }
            )

            DropdownMenu(
                expanded = sourceMenu,
                onDismissRequest = { sourceMenu = false }
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

        Box {
            OutlinedTextField(
                value = destination?.warehouseName ?: "",
                onValueChange = {},
                readOnly = true,
                label = { Text("المخزن المستلم") },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { destinationMenu = true }
            )

            DropdownMenu(
                expanded = destinationMenu,
                onDismissRequest = { destinationMenu = false }
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

        OutlinedTextField(
            value = quantity,
            onValueChange = { quantity = it },
            label = { Text("الكمية") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = notes,
            onValueChange = { notes = it },
            label = { Text("ملاحظات") },
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = {
                val item = selectedItem
                val from = source
                val to = destination
                val amount = quantity.toDoubleOrNull()

                if (
                    item == null ||
                    from == null ||
                    to == null ||
                    amount == null ||
                    amount <= 0 ||
                    from.id == to.id
                ) {
                    message = "يرجى التأكد من جميع البيانات والكمية"
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
                            notes = notes
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
                    modifier = Modifier.size(22.dp)
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
