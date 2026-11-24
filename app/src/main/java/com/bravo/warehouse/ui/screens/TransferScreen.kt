package com.bravo.warehouse.ui.screens

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
val context = LocalContext.current

val preferences = remember {
    context.getSharedPreferences(
        "bravo_settings",
        Context.MODE_PRIVATE
    )
}

val defaultWarehouseId = remember {
    preferences.getLong("default_warehouse_id", 0L)
        .takeIf { it > 0L }
}

var selectedItem by remember {
    mutableStateOf<ItemEntity?>(null)
}

var source by remember {
    mutableStateOf<WarehouseEntity?>(null)
}

var destination by remember {
    mutableStateOf<WarehouseEntity?>(null)
}

var quantity by remember { mutableStateOf("") }
var notes by remember { mutableStateOf("") }
var message by remember { mutableStateOf("") }
var loading by remember { mutableStateOf(false) }

var itemMenu by remember { mutableStateOf(false) }
var sourceMenu by remember { mutableStateOf(false) }
var destinationMenu by remember { mutableStateOf(false) }

val scope = rememberCoroutineScope()

// تعيين المخزن الافتراضي كمصدر عند تحميل المخازن.
LaunchedEffect(warehouses, defaultWarehouseId) {
    val currentSourceExists = warehouses.any {
        it.id == source?.id
    }

    if (source == null || !currentSourceExists) {
        source = warehouses.firstOrNull {
            it.id == defaultWarehouseId
        }
    }

    if (destination?.id == source?.id) {
        destination = null
    }
}

Column(
    modifier = Modifier
        .fillMaxSize()
        .verticalScroll(rememberScrollState())
        .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp)
) {

    Text(
        text = "التحويل بين المخازن",
        style = MaterialTheme.typography.headlineSmall
    )

    HorizontalDivider()

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
                        message = ""
                    }
                )
            }
        }
    }

    // مخزن المصدر
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
                        message = ""
                    }
                )
            }
        }
    }

    // مخزن الوجهة مستقل عن المخزن الافتراضي
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
                        text = {
                            Text(warehouse.warehouseName)
                        },
                        onClick = {
                            destination = warehouse
                            destinationMenu = false
                            message = ""
                        }
                    )
                }
        }
    }

    OutlinedTextField(
        value = quantity,
        onValueChange = {
            quantity = it
            message = ""
        },
        label = { Text("الكمية") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true
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

            if (item == null) {
                message = "يرجى اختيار الصنف"
                return@Button
            }

            if (from == null) {
                message = "يرجى اختيار مخزن المصدر"
                return@Button
            }

            if (to == null) {
                message = "يرجى اختيار المخزن المستلم"
                return@Button
            }

            if (from.id == to.id) {
                message = "لا يمكن التحويل إلى المخزن نفسه"
                return@Button
            }

            if (
                amount == null ||
                !amount.isFinite() ||
                amount <= 0.0
            ) {
                message = "يرجى إدخال كمية صحيحة أكبر من صفر"
                return@Button
            }

            if (loading) return@Button

            scope.launch {
                loading = true
                message = ""

                try {
                    val transferNumber =
                        "TR-${System.currentTimeMillis()}"

                    transferRepository.createTransfer(
                        transferNumber = transferNumber,
                        itemId = item.id,
                        sourceWarehouseId = from.id,
                        destinationWarehouseId = to.id,
                        quantity = amount,
                        notes = notes.trim()
                    )

                    message = "تم التحويل بنجاح. رقم المستند: $transferNumber"

                    selectedItem = null
                    destination = null
                    quantity = ""
                    notes = ""

                } catch (e: Exception) {

                    message = when {
                        e.message?.contains(
                            "Insufficient stock",
                            ignoreCase = true
                        ) == true ->
                            "الرصيد غير كافٍ لتنفيذ التحويل"

                        else ->
                            "فشل تنفيذ التحويل: ${
                                e.message ?: "خطأ غير معروف"
                            }"
                    }

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
                strokeWidth = 2.dp
            )
        } else {
            Text("تنفيذ التحويل")
        }
    }

    if (message.isNotEmpty()) {
        Text(
            text = message,
            color = if (message.startsWith("تم"))
                MaterialTheme.colorScheme.primary
            else
                MaterialTheme.colorScheme.error
        )
    }
}

}
