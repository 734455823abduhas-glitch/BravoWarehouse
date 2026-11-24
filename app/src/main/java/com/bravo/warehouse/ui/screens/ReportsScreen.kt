package com.bravo.warehouse.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items as lazyItems
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bravo.warehouse.data.repository.ItemRepository
import com.bravo.warehouse.data.repository.WarehouseRepository
import com.bravo.warehouse.data.repository.StockMovementRepository

@Composable
fun ReportsScreen(
itemRepository: ItemRepository,
warehouseRepository: WarehouseRepository,
movementRepository: StockMovementRepository,
onBack: () -> Unit
) {
val items by itemRepository.getAllItems()
.collectAsState(initial = emptyList())

val warehouses by warehouseRepository.getAllWarehouses()
    .collectAsState(initial = emptyList())

val movements by movementRepository.getAllMovements()
    .collectAsState(initial = emptyList())

var searchText by remember { mutableStateOf("") }

val approvedMovements = movements.filter { it.isApproved }

val incomingTypes = listOf(
    "IN", "RETURN_IN", "ADJUSTMENT_IN"
)

val outgoingTypes = listOf(
    "OUT", "RETURN_OUT", "ADJUSTMENT_OUT"
)

val totalIncoming = approvedMovements
    .filter { it.movementType in incomingTypes }
    .sumOf { it.quantity }

val totalOutgoing = approvedMovements
    .filter { it.movementType in outgoingTypes }
    .sumOf { it.quantity }

val filteredItems = items.filter {
    it.itemName.contains(searchText, ignoreCase = true) ||
    it.itemCode.contains(searchText, ignoreCase = true)
}

Column(
    modifier = Modifier
        .fillMaxSize()
        .padding(16.dp)
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        TextButton(onClick = onBack) {
            Text("رجوع")
        }

        Text(
            "التقارير المخزنية",
            style = MaterialTheme.typography.titleLarge
        )
    }

    HorizontalDivider()

    Spacer(modifier = Modifier.height(12.dp))

    Text(
        "ملخص حركة المخزون",
        style = MaterialTheme.typography.titleMedium
    )

    Spacer(modifier = Modifier.height(10.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SummaryCard(
            title = "إجمالي الوارد",
            value = totalIncoming.toString(),
            modifier = Modifier.weight(1f)
        )

        SummaryCard(
            title = "إجمالي المنصرف",
            value = totalOutgoing.toString(),
            modifier = Modifier.weight(1f)
        )
    }

    Spacer(modifier = Modifier.height(16.dp))

    OutlinedTextField(
        value = searchText,
        onValueChange = { searchText = it },
        label = { Text("ابحث باسم الصنف أو رقمه") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true
    )

    Spacer(modifier = Modifier.height(12.dp))

    Text(
        "أرصدة الأصناف حسب المخزن",
        style = MaterialTheme.typography.titleMedium
    )

    Spacer(modifier = Modifier.height(8.dp))

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        lazyItems(filteredItems) { item ->

            val itemWarehouses = warehouses

            itemWarehouses.forEach { warehouse ->

                val itemMovements = approvedMovements.filter {
                    it.itemId == item.id &&
                    it.warehouseId == warehouse.id
                }

                val incoming = itemMovements
                    .filter { it.movementType in incomingTypes }
                    .sumOf { it.quantity }

                val outgoing = itemMovements
                    .filter { it.movementType in outgoingTypes }
                    .sumOf { it.quantity }

                val balance = incoming - outgoing

                if (incoming != 0.0 || outgoing != 0.0 || balance != 0.0) {
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Text(
                                "${item.itemCode} - ${item.itemName}",
                                style = MaterialTheme.typography.titleMedium
                            )

                            Text("المخزن: ${warehouse.warehouseName}")

                            Text("الوارد: $incoming ${item.unit}")

                            Text("المنصرف: $outgoing ${item.unit}")

                            Text(
                                "الرصيد الحالي: $balance ${item.unit}",
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                }
            }
        }
    }
}

}

@Composable
private fun SummaryCard(
title: String,
value: String,
modifier: Modifier = Modifier
) {
Card(modifier = modifier) {
Column(
modifier = Modifier.padding(12.dp)
) {
Text(title, style = MaterialTheme.typography.bodyMedium)

        Text(
            value,
            style = MaterialTheme.typography.titleLarge
        )
    }
}

}
