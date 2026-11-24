package com.bravo.warehouse.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bravo.warehouse.data.entity.StockMovementEntity
import com.bravo.warehouse.data.repository.ItemRepository
import com.bravo.warehouse.data.repository.WarehouseRepository
import com.bravo.warehouse.data.repository.StockMovementRepository
import kotlinx.coroutines.launch

@Composable
fun StockMovementsScreen(
itemRepository: ItemRepository,
warehouseRepository: WarehouseRepository,
movementRepository: StockMovementRepository,
onBack: () -> Unit
) {
val scope = rememberCoroutineScope()

val itemList by itemRepository.getAllItems()
    .collectAsState(initial = emptyList())

val warehouseList by warehouseRepository.getAllWarehouses()
    .collectAsState(initial = emptyList())

val movementList by movementRepository.getAllMovements()
    .collectAsState(initial = emptyList())

var movementType by remember { mutableStateOf("IN") }
var selectedItemId by remember { mutableStateOf<Long?>(null) }
var selectedWarehouseId by remember { mutableStateOf<Long?>(null) }
var quantity by remember { mutableStateOf("") }
var notes by remember { mutableStateOf("") }

var itemMenu by remember { mutableStateOf(false) }
var warehouseMenu by remember { mutableStateOf(false) }

var message by remember { mutableStateOf("") }
var isSaving by remember { mutableStateOf(false) }
var currentBalance by remember { mutableStateOf(0.0) }

val selectedItem = itemList.firstOrNull {
    it.id == selectedItemId
}

val selectedWarehouse = warehouseList.firstOrNull {
    it.id == selectedWarehouseId
}

val movementTitle = when (movementType) {
    "IN" -> "وارد"
    "OUT" -> "منصرف"
    "RETURN_IN" -> "مرتجع للمخزن"
    "RETURN_OUT" -> "مرتجع من المخزن"
    else -> "حركة مخزون"
}

LaunchedEffect(selectedItemId, selectedWarehouseId) {
    currentBalance = if (
        selectedItemId != null &&
        selectedWarehouseId != null
    ) {
        movementRepository.getCurrentBalance(
            selectedItemId!!,
            selectedWarehouseId!!
        )
    } else {
        0.0
    }
}

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
        TextButton(onClick = onBack) {
            Text("رجوع")
        }

        Text(
            text = "حركة المخزون",
            style = MaterialTheme.typography.headlineSmall
        )
    }

    Spacer(modifier = Modifier.height(12.dp))

    Text(
        text = "نوع الحركة",
        style = MaterialTheme.typography.titleLarge
    )

    Spacer(modifier = Modifier.height(8.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        listOf(
            "IN" to "وارد",
            "OUT" to "منصرف"
        ).forEach { (type, title) ->
            Button(
                onClick = {
                    movementType = type
                    message = ""
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (movementType == type)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.secondary
                )
            ) {
                Text(title)
            }
        }
    }

    Spacer(modifier = Modifier.height(6.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        listOf(
            "RETURN_IN" to "مرتجع للمخزن",
            "RETURN_OUT" to "مرتجع من المخزن"
        ).forEach { (type, title) ->
            Button(
                onClick = {
                    movementType = type
                    message = ""
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (movementType == type)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.secondary
                )
            ) {
                Text(title)
            }
        }
    }

    Spacer(modifier = Modifier.height(12.dp))

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
            itemList.filter { it.isActive }.forEach { item ->
                DropdownMenuItem(
                    text = {
                        Text("${item.itemCode} - ${item.itemName}")
                    },
                    onClick = {
                        selectedItemId = item.id
                        itemMenu = false
                    }
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(8.dp))

    Box {
        OutlinedTextField(
            value = selectedWarehouse?.warehouseName ?: "",
            onValueChange = {},
            readOnly = true,
            label = { Text("اختر المخزن") },
            modifier = Modifier.fillMaxWidth()
        )

        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable { warehouseMenu = true }
        )

        DropdownMenu(
            expanded = warehouseMenu,
            onDismissRequest = { warehouseMenu = false },
            modifier = Modifier.heightIn(max = 300.dp)
        ) {
            warehouseList.forEach { warehouse ->
                DropdownMenuItem(
                    text = {
                        Text(warehouse.warehouseName)
                    },
                    onClick = {
                        selectedWarehouseId = warehouse.id
                        warehouseMenu = false
                    }
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(8.dp))

    if (selectedItem != null && selectedWarehouse != null) {
        Text(
            text = "الرصيد الحالي: $currentBalance ${selectedItem.unit}",
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(modifier = Modifier.height(8.dp))
    }

    OutlinedTextField(
        value = quantity,
        onValueChange = { quantity = it },
        label = { Text("الكمية") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true
    )

    Spacer(modifier = Modifier.height(8.dp))

    OutlinedTextField(
        value = notes,
        onValueChange = { notes = it },
        label = { Text("ملاحظات") },
        modifier = Modifier.fillMaxWidth()
    )

    Spacer(modifier = Modifier.height(12.dp))

    Button(
        onClick = {
            val qty = quantity.toDoubleOrNull()

            if (selectedItemId == null) {
                message = "يرجى اختيار الصنف"
                return@Button
            }

            if (selectedWarehouseId == null) {
                message = "يرجى اختيار المخزن"
                return@Button
            }

            if (qty == null || !qty.isFinite() || qty <= 0) {
                message = "يرجى إدخال كمية صحيحة"
                return@Button
            }

            if (isSaving) return@Button

            isSaving = true
            message = ""

            scope.launch {
                try {
                    val movement = StockMovementEntity(
                        documentNumber = "MOV-${System.currentTimeMillis()}",
                        movementType = movementType,
                        itemId = selectedItemId!!,
                        warehouseId = selectedWarehouseId!!,
                        quantity = qty,
                        notes = notes.trim(),
                        isApproved = false
                    )

                    movementRepository.insertAndApproveMovement(movement)

                    message = "تم حفظ واعتماد $movementTitle بنجاح"

                    quantity = ""
                    notes = ""

                    currentBalance =
                        movementRepository.getCurrentBalance(
                            selectedItemId!!,
                            selectedWarehouseId!!
                        )

                } catch (e: Exception) {
                    message = when {
                        e.message?.contains("Insufficient stock") == true ->
                            "الرصيد غير كافٍ لتنفيذ الصرف"

                        else ->
                            "فشل حفظ الحركة: ${e.message ?: "خطأ غير معروف"}"
                    }
                } finally {
                    isSaving = false
                }
            }
        },
        modifier = Modifier.fillMaxWidth(),
        enabled = !isSaving
    ) {
        if (isSaving) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp
            )
        } else {
            Text("حفظ واعتماد $movementTitle")
        }
    }

    if (message.isNotEmpty()) {
        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = message,
            color = MaterialTheme.colorScheme.primary
        )
    }

    Spacer(modifier = Modifier.height(16.dp))

    HorizontalDivider()

    Spacer(modifier = Modifier.height(12.dp))

    Text(
        text = "سجل الحركات",
        style = MaterialTheme.typography.titleLarge
    )

    Spacer(modifier = Modifier.height(8.dp))

    if (movementList.isEmpty()) {
        Text("لا توجد حركات مسجلة حتى الآن")
    } else {
        LazyColumn(
            modifier = Modifier.weight(1f)
        ) {
            items(movementList) { movement ->

                val item = itemList.firstOrNull {
                    it.id == movement.itemId
                }

                val warehouse = warehouseList.firstOrNull {
                    it.id == movement.warehouseId
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Text(
                            text = when (movement.movementType) {
                                "IN" -> "وارد"
                                "OUT" -> "منصرف"
                                "RETURN_IN" -> "مرتجع للمخزن"
                                "RETURN_OUT" -> "مرتجع من المخزن"
                                "ADJUSTMENT_IN" -> "تسوية بالزيادة"
                                "ADJUSTMENT_OUT" -> "تسوية بالنقص"
                                "TRANSFER" -> "تحويل"
                                else -> "حركة أخرى"
                            },
                            style = MaterialTheme.typography.titleMedium
                        )

                        Text("الصنف: ${item?.itemName ?: movement.itemId}")

                        Text(
                            "المخزن: ${warehouse?.warehouseName ?: movement.warehouseId}"
                        )

                        Text("الكمية: ${movement.quantity}")

                        if (movement.notes.isNotBlank()) {
                            Text("ملاحظات: ${movement.notes}")
                        }

                        Text(
                            if (movement.isApproved)
                                "الحالة: معتمدة"
                            else
                                "الحالة: غير معتمدة"
                        )
                    }
                }
            }
        }
    }
}

}
