package com.bravo.warehouse.ui.screens

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
val context = LocalContext.current

val preferences = remember {
    context.getSharedPreferences(
        "bravo_settings",
        Context.MODE_PRIVATE
    )
}

val items by itemRepository.getAllItems()
    .collectAsState(initial = emptyList())

val warehouses by warehouseRepository.getAllWarehouses()
    .collectAsState(initial = emptyList())

var selectedItemId by remember { mutableStateOf<Long?>(null) }

var selectedWarehouseId by remember {
    mutableStateOf(
        preferences.getLong("default_warehouse_id", 0L)
            .takeIf { it > 0L }
    )
}

var actualQuantity by remember { mutableStateOf("") }
var systemBalance by remember { mutableStateOf(0.0) }

var itemMenu by remember { mutableStateOf(false) }
var warehouseMenu by remember { mutableStateOf(false) }

var message by remember { mutableStateOf("") }
var messageIsError by remember { mutableStateOf(false) }
var isSaving by remember { mutableStateOf(false) }

val selectedItem = items.firstOrNull {
    it.id == selectedItemId
}

val selectedWarehouse = warehouses.firstOrNull {
    it.id == selectedWarehouseId
}

val actual = actualQuantity.trim().toDoubleOrNull()

val difference = if (actual != null && actual.isFinite()) {
    actual - systemBalance
} else {
    0.0
}

// اختيار المخزن الافتراضي بعد تحميل المخازن
LaunchedEffect(warehouses) {
    if (warehouses.isNotEmpty()) {
        val defaultId = preferences.getLong(
            "default_warehouse_id",
            0L
        )

        if (
            selectedWarehouseId == null ||
            warehouses.none { it.id == selectedWarehouseId }
        ) {
            selectedWarehouseId =
                warehouses.firstOrNull { it.id == defaultId }?.id
                    ?: warehouses.firstOrNull()?.id
        }
    }
}

// تحديث رصيد الصنف في المخزن المختار
LaunchedEffect(selectedItemId, selectedWarehouseId) {
    val itemId = selectedItemId
    val warehouseId = selectedWarehouseId

    systemBalance = if (itemId != null && warehouseId != null) {
        try {
            movementRepository.getCurrentBalance(
                itemId,
                warehouseId
            )
        } catch (_: Exception) {
            0.0
        }
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
        TextButton(
            onClick = onBack,
            enabled = !isSaving
        ) {
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

    // اختيار الصنف
    Box {
        OutlinedTextField(
            value = selectedItem?.let {
                "${it.itemCode} - ${it.itemName}"
            } ?: "",
            onValueChange = {},
            readOnly = true,
            enabled = !isSaving,
            label = { Text("اختر الصنف") },
            modifier = Modifier.fillMaxWidth()
        )

        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable(enabled = !isSaving) {
                    itemMenu = true
                }
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
                        actualQuantity = ""
                        message = ""
                    }
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(12.dp))

    // اختيار المخزن
    Box {
        OutlinedTextField(
            value = selectedWarehouse?.warehouseName ?: "",
            onValueChange = {},
            readOnly = true,
            enabled = !isSaving,
            label = { Text("المخزن") },
            modifier = Modifier.fillMaxWidth()
        )

        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable(enabled = !isSaving) {
                    warehouseMenu = true
                }
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
                        actualQuantity = ""
                        message = ""
                    }
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(20.dp))

    // رصيد النظام
    if (selectedItem != null && selectedWarehouse != null) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor =
                    MaterialTheme.colorScheme.primaryContainer
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

    // الكمية الفعلية
    OutlinedTextField(
        value = actualQuantity,
        onValueChange = {
            actualQuantity = it
            message = ""
        },
        enabled = !isSaving,
        label = {
            Text("الكمية الفعلية بعد العد")
        },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true
    )

    Spacer(modifier = Modifier.height(16.dp))

    // نتيجة الجرد
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

    // اعتماد الجرد
    Button(
        onClick = {
            val itemId = selectedItemId
            val warehouseId = selectedWarehouseId
            val actualValue = actual

            if (itemId == null) {
                message = "يرجى اختيار الصنف"
                messageIsError = true
                return@Button
            }

            if (warehouseId == null) {
                message = "يرجى اختيار المخزن"
                messageIsError = true
                return@Button
            }

            if (
                actualValue == null ||
                !actualValue.isFinite() ||
                actualValue < 0
            ) {
                message = "أدخل كمية فعلية صحيحة، ويمكن أن تكون صفراً"
                messageIsError = true
                return@Button
            }

            if (isSaving) return@Button

            isSaving = true
            message = ""

            scope.launch {
                try {
                    // قراءة الرصيد الفعلي لحظة الاعتماد
                    val currentBalance =
                        movementRepository.getCurrentBalance(
                            itemId,
                            warehouseId
                        )

                    val actualDifference =
                        actualValue - currentBalance

                    if (
                        kotlin.math.abs(actualDifference) < 0.000001
                    ) {
                        systemBalance = currentBalance
                        message = "الجرد مطابق. لا توجد فروقات لتسويتها"
                        messageIsError = false
                    } else {
                        val adjustmentType =
                            if (actualDifference > 0) {
                                "ADJUSTMENT_IN"
                            } else {
                                "ADJUSTMENT_OUT"
                            }

                        val now = System.currentTimeMillis()

                        val movement = StockMovementEntity(
                            documentNumber = "STK-$now",
                            movementType = adjustmentType,
                            itemId = itemId,
                            warehouseId = warehouseId,
                            quantity = kotlin.math.abs(
                                actualDifference
                            ),
                            notes =
                                "تسوية جرد. " +
                                "رصيد النظام: $currentBalance، " +
                                "الكمية الفعلية: $actualValue، " +
                                "الفرق: $actualDifference",
                            movementDate = now,
                            createdAt = now,
                            isApproved = false
                        )

                        movementRepository.insertAndApproveMovement(
                            movement
                        )

                        systemBalance =
                            movementRepository.getCurrentBalance(
                                itemId,
                                warehouseId
                            )

                        actualQuantity = ""

                        message =
                            if (actualDifference > 0) {
                                "تم اعتماد الجرد وتسوية الزيادة بنجاح"
                            } else {
                                "تم اعتماد الجرد وتسوية العجز بنجاح"
                            }

                        messageIsError = false
                    }

                } catch (e: Exception) {
                    message = when {
                        e.message?.contains(
                            "Insufficient stock",
                            ignoreCase = true
                        ) == true ->
                            "الرصيد غير كافٍ لتنفيذ تسوية العجز"

                        else ->
                            "حدث خطأ أثناء تنفيذ الجرد: ${
                                e.message ?: "خطأ غير معروف"
                            }"
                    }

                    messageIsError = true

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
            color = if (messageIsError)
                MaterialTheme.colorScheme.error
            else
                MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.bodyLarge
        )
    }

    Spacer(modifier = Modifier.height(20.dp))
}

}
