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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bravo.warehouse.data.repository.ItemRepository
import com.bravo.warehouse.data.repository.WarehouseRepository
import com.bravo.warehouse.data.repository.StockMovementRepository

private val ReportNavy = Color(0xFF17365D)
private val ReportGreen = Color(0xFF238B65)

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

var selectedReport by remember { mutableStateOf<String?>(null) }
var searchText by remember { mutableStateOf("") }

val approved = movements.filter { it.isApproved }

val incomingTypes = listOf("IN")
val outgoingTypes = listOf("OUT")
val returnTypes = listOf("RETURN_IN", "RETURN_OUT")
val adjustmentTypes = listOf("ADJUSTMENT_IN", "ADJUSTMENT_OUT")

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

    Spacer(modifier = Modifier.height(16.dp))

    if (selectedReport == null) {

        Text(
            "اختر التقرير المطلوب",
            style = MaterialTheme.typography.titleMedium,
            color = ReportNavy
        )

        Spacer(modifier = Modifier.height(16.dp))

        reportButtons.chunked(2).forEach { rowItems ->

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

            Spacer(modifier = Modifier.height(12.dp))
        }

        Spacer(modifier = Modifier.height(8.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = ReportNavy
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "ملخص المخزون",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

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

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    lazyItems(items.filter {
                        it.itemName.contains(searchText, true) ||
                        it.itemCode.contains(searchText, true)
                    }) { item ->

                        warehouses.forEach { warehouse ->

                            val itemMovements = approved.filter {
                                it.itemId == item.id &&
                                it.warehouseId == warehouse.id
                            }

                            val incoming = itemMovements
                                .filter {
                                    it.movementType in listOf(
                                        "IN", "RETURN_IN", "ADJUSTMENT_IN"
                                    )
                                }
                                .sumOf { it.quantity }

                            val outgoing = itemMovements
                                .filter {
                                    it.movementType in listOf(
                                        "OUT", "RETURN_OUT", "ADJUSTMENT_OUT"
                                    )
                                }
                                .sumOf { it.quantity }

                            val balance = incoming - outgoing

                            if (incoming != 0.0 || outgoing != 0.0) {
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
            }

            "تقرير الوارد",
            "تقرير المنصرف",
            "تقرير المرتجعات",
            "تقرير الجرد",
            "تقرير التحويلات",
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

                        "تقرير التحويلات" ->
                            movement.notes.contains("تحويل", true) ||
                            movement.notes.contains("transfer", true)

                        "كشف حركة صنف" -> true

                        else -> false
                    }

                    matchesSearch && matchesReport
                }

                if (selectedReport == "كشف حركة صنف" && searchText.isBlank()) {
                    Text(
                        "ابحث باسم الصنف أو رقمه لعرض حركته.",
                        color = ReportNavy
                    )
                } else if (
                    selectedReport == "تقرير التحويلات" &&
                    filtered.isEmpty()
                ) {
                    Text(
                        "لا توجد حركات تحويل يمكن تمييزها من ملاحظات الحركات. سنربط سجل التحويلات مباشرة في الخطوة التالية.",
                        color = ReportNavy
                    )
                } else if (filtered.isEmpty()) {
                    Text(
                        "لا توجد حركات مطابقة.",
                        color = ReportNavy
                    )
                }

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
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
                                "نوع الحركة: $typeName",
                                "المخزن: ${warehouse?.warehouseName ?: "غير معروف"}",
                                "الكمية: ${movement.quantity} ${item?.unit ?: ""}",
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
