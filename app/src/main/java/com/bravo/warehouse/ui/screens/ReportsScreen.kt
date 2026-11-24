package com.bravo.warehouse.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items as lazyItems
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bravo.warehouse.data.repository.ItemRepository
import com.bravo.warehouse.data.repository.WarehouseRepository
import com.bravo.warehouse.data.repository.StockMovementRepository
import com.bravo.warehouse.data.repository.TransferRepository
import com.bravo.warehouse.util.DateTimeUtils

private val ReportNavy = Color(0xFF17365D)
private val ReportGreen = Color(0xFF238B65)

@Composable
fun ReportsScreen(
itemRepository: ItemRepository,
warehouseRepository: WarehouseRepository,
movementRepository: StockMovementRepository,
transferRepository: TransferRepository,
onBack: () -> Unit
) {
val context = LocalContext.current

val items by itemRepository.getAllItems()
    .collectAsState(initial = emptyList())

val warehouses by warehouseRepository.getAllWarehouses()
    .collectAsState(initial = emptyList())

val movements by movementRepository.getAllMovements()
    .collectAsState(initial = emptyList())

val transfers by transferRepository.getAllTransfers()
    .collectAsState(initial = emptyList())

var selectedReport by remember { mutableStateOf<String?>(null) }
var searchText by remember { mutableStateOf("") }

val approved = remember(movements) {
    movements.filter { it.isApproved }
        .sortedByDescending { it.movementDate }
}

val approvedTransfers = remember(transfers) {
    transfers.filter { it.isApproved }
        .sortedByDescending { it.transferDate }
}

val incomingTypes = listOf("IN")
val outgoingTypes = listOf("OUT")
val returnTypes = listOf("RETURN_IN", "RETURN_OUT")
val adjustmentTypes = listOf(
    "ADJUSTMENT_IN",
    "ADJUSTMENT_OUT"
)

val reportButtons = listOf(
    "أرصدة الأصناف" to "📦",
    "تقرير الوارد" to "📥",
    "تقرير المنصرف" to "📤",
    "تقرير التحويلات" to "🔄",
    "تقرير المرتجعات" to "↩️",
    "تقرير الجرد" to "📋",
    "كشف حركة صنف" to "🔎"
)

Column(
    modifier = Modifier
        .fillMaxSize()
        .padding(16.dp)
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextButton(
            onClick = {
                if (selectedReport != null) {
                    selectedReport = null
                    searchText = ""
                } else {
                    onBack()
                }
            }
        ) {
            Text("رجوع")
        }

        Text(
            text = selectedReport ?: "التقارير المخزنية",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = ReportNavy
        )
    }

    HorizontalDivider()

    Spacer(modifier = Modifier.height(12.dp))

    if (selectedReport == null) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    "اختر التقرير المطلوب",
                    style = MaterialTheme.typography.titleMedium,
                    color = ReportNavy
                )
            }

            items(
                count = reportButtons.size / 2 +
                    reportButtons.size % 2
            ) { rowIndex ->
                val rowItems = reportButtons
                    .drop(rowIndex * 2)
                    .take(2)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    rowItems.forEach { report ->
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .height(115.dp)
                                .clickable {
                                    selectedReport = report.first
                                },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color.White
                            ),
                            elevation = CardDefaults.cardElevation(
                                defaultElevation = 3.dp
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(8.dp),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    report.second,
                                    style = MaterialTheme.typography.headlineMedium
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    report.first,
                                    fontWeight = FontWeight.Bold,
                                    color = ReportNavy
                                )
                            }
                        }
                    }

                    if (rowItems.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = ReportNavy
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "ملخص المخزون",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            "عدد الأصناف: ${items.size}",
                            color = Color.White
                        )

                        Text(
                            "عدد المخازن: ${warehouses.size}",
                            color = Color.White
                        )

                        Text(
                            "عدد الحركات المعتمدة: ${approved.size}",
                            color = Color.White
                        )

                        Text(
                            "عدد التحويلات المعتمدة: ${approvedTransfers.size}",
                            color = Color.White
                        )
                    }
                }
            }
        }
    } else {
        OutlinedTextField(
            value = searchText,
            onValueChange = { searchText = it },
            label = {
                Text("بحث باسم الصنف أو رقم المستند")
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        when (selectedReport) {
            "أرصدة الأصناف" -> {
                val filteredItems = items.filter {
                    it.itemName.contains(searchText, true) ||
                        it.itemCode.contains(searchText, true)
                }

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (filteredItems.isEmpty() || warehouses.isEmpty()) {
                        item {
                            Text(
                                "لا توجد أصناف أو مخازن لعرض الأرصدة.",
                                color = ReportNavy
                            )
                        }
                    }

                    lazyItems(filteredItems) { item ->
                        warehouses.forEach { warehouse ->
                            val itemMovements = approved.filter {
                                it.itemId == item.id &&
                                    it.warehouseId == warehouse.id
                            }

                            val incoming = itemMovements
                                .filter {
                                    it.movementType in listOf(
                                        "IN",
                                        "RETURN_IN",
                                        "ADJUSTMENT_IN"
                                    )
                                }
                                .sumOf { it.quantity }

                            val outgoing = itemMovements
                                .filter {
                                    it.movementType in listOf(
                                        "OUT",
                                        "RETURN_OUT",
                                        "ADJUSTMENT_OUT"
                                    )
                                }
                                .sumOf { it.quantity }

                            val balance = incoming - outgoing

                            ReportCard(
                                title = "${item.itemCode} - ${item.itemName}",
                                lines = listOf(
                                    "المخزن: ${warehouse.warehouseName}",
                                    "الوارد: $incoming ${item.unit}",
                                    "المنصرف: $outgoing ${item.unit}",
                                    "الرصيد: $balance ${item.unit}"
                                )
                            )
                        }
                    }
                }
            }

            "تقرير التحويلات" -> {
                val filteredTransfers = approvedTransfers.filter { transfer ->
                    val item = items.firstOrNull {
                        it.id == transfer.itemId
                    }

                    searchText.isBlank() ||
                        transfer.transferNumber.contains(searchText, true) ||
                        (item?.itemName?.contains(searchText, true) == true) ||
                        (item?.itemCode?.contains(searchText, true) == true)
                }

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (filteredTransfers.isEmpty()) {
                        item {
                            Text("لا توجد تحويلات معتمدة مطابقة.")
                        }
                    }

                    lazyItems(filteredTransfers) { transfer ->
                        val item = items.firstOrNull {
                            it.id == transfer.itemId
                        }

                        val source = warehouses.firstOrNull {
                            it.id == transfer.sourceWarehouseId
                        }

                        val destination = warehouses.firstOrNull {
                            it.id == transfer.destinationWarehouseId
                        }

                        ReportCard(
                            title = "تحويل رقم: ${transfer.transferNumber}",
                            lines = listOf(
                                "الصنف: ${item?.itemName ?: "غير معروف"}",
                                "رمز الصنف: ${item?.itemCode ?: "غير معروف"}",
                                "من المخزن: ${source?.warehouseName ?: "غير معروف"}",
                                "إلى المخزن: ${destination?.warehouseName ?: "غير معروف"}",
                                "الكمية: ${transfer.quantity} ${item?.unit ?: ""}",
                                "التاريخ والوقت: ${
                                    DateTimeUtils.formatDateTime(
                                        context,
                                        transfer.transferDate
                                    )
                                }",
                                "الحالة: معتمد",
                                "الملاحظات: ${transfer.notes}"
                            )
                        )
                    }
                }
            }

            "تقرير الوارد",
            "تقرير المنصرف",
            "تقرير المرتجعات",
            "تقرير الجرد",
            "كشف حركة صنف" -> {
                val filtered = approved.filter { movement ->
                    val item = items.firstOrNull {
                        it.id == movement.itemId
                    }

                    val matchesSearch =
                        searchText.isBlank() ||
                            (item?.itemName?.contains(searchText, true) == true) ||
                            (item?.itemCode?.contains(searchText, true) == true) ||
                            movement.documentNumber.contains(searchText, true)

                    val matchesReport = when (selectedReport) {
                        "تقرير الوارد" ->
                            movement.movementType in incomingTypes

                        "تقرير المنصرف" ->
                            movement.movementType in outgoingTypes

                        "تقرير المرتجعات" ->
                            movement.movementType in returnTypes

                        "تقرير الجرد" ->
                            movement.movementType in adjustmentTypes

                        "كشف حركة صنف" -> true

                        else -> false
                    }

                    matchesSearch && matchesReport
                }

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (
                        selectedReport == "كشف حركة صنف" &&
                        searchText.isBlank()
                    ) {
                        item {
                            Text(
                                "ابحث باسم الصنف أو رقمه لعرض حركته.",
                                color = ReportNavy
                            )
                        }
                    } else if (filtered.isEmpty()) {
                        item {
                            Text(
                                "لا توجد حركات مطابقة.",
                                color = ReportNavy
                            )
                        }
                    }

                    lazyItems(filtered) { movement ->
                        val item = items.firstOrNull {
                            it.id == movement.itemId
                        }

                        val warehouse = warehouses.firstOrNull {
                            it.id == movement.warehouseId
                        }

                        val typeName = when (movement.movementType) {
                            "IN" -> "وارد"
                            "OUT" -> "منصرف"
                            "RETURN_IN" -> "مرتجع للمخزن"
                            "RETURN_OUT" -> "مرتجع من المخزن"
                            "ADJUSTMENT_IN" -> "زيادة جرد"
                            "ADJUSTMENT_OUT" -> "عجز جرد"
                            else -> movement.movementType
                        }

                        ReportCard(
                            title = item?.itemName ?: "صنف غير معروف",
                            lines = listOf(
                                "رقم المستند: ${movement.documentNumber}",
                                "رمز الصنف: ${item?.itemCode ?: "غير معروف"}",
                                "نوع الحركة: $typeName",
                                "المخزن: ${warehouse?.warehouseName ?: "غير معروف"}",
                                "الكمية: ${movement.quantity} ${item?.unit ?: ""}",
                                "التاريخ والوقت: ${
                                    DateTimeUtils.formatDateTime(
                                        context,
                                        movement.movementDate
                                    )
                                }",
                                "الملاحظات: ${movement.notes}"
                            )
                        )
                    }
                }
            }
        }
    }
}

}

@Composable
private fun ReportCard(
title: String,
lines: List<String>
) {
Card(
modifier = Modifier.fillMaxWidth(),
shape = RoundedCornerShape(12.dp),
colors = CardDefaults.cardColors(
containerColor = Color.White
),
elevation = CardDefaults.cardElevation(
defaultElevation = 2.dp
)
) {
Column(
modifier = Modifier.padding(12.dp)
) {
Text(
title,
fontWeight = FontWeight.Bold,
color = ReportGreen,
style = MaterialTheme.typography.titleMedium
)

        Spacer(modifier = Modifier.height(6.dp))

        lines.forEach { line ->
            Text(
                line,
                color = ReportNavy,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

}
