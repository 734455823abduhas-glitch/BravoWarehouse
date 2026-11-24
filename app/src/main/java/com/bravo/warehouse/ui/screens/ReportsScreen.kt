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

    // جلب جميع الأصناف بما فيها المتوقفة
    val items by itemRepository.getAllItemsIncludingInactive()
        .collectAsState(initial = emptyList())

    // جلب جميع المخازن بما فيها المتوقفة
    val warehouses by warehouseRepository.getAllWarehousesIncludingInactive()
        .collectAsState(initial = emptyList())

    val movements by movementRepository.getAllMovements()
        .collectAsState(initial = emptyList())

    val transfers by transferRepository.getAllTransfers()
        .collectAsState(initial = emptyList())

    var selectedReport by remember { mutableStateOf<String?>(null) }
    var searchText by remember { mutableStateOf("") }

    // null تعني عرض جميع المخازن
    var selectedWarehouseId by remember {
        mutableStateOf<Long?>(null)
    }

    var warehouseMenuExpanded by remember {
        mutableStateOf(false)
    }

    val approved = movements
        .filter { it.isApproved }
        .sortedByDescending { it.movementDate }

    val approvedTransfers = transfers
        .filter { it.isApproved }
        .sortedByDescending { it.transferDate }

    val selectedWarehouse = warehouses.firstOrNull {
        it.id == selectedWarehouseId
    }

    val incomingTypes = listOf(
        "IN",
        "RETURN_IN",
        "ADJUSTMENT_IN"
    )

    val outgoingTypes = listOf(
        "OUT",
        "RETURN_OUT",
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
                        selectedWarehouseId = null
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
                        "جميع الأصناف: ${items.size}",
                        color = Color.White
                    )

                    Text(
                        "الأصناف النشطة: ${items.count { it.isActive }}",
                        color = Color.White
                    )

                    Text(
                        "الأصناف المتوقفة: ${items.count { !it.isActive }}",
                        color = Color.White
                    )

                    Text(
                        "جميع المخازن: ${warehouses.size}",
                        color = Color.White
                    )

                    Text(
                        "المخازن النشطة: ${warehouses.count { it.isActive }}",
                        color = Color.White
                    )

                    Text(
                        "المخازن المتوقفة: ${warehouses.count { !it.isActive }}",
                        color = Color.White
                    )

                    Text(
                        "الحركات المعتمدة: ${approved.size}",
                        color = Color.White
                    )

                    Text(
                        "التحويلات المعتمدة: ${approvedTransfers.size}",
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        "تاريخ التقرير: ${
                            DateTimeUtils.formatDateTime(
                                context,
                                System.currentTimeMillis()
                            )
                        }",
                        color = Color.White
                    )
                }
            }

        } else {

            // اختيار المخزن الذي سيعرض التقرير بياناته
            Text(
                "تصفية حسب المخزن",
                color = ReportNavy,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(6.dp))

            Box(modifier = Modifier.fillMaxWidth()) {

                OutlinedButton(
                    onClick = {
                        warehouseMenuExpanded = true
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        selectedWarehouse?.let {
                            it.warehouseName +
                                if (it.isActive) " (نشط)" else " (متوقف)"
                        } ?: "جميع المخازن"
                    )
                }

                DropdownMenu(
                    expanded = warehouseMenuExpanded,
                    onDismissRequest = {
                        warehouseMenuExpanded = false
                    }
                ) {

                    DropdownMenuItem(
                        text = {
                            Text("جميع المخازن")
                        },
                        onClick = {
                            selectedWarehouseId = null
                            warehouseMenuExpanded = false
                        }
                    )

                    warehouses.forEach { warehouse ->

                        DropdownMenuItem(
                            text = {
                                Text(
                                    warehouse.warehouseName +
                                        if (warehouse.isActive) {
                                            " (نشط)"
                                        } else {
                                            " (متوقف)"
                                        }
                                )
                            },
                            onClick = {
                                selectedWarehouseId = warehouse.id
                                warehouseMenuExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = searchText,
                onValueChange = {
                    searchText = it
                },
                label = {
                    Text("بحث باسم الصنف أو رقمه أو رقم المستند")
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            when (selectedReport) {

                // =========================
                // أرصدة الأصناف
                // =========================

                "أرصدة الأصناف" -> {

                    val filteredItems = items.filter {
                        searchText.isBlank() ||
                            it.itemName.contains(searchText, true) ||
                            it.itemCode.contains(searchText, true)
                    }

                    val balanceRows = filteredItems.flatMap { item ->

                        warehouses
                            .filter { warehouse ->
                                selectedWarehouseId == null ||
                                    warehouse.id == selectedWarehouseId
                            }
                            .map { warehouse ->

                                val itemMovements = approved.filter {
                                    it.itemId == item.id &&
                                        it.warehouseId == warehouse.id
                                }

                                val incoming = itemMovements
                                    .filter {
                                        it.movementType in incomingTypes
                                    }
                                    .sumOf { it.quantity }

                                val outgoing = itemMovements
                                    .filter {
                                        it.movementType in outgoingTypes
                                    }
                                    .sumOf { it.quantity }

                                Triple(
                                    item,
                                    warehouse,
                                    Triple(
                                        incoming,
                                        outgoing,
                                        incoming - outgoing
                                    )
                                )
                            }
                    }

                    if (balanceRows.isEmpty()) {
                        Text(
                            "لا توجد أرصدة مطابقة.",
                            color = ReportNavy
                        )
                    }

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {

                        lazyItems(balanceRows) { row ->

                            val item = row.first
                            val warehouse = row.second
                            val incoming = row.third.first
                            val outgoing = row.third.second
                            val balance = row.third.third

                            ReportCard(
                                title = "${item.itemCode} - ${item.itemName}",
                                lines = listOf(
                                    "حالة الصنف: ${
                                        if (item.isActive) "نشط" else "متوقف"
                                    }",
                                    "المخزن: ${warehouse.warehouseName}",
                                    "حالة المخزن: ${
                                        if (warehouse.isActive) "نشط" else "متوقف"
                                    }",
                                    "الوارد: $incoming ${item.unit}",
                                    "المنصرف: $outgoing ${item.unit}",
                                    "الرصيد: $balance ${item.unit}"
                                )
                            )
                        }
                    }
                }

                // =========================
                // تقرير التحويلات
                // =========================

                "تقرير التحويلات" -> {

                    val filteredTransfers = approvedTransfers.filter { transfer ->

                        val item = items.firstOrNull {
                            it.id == transfer.itemId
                        }

                        val matchesWarehouse =
                            selectedWarehouseId == null ||
                                transfer.sourceWarehouseId == selectedWarehouseId ||
                                transfer.destinationWarehouseId == selectedWarehouseId

                        val matchesSearch =
                            searchText.isBlank() ||
                                transfer.transferNumber.contains(searchText, true) ||
                                (item?.itemName?.contains(searchText, true) == true) ||
                                (item?.itemCode?.contains(searchText, true) == true)

                        matchesWarehouse && matchesSearch
                    }

                    if (filteredTransfers.isEmpty()) {
                        Text(
                            "لا توجد تحويلات مطابقة.",
                            color = ReportNavy
                        )
                    }

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {

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
                                    "حالة الصنف: ${
                                        item?.let {
                                            if (it.isActive) "نشط" else "متوقف"
                                        } ?: "غير معروف"
                                    }",
                                    "من المخزن: ${source?.warehouseName ?: "غير معروف"}",
                                    "إلى المخزن: ${destination?.warehouseName ?: "غير معروف"}",
                                    "الكمية: ${transfer.quantity} ${item?.unit ?: ""}",
                                    "التاريخ: ${
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

                // =========================
                // تقارير الحركات
                // =========================

                "تقرير الوارد",
                "تقرير المنصرف",
                "تقرير المرتجعات",
                "تقرير الجرد",
                "كشف حركة صنف" -> {

                    val filtered = if (
                        selectedReport == "كشف حركة صنف" &&
                        searchText.isBlank()
                    ) {
                        emptyList()
                    } else {

                        approved.filter { movement ->

                            val item = items.firstOrNull {
                                it.id == movement.itemId
                            }

                            val matchesWarehouse =
                                selectedWarehouseId == null ||
                                    movement.warehouseId == selectedWarehouseId

                            val matchesSearch =
                                searchText.isBlank() ||
                                    (item?.itemName?.contains(searchText, true) == true) ||
                                    (item?.itemCode?.contains(searchText, true) == true) ||
                                    movement.documentNumber.contains(searchText, true)

                            val matchesReport = when (selectedReport) {

                                "تقرير الوارد" ->
                                    movement.movementType == "IN"

                                "تقرير المنصرف" ->
                                    movement.movementType == "OUT"

                                "تقرير المرتجعات" ->
                                    movement.movementType in listOf(
                                        "RETURN_IN",
                                        "RETURN_OUT"
                                    )

                                "تقرير الجرد" ->
                                    movement.movementType in listOf(
                                        "ADJUSTMENT_IN",
                                        "ADJUSTMENT_OUT"
                                    )

                                "كشف حركة صنف" -> true

                                else -> false
                            }

                            matchesWarehouse &&
                                matchesSearch &&
                                matchesReport
                        }
                    }

                    if (
                        selectedReport == "كشف حركة صنف" &&
                        searchText.isBlank()
                    ) {
                        Text(
                            "ابحث باسم الصنف أو رقمه لعرض حركته.",
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
                                    "رمز الصنف: ${item?.itemCode ?: "غير معروف"}",
                                    "حالة الصنف: ${
                                        item?.let {
                                            if (it.isActive) "نشط" else "متوقف"
                                        } ?: "غير معروف"
                                    }",
                                    "رقم المستند: ${movement.documentNumber}",
                                    "نوع الحركة: $typeName",
                                    "المخزن: ${warehouse?.warehouseName ?: "غير معروف"}",
                                    "حالة المخزن: ${
                                        warehouse?.let {
                                            if (it.isActive) "نشط" else "متوقف"
                                        } ?: "غير معروف"
                                    }",
                                    "الكمية: ${movement.quantity} ${item?.unit ?: ""}",
                                    "التاريخ: ${
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
