package com.bravo.warehouse.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
fun StocktakingScreen(
itemRepository: ItemRepository,
warehouseRepository: WarehouseRepository,
movementRepository: StockMovementRepository,
onBack: () -> Unit
) {
val scope = rememberCoroutineScope()

val items by itemRepository.getAllItems()
    .collectAsState(initial = emptyList())

val warehouses by warehouseRepository.getAllWarehouses()
    .collectAsState(initial = emptyList())

var selectedItemId by remember { mutableStateOf<Long?>(null) }
var selectedWarehouseId by remember { mutableStateOf<Long?>(null) }

var actualQuantity by remember { mutableStateOf("") }
var systemBalance by remember { mutableStateOf(0.0) }

var itemMenu by remember { mutableStateOf(false) }
var warehouseMenu by remember { mutableStateOf(false) }

var message by remember { mutableStateOf("") }
var isSaving by remember { mutableStateOf(false) }

val selectedItem = items.firstOrNull {
    it.id == selectedItemId
}

val selectedWarehouse = warehouses.firstOrNull {
    it.id == selectedWarehouseId
}

val actual = actualQuantity.toDoubleOrNull()

val difference = if (
    actual != null && actual.isFinite()
) {
    actual - systemBalance
} else {
    0.0
}

LaunchedEffect(selectedItemId, selectedWarehouseId) {
    systemBalance = if (
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
        .verticalScroll(rememberScrollState())
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
            "الجرد الفعلي",
            style = MaterialTheme.typography.headlineSmall
        )
    }

    HorizontalDivider()

    Spacer(modifier = Modifier.height(16.dp))

    Text(
        "تسجيل جرد جديد",
        style = MaterialTheme.typography.titleLarge
    )

    Spacer(modifier = Modifier.height(16.dp))

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
                        selectedItemId = item.id
                        itemMenu = false
                        message = ""
                    }
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(12.dp))

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
            warehouses.forEach { warehouse ->
                DropdownMenuItem(
                    text = {
                        Text(warehouse.warehouseName)
                    },
                    onClick = {
                        selectedWarehouseId = warehouse.id
                        warehouseMenu = false
                        message = ""
                    }
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(20.dp))

    if (selectedItem != null && selectedWarehouse != null) {

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text("رصيد النظام")

                Text(
                    "$systemBalance ${selectedItem.unit}",
                    style = MaterialTheme.typography.headlineMedium
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }

    OutlinedTextField(
        value = actualQuantity,
        onValueChange = {
            actualQuantity = it
            message = ""
        },
        label = {
            Text("الكمية الفعلية بعد العد")
        },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true
    )

    Spacer(modifier = Modifier.height(16.dp))

    if (
        selectedItem != null &&
        selectedWarehouse != null &&
        actual != null &&
        actual.isFinite() &&
        actual >= 0
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = when {
                    difference > 0 ->
                        MaterialTheme.colorScheme.primaryContainer

                    difference < 0 ->
                        MaterialTheme.colorScheme.errorContainer

                    else ->
                        MaterialTheme.colorScheme.secondaryContainer
                }
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    "نتيجة الجرد",
                    style = MaterialTheme.typography.titleMedium
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    "الكمية الفعلية: $actual ${selectedItem.unit}"
                )

                Text(
                    "رصيد النظام: $systemBalance ${selectedItem.unit}"
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = when {
                        difference > 0 ->
                            "زيادة: $difference ${selectedItem.unit}"

                        difference < 0 ->
                            "عجز: ${-difference} ${selectedItem.unit}"

                        else ->
                            "الجرد مطابق لرصيد النظام"
                    },
                    style = MaterialTheme.typography.titleLarge
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    Button(
        onClick = {
            if (selectedItemId == null) {
                message = "يرجى اختيار الصنف"
                return@Button
            }

            if (selectedWarehouseId == null) {
                message = "يرجى اختيار المخزن"
                return@Button
            }

            if (
                actual == null ||
                !actual.isFinite() ||
                actual < 0
            ) {
                message = "أدخل كمية فعلية صحيحة، ويمكن أن تكون صفراً"
                return@Button
            }

            if (isSaving) return@Button

            isSaving = true
            message = ""

            scope.launch {
                try {
                    val currentSystemBalance =
                        movementRepository.getCurrentBalance(
                            selectedItemId!!,
                            selectedWarehouseId!!
                        )

                    val actualDifference =
                        actual - currentSystemBalance

                    if (kotlin.math.abs(actualDifference) < 0.000001) {
                        systemBalance = currentSystemBalance
                        message = "الجرد مطابق. لا توجد فروقات لتسويتها"
                    } else {

                        val adjustmentType =
                            if (actualDifference > 0) {
                                "ADJUSTMENT_IN"
                            } else {
                                "ADJUSTMENT_OUT"
                            }

                        val movement = StockMovementEntity(
                            documentNumber =
                                "STK-${System.currentTimeMillis()}",
                            movementType = adjustmentType,
                            itemId = selectedItemId!!,
                            warehouseId = selectedWarehouseId!!,
                            quantity = kotlin.math.abs(actualDifference),
                            notes = "تسوية جرد. " +
                                "رصيد النظام: $currentSystemBalance، " +
                                "الكمية الفعلية: $actual، " +
                                "الفرق: $actualDifference",
                            isApproved = false
                        )

                        movementRepository.insertAndApproveMovement(
                            movement
                        )

                        systemBalance =
                            movementRepository.getCurrentBalance(
                                selectedItemId!!,
                                selectedWarehouseId!!
                            )

                        actualQuantity = ""

                        message =
                            if (actualDifference > 0) {
                                "تم اعتماد الجرد وتسوية الزيادة بنجاح"
                            } else {
                                "تم اعتماد الجرد وتسوية العجز بنجاح"
                            }
                    }

                } catch (e: Exception) {
                    message = when {
                        e.message?.contains("Insufficient stock") == true ->
                            "الرصيد غير كافٍ لتنفيذ تسوية العجز"

                        else ->
                            "حدث خطأ أثناء تنفيذ الجرد: ${e.message ?: "خطأ غير معروف"}"
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
            Text("اعتماد الجرد وتسوية الفرق")
        }
    }

    if (message.isNotEmpty()) {
        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = message,
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

}
