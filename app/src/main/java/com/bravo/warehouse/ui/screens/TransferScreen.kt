package com.bravo.warehouse.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import com.bravo.warehouse.data.entity.ItemEntity
import com.bravo.warehouse.data.entity.WarehouseEntity
import com.bravo.warehouse.data.repository.TransferRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransferScreen(
    items: List<ItemEntity>,
    warehouses: List<WarehouseEntity>,
    transferRepository: TransferRepository
) {
    var selectedItem by remember { mutableStateOf<ItemEntity?>(null) }
    var sourceWarehouse by remember { mutableStateOf<WarehouseEntity?>(null) }
    var destinationWarehouse by remember { mutableStateOf<WarehouseEntity?>(null) }
    var quantity by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }

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

        Text("الصنف")

        var itemExpanded by remember { mutableStateOf(false) }

        ExposedDropdownMenuBox(
            expanded = itemExpanded,
            onExpandedChange = { itemExpanded = !itemExpanded }
        ) {
            OutlinedTextField(
                value = selectedItem?.itemName ?: "",
                onValueChange = {},
                readOnly = true,
                label = { Text("اختر الصنف") },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor()
            )

            ExposedDropdownMenu(
                expanded = itemExpanded,
                onDismissRequest = { itemExpanded = false }
            ) {
                items.forEach { item ->
                    DropdownMenuItem(
                        text = { Text(item.itemName) },
                        onClick = {
                            selectedItem = item
                            itemExpanded = false
                        }
                    )
                }
            }
        }

        Text("المخزن المصدر")

        var sourceExpanded by remember { mutableStateOf(false) }

        ExposedDropdownMenuBox
